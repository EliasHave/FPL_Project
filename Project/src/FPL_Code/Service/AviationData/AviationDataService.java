package FPL_Code.Service.AviationData;

import FPL_Code.Model.Point;
import FPL_Code.Model.Feature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * PUBLIC FACADE: Koko muun sovelluksen ainoa yhteyspiste ilmailudataan.
 * Pitää huolen päivityksistä ja komentaa package-private -cacheluokkia[cite: 7].
 */
@Service
public class AviationDataService {

    private static final String GCS_BASE = "https://storage.openaip.net/openaip-system-exports/";

    // Lista maista joista tiedot haetaan
    private static final List<String> MAAT = List.of("fi", "se", "no", "ee", "lv", "lt", "de");

    // Spring injektoi nämä automaattisesti. Kukaan ulkopuolinen ei pääse näihin käsiksi!
    private final AirportCache airportCache;
    private final AirspaceCache airspaceCache;
    private final NavaidCache navaidCache;

    private final ObjectMapper mapper;
    private final HttpClient httpClient;

    public AviationDataService(AirportCache airportCache, AirspaceCache airspaceCache, NavaidCache navaidCache) {
        this.airportCache = airportCache;
        this.airspaceCache = airspaceCache;
        this.navaidCache = navaidCache;

        this.mapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @PostConstruct
    public void lataaAlkuData() {
        System.out.println("Aloitetaan ilmailudatan lataus ja karsinta käynnistyksessä...");
        paivitaKaikkiDatat();
    }

    @Scheduled(cron = "0 0 3 * * *") // Joka yö klo 03:00
    public void paivitaKaikkiDatat() {
        System.out.println("🔄 Päivitetään ilmatilat...");
        paivitaTyyppi("asp", airspaceCache);

        System.out.println("🔄 Päivitetään lentokentät...");
        paivitaTyyppi("apt", airportCache);

        System.out.println("🔄 Päivitetään navaidit...");
        paivitaTyyppi("nav", navaidCache);

        System.out.println("✅ Kaikki data päivitetty ja karsittu onnistuneesti!");
    }


    /**
     * Metodi joka hakee parametrina tuodun tyypin (ilmatila, lentokenttä, navaid) kaikki esiintymät yksi maa kerrallaan.
     * Tekee ensin raasta palautuksesta Feature olioita ja sitten laittaa tämän Feature listan karsittavaksi ja talletettavaksi kyseisen tyypin cache luokkaan
     * @param tyyppi Tyyppi joka kertoo mitä tietoja haetaan. Mahdollisia vaihtoehtoja on (Ilmatilat, lentokentät, navaidit)
     * @param cache Tallennuspaikka johon sitten tallennetaan lopulliset (karsitut) Feature oliot ja jota käytetään softan keskusvarastona näille tiedoille
     */
    private void paivitaTyyppi(String tyyppi, Object cache) {
        // Tyhjennetään cache ensin (koska jos päivitys tapahtuu yöllä, emme halua tuplia)
        if (cache instanceof AirspaceCache c) c.tyhjennaVanhat();
        else if (cache instanceof AirportCache c) c.tyhjennaVanhat();
        else if (cache instanceof NavaidCache c) c.tyhjennaVanhat();

        boolean loytyiYhtaanDataa = false;

        for (String maa : MAAT) {
            List<Feature> maanRaakaData = haeMaasta(maa, tyyppi);

            if (!maanRaakaData.isEmpty()) {
                loytyiYhtaanDataa = true;
                // Syötetään erä Cachelle karsittavaksi
                if (cache instanceof AirspaceCache c) c.lisaaJaKarsiIlmatilat(maanRaakaData);
                else if (cache instanceof AirportCache c) c.lisaaJaKarsiLentokentat(maanRaakaData);
                else if (cache instanceof NavaidCache c) c.lisaaJaKarsiNavaidit(maanRaakaData);
            }
            // Tässä vaiheessa lenkkiä Javan Garbage Collector tuhoaa maanRaakaData-listan! (Muisti säästyy)
        }

        // Virheenkäsittely: Jos KAIKKIEN maiden kohdalla epäonnistuttiin (esim GCS kokonaan alhaalla)
        if (!loytyiYhtaanDataa) {
            System.err.println("KRIITTINEN VAROITUS: Yhtäkään maata tyyppiä " + tyyppi + " ei saatu ladattua!");
            // HUOM: Ohjelma ei kaadu. Jos tämä oli yölataus, eiliset tiedot pyyhkiytyivät,
            // mutta se on parempi kuin näyttää vaarallista vanhentunutta dataa ilman että tiedämme siitä.
        }
    }


    /**
     * Metodi joka hakee tekee parametrina tulevasta maasta parametrina tulevan tyypin tiedot ja parsii ne Feature olioiksi
     * Palauttaa lopuksi tämän listan Feature olioita
     * @param maa Maa josta haetaan
     * @param tyyppi Tyyppi joka kertoo mitä tietoja haetaan (mahdollisia vaihtoehtoja on Ilmatilat, lentokentät, navaidit)
     * @return Palauttaa Feature listan joka sisältää kaikki kyseisen maan tämän tyyppiset esiintymät (Esimerkiksi kaikki suomen ilmatilat)
     */
    private List<Feature> haeMaasta(String maa, String tyyppi) {
        List<Feature> era = new ArrayList<>();
        String url = GCS_BASE + maa + "_" + tyyppi + ".geojson";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = mapper.readTree(response.body());
                JsonNode featuresNode = root.get("features");

                if (featuresNode != null && featuresNode.isArray()) {
                    for (JsonNode node : featuresNode) {
                        era.add(new Feature(node.get("geometry"), node.get("properties")));
                    }
                    System.out.println("  ✅ " + maa + "_" + tyyppi + " ladattu (" + era.size() + " kpl)");
                }
            } else {
                System.err.println("  ⚠️ " + maa + "_" + tyyppi + " ei löytynyt (HTTP " + response.statusCode() + ")");
            }
        } catch (Exception e) {
            System.err.println("  ❌ Virhe ladattaessa " + maa + "_" + tyyppi + ": " + e.getMessage());
        }

