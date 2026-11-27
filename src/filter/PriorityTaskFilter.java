package filter;
import model.Task;
import model.TaskQuery;
public class PriorityTaskFilter implements TaskFilter {
    public boolean matches(Task t, TaskQuery q) { return q.priority == null || t.getPriority() == q.priority; }
}
