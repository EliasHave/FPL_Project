package FPL_Code;

import FPL_Code.Aircraft;
import FPL_Code.Weather;
import FPL_Code.Notam;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.locationtech.jts.io.ParseException;

import java.io.File;
import java.io.IOException;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static FPL_Code.Point.etsiKoordinaatit;

/**
 * luokka joka laskee tekoälyn avulla lentosuunnitelmia
 * Käyttää kaiken mahdollisen tiedon joka koneesta, säästä sekä notemseista irtoaa
 * collabs: Aircraft, Notam, Weather
 */
public class FlightPlanner {

    private String maaranpaaKentta;
    private Weather saaMaapanpaa;
    private Weather saaLahto;
    private Aircraft kone;
    private String notam;
    private Pilot pilot;

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


    public void setPilot(Pilot pilot) {
        this.pilot = pilot;
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


    public Pilot getPilot() {
        return pilot;
    }

//--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    public static class Feature {
        public JsonNode geometry;
        public JsonNode properties;

        public Feature(JsonNode geometry, JsonNode properties) {
            this.geometry = geometry;
            this.properties = properties;
        }
    }


    public class WeatherSamplePoint {
        private double lat;
        private double lon;
        private ZonedDateTime aika;
        private String ennusteTeksti = ""; // tähän voi myöhemmin laittaa sääkuvauksen/metarin jne.
        private Map<String, Object> forecastData;

        public WeatherSamplePoint(double lat, double lon) {
            this.lat = lat;
            this.lon = lon;
        }

        public double getLat() { return lat; }
        public double getLon() { return lon; }
        public ZonedDateTime getAika() { return aika; }
        public String getEnnusteTeksti() { return ennusteTeksti; }

        public void setEnnusteTeksti(String teksti) {
            this.ennusteTeksti = teksti;
        }

        public void setAika(ZonedDateTime aika) {
            this.aika = aika;
        }


        public void setForecastData(Map<String, Object> data) {
            this.forecastData = data;
        }

        public Map<String, Object> getForecastData() {
            return forecastData;
        }

        @Override
        public String toString() {
            return String.format("Lat: %.5f, Lon: %.5f, Ennuste: %s", lat, lon, ennusteTeksti);
        }
    }


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

        suodataIlmatilatJaKirjoitaGeoJson(olennaisetIlmatilat);

        suodataLentokentat(lahtoPiste, maaranpaaPiste);
        suodataNavaidit(lahtoPiste, maaranpaaPiste);

        List<WeatherSamplePoint> saanMittausPisteet = kartoitaSaaReitilla(lahtoPiste, maaranpaaPiste);

