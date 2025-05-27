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

    private Weather saa;
    private Aircraft kone;
    private String notam;
    private Pilot pilot;


    public void setSaa(Weather saa) {
        this.saa = saa;
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

}
