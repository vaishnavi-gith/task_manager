package sort;
import model.Task;
import java.util.Comparator;
public class PrioritySortStrategy implements SortStrategy { public Comparator<Task> comparator(){return Comparator.comparingInt(t->t.getPriority().ordinal());} }
