package FPL_Code;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * luokka jossa kerätään kaikki mahdollinen tieto liittyen esimerkiksi lähtö ja määränpää kenttien turvallisuudesta
 * Lajitellaan myös tietoja paikkojen perusteella ja mahdollisesti tehdään niistä selvemmin luettavia
 * Tiedot lähtee FlightPlannerille jossa niiden pohjalta jalostetaan lentosuunnitelma
 */
public class Notam {

    public static void main(String[] args) {
        String paikka = "";
        haeNotam(paikka);
    }

    public static class NotamOlio {
        private String location;
        private String otsikko;
        private String kuvaus;
        private LocalDateTime voimassaAlkaen;
        private LocalDateTime voimassaAsti;

        public NotamOlio(String location, String otsikko, String kuvaus,
                         LocalDateTime voimassaAlkaen, LocalDateTime voimassaAsti) {
            this.location = location;
            this.otsikko = otsikko;
            this.kuvaus = kuvaus;
            this.voimassaAlkaen = voimassaAlkaen;
            this.voimassaAsti = voimassaAsti;
        }

        public String getLocation() { return location; }
        public String getOtsikko() { return otsikko; }
        public String getKuvaus() { return kuvaus; }
        public LocalDateTime getVoimassaAlkaen() { return voimassaAlkaen; }
        public LocalDateTime getVoimassaAsti() { return voimassaAsti; }

        @Override
        public String toString() {
            return String.format("📍 %s\n⏳ %s → %s\n%s\n",
                    location,
                    voimassaAlkaen != null ? voimassaAlkaen : "-",
                    voimassaAsti != null ? voimassaAsti : "-",
                    kuvaus);
        }
    }


    /**
     * tekee parametrina tulevasta notam tekstimössöstä Notam olioita joihin saadaan tallennettua kaikki olennainen tieto järjestelmällisesti
     * @param raakadata notam tekstimassa josta yksittäiset oliot luodaan
     * @return palauttaa listan NotamOlioita jotka saadaan revittyä parametrina tulevasta Notam tekstistä
     */
    public static List<NotamOlio> teeNotamOliot(String raakadata) {
        List<NotamOlio> lista = new ArrayList<>();

        String[] osiot = raakadata.split("==== ");
        for (String osio : osiot) {
            if (osio.isBlank()) continue;

            // Erotetaan ensimmäinen rivi ja muu sisältö
            String[] rivit = osio.strip().split("\n", 2);
            if (rivit.length < 2) continue;

            String location = rivit[0].replaceAll("====", "").trim();
            String sisalto = rivit[1];

            // Pilkotaan NOTAMit "+"-alkuisiksi riveiksi
            String[] notamRivit = sisalto.split("\n\\+");

            for (String raw : notamRivit) {
                String rivi = raw.strip();
                if (rivi.isBlank()) continue;
                if (!rivi.startsWith("+")) rivi = "+" + rivi;  // lisää takaisin plus jos splitissä jäi pois

                String otsikko = rivi;
                String kuvaus = rivi;
                LocalDateTime from = null;
                LocalDateTime to = null;

                // Etsitään FROM/TO aikavälit
                Matcher matcher = Pattern.compile(
                        "FROM: (\\d{2}[A-Z]{3}\\d{2}) (\\d{4}) TO: (\\d{2}[A-Z]{3}\\d{2}) (\\d{4})",
                        Pattern.CASE_INSENSITIVE
                ).matcher(rivi);

                if (matcher.find()) {
                    from = parseNotamAika(matcher.group(1) + " " + matcher.group(2));
                    to = parseNotamAika(matcher.group(3) + " " + matcher.group(4));
                }

                lista.add(new NotamOlio(location, otsikko, kuvaus, from, to));
            }
        }

        return lista;
    }


