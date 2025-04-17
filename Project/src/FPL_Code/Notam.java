package FPL_Code;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

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

            // Etsi kaikki h3-elementit (kenttien otsikot)
            var headers = doc.select("h3");

            for (var header : headers) {
                String location = header.text().trim(); // Esim. "EFKI - KAJAANI"
                notamBuilder.append("==== ").append(location).append(" ====\n");

                // Etsi kaikki seuraavat taulukot tähän kenttään asti
                var sibling = header.nextElementSibling();
                while (sibling != null && !sibling.tagName().equals("h3")) {
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
