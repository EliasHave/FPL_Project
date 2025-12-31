package FPL_Code;

import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class OpenAIClient implements AIClient{

    private final String apiKey;
    private final String model;

    public OpenAIClient(String apiKey, String model) {
        this.apiKey = apiKey;
        this.model = model;
    }

    /**
     * Lähettää parametrina tulevan promptin tekoälylle ja palauttaa vastauksen sellaisenaan
     * @param prompt prompti joka lähetetään tekoälylle
     * @return palauttaa tekoälyn vastauksen
     */
    @Override
    public String sendPrompt(String prompt) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            String jsonRequest = """
            {
              "model": "%s",
              "messages": [
                { "role": "user", "content": %s }
              ],
              "temperature": 0.4
            }
            """.formatted(model, JSONObject.quote(prompt));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            return response.body();

        } catch (Exception e) {
            throw new AIClientException("OpenAI request failed", e);
        }
    }

}
