package filter;
import model.Task;
import model.TaskQuery;
public class CompletedTaskFilter implements TaskFilter {
    public boolean matches(Task t, TaskQuery q) { return q.completed == null || t.isCompleted() == q.completed; }
}