    /**
     * Erottaa ajat parametrina tulevista päiväyksistä
     * @param teksti teksti josta ajat erotellaan (muodossa ppKUUvv ttmm)
     * @return palauttaa DateTime olion joka vastaa parametrina tulevaa päiväystä
     */
    private static LocalDateTime parseNotamAika(String teksti) {
        teksti = teksti.trim().toUpperCase().replaceAll("\\s+", " ");
        try {
            // Parsitaan päivämuotoa kuten "10MAR25 0900"
            Pattern pattern = Pattern.compile("(\\d{2})([A-Z]{3})(\\d{2}) (\\d{4})");
            Matcher matcher = pattern.matcher(teksti);
            if (matcher.find()) {
                int day = Integer.parseInt(matcher.group(1));
                String monthStr = matcher.group(2);
                int year = 2000 + Integer.parseInt(matcher.group(3)); // tulkitaan esim 25 → 2025
                int hour = Integer.parseInt(matcher.group(4).substring(0, 2));
                int minute = Integer.parseInt(matcher.group(4).substring(2, 4));

                // Muunnetaan kuukausi tekstistä numeroksi
                int month = kuukaudenNumero(monthStr);
                return LocalDateTime.of(year, month, day, hour, minute);
            }
        } catch (Exception e) {
            System.err.println("⚠️ Aikaparsointivirhe: '" + teksti + "'");
        }
        return null;
    }


    private static int kuukaudenNumero(String kuukausi) {
        return switch (kuukausi) {
            case "JAN" -> 1;
            case "FEB" -> 2;
            case "MAR" -> 3;
            case "APR" -> 4;
            case "MAY" -> 5;
            case "JUN" -> 6;
            case "JUL" -> 7;
            case "AUG" -> 8;
            case "SEP" -> 9;
            case "OCT" -> 10;
            case "NOV" -> 11;
            case "DEC" -> 12;
            default -> throw new IllegalArgumentException("Tuntematon kuukausi: " + kuukausi);
        };
    }


    /**
     * hakee ajantasaiset suomen notamit
     * ei erottele paikan mukaan niitö vaan palauttaa kaiken mahdollisen tiedon
     * @param paikka
     * @return String tyyppinen notam teksti
     */
    public static String haeNotam(String paikka) {
        String notam = scrapeAIS();
        return notam;
    }

    /**
     * Aliohjelma joka hakee paikan perusteella notamit AIS sivustolta
     * @return palautaa raakana notam tiedot String muodossa
     */
    public static String scrapeAIS() {

        String palautus = "";

        try {
            Document doc = Jsoup.connect("https://www.ais.fi/bulletins/efinen.htm")
                    .userAgent("Mozilla")
                    .get();

            StringBuilder notamBuilder = new StringBuilder();
            StringBuilder notamBuilder1 = new StringBuilder();

            Pattern pattern = Pattern.compile("Date:\\s*<b>(\\d{2}[A-Z]{3}\\d{4} \\d{4})</b>\\s*UTC");
            Matcher matcher = pattern.matcher(doc.toString());
            if (matcher.find()) {
                String date = matcher.group(1);
                notamBuilder.append("Tiedot haettu: " + date + " UTC \n");
            }

            // Etsi kaikki h3-elementit (kenttien otsikot)
            var headers3 = doc.select("h3");
            var h2 = doc.select("h2");

            for (var header : headers3) {
                String location = header.text().trim(); // Esim. "EFKI - KAJAANI"
                notamBuilder.append("==== ").append(location).append(" ====\n");

                // Etsi kaikki seuraavat taulukot tähän kenttään asti
                var sibling = header.nextElementSibling();
                while (sibling != null && !sibling.tagName().matches("h3|h2")) {
                    if (sibling.tagName().equals("div") && sibling.select("table").size() > 0) {
                        notamBuilder.append(sibling.text()).append("\n\n");
                    }
                    sibling = sibling.nextElementSibling();
                }
            }

            for (var header : h2) {
                String section = header.text().trim(); // Esim. "EN-ROUTE"
                if ( !section.equals("AERODROMES") ) {
                    notamBuilder.append("==== ").append(section).append(" ====\n");
                }
                var sibling = header.nextElementSibling();
                while (sibling != null && !sibling.tagName().matches("h2|h3")) {
                    if (sibling.tagName().equals("div") && sibling.select("table").size() > 0) {
                        notamBuilder.append(sibling.text()).append("\n\n");
                    }
                    sibling = sibling.nextElementSibling();
                }
            }

            System.out.println(notamBuilder);
            palautus = notamBuilder.toString();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return palautus;
    }
}
