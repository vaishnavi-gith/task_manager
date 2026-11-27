package parser;
public class FaultyLlmClient implements LlmClient {
    private final String reply; private final boolean throwException;
    public FaultyLlmClient(String reply){this(reply,false);}
    public FaultyLlmClient(String reply,boolean throwException){this.reply=reply;this.throwException=throwException;}
    public String complete(String prompt){if(throwException)throw new RuntimeException("Simulated LLM outage");return reply;}
}
