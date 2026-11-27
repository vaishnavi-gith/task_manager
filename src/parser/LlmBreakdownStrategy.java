package parser;
import java.util.*;
import exception.*;
public class LlmBreakdownStrategy implements BreakdownStrategy {
    private final LlmClient client; private final boolean requireApiKey;
    public LlmBreakdownStrategy(LlmClient c){this(c,true);}
    public LlmBreakdownStrategy(LlmClient c,boolean requireApiKey){client=c;this.requireApiKey=requireApiKey;}
    public List<String[]> breakDown(String title,String description){
        if(client==null)throw new ServiceUnavailableException("LLM client is not configured");
        if(requireApiKey&&(System.getenv("LLM_API_KEY")==null||System.getenv("LLM_API_KEY").isBlank()))throw new ServiceUnavailableException("LLM_API_KEY is not configured");
        try{
            String prompt="Break the task into 2-6 small, actionable subtasks. Return ONLY one subtask per line as title|description. No numbering, markdown, or extra text. Task: "+title+". Description: "+description;
            String reply=client.complete(prompt); List<String[]> out=new ArrayList<>();
            if(reply==null||reply.isBlank())throw new BadGatewayException("LLM returned no subtasks");
            for(String line:reply.split("\\R")){if(line.isBlank())continue;String[] p=line.split("\\|",2);if(p.length!=2||p[0].isBlank()||p[1].isBlank())throw new BadGatewayException("LLM returned malformed subtask data");out.add(new String[]{p[0].trim(),p[1].trim()});}
            if(out.isEmpty())throw new BadGatewayException("LLM returned no subtasks");return out;
        }catch(ApiException e){throw e;}catch(Exception e){throw new BadGatewayException("LLM breakdown failed");}
    }
}
