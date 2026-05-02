package FPL_Code;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
// import java.lang.classfile.constantpool.LongEntry;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;

/**
 * luokka jossa kerätään kaikki mahdollinen tieto säästä lähtökentässä, määränpäässä ja suunnitellulla reitillä
 */
public class Weather {
    private String paikka;
    private String ajankohta;
    private ZonedDateTime ajankohtaZDT;
    private double temp;
    private String tuuli;
    private String sade = ""; //todennäköisyys ja muoto
    private double kasteP;
    private String pilvet;
    private double ilmanP;
    private int nakyvyys;
    private String tempo;


    public static void main(String[] args) {
        haeSaa("VABB");
        // testaa();

    }


    public String getPaikka() {
        return paikka;
    }

    public String getAjankohta() {
        return ajankohta;
    }

    public ZonedDateTime getAjankohtaZDT() {
        return ajankohtaZDT;
    }

    public double getTemp() {
        return temp;
    }
    public String getTuuli() {
        return tuuli;
    }
    public String getSade() {
        return sade;
    }
    public double getKasteP() {
        return kasteP;
    }
    public String getPilvet() {
        return pilvet;
    }
    public double getIlmanP() {
        return ilmanP;
    }
    public int getNakyvyys() {
        return nakyvyys;
    }
    public String getTempo() {
        return tempo;
    }


    /**
     * haetaan ajantasaisset säätiedot parametrina tuodusta paikasta ja tehdään sää-olio niillä tiedoilla
     * @param paikka paikka josta sää haetaan, pitää olla ICAO muodossa
     */
    public static void haeSaa(String paikka) {
        Weather saa = new Weather();
        StringBuilder rivi = new StringBuilder(haeSaaTiedote(paikka));
        System.out.println(rivi);
        String raw = erotaEnsimmainenRivi(rivi);
        saa.teeOlio(raw);
        String taf = rivi.toString();
        System.out.println(taf);
        System.out.println(raw);
        System.out.println(saa);

    }


    /**
     * hakee säätiedot parametrina tulevasta lentokentästä parametrina tulevann aikaan ja tekee siitä Weather olion
     * @param paikka Paikka josta sää haetaan (Lentokentän ICAO-koodi)
     * @param lahtoaikaZDT aika jona olisi tarkoitus lähteä(tai saapua) kentälle, ZonedDateTime-muodossa
     * @return SaaOlio joka on luotu joko METAR-tiedotteen tai Open-Meteo-ennusteen perusteella riippuen siitä kuinka kaukana lahtoaika(tai saapumisaika) on nykyhetkestä
     */
    public static Weather haeSaaOlio(String paikka, ZonedDateTime lahtoaikaZDT) {

        // Normaali referenssihetki on nyt, Suomen ajassa jotta pystytään vertaamaan kannattako sää hakea metarilla vai ennusteella open meteosta
        // UTC ajassa kirjoitettu 28.4.2026
        ZonedDateTime nyt = ZonedDateTime.now(ZoneId.of("UTC"));

        // Erotus tunteina
        long tuntierotus = Duration.between(nyt, lahtoaikaZDT).toHours();

        // Jos ajankohta on "nyt" tai lähellä nykyhetkeä → METAR
        if (Math.abs(tuntierotus) <= 1) {
            return haeSaaOlio(paikka); // Tämä on METAR-tyyppinen haku
        } else {
            return haesaaOlioOpenMeteo(paikka, lahtoaikaZDT); // Ennuste aikaviiveellä
        }
    }


    /**
     * tekee sääolion parametrina tuodusta paikasta ja palauttaa sen
     * @param paikka paikka josta sää halutaan hakea, paikka ICAO muodossa
     * @return sääolio paikasta
     */
    public static Weather haeSaaOlio(String paikka) {

        System.out.println("=== Haetaan METAR-säätiedot kentälle " + paikka + " ===");

        String raaka = haeSaaTiedote(paikka);
        System.out.println("Saatiin AviationWeatherilta:\n" + raaka);

        Weather saa = new Weather();
        StringBuilder rivi = new StringBuilder(raaka);

        String raw = erotaEnsimmainenRivi(rivi);
        System.out.println("Erotettu METAR-rivi: " + raw);
        System.out.println("Erotettu TAF-jatko: " + rivi);

        saa.teeOlio(raw);

        System.out.println("✅ Luotu sääolio: " + saa);
        return saa;
    }


