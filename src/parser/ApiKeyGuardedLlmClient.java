package parser;
import exception.ServiceUnavailableException;
/** Wraps the instructor/provider LlmClient and prevents calls without LLM_API_KEY. */
public class ApiKeyGuardedLlmClient implements LlmClient {
    private final LlmClient delegate;
    public ApiKeyGuardedLlmClient(LlmClient delegate){this.delegate=delegate;}
    public String complete(String prompt){
        String key=System.getenv("LLM_API_KEY");
        if(key==null||key.isBlank())throw new ServiceUnavailableException("LLM_API_KEY is not configured");
        if(delegate==null)throw new ServiceUnavailableException("LLM client is not configured");
        return delegate.complete(prompt);
    }
}
