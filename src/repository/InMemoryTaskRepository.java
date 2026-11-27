package repository;
import model.Task;
import java.util.*;
public final class InMemoryTaskRepository implements TaskRepository {
    private static final InMemoryTaskRepository INSTANCE=new InMemoryTaskRepository();
    private final Map<Integer,Task> tasks=new LinkedHashMap<>();
    private InMemoryTaskRepository(){}
    public static InMemoryTaskRepository getInstance(){return INSTANCE;}
    public synchronized void save(Task t){tasks.put(t.getId(),t);}
    public synchronized Task findById(int id){return tasks.get(id);}
    public synchronized List<Task> findAll(){return new ArrayList<>(tasks.values());}
    public synchronized boolean delete(int id){return tasks.remove(id)!=null;}
    public synchronized int nextId(){return tasks.keySet().stream().mapToInt(Integer::intValue).max().orElse(0)+1;}
    public synchronized void clear(){tasks.clear();}
}
