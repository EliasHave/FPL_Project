package FPL_Code;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * luokka jossa kerätään kaikki mahdollinen tieto liittyen esimerkiksi lähtö ja määränpää kenttien turvallisuudesta
 * Tiedot lähtee FlightPlannerille jossa niiden pohjalta jalostetaan lentosuunnitelma
 */
public class Notam {

    public static void main(String[] args) {
        String paikka = "";
        haeNotam(paikka);
    }


    public static String haeNotam(String paikka) {
        scrapeAIS();
        return "";
    }

    /**
     * Aliohjelma joka hakee paikan perusteella notamit AIS sivustolta
     */
    public static String scrapeAIS() {
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

        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}
