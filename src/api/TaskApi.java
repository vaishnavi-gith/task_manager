package api;
import model.*;
import service.TaskService;
import repository.InMemoryTaskRepository;
import repository.IdempotencyStore;
import exception.*;
import java.util.*;
public class TaskApi {
    private final TaskService service;
    public TaskApi(){this(new TaskService(InMemoryTaskRepository.getInstance()));}
    public TaskApi(TaskService service){this.service=service;}
    private ApiResponse run(int success, java.util.function.Supplier<String> action){
        try{return new ApiResponse(success,action.get());}
        catch(ApiException e){return ErrorResponseFactory.from(e);}
        catch(Exception e){return ErrorResponseFactory.internal();}
    }
    private String taskJson(Task t){return "{\"id\":"+t.getId()+",\"title\":\""+esc(t.getTitle())+"\",\"description\":\""+esc(t.getDescription())+"\",\"completed\":"+t.isCompleted()+",\"dueDate\":"+(t.getDueDate()==null?"null":"\""+esc(t.getDueDate())+"\"")+",\"priority\":\""+t.getPriority()+"\",\"parentId\":"+(t.getParentId()==null?"null":t.getParentId())+"}";}
    private String esc(String s){return s==null?"":s.replace("\\\\","\\\\\\\\").replace("\"","\\\\\"").replace("\n","\\\\n");}
    private String listJson(List<Task> tasks){StringJoiner j=new StringJoiner(",","[","]");for(Task t:tasks)j.add(taskJson(t));return j.toString();}
    private ApiResponse idempotent(String key,String fingerprint,java.util.function.Supplier<ApiResponse> action){
        if(key==null||key.isBlank()) return action.get();
        IdempotencyStore store=IdempotencyStore.getInstance();
        IdempotencyStore.Record old=store.get(key);
        if(old!=null){
            if(!old.fingerprint.equals(fingerprint))
                return ErrorResponseFactory.from(new ConflictException("Idempotency key was already used with a different request"));
            return (ApiResponse)old.response;
        }
        ApiResponse response=action.get();
        if(response.getStatusCode()>=200&&response.getStatusCode()<300) store.put(key,fingerprint,response);
        return response;
    }
    public ApiResponse createTask(TaskRequest r){return createTask(r,null);}
    public ApiResponse createTask(TaskRequest r,String key){
        String fp=String.valueOf(r.getTitle())+"|"+r.getDescription()+"|"+r.getCompleted()+"|"+r.getDueDate()+"|"+r.getPriority();
        return idempotent(key,fp,()->run(201,()->taskJson(service.createTask(r))));
    }
    public ApiResponse getTask(int id){return run(200,()->taskJson(service.getTask(id)));}
    public ApiResponse getAllTasks(){return run(200,()->listJson(service.getAllTasks()));}
    public ApiResponse updateTask(int id,TaskRequest r){return run(200,()->taskJson(service.updateTask(id,r)));}
    public ApiResponse deleteTask(int id){return run(200,()->{service.deleteTask(id);return "{\"message\":\"Task deleted successfully\"}";});}
    public ApiResponse createSubtask(int parentId,TaskRequest r){return run(201,()->taskJson(service.createSubtask(parentId,r)));}
    public ApiResponse listSubtasks(int parentId){return run(200,()->listJson(service.listSubtasks(parentId)));}
    public ApiResponse breakDownTask(int id){return breakDownTask(id,null);}
    public ApiResponse breakDownTask(int id,String key){return idempotent(key,"breakdown|"+id,()->run(201,()->listJson(service.breakDownTask(id))));}
    public ApiResponse createTaskFromText(String text){return createTaskFromText(text,null);}
    public ApiResponse createTaskFromText(String text,String key){return idempotent(key,"text|"+text,()->run(201,()->taskJson(service.createTaskFromText(text))));}
    public ApiResponse query(TaskQuery q){return run(200,()->listJson(service.query(q)));}
    public TaskService getService(){return service;}
}
