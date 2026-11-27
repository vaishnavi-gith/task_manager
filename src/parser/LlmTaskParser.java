package parser;
import exception.*;
import java.util.regex.*;

public class LlmTaskParser implements TaskParser {
    private final LlmClient client;
    private final boolean requireApiKey;
    public LlmTaskParser(LlmClient client){this(client,true);}
    // requireApiKey=false is intended for deterministic fake-client tests only.
    public LlmTaskParser(LlmClient client, boolean requireApiKey){this.client=client;this.requireApiKey=requireApiKey;}
    public ParsedTask parse(String text,String today){
        if(text==null||text.isBlank()) throw new BadGatewayException("Task text is empty");
        if(client==null) throw new ServiceUnavailableException("LLM client is not configured");
        if(requireApiKey&&(System.getenv("LLM_API_KEY")==null||System.getenv("LLM_API_KEY").isBlank()))
            throw new ServiceUnavailableException("LLM_API_KEY is not configured");
        String prompt="You convert natural language into a task. Today's date is "+today+". " +
            "Return ONLY a valid JSON object with string fields title, description, dueDate, recurrence. " +
            "Use an empty string for unknown dueDate or recurrence. Resolve relative dates using today's date. " +
            "Do not include markdown fences or extra text. Input: "+text;
        try {
            String reply=client.complete(prompt);
            String title=field(reply,"title"), description=field(reply,"description");
            String due=field(reply,"dueDate"), recurrence=field(reply,"recurrence");
            if(title==null||title.isBlank()||description==null||description.isBlank())
                throw new BadGatewayException("LLM returned missing or empty title/description");
            return new ParsedTask(title.trim(),description.trim(),emptyToNull(due),emptyToNull(recurrence));
        } catch(ApiException e){throw e;} catch(Exception e){throw new BadGatewayException("LLM request failed or returned malformed JSON");}
    }
    private String field(String json,String name){
        if(json==null)return null;
        Matcher m=Pattern.compile("\\\""+Pattern.quote(name)+"\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"",Pattern.DOTALL).matcher(json);
        if(!m.find())return null;
        return m.group(1).replace("\\\\\"","\"").replace("\\\\n","\\n").replace("\\\\r","\\r").replace("\\\\t","\\t").replace("\\\\\\\\","\\\\");
    }
    private String emptyToNull(String s){return s==null||s.isBlank()?null:s.trim();}
}