        return reittiPisteet;

    }


    /**
     * Asettaa määränpääkentän sään
     * @param lahtoKoord
     * @param maaranpaaKoord
     */
    public void asetaMaaranpaanSaa(Point lahtoKoord, Point maaranpaaKoord) {
        // tässä täytyy tehdä määränpään sään hakeminen ja asettaminen koska sitä ei voi tehdä RouteControllerissa
        // sillä siellä ei vielä tiedetä koneen nopeutta joka tarvitaan että voidaan arvioida monen aikaan sää kannattaa ennustaa
        // tässä kohdassa tiedetään koneen nopeus ja pystytään arvioimaan moneltako sää kannattaa ennustaa sekä päästään käsiksi weather olioon
        // aika = nykyaika + matka/nopeus
        // Weather maaranpaanSaa = haeSaaOlio(maaranpaaKentta, aika)

        ZonedDateTime lahtoHetki = saaLahto.getAjankohtaZDT();

        // Lasketaan matkustusaika Duration-oliona
        double matkaKM = haversineKm(lahtoKoord.getLat(), lahtoKoord.getLon(), maaranpaaKoord.getLat(), maaranpaaKoord.getLon());
        double nopeusKMh = kone.getCruiseSpeed() * 1.852;
        long minuutit = Math.round((matkaKM / nopeusKMh) * 60);
        Duration kesto = Duration.ofMinutes(minuutit);

        // Arvioitu saapumisaika = lähtöaika + lentoaika
        ZonedDateTime lahtoHetkiUTC = lahtoHetki.withZoneSameInstant(ZoneOffset.UTC);
        ZonedDateTime arvioituSaapumisaika = lahtoHetkiUTC.plus(kesto);

        // String aikaStr = arvioituSaapumisaika.format(DateTimeFormatter.ofPattern("HH:mm"));

        Weather maapanpaanSaa = Weather.haeSaaOlio(maaranpaaKentta, arvioituSaapumisaika);
        this.saaMaapanpaa = maapanpaanSaa;
    }


    /**
     * suodatta parametrina tulevasta fetaures (ilmatilat) listasta turhat kentätä pois ja kirjoittaa uuden geojson tiedoston näistä ilmatiloista
     * @param olennaisetIlmatilat Ilmatilojen lista jonka ilmatiloista halutaan karsia turhat tiedot pois
     */
    public void suodataIlmatilatJaKirjoitaGeoJson(List<Feature> olennaisetIlmatilat) {
        // karsitaan turhat tiedot
        List<Feature> tiivistetyt = new ArrayList<>();
        for (Feature f : olennaisetIlmatilat) {
            tiivistetyt.add(karsiIlmatilanProperties(f));
        }
        kirjoitaGeoJson("suodatetutIlmatilat2.geojson", tiivistetyt);
    }


    /**
     * karsii parametrina tulevasta fetaure (ilmatila) oliosta turha kentät pois ja palauttaa karsitun feature olion
     * @param alkuperainen alkuperäinen olio josta katsotaan mitä tietoja uuteen karsittuun olioon jätetään
     * @return palauttaa uuden feature olion joka on karisittu veriso alkuperäisestä
     */
    public Feature karsiIlmatilanProperties(Feature alkuperainen) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode props = mapper.createObjectNode();

        // Nimi ja luokka
        props.put("name", alkuperainen.properties.path("name").asText(""));

        int icaoTyyppi = alkuperainen.properties.path("icaoClass").asInt(-1);
        String icaoClass = switch (icaoTyyppi) {
            case 0  -> "A";
            case 1  -> "B";
            case 2  -> "C";
            case 3  -> "D";
            case 4  -> "E";
            case 5  -> "F";
            case 6  -> "G";
            case 8  -> "Unclassified / Special Use Airspace (SUA)";
            default -> "tuntematon";
        };
        props.put("icaoClass", icaoClass);

        int tyyppi = alkuperainen.properties.path("type").asInt(-1);
        String tyyppiKirjain = switch (tyyppi) {
            case 0  -> "Other";
            case 1  -> "Restricted";
            case 2  -> "Danger";
            case 3  -> "Prohibited";
            case 4  -> "Controlled Tower Region (CTR)";
            case 5  -> "Transponder Mandatory Zone (TMZ)";
            case 6  -> "Radio Mandatory Zone (RMZ)";
            case 7  -> "Terminal Maneuvering Area (TMA)";
            case 8  -> "Temporary Reserved Area (TRA)";
            case 9  -> "Temporary Segregated Area (TSA)";
            case 10 -> "Flight Information Region (FIR)";
            case 11 -> "Upper Flight Information Region (UIR)";
            case 12 -> "Air Defense Identification Zone (ADIZ)";
            case 13 -> "Airport Traffic Zone (ATZ)";
            case 14 -> "Military Airport Traffic Zone (MATZ)";
            case 15 -> "Airway";
            case 16 -> "Military Training Route (MTR)";
            case 17 -> "Alert Area";
            case 18 -> "Warning Area";
            case 19 -> "Protected Area";
            case 20 -> "Helicopter Traffic Zone (HTZ)";
            case 21 -> "Gliding Sector";
            case 22 -> "Transponder Setting (TRP)";
            case 23 -> "Traffic Information Zone (TIZ)";
            case 24 -> "Traffic Information Area (TIA)";
            case 25 -> "Military Training Area (MTA)";
            case 26 -> "Control Area (CTA)";
            case 27 -> "ACC Sector (ACC)";
            case 28 -> "Aerial Sporting Or Recreational Activity";
            case 29 -> "Low Altitude Overflight Restriction";
            case 30 -> "Military Route (MRT)";
            case 31 -> "TSA/TRA Feeding Route (TFR)";
            case 32 -> "VFR Sector";
            case 33 -> "FIS Sector";
            case 34 -> "Lower Traffic Area (LTA)";
            case 35 -> "Upper Traffic Area (UTA)";
            case 36 -> "Military Controlled Tower Region (MCTR)";
            default -> "tuntematon";
        };
        props.put("type", tyyppiKirjain);

        // Korkeudet
        props.set("lowerLimit", muodostaKorkeusNode(alkuperainen.properties.path("lowerLimit")));
        props.set("upperLimit", muodostaKorkeusNode(alkuperainen.properties.path("upperLimit")));

        // byNotam
        if (alkuperainen.properties.path("byNotam").asBoolean(false)) {
            props.put("byNotam", true);
        }

        // Aukioloajat kuten ennen
        JsonNode hours = alkuperainen.properties.path("hoursOfOperation").path("operatingHours");
        if (hours.isArray()) {
            boolean kaikkiStandardia = true;
            for (JsonNode h : hours) {
                if (!h.path("startTime").asText("").equals("00:00") ||
                        !h.path("endTime").asText("").equals("00:00") ||
                        h.path("byNotam").asBoolean(false) ||
                        h.path("sunrise").asBoolean(false) ||
                        h.path("sunset").asBoolean(false) ||
                        h.path("publicHolidaysExcluded").asBoolean(false)) {
                    kaikkiStandardia = false;
                    break;
                }
            }

            if (kaikkiStandardia) {
                props.put("hoursOfOperation", "24/7");
            } else {
                ArrayNode slimmedHours = mapper.createArrayNode();
                for (JsonNode h : hours) {
                    ObjectNode d = mapper.createObjectNode();
                    d.put("dayOfWeek", h.path("dayOfWeek").asInt());
                    d.put("startTime", h.path("startTime").asText());
                    d.put("endTime", h.path("endTime").asText());
                    if (h.path("byNotam").asBoolean(false)) d.put("byNotam", true);
                    if (h.path("sunrise").asBoolean(false)) d.put("sunrise", true);
                    if (h.path("sunset").asBoolean(false)) d.put("sunset", true);
                    if (h.path("publicHolidaysExcluded").asBoolean(false)) d.put("publicHolidaysExcluded", true);
                    slimmedHours.add(d);
                }
                ObjectNode hoursWrapper = mapper.createObjectNode();
                hoursWrapper.set("operatingHours", slimmedHours);
                props.set("hoursOfOperation", hoursWrapper);
            }
        }

        return new Feature(alkuperainen.geometry, props);
    }


    private ObjectNode muodostaKorkeusNode(JsonNode korkeus) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.put("value", korkeus.path("value").asInt(-1));

        int yksikko = korkeus.path("unit").asInt(-1);
        String yksikkoStr = switch (yksikko) {
            case 1 -> "ft MSL";
            case 6 -> "FL";
            default -> "tuntematon";
        };
        node.put("unit", yksikkoStr);

        return node;
    }


    /**
     * Suodattaa pois kaikki turhat ilmatilat joita ei tarvita reitillä
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
     * Lataa ilmatilat geoJson tiedostosta ja tekee niistä Feature olion sekä tallentaa oliot listaan joka palutetaan
     * @return palauttaa listan johon nämä luodut feature oliot on lisätty
     */
    public List<Feature> lataaIlmatilatGeoJsonista() {
        List<Feature> features = new ArrayList<>();

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(new File("fi_asp.geojson"));

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
     * Suodattaa reitin kannalta olennaiset lentokentät GeoJSONista.
     */
    public void suodataLentokentat(Point lahtoPiste, Point maaranpaaPiste) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(new File("fi_apt.geojson"));  // oikea tiedosto

            List<Feature> kaikkiKentat = new ArrayList<>();
            JsonNode features = root.get("features");

            for (JsonNode f : features) {
                /**
                JsonNode geom = f.get("geometry");
                JsonNode props = f.get("properties");
                 **/
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
                tiivistetyt.add(karsiLentokentanProperties(f));
            }

            kirjoitaGeoJson("suodatetutLentokentat.geojson", tiivistetyt);

        } catch (IOException e) {
            System.err.println("❌ Lentokenttien suodatus epäonnistui: " + e.getMessage());
        }
    }

    /**
     * Karsii lentokenttä-Featuresta pois tekoälyn kannalta epäolennaiset tiedot ja palauttaa uuden Feature-olion.
     */
    private Feature karsiLentokentanProperties(Feature alkuperainen) {
        ObjectMapper mapper = new ObjectMapper();

        ObjectNode slimProps = mapper.createObjectNode();
        slimProps.put("name", alkuperainen.properties.path("name").asText(""));
        slimProps.put("icaoCode", alkuperainen.properties.path("icaoCode").asText(""));
        // Lentokentän tyyppi selkokielellä
        int kenttaTyyppi = alkuperainen.properties.path("type").asInt(-1);
        String kenttaTyyppiNimi = switch (kenttaTyyppi) {
            case 0 -> "Airport (civil/military)";
            case 1 -> "Glider Site";
            case 2 -> "Civil Airfield";
            case 3 -> "International Airport";
            case 4 -> "Heliport Military";
            case 5 -> "Military Airfield";
            case 6 -> "Ultra Light Airfield";
            case 8 -> "Closed Airfield";
            case 9 -> "Airport resp. Airfield IFR";
            case 10 -> "Airfield Water";
            case 11 -> "Landing Strip";
            case 12 -> "Agricultural Landing Strip";
            case 13 -> "Altiport";
            default -> "tuntematon";
        };
        slimProps.put("type", kenttaTyyppiNimi);

        // Traffic type (0 = VFR, 1 = IFR, 2 = VFR+IFR)
        ArrayNode trafficArray = (ArrayNode) alkuperainen.properties.path("trafficType");
        List<String> liikenneTyypit = new ArrayList<>();
        for (JsonNode t : trafficArray) {
            switch (t.asInt()) {
                case 0 -> liikenneTyypit.add("VFR");
                case 1 -> liikenneTyypit.add("IFR");
                case 2 -> liikenneTyypit.add("VFR + IFR");
            }
        }
        if (!liikenneTyypit.isEmpty()) {
            slimProps.put("trafficType", String.join(", ", liikenneTyypit));
        }

        // Elevation mukaan + yksikkö selkokielellä
        JsonNode elevation = alkuperainen.properties.path("elevation");
        if (!elevation.isMissingNode()) {
            ObjectNode elev = mapper.createObjectNode();
            elev.put("value", elevation.path("value").asInt());

            int yksikko = elevation.path("unit").asInt(-1);
            String yksikkoStr = switch (yksikko) {
                case 0 -> "m MSL";
                case 1 -> "ft MSL";
                default -> "tuntematon";
            };
            elev.put("unit", yksikkoStr);

            slimProps.set("elevation", elev);
        }

        // PPR, vain jos true
        if (alkuperainen.properties.path("ppr").asBoolean(false)) {
            slimProps.put("ppr", true);
        }

        // Skydive, Winch, jne.
        if (alkuperainen.properties.path("skydiveActivity").asBoolean(false)) {
            slimProps.put("skydive", true);
        }
        if (alkuperainen.properties.path("winchOnly").asBoolean(false)) {
            slimProps.put("winchOnly", true);
        }

        // Radiotaajuudet (vain yksi tärkein)
        JsonNode freqs = alkuperainen.properties.path("frequencies");
        if (freqs.isArray() && freqs.size() > 0) {
            for (JsonNode f : freqs) {
                if (f.path("primary").asBoolean(true)) {
                    ObjectNode freq = mapper.createObjectNode();
                    freq.put("name", f.path("name").asText());
                    freq.put("value", f.path("value").asText());
                    slimProps.set("frequency", freq);
                    break;
                }
            }
        }

        // Kiitotiet
        JsonNode runways = alkuperainen.properties.path("runways");
        if (runways.isArray() && runways.size() > 0) {
            ArrayNode uusiRunwayt = mapper.createArrayNode();
            for (JsonNode rw : runways) {
                ObjectNode r = mapper.createObjectNode();
                r.put("designator", rw.path("designator").asText());
                r.put("heading", rw.path("trueHeading").asInt());

                // Pinta (yksinkertaistettu)
                int materialCode = rw.path("surface").path("mainComposite").asInt(-1);
                String pinta = switch (materialCode) {
                    case 0 -> "asfaltti";
                    case 1 -> "betoni";
                    case 2 -> "nurmi";
                    case 5 -> "sora";
                    case 12 -> "päällystetty";
                    default -> "tuntematon";
                };
                r.put("surface", pinta);

                // Mitat
                JsonNode dim = rw.path("dimension");
                ObjectNode mitat = mapper.createObjectNode();
                mitat.put("length_m", dim.path("length").path("value").asInt(-1));
                mitat.put("width_m", dim.path("width").path("value").asInt(-1));
                r.set("size", mitat);

                // Poikkeavuudet
                if (rw.path("pilotCtrlLighting").asBoolean(false)) {
                    r.put("pilotCtrlLighting", true);
                }
                if (rw.path("takeOffOnly").asBoolean(false)) {
                    r.put("takeoffOnly", true);
                }
                if (rw.path("landingOnly").asBoolean(false)) {
                    r.put("landingOnly", true);
                }

                uusiRunwayt.add(r);
            }
            slimProps.set("runways", uusiRunwayt);
        }

        return new Feature(alkuperainen.geometry, slimProps);
    }


    /**
     * Suodattaa navaidit reitin läheltä.
     */
    public void suodataNavaidit(Point lahtoPiste, Point maaranpaaPiste) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(new File("fi_nav.geojson"));

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
                tiivistetyt.add(karsiNavaidinProperties(f));
            }

            kirjoitaGeoJson("suodatetutNavaidit.geojson", tiivistetyt);

        } catch (IOException e) {
            System.err.println("❌ Navaidien suodatus epäonnistui: " + e.getMessage());
        }
    }


    /**
     * aliohjelma joka karsii navaidien properties osiosta turhat tiedot pois
     * @param alkuperainen
     * @return
     */
    private Feature karsiNavaidinProperties(Feature alkuperainen) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode props = mapper.createObjectNode();

        // Perustiedot
        props.put("name", alkuperainen.properties.path("name").asText(""));
        props.put("identifier", alkuperainen.properties.path("identifier").asText(""));

        // Tyyppi ja selite
        int typeCode = alkuperainen.properties.path("type").asInt(-1);
        props.put("type", typeCode);

        String typeName = switch (typeCode) {
            case 0 -> "DME";
            case 1 -> "TACAN";
            case 2 -> "NDB";
            case 3 -> "VOR";
            case 4 -> "VOR-DME";
            case 5 -> "VORTAC";
            case 6 -> "DVOR";
            case 7 -> "DVOR-DME";
            case 8 -> "DVORTAC";
            default -> "tuntematon";
        };
        props.put("typeName", typeName);

        // Taajuus (frequency)
        JsonNode freq = alkuperainen.properties.path("frequency");
        if (!freq.isMissingNode()) {
            ObjectNode f = mapper.createObjectNode();
            f.put("value", freq.path("value").asText(""));

            String unitStr = switch (freq.path("unit").asInt(-1)) {
                case 1 -> "kHz";
                case 2 -> "MHz";
                default -> "tuntematon";
            };
            f.put("unit", unitStr);

            props.set("frequency", f);
        }

        // Kanava
        if (alkuperainen.properties.has("channel")) {
            props.put("channel", alkuperainen.properties.path("channel").asText());
        }

        // Korkeus
        JsonNode elevation = alkuperainen.properties.path("elevation");
        if (!elevation.isMissingNode()) {
            ObjectNode elev = mapper.createObjectNode();
            elev.put("value", elevation.path("value").asInt(-1));
            elev.put("unit", elevation.path("unit").asInt(-1));
            props.set("elevation", elev);
        }

        // Kantama (range)
        JsonNode range = alkuperainen.properties.path("range");
        if (!range.isMissingNode()) {
            ObjectNode r = mapper.createObjectNode();
            r.put("value", range.path("value").asInt(-1));

            String unitStr = switch (range.path("unit").asInt(-1)) {
                case 2 -> "NM";
                default -> "tuntematon";
            };
            r.put("unit", unitStr);

            props.set("range", r);
        }

        // HoursOfOperation – jätetään vain jos ei ole täysin oletusarvo (00:00-00:00 joka päivä)
        JsonNode hours = alkuperainen.properties.path("hoursOfOperation").path("operatingHours");
        if (hours.isArray()) {
            boolean onPoikkeavaa = false;

            for (JsonNode h : hours) {
                if (!h.path("startTime").asText().equals("00:00") ||
                        !h.path("endTime").asText().equals("00:00") ||
                        h.path("byNotam").asBoolean(false) ||
                        h.path("sunrise").asBoolean(false) ||
                        h.path("sunset").asBoolean(false) ||
                        h.path("publicHolidaysExcluded").asBoolean(false)) {
                    onPoikkeavaa = true;
                    break;
                }
            }

            if (onPoikkeavaa) {
                props.set("hoursOfOperation", hours);
            }
        }

        // (Poistetaan: _id, createdAt, updatedAt, elevationGeoid jne.)
        return new Feature(alkuperainen.geometry, props);
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
     * kirjoittaa parametrina tulevasta features listasta geoJson tiedoston. Ei karsi enää tässä vaiheessa mitään pois vaan kirjoittaa kaiken mitä fetaures listassa on
     * @param features
     */
    public void kirjoitaGeoJson(String tiedNimi, List<Feature> features) {
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

        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(tiedNimi), root);
            System.out.println("✅ Suodatettu GeoJSON tallennettu: " + tiedNimi);
        } catch (IOException e) {
            System.err.println("❌ GeoJSON-tiedoston tallennus epäonnistui: " + e.getMessage());
        }
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

        haeSaat(pisteet);

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
            try {
                double lat = p.getLat();
                double lon = p.getLon();

                // Haetaan suoraan ZonedDateTime-oliona
                ZonedDateTime aika = p.getAika(); // oletetaan että tämä ei ole null
                ZonedDateTime utc = aika.withZoneSameInstant(ZoneOffset.UTC);

                // Pyöristetään lähimpään tuntiin
                int minuutit = utc.getMinute();
                ZonedDateTime pyoristetty;
                if (minuutit >= 30) {
                    // Jos yli puolen tunnin → seuraava tunti
                    pyoristetty = utc.truncatedTo(ChronoUnit.HOURS).plusHours(1);
                } else {
                    // Muuten → alas tuntiin
                    pyoristetty = utc.truncatedTo(ChronoUnit.HOURS);
                }

                // Formatoidaan
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
                String utcAika = pyoristetty.format(formatter);

                System.out.println("🌐 WeatherSamplePoint.getAika(): " + aika);
                System.out.println("🔄 Muunnettu UTC-aika: " + utcAika);

                // API-kutsu
                String url = String.format(
                        Locale.US,
                        "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&hourly=temperature_2m,cloudcover,visibility,windspeed_10m,winddirection_10m,precipitation,relative_humidity_2m,pressure_msl,dew_point_2m&timezone=UTC",
                        lat, lon
                );

                JsonNode root = mapper.readTree(new java.net.URL(url));
                JsonNode tuntiLista = root.path("hourly");
                JsonNode ajat = tuntiLista.path("time");

                if (!tehty) {
                    System.out.println("📅 Saatavilla olevat ajat:");
                    for (JsonNode a : ajat) {
                        System.out.println("  - " + a.asText());
                    }
                    tehty = true;
                }

                int indeksi = -1;
                for (int i = 0; i < ajat.size(); i++) {
                    if (ajat.get(i).asText().equals(utcAika)) {
                        indeksi = i;
                        break;
                    }
                }

                if (indeksi == -1) {
                    p.setEnnusteTeksti("❌ Sääennuste puuttuu ajalle " + utcAika);
                    continue;
                }

                // Haetaan arvot
                double temp = tuntiLista.path("temperature_2m").get(indeksi).asDouble();
                double dew = tuntiLista.path("dew_point_2m").get(indeksi).asDouble();
                double pilvikorkeusFt = (temp - dew) * 400.0;
                int pilviFt = (int) Math.round(pilvikorkeusFt);

                double pilvisyys = tuntiLista.path("cloudcover").get(indeksi).asDouble();
                double visibility = tuntiLista.path("visibility").get(indeksi).asDouble();
                double wind = tuntiLista.path("windspeed_10m").get(indeksi).asDouble();
                double windDir = tuntiLista.path("winddirection_10m").get(indeksi).asDouble();
                double sade = tuntiLista.path("precipitation").get(indeksi).asDouble();
                double humidity = tuntiLista.path("relative_humidity_2m").get(indeksi).asDouble();
                double paine = tuntiLista.path("pressure_msl").get(indeksi).asDouble();

                // Rakennetaan strukturoitu data Map<String, Object>
                Map<String, Object> forecast = new LinkedHashMap<>();
                forecast.put("utcTime", utcAika);
                forecast.put("temperature_C", temp);
                forecast.put("dewPoint_C", dew);
                forecast.put("cloudBase_ft", pilviFt);
                forecast.put("cloudCover_pct", pilvisyys);
                forecast.put("visibility_m", visibility);
                forecast.put("wind_mps", wind);
                forecast.put("windDirection_deg", windDir);
                forecast.put("precip_mm", sade);
                forecast.put("humidity_pct", humidity);
                forecast.put("pressure_hPa", paine);

                p.setForecastData(forecast);

                // Rakennetaan ennusteteksti
                StringBuilder sb = new StringBuilder();
                sb.append("Aika (UTC): ").append(utcAika).append("\n");
                sb.append("Lämpötila: ").append(temp).append(" °C\n");
                sb.append("Kastepiste: ").append(dew).append(" °C\n");
                sb.append("Pilvikorkeus (laskennallinen): ").append(pilviFt).append(" ft AGL\n");
                sb.append("Pilvisyys: ").append(tuntiLista.path("cloudcover").get(indeksi).asText()).append(" %\n");
                sb.append("Näkyvyys: ").append(tuntiLista.path("visibility").get(indeksi).asText()).append(" m\n");
                sb.append("Tuuli: ").append(tuntiLista.path("windspeed_10m").get(indeksi).asText()).append(" m/s\n").append(" Suunta: ").append(tuntiLista.path("winddirection_10m").get(indeksi).asText()).append(" Deg\n");
                sb.append("Sade: ").append(tuntiLista.path("precipitation").get(indeksi).asText()).append(" mm\n");
                sb.append("Ilmankosteus: ").append(tuntiLista.path("relative_humidity_2m").get(indeksi).asText()).append(" %\n");
                sb.append("Ilmanpaine: ").append(tuntiLista.path("pressure_msl").get(indeksi).asText()).append(" hPa");

                p.setEnnusteTeksti(sb.toString());

            } catch (Exception e) {
                p.setEnnusteTeksti("⚠️ Säänhakuvirhe: " + e.getMessage());
            }
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

