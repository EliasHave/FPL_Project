package FPL_Code;

import FPL_Code.DTO_Package.DTO_Boss;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.json.JSONArray;
import org.json.JSONObject;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.springframework.beans.factory.annotation.Autowired;;

import java.io.File;
import java.io.IOException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;


/**
 * luokka joka laskee tekoälyn avulla lentosuunnitelmia
 * Käyttää kaiken mahdollisen tiedon joka koneesta, säästä sekä notemseista irtoaa
 * collabs: Aircraft, Notam, Weather
 */
public class FlightPlanner {

    // AviationDataService injektoidaan konstruktorissa.
    // Se tarjoaa (melkein) maailmanlaajuisen ilmailutietokannan muistista,
    // josta voi suodattaa vain reitille olennaiset kohteet.
    private final AviationDataService aviationDataService;

    public FlightPlanner(AviationDataService aviationDataService) {
        this.aviationDataService = aviationDataService;
    }

    // mahdollisesti kannattaisi lisätä lähtö- ja määränpää Point oliona tähän attribuutiksi, jotta niihin pääsee helposti käsiksi missä
    // tahansa metodissa eikä tarvitse hakea niitä erikseen sään kautta joka on huono tapa
    private String maaranpaaKentta;
    private Weather saaMaapanpaa;
    private Weather saaLahto;
    private Aircraft kone;
    private String notam;
    private List<Notam.NotamOlio> notamOliot;
    private Pilot pilot;
    private String relevantAirspacesGeoJson;
    private  String relevantAirportsGeoJson;
    private String relevantNavaidsGeoJson;


    public void setMaaranpaaKentta(String maaranpaaKentta) {
        this.maaranpaaKentta = maaranpaaKentta;
    }

    public void setSaaLahto(Weather saa) {
        this.saaLahto = saa;
    }


    public void setSaaMaapanpaa(Weather saa) {
        this.saaMaapanpaa = saa;
    }


    public void setKone(Aircraft kone) {
        this.kone = kone;
    }


    public void setNotam(String notam) {
        this.notam = notam;
    }


    public void setNotamOliot(List<Notam.NotamOlio> notamOliot) {
        this.notamOliot = notamOliot;
    }

    public void setPilot(Pilot pilot) {
        this.pilot = pilot;
    }

    public void setRelevantAirspacesGeoJson(String relevantAirspacesGeoJson) {
        this.relevantAirspacesGeoJson = relevantAirspacesGeoJson;
    }

//------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    public String getMaaranpaaKentta() {
        return maaranpaaKentta;
    }

    public Weather getSaaLahto() {
        return saaLahto;
    }


    public Weather getSaaMaaranpaa() {
        return saaMaapanpaa;
    }


    public Aircraft getKone() {
        return kone;
    }


    public String getNotam() {
        return notam;
    }


    public List<Notam.NotamOlio> getNotamOliot() {
        return notamOliot;
    }


    public Pilot getPilot() {
        return pilot;
    }


    public String getRelevantAirspacesGeoJson() {
        return relevantAirspacesGeoJson;
    }


    public String getRelevantAirportsGeoJson() {
        return relevantAirportsGeoJson;
    }


    public String getRelevantNavaidsGeoJson() {
        return relevantNavaidsGeoJson;
    }


    public AviationDataService getAviationDataService() {
        return aviationDataService;
    }


