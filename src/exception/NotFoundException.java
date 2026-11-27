package exception;
public class NotFoundException extends ApiException {public NotFoundException(String m){super(404,"NOT_FOUND",m);}}
