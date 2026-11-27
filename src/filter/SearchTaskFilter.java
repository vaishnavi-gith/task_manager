package filter;
import model.Task;
import model.TaskQuery;
import java.util.Locale;
public class SearchTaskFilter implements TaskFilter {
    public boolean matches(Task t, TaskQuery q) {
        if (q.search == null || q.search.isBlank()) return true;
        String needle=q.search.toLowerCase(Locale.ROOT);
        return (t.getTitle()+" "+t.getDescription()).toLowerCase(Locale.ROOT).contains(needle);
    }
}
