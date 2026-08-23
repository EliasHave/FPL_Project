package FPL_Code;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

@Service
public class AviationDataService {

    private static final String GCS_BASE = "https://storage.openaip.net/openaip-system-exports/";

    private static final List<String> MAAT = List.of(
            "fi", "se", "no", "ee", "lv", "lt", "de"
    );

    private String ilmatilatGeoJson;
    private String lentokentatGeoJson;
    private String navaiditGeoJson;


     //TÄHÄN FEATURE OLIOT LISTANA KOSKA TÄMÄ ON SINGLETON JOTA KAIKKI VOIVAT KÄYTTÄÄ NIIN YKSITTÄISET PYYNNÖT EIVÄT TEE OMIA KOPIOITA SAMASTA TIEDOSTA
     private List<Feature> ilmatilatFeatures;
     private List<Feature> lentokentatFeatures;
     private List<Feature> navaiditFeatures;

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @PostConstruct
    public void lataaData() {
        System.out.println("🛫 Ladataan ilmailutietoja GCS:stä...");

        ilmatilatGeoJson = yhdista("asp");
        lentokentatGeoJson = yhdista("apt");
        navaiditGeoJson = yhdista("nav");

        //parsitaan Feature (olio) muotoon
        ilmatilatFeatures = parseFeaturesFromGeoJson(ilmatilatGeoJson);
        lentokentatFeatures = parseFeaturesFromGeoJson(lentokentatGeoJson);
        navaiditFeatures = parseFeaturesFromGeoJson(navaiditGeoJson);


        System.out.println("✅ Ilmailutiedot ladattu");
    }

    /**
     * Lataa ja yhdistää GeoJSON-tiedot kaikista maista tietystä tyypistä (asp, apt, nav).
     * @param tyyppi Data tyyppi: "asp" (ilmatilat), "apt" (lentokentät) tai "nav" (navaidit)
     * @return
     */
    private String yhdista(String tyyppi) {
        List<Object> kaikki = new ArrayList<>();

        for (String maa : MAAT) {
            try {
                String url = GCS_BASE + maa + "_" + tyyppi + ".geojson";
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    var json = mapper.readTree(response.body());
                    var features = json.get("features");
                    if (features != null && features.isArray()) {
                        features.forEach(kaikki::add);
                        System.out.println("✅ " + maa + "_" + tyyppi + " ladattu (" + features.size() + " kohdetta)");
                    }
                } else {
                    System.out.println("⚠️ " + maa + "_" + tyyppi + " ei löydy");
                    System.out.println("response: " + response);
                }
            } catch (Exception e) {
                System.out.println("❌ Virhe ladattaessa " + maa + "_" + tyyppi + ": " + e.getMessage());
            }
        }

        try {
            var root = mapper.createObjectNode();
            root.put("type", "FeatureCollection");
            root.set("features", mapper.valueToTree(kaikki));
            return mapper.writeValueAsString(root);
        } catch (Exception e) {
            return "{\"type\":\"FeatureCollection\",\"features\":[]}";
        }
    }


    /**
     * Metodi joka ottaa geoJson muotoisen ilmailudatan ja tekee siitä Feature (olio) listan
     * @param geoJson Vastaanotettava ilmailudata geoJson muodossa (String tietotyyppi)
     * @return palauttaa Feature listan parametrina tulleesta datasta
     */
    private List<Feature> parseFeaturesFromGeoJson(String geoJson) {
        List<Feature> features = new ArrayList<>();
        try {
            JsonNode root = mapper.readTree(geoJson);
            JsonNode featureNodes = root.get("features");
            for (JsonNode node : featureNodes) {
                features.add(new Feature(node.get("geometry"), node.get("properties")));
            }
        } catch (Exception e) {
            System.err.println("❌ Parsinta epäonnistui: " + e.getMessage());
        }
        return features;
    }


    /**
     * Getteri joka palauttaa KAIKKI ladatut ilmatilat karsimattomana GeoJSON-muodossa (String). Tämä data on yhdistettynä kaikista maista, ja se on ladattu GCS:stä sovelluksen käynnistyessä.
     * @return String muodossa oleva GeoJSON, joka sisältää kaikki ladatut ilmatilat.
     */
    public String getIlmatilatGeoJson() { return ilmatilatGeoJson; }
    public String getLentokentatGeoJson() { return lentokentatGeoJson; }
    public String getNavaiditGeoJson() { return navaiditGeoJson; }

    public List<Feature> getIlmatilatFeatures() { return ilmatilatFeatures; }
    public List<Feature> getLentokentatFeatures() { return lentokentatFeatures; }
    public List<Feature> getNavaiditFeatures() { return navaiditFeatures; }
}