//--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------


    /**
     * Metodi joka muodostaa kaiken mahdollisen lentoon liittyvan datan avulla reittipisteet joita pitkin lento kannattaa suorittaa
     * @return palauttaa reittipiste listan oikassa järjestyksessä jotta nämä pisteet voidaan laittaa kartalle ja saadaan reitti piirtymään oikein
     */
    public List<Point> teeReitti(Point lahtoKoord, Point maaranpaaKoord) {

        // maaranpaa kentän sää täytyy katsoa ja asettaa tässä koska nyt tiedetään nopeus ja matka yms jotta pystytään tarkemmin arviomaan monelta sää kannattaa ennustaa
        asetaMaaranpaanSaa(lahtoKoord, maaranpaaKoord);

        List<Point> reittiPisteet = new ArrayList<>();

        Point lahtoPiste = lahtoKoord;
        Point maaranpaaPiste = maaranpaaKoord;
        List<Feature> kaikkiIlmatilat = lataaIlmatilatGeoJsonista();
        List<Feature> olennaisetIlmatilat = suodataIlmatilat(kaikkiIlmatilat, lahtoPiste, maaranpaaPiste, 50.0);

        List<Feature> olennaisetJaKarsitutIlmatilat = suodataIlmatilatJaKirjoitaGeoJson(olennaisetIlmatilat); // voisiko tämä palauttaa karsitut ilmatilat feature listana jotta on helppo viedä aliohjelmaan

        List<Feature> suodatetutLentokentat = suodataLentokentat(lahtoPiste, maaranpaaPiste); // Sama näissä kahdessa
        List<Feature> suodatetutNavaidit = suodataNavaidit(lahtoPiste, maaranpaaPiste);

        List<WeatherSamplePoint> saanMittausPisteet = kartoitaSaaReitilla(lahtoPiste, maaranpaaPiste);

        if (olennaisetJaKarsitutIlmatilat.isEmpty() || suodatetutNavaidit.isEmpty() || suodatetutLentokentat.isEmpty() || saanMittausPisteet.isEmpty()) {
            System.out.println("Jotain meni pieleen teeReitti metodissa kun ilmatila, lentokenttä, navaid tai saa tieojen listoja haettiin");
        }

        // Tähän pitäisi mielellään saada ryöstettyä vielä notamit, koneen tiedot, pilotin tiedot jotta niiden kanssa voidaan mennä kirjoittamaan se input geoJson tiedosto
        DTO_Boss dtoData = DTO_Boss.haeDTO(olennaisetJaKarsitutIlmatilat, suodatetutLentokentat, suodatetutNavaidit, saanMittausPisteet, notamOliot, kone, pilot, saaLahto, saaMaapanpaa);

        AIAgent agent = new AIAgent();// tämä tekoäly agentti ohjaa tekoälyltä kysymisen, parsimisen, tarkastuksen/validoinnin yms
        // reittiPisteet = agent.reitita(dtoData);
        // kysyTekoalyltaOPENAI("flight_input..json");
        //kysyCLAUDE("flight_input..json");  // nyt kun data onkerätty ja jäsennelty sopivasti niin kysytään tekoälyä tekemään lentosuunnitelma

        return reittiPisteet;

    }


    /**
     * Kysyy tekoälyltä (Claude) lentosuunnitelman datan perusteella
     * @param dataTiedosto Datatiedosto joka sisältää lennon kannalta oleellisen tiedon järjesteltynä
     */
    public void kysyCLAUDE(String dataTiedosto) {
        try {
            // 1. Lue JSON tiedoston sisältö
            String jsonData = Files.readString(Paths.get(dataTiedosto), StandardCharsets.UTF_8);

            // 2. Luo prompt
            String prompt = """
                You are an expert flight planner and aviation safety analyst.
                Analyze the following comprehensive flight planning JSON data.
                
                Your tasks:
                1. Identify departure (ADEP) and destination (ADES) airports
                2. Analyze weather conditions (METARs, TAFs) for safety considerations
                3. Review airspace restrictions and NOTAMs along the route
                4. Evaluate fuel requirements and alternate airport suitability
                5. Check navigation aids (VOR, NDB, etc.) availability
                6. Identify any safety concerns or regulatory issues
                7. Provide a structured risk assessment
                
                Return your analysis in this JSON format:
                {
                  "departure": "ICAO code",
                  "destination": "ICAO code",
                  "weather_summary": "brief overview",
                  "safety_concerns": ["concern1", "concern2"],
                  "airspace_issues": ["issue1"],
                  "fuel_assessment": "adequate/marginal/insufficient",
                  "alternates_status": "suitable/limited/none",
                  "overall_risk": "low/medium/high",
                  "recommendations": ["recommendation1", "recommendation2"]
                }
                
                Flight data:
                """ + jsonData;

            // 3. Lähetä Claude API:lle
            String response = muodostaYhteysClaude(prompt);

            // 4. Tulosta tai tallenna vastaus
            System.out.println("✈️ Clauden analyysi:\n" + response);

            Files.writeString(Paths.get("ai_flight_analysis.json"), response);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }


    /**
     * Muodostaa yhteyden tekoälyyn (Claude) antaa sille parametrina tulevan promptin ja palauttaa vastauksen content osion String muodossa
     * @param prompt Prompti joka halutaan antaa
     * @return palauttaa vastauksen content osion String muodossa
     * @throws IOException
     * @throws InterruptedException
     */
    public String muodostaYhteysClaude(String prompt) throws IOException, InterruptedException {
        String apiKey = System.getenv("CLAUDE_API_KEY");
        System.out.println(apiKey);

        HttpClient client = HttpClient.newHttpClient();

        String jsonRequest = """
    {
      "model": "claude-opus-4-20250514",
      "max_tokens": 4096,
      "temperature": 0.3,
      "messages": [
        {
          "role": "user",
          "content": %s
        }
      ]
    }
    """.formatted(JSONObject.quote(prompt));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.anthropic.com/v1/messages"))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Parsitaan Clauden vastaus (content on arrayssä)
        JSONObject jsonResponse = new JSONObject(response.body());
        JSONArray contentArray = jsonResponse.getJSONArray("content");
        String textContent = contentArray.getJSONObject(0).getString("text");

        return textContent;
    }


    /**
     * Kysyy tekoälyltä (OpenAI) lentosuunnitelman parametrina tulevan datan perusteella
     * @param dataTiedosto datatiedosto jossa on kaikki lentoon liittyvä oleelline tieto
     */
    public void kysyTekoalyltaOPENAI(String dataTiedosto) {
        try {
            // 1. Lue JSON tiedoston sisältö
            String jsonData = Files.readString(Paths.get(dataTiedosto), StandardCharsets.UTF_8);

            // 2. Luo prompt
            String prompt = """
                    You are an expert flight planner and meteorologist.
                    Analyze the following JSON flight data.\s
                    
                    1. Identify the departure and destination airports.
                    2. Summarize the weather at both locations.
                    3. Estimate possible flight duration and major weather or airspace challenges.
                    4. DO NOT generate a route yet — just confirm that you understand the flight context.
                    
                    Flight data:
        """ + jsonData;

            // 3. Lähetä OpenAI:lle
            String response = muodostaYhteysAI(prompt);

            // 4. Tulosta tai tallenna vastaus
            System.out.println("✈️ Tekoälyn vastaus:\n" + response);

            Files.writeString(Paths.get("ai_output.json"), response);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }


    /**
     * Muodostaa yhteyden tekoälyyn (OpenAI) ja antaa sille inputtina parametrina tuodun promptin, palauttaa vastauksen String muodossa
     * @param prompt Prompt joka halutaan antaa tekoälylle
     * @return Palauttaa tekoälyn vastauksen String muodossa
     * @throws IOException
     * @throws InterruptedException
     */
    public String muodostaYhteysAI(String prompt) throws IOException, InterruptedException {
        String apiKey = System.getenv("OPENAI_API_KEY");

        HttpClient client = HttpClient.newHttpClient();

        String jsonRequest = """
        {
          "model": "gpt-4-turbo",
          "messages": [
            {"role": "system", "content": "You are an expert flight planner and meteorologist."},
            {"role": "user", "content": %s}
          ],
          "temperature": 0.4
        }
        """.formatted(JSONObject.quote(prompt));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequest))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }


    /**
     * Asettaa määränpääkentän sään laskemalla arvioitu saapumisaika lähtöhetkestä, matkan pituudesta ja koneen nopeudesta, ja hakee sitten sään tuolle arvioidulle saapumisajalle
     * @param lahtoKoord Lähtöpiste Point oliona
     * @param maaranpaaKoord Määränpääpiste Point oliona
     */
    public void asetaMaaranpaanSaa(Point lahtoKoord, Point maaranpaaKoord) {

        ZonedDateTime lahtoHetki = saaLahto.getAjankohtaZDT();

        // Lasketaan matkustusaika Duration-oliona
        double matkaKM = haversineKm(lahtoKoord.getLat(), lahtoKoord.getLon(), maaranpaaKoord.getLat(), maaranpaaKoord.getLon());
        double nopeusKMh = kone.getCruiseSpeed() * 1.852;
        long minuutit = Math.round((matkaKM / nopeusKMh) * 60);
        Duration kesto = Duration.ofMinutes(minuutit);

        // Arvioitu saapumisaika = lähtöaika + lentoaika
        ZonedDateTime lahtoHetkiUTC = lahtoHetki.withZoneSameInstant(ZoneOffset.UTC);
        ZonedDateTime arvioituSaapumisaika = lahtoHetkiUTC.plus(kesto);

        Weather maapanpaanSaa = Weather.haeSaaOlio(maaranpaaKentta, arvioituSaapumisaika);
        this.saaMaapanpaa = maapanpaanSaa;
    }


    /**
     * suodatta parametrina tulevasta fetaures (ilmatilat) listasta turhat kentät/ominaisuudet pois
     * Kirjoittaa uuden geojson tiedoston näistä ilmatiloista sekä tallentaa samassa geojson muodossa tämän datan plannerin relevantAirspacesGeoJson attribuuttiin jmyöhempää käyttöä varten
     * @param olennaisetIlmatilat Ilmatilojen lista jonka ilmatiloista halutaan karsia turhat tiedot pois
     */
    public List<Feature> suodataIlmatilatJaKirjoitaGeoJson(List<Feature> olennaisetIlmatilat) {
        // karsitaan turhat tiedot
        List<Feature> tiivistetyt = new ArrayList<>();
        for (Feature f : olennaisetIlmatilat) {
            tiivistetyt.add(f.karsiIlmatilanProperties());
        }
        // Muunnetaan GeoJSON-stringiksi
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode root = mapper.createObjectNode();
            root.put("type", "FeatureCollection");
            root.set("features", mapper.valueToTree(tiivistetyt));
            this.relevantAirspacesGeoJson = mapper.writeValueAsString(root); // tallennetaan plannerin attribuutiksi myöhempää käyttöä varten
        } catch (Exception e) {
            this.relevantAirspacesGeoJson = "{\"type\":\"FeatureCollection\",\"features\":[]}";
        }
        this.relevantAirspacesGeoJson = kirjoitaGeoJson("suodatetutIlmatilat2.geojson", tiivistetyt);  //TODO 02.05.2026 miksi tässä asetetaan relevanAirspacesGeoJson toista kertaa???
        return tiivistetyt;
    }


    /**
     * Suodattaa pois geometrisesti kaikki turhat ilmatilat joita ei tarvita reitillä
     * Ei kuitenkaa karsi ominaisuuksia pois tms
     * @param kaikkiIlmatilat Kaikki ilmatilat eli mukana myös mahdolisesti turhia
     * @param lahtoPiste Lähtöpiste, Point olio jolla koordinaatit
     * @param maaranpaaPiste Määränpääpiste, Point olio jolla koordinaatit
     * @param sade Säde jonka ulkopuolella olevat ilmatilat katsotaan "turhiksi"
     * @return Palauttaa saman tyylisen feature(ilmatila) listan kuin parametrina tuli mutta karsitun versio jossa on vain jäljellä kaikista tärkeimmät
     */
    public List<Feature> suodataIlmatilat(List<Feature> kaikkiIlmatilat, Point lahtoPiste, Point maaranpaaPiste, double sade) {
        List<Feature> kaikki = kaikkiIlmatilat;
        List<Feature> olennaiset = new ArrayList<>();

        GeometryFactory gf = new GeometryFactory();
        Coordinate[] lineCoords = new Coordinate[]{
                new Coordinate(lahtoPiste.getLon(), lahtoPiste.getLat()),
                new Coordinate(maaranpaaPiste.getLon(), maaranpaaPiste.getLat())
        };

        LineString reitti = gf.createLineString(lineCoords);
        Geometry puskuri = reitti.buffer(sade / 111.32); // km → asteet (1° ~ 111.32km)

        GeoJsonReader reader = new GeoJsonReader(gf);

        for (Feature f : kaikki) {
            try {
                Geometry geom = reader.read(f.geometry.toString());
                if (puskuri.intersects(geom)) {
                    olennaiset.add(f);
                }
            } catch (org.locationtech.jts.io.ParseException e) {
                System.err.println("⚠️ Virhe geojson-geometriaa tulkittaessa: " + e.getMessage());
            }
        }

        return olennaiset;
    }


    /**
     * Lataa/hakee ilmatilat aviationDataService luokasa joka palauttaa ne String muodossa karsimattomana
     * Tämä metodi tekee niistä Feature olioita sekä tallentaa oliot listaan joka palutetaan
     * @return palauttaa listan johon nämä luodut feature oliot on lisätty
     */
    public List<Feature> lataaIlmatilatGeoJsonista() {
        List<Feature> features = new ArrayList<>();

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(aviationDataService.getIlmatilatGeoJson());  //String muuttujasta tiedoston sijaan (sama geojson muoto)

            JsonNode featureNodes = root.get("features");
            for (JsonNode node : featureNodes) {
                JsonNode geometry = node.get("geometry");
                JsonNode properties = node.get("properties");

                features.add(new Feature(geometry, properties));
            }

        } catch (IOException e) {
            System.err.println("❌ Ilmatilojen lataus epäonnistui: " + e.getMessage());
        }

        return features;
    }


    /**
     * Suodattaa reitin kannalta olennaiset lentokentät sekä karsii turhat ominaisuudet pois
     * Lentokentät haetaan aviationDataServicen getLentokentatGeoJson metodilla joka palauttaa kaikki ladatut lentokentät geoJson muotoisena Stringinä
     * @param lahtoPiste, Point olio jolla lähtöpisteen koordinaatit
     *  @param maaranpaaPiste, Point olio jolla määränpään koordinaatit
     * @return palauttaa Feature listan johon on lisätty reitin läheisyydessä olevat lentokentät karsittuna
     */
    public List<Feature> suodataLentokentat(Point lahtoPiste, Point maaranpaaPiste) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(aviationDataService.getLentokentatGeoJson());  //String muuttujasta tiedoston sijaan (sama geojson muoto)

            List<Feature> kaikkiKentat = new ArrayList<>();
            JsonNode features = root.get("features");

            for (JsonNode f : features) {
                JsonNode props = f.get("properties");
                int tyyppi = props.path("type").asInt(-1);

                // Jätetään helikopterikentät (type == 7) pois
                if (tyyppi == 7) continue;

                JsonNode geom = f.get("geometry");
                kaikkiKentat.add(new Feature(geom, props));
            }

            List<Feature> olennaiset = suodataPointFeaturesLahellaReittia(kaikkiKentat, lahtoPiste, maaranpaaPiste, 50.0);

            // karsitaan turhat tiedot
            List<Feature> tiivistetyt = new ArrayList<>();
            for (Feature f : olennaiset) {
                tiivistetyt.add(f.karsiLentokentanProperties());
            }

            this.relevantAirportsGeoJson = kirjoitaGeoJson("suodatetutLentokentat.geojson", tiivistetyt);
            return tiivistetyt;

        } catch (IOException e) {
            System.err.println("❌ Lentokenttien suodatus epäonnistui: " + e.getMessage());
        }

        return null;
    }


    /**
     * Suodattaa navaidit reitin läheltä ja karsii turhat ominaisuudet pois. Tallentaa suodatetut navaidit geojson muodossa plannerin relevantNavaidsGeoJson attribuuttiin myöhempää käyttöä varten
     * @param lahtoPiste Lähtöpiste, Point olio jolla koordinaatit
     *  @param maaranpaaPiste Määränpääpiste, Point olio jolla koordinaatit
     * @return  palauttaa listan johon on lisätty reitin läheltä suodatetut navaidit, ja näissä navaideissa on vain olennaiset ominaisuudet jäljellä (karsittu versio)
     */
    public List<Feature> suodataNavaidit(Point lahtoPiste, Point maaranpaaPiste) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(aviationDataService.getNavaiditGeoJson());  //String muuttujasta tiedoston sijaan (sama geojson muoto)

            List<Feature> kaikkiNavaidit = new ArrayList<>();
            JsonNode features = root.get("features");

            for (JsonNode f : features) {
                JsonNode geom = f.get("geometry");
                JsonNode props = f.get("properties");
                kaikkiNavaidit.add(new Feature(geom, props));
            }

            List<Feature> olennaiset = suodataPointFeaturesLahellaReittia(kaikkiNavaidit, lahtoPiste, maaranpaaPiste, 50.0);

            // Tiivistys
            List<Feature> tiivistetyt = new ArrayList<>();
            for (Feature f : olennaiset) {
                tiivistetyt.add(f.karsiNavaidinProperties());
            }

            this.relevantNavaidsGeoJson = kirjoitaGeoJson("suodatetutNavaidit.geojson", tiivistetyt);
            return tiivistetyt;

        } catch (IOException e) {
            System.err.println("❌ Navaidien suodatus epäonnistui: " + e.getMessage());
        }
        return null;
    }


    /**
     * Suodattaa piste-tyyppiset geo-objektit (esim. lentokentät, navaidit), jotka ovat lähellä reittiä.
     */
    private List<Feature> suodataPointFeaturesLahellaReittia(List<Feature> kaikki, Point lahtoPiste, Point maaranpaaPiste, double sadeKm) {
        List<Feature> tulokset = new ArrayList<>();

        GeometryFactory gf = new GeometryFactory();
        LineString reitti = gf.createLineString(new Coordinate[]{
                new Coordinate(lahtoPiste.getLon(), lahtoPiste.getLat()),
                new Coordinate(maaranpaaPiste.getLon(), maaranpaaPiste.getLat())
        });

        Geometry puskuri = reitti.buffer(sadeKm / 111.32);  // asteiksi

        for (Feature f : kaikki) {
            try {
                JsonNode coords = f.geometry.get("coordinates");
                double lon = coords.get(0).asDouble();
                double lat = coords.get(1).asDouble();

                org.locationtech.jts.geom.Point p = gf.createPoint(new Coordinate(lon, lat));
                if (puskuri.contains(p)) {
                    tulokset.add(f);
                }
            } catch (Exception e) {
                System.err.println("⚠️ Lentokentän/navaidin pistevirhe: " + e.getMessage());
            }
        }

        return tulokset;
    }



    /**
     * kirjoittaa parametrina tulevasta features listasta geoJson tiedoston ja palauttaa sen String muodossa. Ei karsi enää tässä vaiheessa mitään pois vaan kirjoittaa kaiken mitä fetaures listassa on
     * @param features features lista jossa on Feature olioita joilla on geometria ja ominaisuuksia. Tämä lista kirjoitetaan geoJson tiedostoon tyylitellysti
     */
    public String kirjoitaGeoJson(String tiedNimi, List<Feature> features) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        ArrayNode featureArray = mapper.createArrayNode();

        for (Feature f : features) {
            ObjectNode featureNode = mapper.createObjectNode();
            featureNode.put("type", "Feature");
            featureNode.set("geometry", f.geometry);
            featureNode.set("properties", f.properties);
            featureArray.add(featureNode);
        }

        root.put("type", "FeatureCollection");
        root.set("features", featureArray);

        String jsonString = "";

        try {
            // Tallennetaan tiedostoon
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(tiedNimi), root);
            System.out.println("✅ GeoJSON tallennettu: " + tiedNimi);

            // Palautetaan myös stringinä
            jsonString = mapper.writeValueAsString(root);
        } catch (IOException e) {
            System.err.println("❌ GeoJSON-käsittely epäonnistui: " + e.getMessage());
            jsonString = "{\"type\":\"FeatureCollection\",\"features\":[]}";
        }

        return jsonString;
    }


    /**
     * Luo säämittauspisteet reitin varrelta ja sen reunoilta
     */
    public List<WeatherSamplePoint> kartoitaSaaReitilla(Point lahto, Point maaranpaa) {
        List<WeatherSamplePoint> pisteet = new ArrayList<>();

        // Lasketaan kokonaismatka kilometreinä
        double kokoMatka = haversineKm(lahto.getLat(), lahto.getLon(), maaranpaa.getLat(), maaranpaa.getLon());

        double puskurinLeveys = 40.0;

        // Säädetään tarkkuutta täällä (esim. 20 km välein)
        if (kokoMatka > 500.0) {
            boolean pitka = true;
            puskurinLeveys = 60.0;
            System.out.println("Matka yli 500km, puskuri 60km");
        }
        double pisteValiKm = 20.0;

        int maara = (int) (kokoMatka / pisteValiKm);

        for (int i = 0; i <= maara; i++) {
            double t = (double) i / maara;
            double lat = lahto.getLat() + t * (maaranpaa.getLat() - lahto.getLat());
            double lon = lahto.getLon() + t * (maaranpaa.getLon() - lahto.getLon());

            // Keskilinja
            pisteet.add(new WeatherSamplePoint(lat, lon));

            // Vasen ja oikea reuna ±20 km kohtisuoraan
            double suuntaRad = Math.atan2(
                    maaranpaa.getLon() - lahto.getLon(),
                    maaranpaa.getLat() - lahto.getLat()
            );

            double poikittain = Math.PI / 2;

            WeatherSamplePoint oikea = siirraKoordinaattia(lat, lon, puskurinLeveys/2, suuntaRad + poikittain);
            WeatherSamplePoint vasen = siirraKoordinaattia(lat, lon, puskurinLeveys/2, suuntaRad - poikittain);

            pisteet.add(oikea);
            pisteet.add(vasen);
        }

        // lasketaan arvioitu saapumisaika mittauspisteeseen jotta voidaan arvioida paremmin säätä juuri sillä hetekllä kun se on oleellista
        laskeSaapumisAika(pisteet, lahto, maaranpaa);

        // haetaan sääennusteet pisteille oikeaan saapumisaikaan
        haeSaat(pisteet);

        // kirjoitetaan geojson tiedosto sääpisteistä.
        kirjoitaSaapisteetGeoJson(pisteet);

        return pisteet;
    }


    /**
     * Laskee kartoitetuille pisteillä arvioidun saapumisajan lentokoneen nopeuden perusteella ja asettaa sen kullekkin weathersamplepoint oliolle
     * @param pisteet pisteet joille ajat lasketaan
     * @param lahto lähtöpiste
     * @param maaranpaa maaranpaapiste
     */
    public void laskeSaapumisAika(List<WeatherSamplePoint> pisteet, Point lahto, Point maaranpaa) {
        double koneenNopeusKmh = kone.getCruiseSpeed() * 1.852;

        ZonedDateTime lahtoAika = saaLahto.getAjankohtaZDT(); // esim. 2025-07-20T07:00+03:00

        for (WeatherSamplePoint p : pisteet) {
            double etaisyysKm = haversineKm(lahto.getLat(), lahto.getLon(), p.getLat(), p.getLon());
            double tunnit = etaisyysKm / koneenNopeusKmh;
            long sekunnit = (long) (tunnit * 3600);

            ZonedDateTime saapumisaika = lahtoAika.plusSeconds(sekunnit);
            p.setAika(saapumisaika); // tallennetaan aikavyöhykkeen kanssa
        }
    }


    /**
     * hakee sääennusteet pisteille ja asettaa ennusteen pisteen ennusteTeksti atribuutiksi
     * @param pisteet pisteet joille sää haetaan
     */
    public void haeSaat(List<WeatherSamplePoint> pisteet) {
        ObjectMapper mapper = new ObjectMapper();
        boolean tehty = false;

        for (WeatherSamplePoint p : pisteet) {
            p.haeEnnuste(mapper, tehty);
        }
    }

    // Maapallon säde
    private static final double R = 6371.0;

    /**
     * Laskee koordinaattien välisen etäisyyden ja palauttaa sen kilometreinä
     * @param lat1 Pisteen 1 leveysaste
     * @param lon1 Pisteen 1 pituusaste
     * @param lat2 Pisteen 2 leveysaste
     * @param lon2 Pisteen 2 pituusaste
     * @return Palauttaa etäisyyden joka 1 ja 2 pisteen välillä on kilometreinä
     */
    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);
        return 2 * R * Math.asin(Math.sqrt(a));
    }

    /**
     * Mittaa koordinaatit parametrinatulevasta ja laskee mitkä koordinaatit tulee kun siirretään parametrina tulevaan suuntaan ja parametrina tulevan matkan. Luo uuden Weather samplepointin tähän kohtaan
     * @param lat Pisteen leveysaste josta halutaan siirtää
     * @param lon pisteen pituusaste josta halutaan siirtää
     * @param km etäisyys joka halutaan siirtää kilometreinä
     * @param suuntaRad Suunta johon halutaan siirtää radiaaneina
     * @return Palauttaa uuden WeatherSamplePointin joka on siirretty parametien perusteella
     */
    private WeatherSamplePoint siirraKoordinaattia(double lat, double lon, double km, double suuntaRad) {
        double uusiLat = lat + (km / R) * Math.cos(suuntaRad) * (180 / Math.PI);
        double uusiLon = lon + (km / R) * Math.sin(suuntaRad) * (180 / Math.PI) / Math.cos(Math.toRadians(lat));
        return new WeatherSamplePoint(uusiLat, uusiLon);
    }


    /**
     * testiohjelma jotta nähdään että pisteet on piirretty oikealle paikalle
     * @param pisteet Sään mittaus pisteet jotka halutaan tallentaa geoJsoniin
     */
    public void kirjoitaSaapisteetGeoJson(List<WeatherSamplePoint> pisteet) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        ArrayNode featureArray = mapper.createArrayNode();

        for (WeatherSamplePoint p : pisteet) {
            ObjectNode feature = mapper.createObjectNode();
            feature.put("type", "Feature");

            // Geometry (Point)
            ObjectNode geometry = mapper.createObjectNode();
            geometry.put("type", "Point");

            ArrayNode coords = mapper.createArrayNode();
            coords.add(p.getLon());  // GeoJSON: lon, lat
            coords.add(p.getLat());
            geometry.set("coordinates", coords);

            // Properties
            ObjectNode properties = mapper.createObjectNode();
            properties.put("ennuste", p.getEnnusteTeksti());

            feature.set("geometry", geometry);
            feature.set("properties", properties);

            featureArray.add(feature);
        }

        root.put("type", "FeatureCollection");
        root.set("features", featureArray);

        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File("saa_pisteet.geojson"), root);
            System.out.println("✅ Sääpisteet tallennettu: saa_pisteet.geojson");
        } catch (IOException e) {
            System.err.println("❌ Sääpisteiden tallennus epäonnistui: " + e.getMessage());
        }
    }

}

