package api;
import exception.*;
import java.util.*;
public final class ErrorResponseFactory {
    private ErrorResponseFactory(){}
    public static ApiResponse from(ApiException e){
        StringBuilder b=new StringBuilder("{\"error\":{\"code\":\"").append(esc(e.getErrorCode()))
            .append("\",\"message\":\"").append(esc(e.getMessage())).append("\"");
        if(e instanceof ValidationException){
            b.append(",\"details\":["); List<String[]> ds=((ValidationException)e).getDetails();
            for(int i=0;i<ds.size();i++){if(i>0)b.append(",");b.append("{\"field\":\"").append(esc(ds.get(i)[0])).append("\",\"issue\":\"").append(esc(ds.get(i)[1])).append("\"}");}
            b.append("]");
        }
        return new ApiResponse(e.getStatusCode(),b.append("}}").toString());
    }
    public static ApiResponse internal(){return from(new ApiException(500,"INTERNAL_ERROR","An unexpected error occurred"));}
    private static String esc(String s){return s==null?"":s.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n").replace("\r","\\r");}
}