    /**
     * etsii paramerina tulevan kentän säätiedot parametrina tulevana ajankohtana
     * @param paikka kenttä josta haetaan
     * @param aika aika jonka sää halutaan ennustaa
     * @return Palauttaa weather olion ennusteen perusteella
     */
    public static Weather haesaaOlioOpenMeteo(String paikka, ZonedDateTime aika) {
        Point kentta = Point.etsiKoordinaatit(paikka);
        String url = muodostaOpenMeteoUrl(kentta.getLat(), kentta.getLon(), aika);

        try {
            String json = haeHttpGet(url);
            return puraOpenMeteoJson(json, paikka, aika);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    private static String muodostaOpenMeteoUrl(double lat, double lon, ZonedDateTime aika) {
        String aikaStr = aika.withZoneSameInstant(ZoneOffset.UTC) // <- pakotetaan UTC:hen
                .truncatedTo(ChronoUnit.HOURS)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:00"));
        return "https://api.open-meteo.com/v1/forecast?" +
                "latitude=" + lat +
                "&longitude=" + lon +
                "&hourly=temperature_2m,cloud_cover,visibility,windspeed_10m,winddirection_10m,weathercode,dew_point_2m,surface_pressure" +
                "&timezone=UTC" +
                "&start=" + aikaStr +
                "&end=" + aikaStr;
    }


    private static String haeHttpGet(String url) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }


    private static Weather puraOpenMeteoJson(String json, String paikka, ZonedDateTime haettuAika) {
        JSONObject root = new JSONObject(json);
        JSONObject hourly = root.getJSONObject("hourly");

        JSONArray ajatJson = hourly.getJSONArray("time");

        // Pyöristetään haettu aika tasatunniksi ja Helsingin aikavyöhykkeelle
        ZonedDateTime pyoristettyAika = haettuAika.withZoneSameInstant(ZoneId.of("UTC"))
                .truncatedTo(ChronoUnit.HOURS);
        String pyoristettyAikaStr = pyoristettyAika.toLocalDateTime().toString(); // esim. 2025-07-20T14:00

        // Etsitään vastaava indeksi time-taulukosta
        int indeksi = -1;
        for (int i = 0; i < ajatJson.length(); i++) {
            if (ajatJson.getString(i).equals(pyoristettyAikaStr)) {
                indeksi = i;
                break;
            }
        }

        if (indeksi == -1) {
            throw new RuntimeException("Sopivaa aikaleimaa ei löytynyt Open-Meteo-datasta: " + pyoristettyAikaStr);
        }

        Weather saa = new Weather();
        saa.paikka = paikka;
        ZonedDateTime zdtUTC = pyoristettyAika.withZoneSameInstant(ZoneOffset.UTC);
        saa.ajankohtaZDT = zdtUTC;
        saa.ajankohta = zdtUTC.withZoneSameInstant(ZoneId.of("UTC"))
                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));

        saa.temp = hourly.getJSONArray("temperature_2m").getDouble(indeksi);
        saa.kasteP = hourly.getJSONArray("dew_point_2m").getDouble(indeksi);
        saa.ilmanP = hourly.getJSONArray("surface_pressure").getDouble(indeksi);
        saa.nakyvyys = hourly.getJSONArray("visibility").getInt(indeksi);
        saa.tuuli = hourly.getJSONArray("windspeed_10m").getDouble(indeksi) + "KT " + hourly.getJSONArray("winddirection_10m").getDouble(indeksi) + "deg";  // tähän voisi lisätä sen tuulen suunnan

        JSONArray pilviArray = hourly.optJSONArray("cloud_cover");
        int pilviProsentti = 0;
        if (pilviArray != null && pilviArray.length() > indeksi) {
            pilviProsentti = pilviArray.optInt(indeksi, 0);
        }

        if (pilviProsentti < 5) {
            saa.pilvet = "CLR";
        } else if (pilviProsentti < 25) {
            saa.pilvet = "FEW";
        } else if (pilviProsentti < 50) {
            saa.pilvet = "SCT";
        } else if (pilviProsentti < 75) {
            saa.pilvet = "BKN";
        } else {
            saa.pilvet = "OVC";
        }

