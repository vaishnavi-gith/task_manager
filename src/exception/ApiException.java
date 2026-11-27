package exception;
public class ApiException extends RuntimeException {
    private final int statusCode; private final String errorCode;
    public ApiException(int status,String code,String message){super(message);statusCode=status;errorCode=code;}
    public int getStatusCode(){return statusCode;} public String getErrorCode(){return errorCode;}
}
