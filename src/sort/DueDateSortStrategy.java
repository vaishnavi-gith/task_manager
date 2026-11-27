package sort;
import model.Task;
import java.util.Comparator;
public class DueDateSortStrategy implements SortStrategy { public Comparator<Task> comparator(){return Comparator.comparing(t->t.getDueDate()==null?"9999-99-99":t.getDueDate());} }
