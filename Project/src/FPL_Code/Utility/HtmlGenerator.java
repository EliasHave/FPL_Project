package FPL_Code.Utility;

import FPL_Code.Model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Puhdas utility-luokka valmiin HTML-suunnitelmasivun luontiin.
 * Ei sisällä liiketoimintalogiikkaa, vain datan muotoilua.
 */
public final class HtmlGenerator {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final DateTimeFormatter AIKA_FORMAATTI = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private HtmlGenerator() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Ottaa sisään kaiken käsitellyn datan ja palauttaa valmiin HTML-merkkijonon selaimelle.
     */
    public static String generoiTulossivu(FlightRequest pyynto, LentosuunnitelmaTulos aiTulos, DTO_Boss paketti) {
        try {
            // 1. Lue template tiedostosta (resources/FPL_Template2.html)
            java.nio.file.Path templatePath = java.nio.file.Paths.get(System.getProperty("user.dir"), "Project", "resources", "FPL_Template2.html");
            String html = java.nio.file.Files.readString(templatePath, java.nio.charset.StandardCharsets.UTF_8);

            // 2. Ylätunnisteen tiedot
            html = html.replace("{{DEPARTURE}}", paketti.getLahto().getPointName());
            html = html.replace("{{DESTINATION}}", paketti.getKohde().getPointName());
            html = html.replace("{{ETD}}", pyynto.getLahtoAika()); // Oletetaan että requestissa on "12:00Z"
            html = html.replace("{{ETA}}", "Arvioitu saapuminen"); // TODO: Laske ETA oikein
            html = html.replace("{{LUOMIS_AIKA}}", LocalDateTime.now().format(AIKA_FORMAATTI));

            // 3. Tekoälyn ja sään tekstit
            html = html.replace("{{WX_LAHTO}}", muotoileSaa(paketti.getReitinSaa(), WeatherSamplePoint.PointType.DEPARTURE));
            html = html.replace("{{WX_REITTI}}", aiTulos.getSafetyConcerns()); // AI analyysi reittisään/riskien tilalle MVP-vaiheessa
            html = html.replace("{{WX_PAATE}}", muotoileSaa(paketti.getReitinSaa(), WeatherSamplePoint.PointType.DESTINATION));

            // 4. NOTAM-tekstit
            html = html.replace("{{NTM_LAHTO}}", muotoileNotamit(paketti.getNotamit(), Notam.NotamType.DEPARTURE));
            html = html.replace("{{NTM_REITTI}}", muotoileNotamit(paketti.getNotamit(), Notam.NotamType.EN_ROUTE));
            html = html.replace("{{NTM_PAATE}}", muotoileNotamit(paketti.getNotamit(), Notam.NotamType.DESTINATION));

            // 5. Lentokone ja Pilotti
            html = html.replace("{{AIRCRAFT}}", muotoileKone(paketti.getKone()));
            html = html.replace("{{PILOT}}", muotoilePilotti(paketti.getPilot()));

            // 6. JSON Injektiot karttaa varten (Raskaat datat)
            html = html.replace("{{RELEVANT_AIRSPACES}}", listToGeoJson(paketti.getIlmatilat()));
            html = html.replace("{{RELEVANT_AIRPORTS}}", listToGeoJson(paketti.getLentokentat()));
            html = html.replace("{{RELEVANT_NAVAIDS}}", listToGeoJson(paketti.getNavaidit()));

            // 7. Reittipisteiden piirto Javascriptille
            html = html.replace("{{JS_POINTS}}", muotoileJsPisteet(aiTulos.getReittiPisteet()));

            return html;

        } catch (Exception e) {
            e.printStackTrace();
            return "<html><body><h2>Kriittinen virhe HTML-generoinnissa</h2><pre>" + e.getMessage() + "</pre></body></html>";
        }
    }

    // ====================================================================================
    // PRIVATE APUMETODIT (Eristää purkulogiikan pois päämetodista)
    // ====================================================================================

    private static String muotoileSaa(List<WeatherSamplePoint> saat, WeatherSamplePoint.PointType tyyppi) {
        if (saat == null) return "Ei säätietoja.";

        return saat.stream()
                .filter(w -> w.getTyyppi() == tyyppi)
                .map(w -> String.format("Tuuli: %03d/%d KT\nNäkyvyys: %d m\nPilvet: %s",
                        w.getTuuliSuuntaDeg(), w.getTuuliNopeusKt(), w.getNakyvyysMetria(), w.getPilviMaara()))
                .collect(Collectors.joining("\n\n"));
    }

    private static String muotoileNotamit(List<Notam> notamit, Notam.NotamType tyyppi) {
        if (notamit == null) return "Ei NOTAMeja.";

        String tulos = notamit.stream()
                .filter(n -> n.getTyyppi() == tyyppi)
                .map(Notam::getRaakaTeksti)
                .collect(Collectors.joining("\n\n"));

        return tulos.isEmpty() ? "Ei merkittäviä NOTAMeja tälle alueelle." : tulos;
    }

    private static String muotoileKone(Aircraft kone) {
        if (kone == null) return "Ei konetietoja.";
        return String.format("Tunnus: %s\nTyyppi: %s\nMatkalentonopeus: %d KT",
                kone.getRekNro(), kone.getKoneTyyppi(), kone.getCruiseSpeed());
    }

    private static String muotoilePilotti(Pilot pilot) {
        if (pilot == null) return "Ei pilottitietoja.";
        return String.format("Nimi: %s\nLupakirjat: %s\nKokemus: %d h",
                pilot.getNimi(), pilot.getLupaKirjat(), pilot.getKokemus());
    }

    /**
     * Rakentaa JavaScript arrayn reittipisteistä HTML-kartalle.
     */
    private static String muotoileJsPisteet(List<Point> pisteet) {
        if (pisteet == null || pisteet.isEmpty()) return "const points = [];";

        StringBuilder js = new StringBuilder("const points = [\n");
        for (Point p : pisteet) {
            js.append(String.format("  {name: '%s', coords: [%.5f, %.5f]},\n",
                    p.getPointName(), p.getLat(), p.getLon()));
        }
        js.append("];\n");
        return js.toString();
    }

    /**
     * Muuttaa Javan List<Feature> -kokoelman aidoksi GeoJSON-merkkijonoksi Leaflet.js:ää varten[cite: 4].
     */
    private static String listToGeoJson(List<Feature> features) {
        if (features == null || features.isEmpty()) {
            return "{\"type\":\"FeatureCollection\",\"features\":[]}";
        }
        try {
            Map<String, Object> featureCollection = new HashMap<>();
            featureCollection.put("type", "FeatureCollection");
            featureCollection.put("features", features);
            return mapper.writeValueAsString(featureCollection);
        } catch (Exception e) {
            System.err.println("GeoJSON serialisointivirhe: " + e.getMessage());
            return "{\"type\":\"FeatureCollection\",\"features\":[]}";
        }
    }
}