package sort;
import model.Task;
import java.util.Comparator;
public interface SortStrategy { Comparator<Task> comparator(); }
