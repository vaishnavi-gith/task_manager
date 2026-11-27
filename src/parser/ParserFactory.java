package parser;
public final class ParserFactory {
    private ParserFactory(){}
    public static TaskParser create(LlmClient client){
        return System.getenv("LLM_API_KEY")==null||System.getenv("LLM_API_KEY").isBlank()
            ? new RuleBasedTaskParser() : new LlmTaskParser(client);
    }
}
