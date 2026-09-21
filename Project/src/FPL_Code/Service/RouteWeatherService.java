package FPL_Code.Service;

import FPL_Code.Model.Point;
import FPL_Code.Model.WeatherSamplePoint;
import FPL_Code.Model.WeatherSamplePoint;
import FPL_Code.Utility.GeoMathUtils;
import FPL_Code.Utility.TimeUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Sääpalvelu joka vastaa sään hakemisesta mutta ei itse säilö mitään säätietoja
 */
@Service
public class RouteWeatherService {

    private static final double REITTIPISTE_VALI_KM = 30.0;

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();


    /**
     * Päämetodi joka orkestroi koko säänhaku prosessin reitille
     * @param lahto Point olio joka sisältää koordinaatit. Tätä käytetään reitin lähtöpisteenä
     * @param kohde Point olio joka sisältää koordinaatit. Tätä käytetään reitin päätepisteenä
     * @param lahtoAika Lähtöaika joka on utc vyöhykkeinen ja jota käytetään reitin ajastamiseen. Lähtöpisteessä ollaan tähän aikaan ja matka alkaa
     * @param koneenNopeus koneen nopeus solmuissa. Tätä käytetään laskemaan aika arvioita eri paikkoihin jotta sää osataan ennustaa oikeaan aikaan
     * @return Palauttaa listan WeatherSamplePoint olioita jotka sisältää säätietoja reittipistessä
     */
    public List<WeatherSamplePoint> kartoitaReitinSaa(Point lahto, Point kohde, String lahtoAika, int koneenNopeus) {
        List<WeatherSamplePoint> kaikkiSaat = new ArrayList<>();

        // 1. AIKA- JA ETÄISYYSLASKELMAT
        // Muutetaan string-aika oikeaksi ZonedDateTimeksi
        ZonedDateTime lahtoAikaUTC = TimeUtils.parseIlmailuAika(lahtoAika);

        // Lasketaan kokonaismatka ja arvioitu lentoaika (ETA)
        double kokonaisMatkaKm = GeoMathUtils.haversineKm(lahto.getLat(), lahto.getLon(), kohde.getLat(), kohde.getLon()); // TODO: tässä voisi suoraan käyttää vain solmuja ja maileja eikä kilometrejä ja km/h
        double nopeusKmh = koneenNopeus * 1.852; // Solmut kilometreiksi tunnissa
        double lentoAikaTunteina = kokonaisMatkaKm / nopeusKmh;

        // Määränpään arvioitu saapumisaika (Lähtöaika + lentoaika)
        long lentoAikaSekunteina = (long) (lentoAikaTunteina * 3600);
        ZonedDateTime saapumisAikaUTC = lahtoAikaUTC.plusSeconds(lentoAikaSekunteina);

        System.out.println("Sääkartoitus alkaa. Matka: " + kokonaisMatkaKm + " km, Lentoaika: " + lentoAikaTunteina + " h");

        // 2. LÄHTÖKENTÄN SÄÄ
        // Haetaan lähtökentän sää lähtöajalle
        WeatherSamplePoint lahtoSaa = haeSaaKentalle(lahto, lahtoAikaUTC, WeatherSamplePoint.PointType.DEPARTURE);
        kaikkiSaat.add(lahtoSaa);

        // 3. REITTIPISTEIDEN SÄÄ (En-route)
        // Luodaan grid/buffer-pisteet ja haetaan niille sää
        List<WeatherSamplePoint> reittiSaat = kartoitaReittiPisteet(lahto, kohde, lahtoAikaUTC, nopeusKmh, kokonaisMatkaKm);
        kaikkiSaat.addAll(reittiSaat);

        // 4. MÄÄRÄNPÄÄN SÄÄ
        // Haetaan kohdekentän sää lasketulle saapumisajalle
        WeatherSamplePoint kohdeSaa = haeSaaKentalle(kohde, saapumisAikaUTC, WeatherSamplePoint.PointType.DESTINATION);
        kaikkiSaat.add(kohdeSaa);

        return kaikkiSaat;
    }