        return era; // Palauttaa tyhjän listan jos tapahtui virhe
    }


    public List<Feature> getKaikkiLentokentat() {
        return airportCache.getKaikkiLentokentat();
    }

    public List<Feature> getKaikkiIlmatilat() {
        return airspaceCache.getKaikkiIlmatilat();
    }

    public List<Feature> getKaikkiNavaidit() {
        return navaidCache.getKaikkiNavaidit();
    }


    /**
     * Funktio joka etsii lentokentän ICAO koodin perusteella.
     * Palauttaa Point olion joka sisältää lentokentän koordinaatit
     * @param icao Lentokentän ICAO koodi jonka perusteella se löydetään
     * @return Palauttaa Point olion joka sisältää kyseisen kentän koordinaatit
     */
    public Point etsiLentokentta(String icao) {
        if (icao == null || icao.trim().isEmpty()) {
            return null;
        }
        String etsittavaIcao = icao.trim().toUpperCase();

        // 1. Kysytään suoraan AirportCachelta (package-private -näkyvyyden ansiosta voimme kutsua tätä)
        Feature f = airportCache.haeIcaolla(etsittavaIcao);

        // 2. Tutkitaan tulos
        if (f != null) {
            // Puretaan GeoJSON-geometria, joka on muodossa [Longitude, Latitude]
            com.fasterxml.jackson.databind.JsonNode coords = f.geometry.path("coordinates");

            if (coords.isArray() && coords.size() >= 2) {
                double lon = coords.get(0).asDouble(); // 0 = Longitude
                double lat = coords.get(1).asDouble(); // 1 = Latitude

                return new Point(etsittavaIcao, lat, lon);
            } else {
                System.err.println("⚠️ Kentän " + etsittavaIcao + " geometria on viallinen!");
                return null;
            }
        }

        // 3. Jos f oli null, kenttää ei löytynyt
        System.err.println("❌ Lentokenttää ICAO-koodilla '" + etsittavaIcao + "' ei löytynyt välimuistista!");
        return null;
    }
}