        // nämä voisi tarkistaa oikeaksi
        int weatherCode = hourly.getJSONArray("weathercode").optInt(indeksi, -1);
        saa.sade = switch (weatherCode) {
            case 0 -> "NSW";                          // Clear sky
            case 1, 2, 3 -> "NSW";                    // Mainly clear, partly cloudy, overcast (no significant weather)

            case 45, 48 -> "FG";                      // Fog, rime fog

            case 51 -> "-DZ";                         // Light drizzle
            case 53 -> "DZ";                          // Moderate drizzle
            case 55 -> "+DZ";                         // Dense drizzle

            case 56 -> "FZDZ";                        // Light freezing drizzle
            case 57 -> "+FZDZ";                       // Dense freezing drizzle

            case 61 -> "-RA";                         // Slight rain
            case 63 -> "RA";                          // Moderate rain
            case 65 -> "+RA";                         // Heavy rain

            case 66 -> "FZRA";                        // Light freezing rain
            case 67 -> "+FZRA";                       // Heavy freezing rain

            case 71 -> "-SN";                         // Slight snow
            case 73 -> "SN";                          // Moderate snow
            case 75 -> "+SN";                         // Heavy snow

            case 77 -> "SG";                          // Snow grains

            case 80 -> "-SHRA";                       // Slight rain showers
            case 81 -> "SHRA";                        // Moderate rain showers
            case 82 -> "+SHRA";                       // Violent rain showers

            case 85 -> "SHSN";                        // Slight snow showers
            case 86 -> "+SHSN";                       // Heavy snow showers

            case 95 -> "TS";                          // Thunderstorm, slight or moderate
            case 96 -> "TSGR";                        // Thunderstorm with slight hail
            case 99 -> "+TSGR";                       // Thunderstorm with heavy hail

            default -> "UNK";                         // Unknown / ei tiedossa
        };