    /**
     * Metodi joka hakee säätiedot parametrina tulevalle paikalle (kenttä) ja parametrina tulevaan ajankohtaan
     * Tekee tunnin sisällä nykyhetkestä sijoittuvat haut Metar hakuna ja yli tunnin päästä sijoittuvat OpenMeteon ennustuksena
     * @param kentta Point olio josta sää haetaan. Sisältää koordinaatit ja kentän ICAO koodin
     * @param ajankohta ZonedDateTime olio joka kertoo UTC ajassa milloin pisteeseen saavutaan
     * @return Palauttaa WeatherSamplePoint olion joka sisältää säätiedot
     */
    public WeatherSamplePoint haeSaaKentalle(Point kentta, ZonedDateTime ajankohta, WeatherSamplePoint.PointType tyyppi) {
        ZonedDateTime nyt = ZonedDateTime.now();
        long tuntiErotus = Duration.between(nyt, ajankohta).toHours();

        // Jos aika on alle 1 tunnin päässä nykyhetkestä, yritetään ensin hakea virallinen METAR
        if (Math.abs(tuntiErotus) <= 1) {
            System.out.println("Haetaan METAR kentälle: " + kentta.getPointName());
            try {
                return haeMetarSaa(kentta, ajankohta, tyyppi);
            } catch (Exception e) {
                System.err.println("METARin haku epäonnistui kentälle " + kentta.getPointName() + ", varaudutaan OpenMeteoon.");
                // Jos METAR epäonnistuu (esim. pieni korpikenttä, jolla ei ole sääasemaa), jatketaan alaspäin OpenMeteoon.
            }
        }

        // Muussa tapauksessa (tai METARin failatessa) haetaan OpenMeteo-ennuste
        System.out.println("Haetaan OpenMeteo-ennuste kentälle: " + kentta.getPointName() + " ajalle " + ajankohta);
        return haeOpenMeteoSaa(kentta.getPointName(), kentta.getLat(), kentta.getLon(), ajankohta, tyyppi);
    }


