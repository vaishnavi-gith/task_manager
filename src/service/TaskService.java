package service;
import model.*;
import repository.*;
import exception.*;
import event.*;
import parser.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import filter.*;
import sort.*;

public class TaskService {
    private final TaskRepository repo;
    private final InMemoryTaskRepository memory;
    private final TaskParser parser;
    private final BreakdownStrategy breakdown;
    private final List<TaskEventListener> listeners=new ArrayList<>();
    private int nextId=1;
    public TaskService(TaskRepository repo){
        this(repo,
             ParserFactory.create(new OpenAiResponsesClient()),
             BreakdownFactory.create(new OpenAiResponsesClient()));
    }
    public TaskService(TaskRepository repo,TaskParser parser,BreakdownStrategy breakdown){
        this.repo=repo;this.memory=repo instanceof InMemoryTaskRepository?(InMemoryTaskRepository)repo:null;
        this.parser=parser;this.breakdown=breakdown;
        if(memory!=null)nextId=memory.nextId();
    }
    public void addListener(TaskEventListener l){listeners.add(l);}
    public void removeListener(TaskEventListener l){listeners.remove(l);}
    private void notify(String type,Task t){for(TaskEventListener l:new ArrayList<>(listeners))l.onEvent(type,t);}
    private List<String[]> validate(TaskRequest r){
        List<String[]> e=new ArrayList<>();
        if(r.getTitle()==null||r.getTitle().isBlank())e.add(new String[]{"title","must not be empty"});
        if(r.getDescription()==null||r.getDescription().isBlank())e.add(new String[]{"description","must not be empty"});
        if(!"true".equals(r.getCompleted())&&!"false".equals(r.getCompleted()))e.add(new String[]{"completed","must be exactly true or false"});
        if(r.getDueDate()!=null&&!r.getDueDate().isBlank()){
            try{if(!r.getDueDate().matches("\\d{4}-\\d{2}-\\d{2}"))throw new Exception();LocalDate.parse(r.getDueDate());}
            catch(Exception ex){e.add(new String[]{"dueDate","must be a real date in YYYY-MM-DD format"});}
        }
        if(r.getPriority()==null||!Arrays.asList("LOW","MEDIUM","HIGH").contains(r.getPriority().toUpperCase(Locale.ROOT)))e.add(new String[]{"priority","must be LOW, MEDIUM, or HIGH"});
        return e;
    }
    private Task make(TaskRequest r,Integer parent){
        List<String[]> errors=validate(r);if(!errors.isEmpty())throw new ValidationException(errors);
        return new Task.Builder(r.getTitle().trim(),r.getDescription().trim(),Boolean.parseBoolean(r.getCompleted()))
            .id(nextId++).dueDate(r.getDueDate()==null||r.getDueDate().isBlank()?null:r.getDueDate())
            .priority(Priority.valueOf(r.getPriority().toUpperCase(Locale.ROOT))).parentId(parent).build();
    }
    public Task createTask(TaskRequest r){Task t=make(r,null);repo.save(t);notify("CREATED",t);return t;}
    public Task getTask(int id){Task t=repo.findById(id);if(t==null)throw new NotFoundException("Task not found");return t;}
    public List<Task> getAllTasks(){return repo.findAll();}
    public Task updateTask(int id,TaskRequest r){
        Task old=getTask(id);Task n=makeWithId(r,old);repo.save(n);
        notify(!old.isCompleted()&&n.isCompleted()?"COMPLETED":"UPDATED",n);
        return n;
    }
    private Task makeWithId(TaskRequest r,Task old){
        List<String[]> errors=validate(r);if(!errors.isEmpty())throw new ValidationException(errors);
        return new Task.Builder(r.getTitle().trim(),r.getDescription().trim(),Boolean.parseBoolean(r.getCompleted())).id(idOf(old))
            .dueDate(r.getDueDate()).priority(Priority.valueOf(r.getPriority().toUpperCase(Locale.ROOT))).parentId(old.getParentId()).build();
    }
    private int idOf(Task t){return t.getId();}
    public void deleteTask(int id){
        Task t=getTask(id);
        for(Task child:repo.findAll()) if(child.getParentId()!=null&&child.getParentId()==id){repo.delete(child.getId());notify("DELETED",child);}
        repo.delete(id);notify("DELETED",t);
    }
    public Task createSubtask(int parentId,TaskRequest r){Task p=getTask(parentId);if(p.getParentId()!=null)throw new ValidationException(java.util.Collections.singletonList(new String[]{"parentId","a subtask cannot have subtasks"}));Task t=make(r,parentId);repo.save(t);notify("CREATED",t);return t;}
    public List<Task> listSubtasks(int parentId){getTask(parentId);return repo.findAll().stream().filter(t->Objects.equals(t.getParentId(),parentId)).collect(Collectors.toList());}
    public List<Task> breakDownTask(int id){Task p=getTask(id);if(p.getParentId()!=null)throw new ValidationException(java.util.Collections.singletonList(new String[]{"id","a subtask cannot be broken down"}));
        List<String[]> parts=breakdown.breakDown(p.getTitle(),p.getDescription());List<Task> pending=new ArrayList<>();
        for(String[] x:parts){TaskRequest r=new TaskRequest(x[0],x[1],"false",null,"MEDIUM");List<String[]> e=validate(r);if(!e.isEmpty())throw new BadGatewayException("LLM or breakdown strategy returned invalid subtask data");}
        for(String[] x:parts)pending.add(make(new TaskRequest(x[0],x[1],"false",null,"MEDIUM"),id));
        for(Task t:pending){repo.save(t);notify("CREATED",t);}return pending;
    }
    public List<Task> query(TaskQuery q){
        if(q.page<1||q.pageSize<1)throw new ValidationException(java.util.Collections.singletonList(new String[]{"page","page and pageSize must be positive"}));
        List<TaskFilter> filters=Arrays.asList(new CompletedTaskFilter(),new PriorityTaskFilter(),new SearchTaskFilter());
        List<Task> list=repo.findAll().stream().filter(t->{for(TaskFilter f:filters)if(!f.matches(t,q))return false;return true;}).collect(Collectors.toList());
        java.util.Comparator<Task> c=SortStrategyFactory.create(q.sortBy).comparator();
        if(q.descending)c=c.reversed();list.sort(c);
        int from=(q.page-1)*q.pageSize;if(from>=list.size())return Collections.emptyList();
        return new ArrayList<>(list.subList(from,Math.min(from+q.pageSize,list.size())));
    }
    public Task createTaskFromText(String text){
        ParsedTask p=parser.parse(text,LocalDate.now().toString());
        try {
            return createTask(new TaskRequest(p.title,p.description,"false",p.dueDate,"MEDIUM"));
        } catch (ValidationException e) {
            // Invalid model output is an upstream failure, not a user-input validation error.
            throw new BadGatewayException("LLM returned invalid task fields");
        }
    }
    public int count(){return repo.findAll().size();}
}