        System.out.println(saa);
        return saa;
    }


    /**
     * Hakee Aviation Weather Center sivulta parametrina tuodun kentän säätiedot raakana tekstinä ja palauttaa sen
     * @param paikka paikka josta sää haetaan
     * @return Palauttaa säätiedot raa'assa muodossa
     */
    public static String haeSaaTiedote(String paikka) {
        HttpClient client = HttpClient.newHttpClient();

        ZonedDateTime utcAika = ZonedDateTime.now(ZoneOffset.UTC);
        // String utcFormatted = utcAika.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://aviationweather.gov/api/data/metar?ids=" + paikka + "&format=raw&taf=true&hours=0&date=" + utcAika))
                .header("User-Agent", "JavaHttpClient/1.0")
                .header("Accept", "application/xml")
                .header("Accept-Encoding", "gzip, deflate")
                .GET()
                .build();

        HttpResponse<String> response = null;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        return response.body();
    }

    @Override
    public String toString() {
        return ("Säätiedot kohteessa: " + paikka + "\n" + "ajankohta: " + ajankohta + "\n" + "tuuli: " + tuuli + "\n" + "näkyvyys: " + nakyvyys + "\n" + "pilvet: " + pilvet + "\n" + "Sade: " + sade + "\n" + "Lämpötila: " + temp + "\n" + "kastepiste: " + kasteP + "\n" + "ilmanpaine: " + ilmanP + "\n" + "TEMPO: " + tempo + "\n");
    }

    /**
     * Ottaa parametrina tuodun merkkijono rivin (Raw) ja tulkitsee sen antaen oliolle arvot rivin perusteella
     * @param rivi rivi josta tiedot pilkotaan
     */
    public void teeOlio(String rivi) {
        //Esikäsittely: METAR / SPECI / COR
        if (rivi.startsWith("METAR ") || rivi.startsWith("SPECI ") || rivi.startsWith("COR ")) {
            rivi = rivi.substring(rivi.indexOf(' ') + 1).trim();
        }

        String[] sanat = rivi.split("\\s+");

        //Pakolliset kentät
        this.paikka = sanat[0];
        this.ajankohta = sanat[1];

        try {
            this.ajankohtaZDT = parseAjankohta(this.ajankohta);
        } catch (Exception e) {
            System.err.println("Virhe aikamuodon parsimisessa: " + this.ajankohta);
            this.ajankohtaZDT = null;
        }

        //Oletusarvot
        this.sade = "";
        this.pilvet = null;
        this.nakyvyys = -1;

        boolean cavok = false;

        //Varsinainen parsinta
        for (String s : sanat) {

            //Tuuli
            if (s.matches("\\d{5}KT") || s.matches("VRB\\d{2}KT")) {
                this.tuuli = s;
                continue;
            }

            // CAVOK
            if (s.equals("CAVOK")) {
                cavok = true;
                this.nakyvyys = 10000;
                this.pilvet = "CAVOK";
                this.sade = "NONE";
                continue;
            }

            // Lämpötila / kastepiste (M09/M13)
            if (s.matches("M?\\d{2}/M?\\d{2}")) {
                String[] osat = s.split("/");
                this.temp = parseSignedTemp(osat[0]);
                this.kasteP = parseSignedTemp(osat[1]);
                continue;
            }

            // Ilmanpaine
            if (s.matches("Q\\d{4}")) {
                this.ilmanP = Double.parseDouble(s.substring(1));
                continue;
            }

            // Näkyvyys metreinä (0000–9999)
            if (!cavok && s.matches("\\d{4}")) {
                this.nakyvyys = Integer.parseInt(s);
                continue;
            }

            // Näkyvyys SM (US-muoto)
            if (s.matches("\\d+SM")) {
                int miles = Integer.parseInt(s.replace("SM", ""));
                this.nakyvyys = miles * 1609;
                continue;
            }

            // Pilvet (vain jos ei CAVOK)
            if (!cavok &&
                    (s.startsWith("FEW") || s.startsWith("SCT") ||
                            s.startsWith("BKN") || s.startsWith("OVC"))) {
                this.pilvet = s;
                continue;
            }

            // Sade / ilmiöt (vain yksi, jos ei vielä asetettu)
            if (this.sade.isEmpty() &&
                    s.matches("^(VC)?[-+]?([A-Z]{2}){1,2}$") &&
                    !s.equals(this.paikka)) {
                this.sade = s;
            }
        }

        //Fallbackit
        if (this.nakyvyys < 0) {
            this.nakyvyys = 0;
        }

        if (this.pilvet == null) {
            this.pilvet = "ei tiedossa";
        }

        if (this.sade.isEmpty()) {
            this.sade = "ei tiedossa";
        }

    }


    /**
     * Parseri joka tulkitsee metarissa olevat lämpötilat
     * Jos alkaa M kirjaimella niin palautetaan - merkkinen arvo (pakkasta)
     * Jos ei ole M etuliitetä niin läpötila on positiivinen
     * @param t merkkijono joka ilmaisee lämpötilan celsius-asteina
     * @return Palauttaa reaalilukuisen läpötilan oikealla etumerkillä (pakkasta tai ei)
     */
    private double parseSignedTemp(String t) {
        if (t.startsWith("M")) {
            return -Double.parseDouble(t.substring(1));
        }
        return Double.parseDouble(t);
    }


    /**
     * metodi joka parsii string muotoisen ajan ja tekee siitä ZonedDateTime olion ja palauttaa sen
     * @param ajankohtaStr
     * @return
     */
    public static ZonedDateTime parseAjankohta(String ajankohtaStr) {

        ajankohtaStr = ajankohtaStr.trim();

        // Jos syöte on esim. 122340Z → tulkitaan muodossa ddHHmm'Z' UTC-aikana
        if (ajankohtaStr.matches("\\d{6}Z")) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("ddHHmm'Z'")
                    .withZone(ZoneOffset.UTC);
            TemporalAccessor parsed = formatter.parse(ajankohtaStr);
            int day = parsed.get(ChronoField.DAY_OF_MONTH);
            int hour = parsed.get(ChronoField.HOUR_OF_DAY);
            int minute = parsed.get(ChronoField.MINUTE_OF_HOUR);

            LocalDate now = LocalDate.now(ZoneOffset.UTC);
            int year = now.getYear();
            int month = now.getMonthValue();

            // Tee päivä varovaisesti (esim. jos päivä 31 ja kuukausi on huhtikuu)
            LocalDate date;
            try {
                date = LocalDate.of(year, month, day);
            } catch (DateTimeException e) {
                date = now; // fallback
            }

            return ZonedDateTime.of(date, LocalTime.of(hour, minute), ZoneOffset.UTC);
        }

        // Jos syöte on vain kellonaika (esim. "13:20") → tulkitaan Helsingin aikavyöhykkeellä tänään
        if (ajankohtaStr.matches("\\d{1,2}:\\d{2}")) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("H:mm");
            LocalTime time = LocalTime.parse(ajankohtaStr, formatter);
            LocalDate today = LocalDate.now(ZoneId.of("UTC"));
            ZonedDateTime UTCTime = ZonedDateTime.of(today, time, ZoneId.of("UTC"));
            // return UTCTime.withZoneSameInstant(ZoneOffset.UTC);  // <- Palautetaan UTC-aikaan muunnettuna
            return UTCTime;
        }

        // Jos ISO 8601 -muoto esim. "2025-07-20T12:30+03:00"
        try {
            return ZonedDateTime.parse(ajankohtaStr);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Tuntematon aikamuoto: " + ajankohtaStr);
        }

    }


    /**
     * Erottaa ja palauttaa ensimmäisen rivin StringBuilder-oliosta ja poistaa sen siitä.
     * Korvaa Mjonot.erota(rivi, '\n') toiminnon.
     * @param rivi StringBuilder josta ensimmäinen rivi erotetaan
     * @return ensimmäinen rivi merkkijonona
     */
    private static String erotaEnsimmainenRivi(StringBuilder rivi) {
        int index = rivi.indexOf("\n");
        if (index >= 0) {
            String eka = rivi.substring(0, index);
            rivi.delete(0, index + 1);
            return eka;
        }
        String eka = rivi.toString();
        rivi.setLength(0);
        return eka;
    }



}