    /**
     * Metodi joka luo reittipisteet lähtöpisteen ja kohdepisteen välille ja hakee niille säätiedot OpenMeteosta.
     * Haetaan pisteet 30 km välein ja jokaiselle pisteelle haetaan sää ennustettuna saapumisajankohtana.
     * Sää haetaan myös 30 km vasemmalle ja oikealle reittipisteestä, jotta saadaan kattava kuva reitin säästä. (Buffer)
     * @param lahto Point olio joka sisältää lähtöpisteen koordinaatit (ja nimen)
     * @param kohde Point olio joka sisältää kohdepisteen koordinaatit (ja nimen)
     * @param lahtoAika ZonedDateTime olio joka kertoo UTC ajassa milloin lähtöpisteessä ollaan
     * @param nopeusKmh koneen nopeus kilometreinä tunnissa. Tätä käytetään laskemaan ETA eri reittipisteisiin
     * @param kokonaisMatkaKm kokonaismatka kilometreinä lähtöpisteestä kohdepisteeseen. Tätä käytetään laskemaan kuinka monta reittipistettä tarvitaan
     * @return Palauttaa listan WeatherSamplePoint olioita jotka sisältää säätiedot reittipisteille
     */
    private List<WeatherSamplePoint> kartoitaReittiPisteet(Point lahto, Point kohde, ZonedDateTime lahtoAika, double nopeusKmh, double kokonaisMatkaKm) {
        List<WeatherSamplePoint> reittiSaat = new ArrayList<>();

        // Lasketaan suuntima (Bearing) lähtöpisteestä kohteeseen
        double suuntaRad = GeoMathUtils.laskeSuuntaRad(lahto.getLat(), lahto.getLon(), kohde.getLat(), kohde.getLon());

        double kuljettuMatkaKm = REITTIPISTE_VALI_KM;
        int pisteNro = 1;

        // Siirretään pistettä eteenpäin, kunnes ollaan perillä
        while (kuljettuMatkaKm < kokonaisMatkaKm) {
            // 1. Lasketaan uuden pisteen koordinaatit
            Point reittiPiste = GeoMathUtils.siirraKoordinaattia(lahto.getLat(), lahto.getLon(), kuljettuMatkaKm, suuntaRad);
            Point reittiPisteVasen = GeoMathUtils.siirraKoordinaattia(reittiPiste.getLat(), reittiPiste.getLon(), REITTIPISTE_VALI_KM, suuntaRad - Math.PI / 2);
            Point reittiPisteOikea = GeoMathUtils.siirraKoordinaattia(reittiPiste.getLat(), reittiPiste.getLon(), REITTIPISTE_VALI_KM, suuntaRad + Math.PI / 2);

            // 2. Lasketaan arvioitu saapumisaika tähän pisteeseen (ETA)
            double tunnitTahanPisteeseen = kuljettuMatkaKm / nopeusKmh;
            ZonedDateTime etaTahanPisteeseen = lahtoAika.plusSeconds((long) (tunnitTahanPisteeseen * 3600));

            // 3. Haetaan pisteen sää OpenMeteosta
            String pisteNimi = "WP" + pisteNro + "C (" + (int)kuljettuMatkaKm + "km)";
            WeatherSamplePoint pisteSaa = haeOpenMeteoSaa(pisteNimi, reittiPiste.getLat(), reittiPiste.getLon(), etaTahanPisteeseen, WeatherSamplePoint.PointType.WAYPOINT);
            reittiSaat.add(pisteSaa);

            pisteNimi = "WP" + pisteNro + "L (" + (int)kuljettuMatkaKm + "km)";
            WeatherSamplePoint pisteVasenSaa = haeOpenMeteoSaa(pisteNimi, reittiPisteVasen.getLat(), reittiPisteVasen.getLon(), etaTahanPisteeseen, WeatherSamplePoint.PointType.WAYPOINT);
            reittiSaat.add(pisteVasenSaa);

            pisteNimi = "WP" + pisteNro + "R (" + (int)kuljettuMatkaKm + "km)";
            WeatherSamplePoint pisteOikeaSaa = haeOpenMeteoSaa(pisteNimi, reittiPisteOikea.getLat(), reittiPisteOikea.getLon(), etaTahanPisteeseen, WeatherSamplePoint.PointType.WAYPOINT);
            reittiSaat.add(pisteOikeaSaa);

            kuljettuMatkaKm += REITTIPISTE_VALI_KM;
            pisteNro++;
        }

        return reittiSaat;
    }


    /**
     * Hakee METAR säätiedot kyseiselle koordinaatille (lentokentälle).
     * Palauttaa uuden WeatherSamplePoint olion joka sisältää säätiedot.
     * @param kentta Point olio joka sisältää koordinaatit ja kentän ICAO koodin (ICAO koodia käytetään METAR-haussa)
     * @param kohdeAika ZonedDateTime olio joka kertoo UTC ajassa milloin pisteeseen saavutaan (tämä annetaan WeatherSamplePoint olioon, mutta METAR-haussa ei käytetä tätä koska METAR on aina nykyhetken sää)
     * @param tyyppi Tyyppi joka kertoo onko kyseessä lähtö, määränpää vai reittipiste
     * @return Palauttaa uuden WeatherSamplePoint olion joka sisältää säätiedot kyseiselle kentälle
     */
    private WeatherSamplePoint haeMetarSaa(Point kentta, ZonedDateTime kohdeAika, WeatherSamplePoint.PointType tyyppi) {
        WeatherSamplePoint piste = new WeatherSamplePoint(tyyppi, kentta.getPointName(), kentta.getLat(), kentta.getLon(), kohdeAika);
        piste.setLahde(WeatherSamplePoint.DataSource.METAR);

        try {
            String url = "https://aviationweather.gov/api/data/metar?ids=" + kentta.getPointName() + "&format=raw&hours=0";
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 && !response.body().trim().isEmpty()) {
                String raakaMetar = response.body().trim();
                piste.setRaakaMetar(raakaMetar);

                // Parsitaan sähke (Delegoitu yksityiselle apumetodille luettavuuden takia)
                parsiMetarString(raakaMetar, piste);
            } else {
                throw new RuntimeException("Tyhjä tai viallinen METAR-vastaus");
            }
        } catch (Exception e) {
            throw new RuntimeException("METARin haku epäonnistui: " + e.getMessage());
        }

