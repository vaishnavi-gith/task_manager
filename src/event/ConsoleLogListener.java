package event;
import model.Task;
public class ConsoleLogListener implements TaskEventListener {
    public void onEvent(String type,Task task){System.out.println("[EVENT] "+type+" taskId="+task.getId()+" title="+task.getTitle());}
}
