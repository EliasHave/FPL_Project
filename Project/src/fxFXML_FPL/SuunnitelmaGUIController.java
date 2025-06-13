package fxFXML_FPL;

import FPL_Code.FlightPlanner;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import com.google.gson.*;
import okhttp3.*;

import java.io.IOException;

public class SuunnitelmaGUIController {

    FlightPlanner planner = FXML_FPLMain.getFlightPlanner();

    @FXML
    private Button Button_test;


    @FXML
    private TextArea TA_AirCraft;

    @FXML
    private TextArea TA_Notam;

    @FXML
    private TextArea TA_Pilot;

    @FXML
    private TextArea TA_Saa;


    @FXML
    void testaa(ActionEvent event) {
        kirjaaKone();
        kirjaaNotam();
        kirjaaPilot();
        kirjaaSaa();
        testaaAI();
    }


    // ==============================================================================================================================================

    public void kirjaaKone() {
        TA_AirCraft.setText(planner.getKone().toString());
    }

    public void kirjaaNotam() {
        TA_Notam.setText(planner.getNotam());
    }

    public void kirjaaPilot() {
        TA_Pilot.setText(planner.getPilot().toString());
    }

    public void kirjaaSaa() {
        StringBuilder saa = new StringBuilder();
        saa.append("Sää Lähtökohteessa:  \n" + planner.getSaaLahto().toString());
        saa.append("\n");
        saa.append("Sää määränpäässä: \n" + planner.getSaaMaapanpaa().toString());
        TA_Saa.setText(saa.toString());
    }

    public void testaaAI() {
        String apiKeyAI = System.getenv("OPENAI_API_KEY");
        if (apiKeyAI == null) {
            System.out.println("API-avain puuttuu. Varmista että ympäristömuuttuja OPENAI_API_KEY on asetettu.");
        }
        else {
            System.out.println("API-avain löytyi: " + apiKeyAI);
            OkHttpClient client = new OkHttpClient();

            String json = """
        {
          "model": "gpt-3.5-turbo",
          "messages": [
            {"role": "user", "content": "Kerro minulle lyhyt iltasatu"}
          ]
        }
        """;

            Request request = new Request.Builder()
                    .url("https://api.openai.com/v1/chat/completions")
                    .header("Authorization", "Bearer " + apiKeyAI)
                    .header("Content-Type", "application/json")
                    .post(RequestBody.create(json, MediaType.get("application/json")))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    JsonObject obj = JsonParser.parseString(response.body().string()).getAsJsonObject();
                    String output = obj.getAsJsonArray("choices")
                            .get(0).getAsJsonObject()
                            .getAsJsonObject("message")
                            .get("content").getAsString();

                    System.out.println("✳️ Vastaus tekoälyltä:\n" + output);
                } else {
                    System.err.println("❌ Virhe: " + response.code() + " " + response.body().string());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }


}