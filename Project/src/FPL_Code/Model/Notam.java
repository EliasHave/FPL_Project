package FPL_Code.Model;

import org.locationtech.jts.geom.Geometry;
import java.time.ZonedDateTime;

/**
 * Puhdas tietoluokka NOTAM-sähkeelle.
 */
public class Notam {

    public enum NotamType { DEPARTURE, DESTINATION, EN_ROUTE }

    private NotamType tyyppi;
    private String icao;           // Esim. EFHK tai EFIN
    private String raakaTeksti;    // Koko sähke sellaisenaan
    private ZonedDateTime alkaa;
    private ZonedDateTime paattyy;

    // TÄMÄ ON TÄRKEIN: Q-rivistä laskettu fyysinen vaikutusalue (Ympyrä/Monikulmio)
    // Tämän avulla NotamService tietää, leikkaako tämä reittiä!
    private transient Geometry vaikutusAlue;

    public Notam(NotamType tyyppi, String icao, String raakaTeksti) {
        this.tyyppi = tyyppi;
        this.icao = icao;
        this.raakaTeksti = raakaTeksti;
    }

    // --- GETTERIT JA SETTERIT ---
    public NotamType getTyyppi() { return tyyppi; }
    public String getIcao() { return icao; }

    public String getRaakaTeksti() { return raakaTeksti; }
    public void setRaakaTeksti(String raakaTeksti) { this.raakaTeksti = raakaTeksti; }

    public ZonedDateTime getAlkaa() { return alkaa; }
    public void setAlkaa(ZonedDateTime alkaa) { this.alkaa = alkaa; }

    public ZonedDateTime getPaattyy() { return paattyy; }
    public void setPaattyy(ZonedDateTime paattyy) { this.paattyy = paattyy; }

    public Geometry getVaikutusAlue() { return vaikutusAlue; }
    public void setVaikutusAlue(Geometry vaikutusAlue) { this.vaikutusAlue = vaikutusAlue; }
}