        return piste;
    }

    /**
     * Metodi joka parsii METAR merkkijono ja täyttää WeatherSamplePoint olion kentät sen perusteella.
     * @param rivi RAAKA METAR merkkijono (esim. "METAR EFHK 121200Z 24012KT 9999 FEW025 15/12 Q1013 NOSIG")
     * @param piste WeatherSamplePoint olio joka täytetään METARin perusteella
     */
    private void parsiMetarString(String rivi, WeatherSamplePoint piste) {
        if (rivi.startsWith("METAR ") || rivi.startsWith("SPECI ") || rivi.startsWith("COR ")) {
            rivi = rivi.substring(rivi.indexOf(' ') + 1).trim();
        }
        String[] sanat = rivi.split("\\s+");

        boolean cavok = false;

        for (String s : sanat) {
            // Tuuli: esim. 24012KT tai 24012G20KT tai VRB03KT
            if (s.matches(".*\\d{2,3}KT")) {
                try {
                    String suuntaStr = s.substring(0, 3);
                    piste.setTuuliSuuntaDeg(suuntaStr.equals("VRB") ? 0 : Integer.parseInt(suuntaStr));

                    int gIndex = s.indexOf('G');
                    int ktIndex = s.indexOf("KT");
                    if (gIndex != -1) {
                        piste.setTuuliNopeusKt(Integer.parseInt(s.substring(3, gIndex)));
                        piste.setTuuliPuuskaKt(Integer.parseInt(s.substring(gIndex + 1, ktIndex)));
                    } else {
                        piste.setTuuliNopeusKt(Integer.parseInt(s.substring(3, ktIndex)));
                    }
                } catch (Exception e) { System.out.println("⚠️ Virhe tuulen parsinnassa: " + s + " " + e.getMessage()); }
                continue;
            }

            // CAVOK
            if (s.equals("CAVOK")) {
                cavok = true;
                piste.setNakyvyysMetria(10000);
                piste.setPilviMaara("CAVOK");
                piste.setSaaIlmio("NSW");
                continue;
            }

            // Lämpötila/Kastepiste: esim. 15/12 tai M05/M08
            if (s.matches("M?\\d{2}/M?\\d{2}")) {
                String[] osat = s.split("/");
                piste.setLampotilaC(osat[0].startsWith("M") ? -Double.parseDouble(osat[0].substring(1)) : Double.parseDouble(osat[0]));
                piste.setKastepisteC(osat[1].startsWith("M") ? -Double.parseDouble(osat[1].substring(1)) : Double.parseDouble(osat[1]));
                continue;
            }

            // Ilmanpaine (QNH)
            if (s.matches("Q\\d{4}")) {
                piste.setQnhHpa(Integer.parseInt(s.substring(1)));
                continue;
            }

            // Näkyvyys metreinä
            if (!cavok && s.matches("\\d{4}")) {
                piste.setNakyvyysMetria(Integer.parseInt(s));
                continue;
            }

            // Pilvet: esim. SCT025 tai OVC040
            if (!cavok && s.length() >= 6 && (s.startsWith("FEW") || s.startsWith("SCT") || s.startsWith("BKN") || s.startsWith("OVC"))) {
                piste.setPilviMaara(s.substring(0, 3));
                try {
                    piste.setPilviKorkeusFt(Integer.parseInt(s.substring(3, 6)) * 100);
                } catch (NumberFormatException ignored) {}
                continue;
            }
        }
    }


    /**
     * Hakee OpenMeteo APIsta säätiedot kyseiselle pisteelle ja ajankohdalle.
     * Palauttaa WeatherSamplePoint olion joka sisältää säätiedot.
     * @param nimi Sääpisteen nimi (esim. ICAO tai "WP 1")
     * @param lat latitude koordinaatti josta sää haetaan
     * @param lon longitude koordinaatti josta sää haetaan
     * @param kohdeAika ZonedDateTime olio joka kertoo UTC ajassa milloin pisteeseen saavutaan ja milloin sää haetaan
     * @param tyyppi Tyyppi kertoo onko kyseessä lähtö, määränpää vai reittipiste
     * @return Palauttaa uuden WeatherSamplePoint olion joka sisältää säätiedot kyseiselle pisteelle ja ajankohdalle
     */
    private WeatherSamplePoint haeOpenMeteoSaa(String nimi, double lat, double lon, ZonedDateTime kohdeAika, WeatherSamplePoint.PointType tyyppi) {
        WeatherSamplePoint piste = new WeatherSamplePoint(tyyppi, nimi, lat, lon, kohdeAika);
        piste.setLahde(WeatherSamplePoint.DataSource.OPEN_METEO);

        try {
            // Pyöristetään pyydetty aika lähimpään tuntiin UTC-ajassa
            ZonedDateTime pyoristettyAika = kohdeAika.withZoneSameInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.HOURS);
            if (kohdeAika.getMinute() >= 30) {
                pyoristettyAika = pyoristettyAika.plusHours(1);
            }
            String aikaStr = pyoristettyAika.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:00"));

            // Rakennetaan URL.
            String url = String.format(
                    java.util.Locale.US,
                    "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f" +
                            "&hourly=temperature_2m,dew_point_2m,cloudcover,visibility,windspeed_10m,winddirection_10m,surface_pressure,weathercode," +
                            "windspeed_850hPa,winddirection_850hPa,temperature_850hPa," +
                            "windspeed_700hPa,winddirection_700hPa,temperature_700hPa" +
                            "&wind_speed_unit=kn&timezone=UTC&start_hour=%s&end_hour=%s",
                    lat, lon, aikaStr, aikaStr
            );

            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                com.fasterxml.jackson.databind.JsonNode hourly = mapper.readTree(response.body()).path("hourly");

                // Koska haimme vain yhtä tuntia (start = end), data on aina indeksissä 0
                int indeksi = 0;

                // Pintasää
                piste.setLampotilaC(hourly.path("temperature_2m").get(indeksi).asDouble());
                piste.setKastepisteC(hourly.path("dew_point_2m").get(indeksi).asDouble());
                piste.setTuuliNopeusKt(hourly.path("windspeed_10m").get(indeksi).asInt());
                piste.setTuuliSuuntaDeg(hourly.path("winddirection_10m").get(indeksi).asInt());
                piste.setNakyvyysMetria(hourly.path("visibility").get(indeksi).asInt());
                piste.setQnhHpa(hourly.path("surface_pressure").get(indeksi).asInt());

                // Pilvikorkeuden arviointi: (Lämpötila - Kastepiste) * 400
                double pilviKorkeusFt = (piste.getLampotilaC() - piste.getKastepisteC()) * 400.0;
                piste.setPilviKorkeusFt(Math.max(0, (int) pilviKorkeusFt));

                int pilvisyysPct = hourly.path("cloudcover").get(indeksi).asInt();
                piste.setPilviMaara(arvioiPilvisyys(pilvisyysPct));

                // Ylemmät ilmakerrokset (Aloft Conditions)
                // 850hPa on n. 5000ft, 700hPa on n. 10000ft
                piste.addAloftCondition(5000,
                        hourly.path("winddirection_850hPa").get(indeksi).asInt(),
                        hourly.path("windspeed_850hPa").get(indeksi).asInt(),
                        hourly.path("temperature_850hPa").get(indeksi).asDouble());

                piste.addAloftCondition(10000,
                        hourly.path("winddirection_700hPa").get(indeksi).asInt(),
                        hourly.path("windspeed_700hPa").get(indeksi).asInt(),
                        hourly.path("temperature_700hPa").get(indeksi).asDouble());

            } else {
                System.err.println("❌ OpenMeteo virhe pisteelle " + nimi + " (HTTP " + response.statusCode() + ")");
            }
        } catch (Exception e) {
            System.err.println("❌ Virhe ladattaessa OpenMeteo-säätä: " + e.getMessage());
        }

        return piste;
    }

    private String arvioiPilvisyys(int prosenttia) {
        if (prosenttia < 5) return "CLR";
        if (prosenttia < 25) return "FEW";
        if (prosenttia < 50) return "SCT";
        if (prosenttia < 75) return "BKN";
        return "OVC";
    }


}