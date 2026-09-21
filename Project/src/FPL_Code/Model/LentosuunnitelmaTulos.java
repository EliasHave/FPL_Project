package FPL_Code.Model;

import java.util.List;

/**
 * Tämä on DTO, joka edustaa Tekoälyn palauttamaa analyysia.
 * Tekoälyä (API) pyydetään palauttamaan JSON, joka mäppäytyy suoraan tähän luokkaan.
 */
public class LentosuunnitelmaTulos {

    private String departure;
    private String destination;
    private String risk;             // Esim. "LOW", "MEDIUM", "HIGH"
    private String safetyConcerns;   // Tekoälyn vapaa tekstianalyysi riskeistä
    private List<Point> reittiPisteet; // MVP:ssä vain [Lahto, Kohde]

    // Tyhjä konstruktori on pakollinen Jackson-kirjastolle (JSON -> Java muunnos)
    public LentosuunnitelmaTulos() {}

    public LentosuunnitelmaTulos(String departure, String destination, String risk, String safetyConcerns, List<Point> reittiPisteet) {
        this.departure = departure;
        this.destination = destination;
        this.risk = risk;
        this.safetyConcerns = safetyConcerns;
        this.reittiPisteet = reittiPisteet;
    }

    // --- GETTERIT JA SETTERIT ---
    public String getDeparture() { return departure; }
    public void setDeparture(String departure) { this.departure = departure; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public String getRisk() { return risk; }
    public void setRisk(String risk) { this.risk = risk; }

    public String getSafetyConcerns() { return safetyConcerns; }
    public void setSafetyConcerns(String safetyConcerns) { this.safetyConcerns = safetyConcerns; }

    public List<Point> getReittiPisteet() { return reittiPisteet; }
    public void setReittiPisteet(List<Point> reittiPisteet) { this.reittiPisteet = reittiPisteet; }
}