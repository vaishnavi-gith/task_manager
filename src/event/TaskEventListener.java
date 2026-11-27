package event;
import model.Task;
public interface TaskEventListener {void onEvent(String eventType,Task task);}
