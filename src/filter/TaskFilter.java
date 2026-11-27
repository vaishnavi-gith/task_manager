package filter;
import model.Task;
import model.TaskQuery;
public interface TaskFilter { boolean matches(Task task, TaskQuery query); }
