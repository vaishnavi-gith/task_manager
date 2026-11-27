package event;
import model.Task;
import java.util.*;
public class AuditTrailListener implements TaskEventListener {
    private final Map<String,Integer> counts=new LinkedHashMap<>();
    public void onEvent(String type,Task task){counts.put(type,counts.getOrDefault(type,0)+1);}
    public Map<String,Integer> getCounts(){return Collections.unmodifiableMap(counts);}
}
