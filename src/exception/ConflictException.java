package exception;
public class ConflictException extends ApiException {public ConflictException(String m){super(422,"CONFLICT",m);}}
