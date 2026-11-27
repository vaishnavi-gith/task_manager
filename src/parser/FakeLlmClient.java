package parser;
public class FakeLlmClient implements LlmClient {
    private final String reply; private int calls;
    public FakeLlmClient(String reply){this.reply=reply;}
    public String complete(String prompt){calls++;return reply;}
    public int getCallCount(){return calls;}
}
