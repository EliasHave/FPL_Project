package fxFXML_FPL;

import FPL_Code.FlightPlanner;
import FPL_Code.Point;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
// import com.google.gson.*;
import javafx.scene.web.WebEngine;
import okhttp3.*;
import javafx.scene.web.WebView;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static FPL_Code.Point.etsiKoordinaatit;

public class SuunnitelmaGUIController {

    FlightPlanner planner = FXML_FPLMain.getFlightPlanner();

    private boolean mapInitialized = false;

    @FXML
    private Button Button_test;

    @FXML
    private WebView webView_Map;

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
        setWebView();
        teeHTML();
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
        saa.append("Sää määränpäässä: \n" + planner.getSaaMaaranpaa().toString());
        TA_Saa.setText(saa.toString());
    }


    public void setWebView() {
        if (mapInitialized) return;

        Platform.runLater(() -> {
            WebEngine webEngine = webView_Map.getEngine();
            URL url = getClass().getClassLoader().getResource("maptesti.html");
            webView_Map.setPrefSize(800, 600); // Tämä voi olla halutessasi isompi
            if (url != null) {
                webEngine.load(url.toExternalForm());
                mapInitialized = true;
            } else {
                System.err.println("Karttatiedostoa ei löytynyt!");
            }
        });
    }

    /**
     * Aliohjelma joka tekee nettisivun kyseisen lennon suunnitelmalle
     */
    public void teeHTML() {

        String pohja = lueTemplate("/FPL_Template2.html");

        String lahto = planner.getSaaLahto().getPaikka();
        String maaranpaa = planner.getSaaMaaranpaa().getPaikka();
        Point lahtoKoord = etsiKoordinaatit(lahto);
        Point maaranpaaKoord = etsiKoordinaatit(maaranpaa);
        System.out.println("lähtökentän tiedot: " + lahtoKoord);
        System.out.println("määränpääkentän tiedot: " + maaranpaaKoord);

        /**
         * Tähän pitäisi saada toteutettua joku tekoäly integraatio
         * Ideana se että nyt kun tiedetään mistä halutaan mennä ja minne sekä tiedetään säät, notamit yms niin mentäisiin tekoälylle näiden tietojen sekä ilmatilatietojen kanssa
         * Tekoäly sitten katsoisi tiedot ja miettisi pari eri mahdollista reittiä.
         * Koska ilmatilasta ja sääilmiöistä sekä rangesta on vaikea joustaa niin ne tarkistettaisiin viimeisenä että varmasti on tullut huomioitua kaikki
         * Sitten tekoäly mietittyään sylkäiseen koordinaattipisteitä ulos jotka laitetaan taulukkoon oikeaan järjestykseen jotta niistä voidaan tehdä reitti kartalle
         **/
        List<Point> reittiPisteet = planner.teeReitti(lahtoKoord, maaranpaaKoord);

        // 3. Muodosta JavaScriptin `points` array:
        String jsPoints = """
        const points = [
            { name: "Lähtö: %s", coords: [%s, %s] },
            { name: "Määränpää: %s", coords: [%s, %s] }
        ];
        """.formatted(
                lahtoKoord.getPointName(), lahtoKoord.getLat(), lahtoKoord.getLon(),
                maaranpaaKoord.getPointName(), maaranpaaKoord.getLat(), maaranpaaKoord.getLon()
        );

        // 4. Muodostaa sisältötekstit
        pohja = pohja.replace("{{JS_POINTS}}", jsPoints);
        pohja = pohja.replace("{{DEPARTURE}}", lahto);
        pohja = pohja.replace("{{DESTINATION}}", maaranpaa);
        pohja = pohja.replace("{{WX_LAHTO}}", planner.getSaaLahto().toString());
        pohja = pohja.replace("{{WX_PAATE}}", planner.getSaaMaaranpaa().toString());
        pohja = pohja.replace("{{NTM_LAHTO}}", planner.getNotam());
        pohja = pohja.replace("{{NTM_PAATE}}", planner.getNotam());
        pohja = pohja.replace("{{AIRCRAFT}}", planner.getKone().toString());
        pohja = pohja.replace("{{PILOT}}", planner.getPilot().toString());

        // 5. Korvaa points[] JavaScriptin sisällä (hakusana on vaikka "const points = [...")
        // pohja = pohja.replaceAll("const points = \\[[^\\]]*\\];", jsPoints);

        // 6. Tallenna tiedostoksi
        try (PrintWriter writer = new PrintWriter("lentosuunnitelma_testi.html", StandardCharsets.UTF_8)) {
            writer.write(pohja);
            System.out.println("✅ HTML-tiedosto tallennettu!");
        } catch (IOException e) {
            System.err.println("❌ Tallennus epäonnistui: " + e.getMessage());
        }
    }



    public String lueTemplate(String polku) {
        try (InputStream is = getClass().getResourceAsStream(polku)) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("❌ Templatea ei löytynyt: " + e.getMessage());
            return "";
        }
    }


    public void testaaAI() {
        String apiKeyAI = System.getenv("OPENAI_API_KEY");
        if (apiKeyAI == null) {
            System.out.println("API-avain puuttuu. Varmista että ympäristömuuttuja OPENAI_API_KEY on asetettu.");
        }
        else {
            System.out.println("API-avain löytyi: " + apiKeyAI);
            /**
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
             **/

        }
    }


}