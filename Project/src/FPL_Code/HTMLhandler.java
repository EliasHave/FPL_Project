package FPL_Code;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static FPL_Code.Point.etsiKoordinaatit;

public class HTMLhandler {


    /**
     * Aliohjelma joka tekee nettisivun kyseisen lennon suunnitelmalle
     */
    public static String teeHTML(FlightPlanner planner) {

        String pohja = lueTemplate("/FPL_Template2.html");

        String lahto = planner.getSaaLahto().getPaikka();
        // String maaranpaa = planner.getSaaMaaranpaa().getPaikka();  // tämä pitää ehkä korvata planner.getMaaranpaaKentta(); metodilla, os otetaan controllerista se sään luominen pois
        String maaranpaa = planner.getMaaranpaaKentta();
        Point lahtoKoord = etsiKoordinaatit(lahto);
        Point maaranpaaKoord = etsiKoordinaatit(maaranpaa);

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

        return pohja;

    }



    public static String lueTemplate(String polku) {
        try (InputStream is = HTMLhandler.class.getResourceAsStream(polku)) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("❌ Templatea ei löytynyt: " + e.getMessage());
            return "";
        }
    }

}
