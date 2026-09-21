package FPL_Code.Model;

import java.util.List;

/**
 * Puhdas tiedonsiirto-olio (Data Transfer Object), joka kantaa kaiken
 * kerätyn reittidatan yhdessä paketissa Serviceiltä toisille.
 */
public class DTO_Boss {
    private final List<Feature> ilmatilat;
    private final List<Feature> lentokentat;
    private final List<Feature> navaidit;
    private final List<WeatherSamplePoint> reitinSaa;
    private final List<Notam> notamit;
    private final Aircraft kone;
    private final Pilot pilot;
    private final Point lahto;
    private final Point kohde;

    public DTO_Boss(List<Feature> ilmatilat, List<Feature> lentokentat, List<Feature> navaidit,
                    List<WeatherSamplePoint> reitinSaa, List<Notam> notamit,
                    Aircraft kone, Pilot pilot, Point lahto, Point kohde) {
        this.ilmatilat = ilmatilat;
        this.lentokentat = lentokentat;
        this.navaidit = navaidit;
        this.reitinSaa = reitinSaa;
        this.notamit = notamit;
        this.kone = kone;
        this.pilot = pilot;
        this.lahto = lahto;
        this.kohde = kohde;
    }

    // --- GETTERIT ---
    public List<Feature> getIlmatilat() { return ilmatilat; }
    public List<Feature> getLentokentat() { return lentokentat; }
    public List<Feature> getNavaidit() { return navaidit; }
    public List<WeatherSamplePoint> getReitinSaa() { return reitinSaa; }
    public List<Notam> getNotamit() { return notamit; }
    public Aircraft getKone() { return kone; }
    public Pilot getPilot() { return pilot; }
    public Point getLahto() { return lahto; }
    public Point getKohde() { return kohde; }
}