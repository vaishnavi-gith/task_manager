package parser;

/** Selects the LLM-backed or offline breakdown strategy from configuration. */
public final class BreakdownFactory {
    private BreakdownFactory() { }

    public static BreakdownStrategy create(LlmClient client) {
        String key = System.getenv("LLM_API_KEY");
        return key == null || key.isBlank()
                ? new RuleBasedBreakdownStrategy()
                : new LlmBreakdownStrategy(client);
    }
}
