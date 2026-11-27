package sort;
import model.Task;
import java.util.Comparator;
public class IdSortStrategy implements SortStrategy { public Comparator<Task> comparator(){return Comparator.comparingInt(Task::getId);} }
