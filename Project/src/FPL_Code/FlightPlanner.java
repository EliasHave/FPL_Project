package FPL_Code;

import FPL_Code.Aircraft;
import FPL_Code.Weather;
import FPL_Code.Notam;

/**
 * luokka joka laskee tekoälyn avulla lentosuunnitelmia
 * Käyttää kaiken mahdollisen tiedon joka koneesta, säästä sekä notemseista irtoaa
 * collabs: Aircraft, Notam, Weather
 */
public class FlightPlanner {

    private Weather saaMaapanpaa;
    private Weather saaLahto;
    private Aircraft kone;
    private String notam;
    private Pilot pilot;


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

    public Weather getSaaLahto() {
        return saaLahto;
    }


    public Weather getSaaMaapanpaa() {
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

}
