package exception;
import java.util.*;
public class ValidationException extends ApiException {
    private final List<String[]> details;
    public ValidationException(List<String[]> d){super(400,"VALIDATION_ERROR","Validation failed");details=new ArrayList<>(d);}
    public List<String[]> getDetails(){return Collections.unmodifiableList(details);}
}
