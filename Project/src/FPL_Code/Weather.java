package FPL_Code;

import fi.jyu.mit.ohj2.Mjonot;

import java.io.IOException;
// import java.lang.classfile.constantpool.LongEntry;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * luokka jossa kerätään kaikki mahdollinen tieto säästä lähtökentässä, määränpäässä ja suunnitellulla reitillä
 */
public class Weather {
    private String paikka;
    private String ajankohta;
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

    /**
     * haetaan ajantasaisset säätiedot parametrina tuodusta paikasta ja tehdään sää-olio niillä tiedoilla
     * @param paikka
     */
    public static void haeSaa(String paikka) {
        Weather saa = new Weather();
        StringBuilder rivi = new StringBuilder(haeSaaTiedote(paikka));
        System.out.println(rivi);
        String raw = Mjonot.erota(rivi, '\n');
        saa.teeOlio(raw);
        String taf = rivi.toString();
        System.out.println(taf);
        System.out.println(raw);
        System.out.println(saa);

    }


    /**
     * tekee sääolion parametrina tuodusta paikasta ja palauttaa sen
     * @param paikka paikka josta sää halutaan hakea
     * @return sääolio paikasta
     */
    public static Weather haeSaaOlio(String paikka) {
        Weather saa = new Weather();   // tehdään tyhjä sää-olio
        StringBuilder rivi = new StringBuilder(haeSaaTiedote(paikka));   // haetaan säätiedot raakana tekstinä
        String raw = Mjonot.erota(rivi, '\n');   // erotetaan raw ja taf osio
        saa.teeOlio(raw);   // Laitetaan olion tiedoiksi säätiedot sen perusteella mitä raw teksti sisältää
        String taf = rivi.toString();   // Loput alkuperäisestä tää tiedottesta laitetaan on taf
        System.out.println(taf);
        System.out.println(raw);
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
        this.paikka = rivi.split(" ")[0];
        this.ajankohta = rivi.split(" ")[1];
        String[] sanat = rivi.split(" ");
        for (String s : sanat) {
            if  (s.matches("\\d{5}KT") || s.matches("VRB\\d{2}KT") ) {
                this.tuuli = s;
            }
            if ( s.matches("^(VC)?[-+]?([A-Z]{2}){1,2}$") && this.sade.isEmpty() && !s.matches(paikka) ) {
                System.out.println(this.sade);
                this.sade = s;
            }
            if ( s.contains("FEW") || s.contains("SCT") ||s.contains("BKN") || s.contains("OVC") ) {
                this.pilvet = s;
            }
            if ( s.matches("[+-]?\\d{2}/[+-]?\\d{2}") ) {
                this.temp = Double.parseDouble(s.split("/")[0]);
                this.kasteP = Double.parseDouble(s.split("/")[1]);
            }
            if ( s.startsWith("Q") && s.length() == 5) {
                this.ilmanP = Double.parseDouble(s.replace("Q", ""));
            }
            if (s.matches("\\d{4}") && !s.startsWith("Q")) {
                this.nakyvyys = Integer.parseInt(s.replace("Q", ""));
            } else if (s.matches("\\d+SM")) {
                this.nakyvyys = Integer.parseInt(s.replace("\\d+SM", ""));
            }
            if (s.matches("TEMPO")) {
                this.tempo = s;
                for (int i = 0; i < sanat.length; i++) {
                    if ( sanat[sanat.length - (1+i)].matches("TEMPO" ) ) {
                        break;
                    }
                    this.tempo += " " + sanat[sanat.length - (1 + i)];
                }
            }

        }

    }
}
