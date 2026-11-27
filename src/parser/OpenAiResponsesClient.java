package parser;

import exception.BadGatewayException;
import exception.ServiceUnavailableException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal OpenAI Responses API adapter using only the Java 17 standard library.
 * Reads credentials from LLM_API_KEY and the optional model from OPENAI_MODEL.
 */
public class OpenAiResponsesClient implements LlmClient {
    private static final String ENDPOINT = "https://api.openai.com/v1/responses";
    private final HttpClient http;
    private final String apiKey;
    private final String model;

    public OpenAiResponsesClient() {
        this(System.getenv("LLM_API_KEY"),
             valueOrDefault(System.getenv("OPENAI_MODEL"), "gpt-6-luna"),
             HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build());
    }

    // Public constructor allows deterministic local testing with a mock HTTP server if desired.
    public OpenAiResponsesClient(String apiKey, String model, HttpClient http) {
        this.apiKey = apiKey;
        this.model = model == null || model.isBlank() ? "gpt-6-luna" : model;
        this.http = http;
    }

    @Override
    public String complete(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ServiceUnavailableException("LLM_API_KEY is not configured");
        }
        if (prompt == null || prompt.isBlank()) {
            throw new BadGatewayException("LLM prompt is empty");
        }

        String body = "{\"model\":\"" + jsonEscape(model) +
                "\",\"store\":false,\"input\":\"" + jsonEscape(prompt) + "\"}";

        HttpRequest request = HttpRequest.newBuilder(URI.create(ENDPOINT))
                .timeout(Duration.ofSeconds(45))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BadGatewayException("OpenAI API request failed with HTTP " + response.statusCode());
            }
            String text = extractOutputText(response.body());
            if (text == null || text.isBlank()) {
                throw new BadGatewayException("OpenAI API returned no output text");
            }
            return text.trim();
        } catch (BadGatewayException | ServiceUnavailableException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BadGatewayException("OpenAI API request was interrupted");
        } catch (IOException | RuntimeException e) {
            throw new BadGatewayException("Could not connect to the OpenAI API");
        }
    }

    private static String extractOutputText(String json) {
        if (json == null) return null;
        // Responses API output items contain content entries with type=output_text and a text field.
        Pattern pattern = Pattern.compile("\\\"type\\\"\\s*:\\s*\\\"output_text\\\"[\\s\\S]*?\\\"text\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"");
        Matcher matcher = pattern.matcher(json);
        if (!matcher.find()) return null;
        return jsonUnescape(matcher.group(1));
    }

    private static String jsonEscape(String value) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"': out.append("\\\""); break;
                case '\\': out.append("\\\\"); break;
                case '\b': out.append("\\b"); break;
                case '\f': out.append("\\f"); break;
                case '\n': out.append("\\n"); break;
                case '\r': out.append("\\r"); break;
                case '\t': out.append("\\t"); break;
                default:
                    if (c < 0x20) out.append(String.format("\\u%04x", (int) c));
                    else out.append(c);
            }
        }
        return out.toString();
    }

    private static String jsonUnescape(String value) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c != '\\' || i + 1 >= value.length()) {
                out.append(c);
                continue;
            }
            char escaped = value.charAt(++i);
            switch (escaped) {
                case '"': out.append('"'); break;
                case '\\': out.append('\\'); break;
                case '/': out.append('/'); break;
                case 'b': out.append('\b'); break;
                case 'f': out.append('\f'); break;
                case 'n': out.append('\n'); break;
                case 'r': out.append('\r'); break;
                case 't': out.append('\t'); break;
                case 'u':
                    if (i + 4 < value.length()) {
                        try {
                            out.append((char) Integer.parseInt(value.substring(i + 1, i + 5), 16));
                            i += 4;
                        } catch (NumberFormatException ex) {
                            throw new BadGatewayException("OpenAI API returned malformed JSON text");
                        }
                    } else {
                        throw new BadGatewayException("OpenAI API returned malformed JSON text");
                    }
                    break;
                default: out.append(escaped);
            }
        }
        return out.toString();
    }

    private static String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
