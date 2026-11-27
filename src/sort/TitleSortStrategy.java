package sort;
import model.Task;
import java.util.Comparator;
public class TitleSortStrategy implements SortStrategy { public Comparator<Task> comparator(){return Comparator.comparing(Task::getTitle,String.CASE_INSENSITIVE_ORDER);} }
