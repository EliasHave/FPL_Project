package FPL_Code.Service;

import FPL_Code.Model.Notam;
import FPL_Code.Model.Point;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class NotamService {

    private static final double KM_PER_DEGREE = 111.32;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();


    /**
     * Metodi joka hakee NOTAMit Autorouterin API:sta ja suodattaa ne reitin läheltä
     * Palauttaa listan Notam-olioita jotka ovat joko lähtö- tai kohdekentän NOTAMit tai EN_ROUTE-notamit jotka leikkaavat reitin puskurialuetta
     * @param lahtoIcao Lähtökentän ICAO-koodi
     * @param kohdeIcao Määränpääkentän ICAO-koodi
     * @param firKoodit FIR alueiden koodit jota suora reittiviiva leikkaa
     * @param lahto Point olio joka sisältää lähtökentän koordinaatit
     * @param kohde Point olio joka sisältää kohdekentän koordinaatit
     * @return Palauttaa listan Notam-olioita jotka ovat relevantteja tälle reitille
     */
    public List<Notam> haeJaSuodataNotamit(String lahtoIcao, String kohdeIcao, List<String> firKoodit, Point lahto, Point kohde) {
        List<Notam> relevantitNotamit = new ArrayList<>();

        List<String> hakuKoodit = new ArrayList<>(firKoodit);
        if (!hakuKoodit.contains(lahtoIcao)) hakuKoodit.add(lahtoIcao);
        if (!hakuKoodit.contains(kohdeIcao)) hakuKoodit.add(kohdeIcao);

        // 1. API-KUTSU SIVUTUKSELLA
        List<Notam> kaikkiHaetutNotamit = haeKaikkiNotamitAutoRouter(hakuKoodit, lahtoIcao, kohdeIcao);

        // 2. GEOMETRINEN SUODATIN (30 km puskuri)
        GeometryFactory factory = new GeometryFactory();
        Geometry reittiPuskuri = factory.createLineString(new Coordinate[]{
                new Coordinate(lahto.getLon(), lahto.getLat()),
                new Coordinate(kohde.getLon(), kohde.getLat())
        }).buffer(30.0 / KM_PER_DEGREE);

        // 3. SUODATUS
        for (Notam n : kaikkiHaetutNotamit) {
            if (n.getTyyppi() == Notam.NotamType.DEPARTURE || n.getTyyppi() == Notam.NotamType.DESTINATION) {
                relevantitNotamit.add(n);
                continue;
            }

            if (n.getTyyppi() == Notam.NotamType.EN_ROUTE) {
                // Notam-oliolla on jo valmiiksi laskettu JTS-alue parsimisvaiheessa!
                if (n.getVaikutusAlue() == null || reittiPuskuri.intersects(n.getVaikutusAlue())) {
                    relevantitNotamit.add(n);
                }
            }
        }

        return relevantitNotamit;
    }


    /**
     * Metodi joka hakee kaikki NOTAMit Autorouterin API:sta annettujen FIR (ja ICAO) koodien perusteella.
     * @param koodit List<String> joka sisältää FIR-koodit
     * @param lahtoIcao Lähtökentän ICAO-koodi
     * @param kohdeIcao Määränpääkentän ICAO-koodi
     * @return Palautaa Listan Notam olioita jotka on paristu Autorouterin API:n JSON-vastauksesta
     */
    private List<Notam> haeKaikkiNotamitAutoRouter(List<String> koodit, String lahtoIcao, String kohdeIcao) {
        List<Notam> kaikkiNotamit = new ArrayList<>();
        try {
            // Autorouter vaatii JSON-arrayn: ["EFHK","EFIN"]
            String itemasJson = mapper.writeValueAsString(koodit);
            String encodedItemas = URLEncoder.encode(itemasJson, StandardCharsets.UTF_8);

            int offset = 0;
            int limit = 100;
            int total = 100; // Alkuarvo, päivitetään ensimmäisestä vastauksesta
            GeometryFactory factory = new GeometryFactory();

            while (offset < total) {
                String apiUrl = "https://api.autorouter.aero/v1.0/notam?itemas=" + encodedItemas + "&limit=" + limit + "&offset=" + offset;

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(apiUrl))
                        .header("Accept", "application/json")
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    JsonNode root = mapper.readTree(response.body());
                    total = root.path("total").asInt(0);

                    JsonNode rows = root.path("rows");
                    if (rows.isArray()) {
                        parsiAutorouterRivit(rows, kaikkiNotamit, lahtoIcao, kohdeIcao, factory);
                    }
                } else {
                    System.err.println("❌ NOTAM API palautti virheen: HTTP " + response.statusCode());
                    break;
                }
                offset += limit;
            }
        } catch (Exception e) {
            System.err.println("❌ Virhe NOTAMien haussa: " + e.getMessage());
        }
        return kaikkiNotamit;
    }


    /**
     * Metodi joka lukee Autorouterin NOTAM-API:n rivit ja rakentaa Notam-oliot
     * @param rows JsonNode joka sisältää (raat) NOTAM-rivit
     * @param notamit Lista johon valmiit, parsitut Notam-oliot lisätään
     * @param lahtoIcao Lähtökentän ICAO-koodi
     * @param kohdeIcao Määränpääkentän ICAO-koodi
     * @param factory Geometriafactory jota käytetään JTS-alueiden luomiseen (ei luoda metodissa uutta)
     */
    private void parsiAutorouterRivit(JsonNode rows, List<Notam> notamit, String lahtoIcao, String kohdeIcao, GeometryFactory factory) {
        for (JsonNode node : rows) {
            JsonNode itemaArray = node.path("itema");
            String icao = (itemaArray.isArray() && !itemaArray.isEmpty()) ? itemaArray.get(0).asText("") : "";

            if (icao.isEmpty()) continue;

            Notam.NotamType tyyppi = Notam.NotamType.EN_ROUTE;
            if (icao.equalsIgnoreCase(lahtoIcao)) tyyppi = Notam.NotamType.DEPARTURE;
            else if (icao.equalsIgnoreCase(kohdeIcao)) tyyppi = Notam.NotamType.DESTINATION;

            // Rakennetaan perinteinen tekstimuoto tekoälyä ja käyttöliittymää varten
            String qRivi = String.format("%s/%s%s/%s/%s/%s/%03d/%03d/",
                    node.path("fir").asText(""),
                    node.path("code23").asText(""), node.path("code45").asText(""),
                    node.path("traffic").asText(""), node.path("purpose").asText("").trim(),
                    node.path("scope").asText("").trim(),
                    node.path("lower").asInt(0), node.path("upper").asInt(999));

            String bAika = muotoileAika(node.path("startvalidity").asLong(0));
            String cAika = muotoileAika(node.path("endvalidity").asLong(4294967295L));
            String eRivi = node.path("iteme").asText("");

            String raakaTeksti = String.format("Q) %s\nA) %s\nB) %s C) %s\nE) %s", qRivi, icao, bAika, cAika, eRivi);

            Notam n = new Notam(tyyppi, icao, raakaTeksti);

            // Koordinaatit ja säde (jos kyseessä on EN_ROUTE-notam)
            if (tyyppi == Notam.NotamType.EN_ROUTE && node.has("lat") && node.has("lon") && node.has("radius")) {
                double lat = (node.path("lat").asLong() * 90.0) / 1073741824.0;
                double lon = (node.path("lon").asLong() * 90.0) / 1073741824.0;
                double sadeNm = node.path("radius").asDouble();

                if (sadeNm > 0) {
                    double sadeAsteina = (sadeNm * 1.852) / KM_PER_DEGREE;
                    Geometry alue = factory.createPoint(new Coordinate(lon, lat)).buffer(sadeAsteina);
                    n.setVaikutusAlue(alue);
                }
            }
            notamit.add(n);
        }
    }


    /**
     * Apumetodi joka muotoilee UNIX-sekunnit luettavaksi muodoksi
     * @param unixSeconds Unix-sekunnit (long)
     * @return Palauttaa merkkijonon muodossa "yyyy-MM-dd HH:mm" tai "PERM" / "EST"
     */
    private String muotoileAika(long unixSeconds) {
        if (unixSeconds == 0) return "PERM";
        if (unixSeconds >= 4294967295L) return "EST"; // Maksimiarvo
        ZonedDateTime zdt = ZonedDateTime.ofInstant(Instant.ofEpochSecond(unixSeconds), ZoneOffset.UTC);
        return zdt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }
}