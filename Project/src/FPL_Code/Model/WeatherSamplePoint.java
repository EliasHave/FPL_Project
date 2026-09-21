package FPL_Code.Model;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * UNIFORMAALINEN SÄÄPISTE
 * Säilyttää sään täsmälleen samassa muodossa riippumatta siitä,
 * haettiinko se METARista vai OpenMeteo-ennusteesta.
 */
public class WeatherSamplePoint {

    public enum PointType { DEPARTURE, DESTINATION, WAYPOINT }
    public enum DataSource { METAR, OPEN_METEO }

    // --- PERUSTIEDOT ---
    private PointType tyyppi;
    private String nimi; // ICAO tai "WP 1"
    private double lat;
    private double lon;
    private ZonedDateTime aikaUTC;
    private DataSource lahde; // Mistä tämä tieto on peräisin?

    // --- PINTASÄÄ (Löytyy sekä METARista että OpenMeteosta) ---
    private double lampotilaC;
    private double kastepisteC;
    private int tuuliSuuntaDeg;
    private int tuuliNopeusKt; // HUOM: Solmuja!
    private int tuuliPuuskaKt;
    private int nakyvyysMetria;
    private int qnhHpa;

    // Pilvet ja sääilmiöt
    private String pilviMaara; // FEW, SCT, BKN, OVC, CAVOK
    private int pilviKorkeusFt;
    private String saaIlmio; // -RA, TS, FG, NSW (No Significant Weather)

    // --- YLEMMÄT ILMAKERROKSET (Vain OpenMeteosta En-route pisteille) ---
    // Avaimena korkeus jaloissa (esim. 3000, 6000), arvona tuulitiedot
    private Map<Integer, AloftCondition> aloftConditions = new HashMap<>();

    // Apuluokka ylempien ilmakerrosten säälle
    public static class AloftCondition {
        public int tuuliSuuntaDeg;
        public int tuuliNopeusKt;
        public double lampotilaC;

        public AloftCondition(int dir, int spd, double temp) {
            this.tuuliSuuntaDeg = dir;
            this.tuuliNopeusKt = spd;
            this.lampotilaC = temp;
        }
    }

    // --- METADATA ---
    private String raakaMetar; // Vain jos lahde == METAR

    // ====================================================================
    // Getterit ja setterit
    // ====================================================================


    public WeatherSamplePoint(PointType tyyppi, String nimi, double lat, double lon, ZonedDateTime aikaUTC) {
        this.tyyppi = tyyppi;
        this.nimi = nimi;
        this.lat = lat;
        this.lon = lon;
        this.aikaUTC = aikaUTC;
    }

    public PointType getTyyppi() {
        return tyyppi;
    }

    public void setTyyppi(PointType tyyppi) {
        this.tyyppi = tyyppi;
    }

    public String getNimi() {
        return nimi;
    }

    public void setNimi(String nimi) {
        this.nimi = nimi;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public ZonedDateTime getAikaUTC() {
        return aikaUTC;
    }

    public void setAikaUTC(ZonedDateTime aikaUTC) {
        this.aikaUTC = aikaUTC;
    }

    public DataSource getLahde() {
        return lahde;
    }

    public void setLahde(DataSource lahde) {
        this.lahde = lahde;
    }

    public double getLampotilaC() {
        return lampotilaC;
    }

    public void setLampotilaC(double lampotilaC) {
        this.lampotilaC = lampotilaC;
    }

    public double getKastepisteC() {
        return kastepisteC;
    }

    public void setKastepisteC(double kastepisteC) {
        this.kastepisteC = kastepisteC;
    }

    public int getTuuliSuuntaDeg() {
        return tuuliSuuntaDeg;
    }

    public void setTuuliSuuntaDeg(int tuuliSuuntaDeg) {
        this.tuuliSuuntaDeg = tuuliSuuntaDeg;
    }

    public int getTuuliNopeusKt() {
        return tuuliNopeusKt;
    }

    public void setTuuliNopeusKt(int tuuliNopeusKt) {
        this.tuuliNopeusKt = tuuliNopeusKt;
    }

    public int getTuuliPuuskaKt() {
        return tuuliPuuskaKt;
    }

    public void setTuuliPuuskaKt(int tuuliPuuskaKt) {
        this.tuuliPuuskaKt = tuuliPuuskaKt;
    }

    public int getNakyvyysMetria() {
        return nakyvyysMetria;
    }

    public void setNakyvyysMetria(int nakyvyysMetria) {
        this.nakyvyysMetria = nakyvyysMetria;
    }

    public int getQnhHpa() {
        return qnhHpa;
    }

    public void setQnhHpa(int qnhHpa) {
        this.qnhHpa = qnhHpa;
    }

    public String getPilviMaara() {
        return pilviMaara;
    }

    public void setPilviMaara(String pilviMaara) {
        this.pilviMaara = pilviMaara;
    }

    public int getPilviKorkeusFt() {
        return pilviKorkeusFt;
    }

    public void setPilviKorkeusFt(int pilviKorkeusFt) {
        this.pilviKorkeusFt = pilviKorkeusFt;
    }

    public String getSaaIlmio() {
        return saaIlmio;
    }

    public void setSaaIlmio(String saaIlmio) {
        this.saaIlmio = saaIlmio;
    }

    public Map<Integer, AloftCondition> getAloftConditions() {
        return aloftConditions;
    }

    public void setAloftConditions(Map<Integer, AloftCondition> aloftConditions) {
        this.aloftConditions = aloftConditions;
    }

    public String getRaakaMetar() {
        return raakaMetar;
    }

    public void setRaakaMetar(String raakaMetar) {
        this.raakaMetar = raakaMetar;
    }

    // Esimerkki: Ylempien tuulien asettaminen
    public void addAloftCondition(int altitudeFt, int dir, int speedKt, double tempC) {
        this.aloftConditions.put(altitudeFt, new AloftCondition(dir, speedKt, tempC));
    }
}