package FPL_Code;

public class FlightRequest {

    // Reitin tiedot
    private String lahtoKentta;
    private String maaranpaa;
    private String lahtoAika;

    // Lentokoneen tiedot
    private String rekNro;
    private String koneTyyppi;
    private String kategoria;
    private double mtow;
    private double emptyWeight;
    private double payLoad;
    private double usefulLoad;
    private double tankSize;
    private double usableFuel;
    private double kulutus;
    private double reserve;
    private double cruiseSpeed;
    private double climbRate;
    private double range;
    private double maxFlightTime;
    private double maxAltitude;
    private String gps;
    private String vor;
    private String radio;
    private String transponder;

    // Pilotin tiedot
    private String nimi;
    private String puhnro;
    private String sposti;
    private String syntymapaiva;
    private int kokemus;
    private String lupakirja;

    // Getterit ja setterit
    public String getLahtoKentta() { return lahtoKentta; }
    public void setLahtoKentta(String lahtoKentta) { this.lahtoKentta = lahtoKentta; }

    public String getMaaranpaa() { return maaranpaa; }
    public void setMaaranpaa(String maaranpaa) { this.maaranpaa = maaranpaa; }

    public String getLahtoAika() { return lahtoAika; }
    public void setLahtoAika(String lahtoAika) { this.lahtoAika = lahtoAika; }

    public String getRekNro() { return rekNro; }
    public void setRekNro(String rekNro) { this.rekNro = rekNro; }

    public String getKoneTyyppi() { return koneTyyppi; }
    public void setKoneTyyppi(String koneTyyppi) { this.koneTyyppi = koneTyyppi; }

    public String getKategoria() { return kategoria; }
    public void setKategoria(String kategoria) { this.kategoria = kategoria; }

    public double getMtow() { return mtow; }
    public void setMtow(double mtow) { this.mtow = mtow; }

    public double getEmptyWeight() { return emptyWeight; }
    public void setEmptyWeight(double emptyWeight) { this.emptyWeight = emptyWeight; }

    public double getPayLoad() { return payLoad; }
    public void setPayLoad(double payLoad) { this.payLoad = payLoad; }

    public double getUsefulLoad() { return usefulLoad; }
    public void setUsefulLoad(double usefulLoad) { this.usefulLoad = usefulLoad; }

    public double getTankSize() { return tankSize; }
    public void setTankSize(double tankSize) { this.tankSize = tankSize; }

    public double getUsableFuel() { return usableFuel; }
    public void setUsableFuel(double usableFuel) { this.usableFuel = usableFuel; }

    public double getKulutus() { return kulutus; }
    public void setKulutus(double kulutus) { this.kulutus = kulutus; }

    public double getReserve() { return reserve; }
    public void setReserve(double reserve) { this.reserve = reserve; }

    public double getCruiseSpeed() { return cruiseSpeed; }
    public void setCruiseSpeed(double cruiseSpeed) { this.cruiseSpeed = cruiseSpeed; }

    public double getClimbRate() { return climbRate; }
    public void setClimbRate(double climbRate) { this.climbRate = climbRate; }

    public double getRange() { return range; }
    public void setRange(double range) { this.range = range; }

    public double getMaxFlightTime() { return maxFlightTime; }
    public void setMaxFlightTime(double maxFlightTime) { this.maxFlightTime = maxFlightTime; }

    public double getMaxAltitude() { return maxAltitude; }
    public void setMaxAltitude(double maxAltitude) { this.maxAltitude = maxAltitude; }


    public String getGps() { return gps; }
    public void setGps(String gps) { this.gps = gps; }

    public String getVor() { return vor; }
    public void setVor(String vor) { this.vor = vor; }

    public String getRadio() { return radio; }
    public void setRadio(String radio) { this.radio = radio; }

    public String getTransponder() { return transponder; }
    public void setTransponder(String transponder) { this.transponder = transponder; }

    public String getNimi() { return nimi; }
    public void setNimi(String nimi) { this.nimi = nimi; }

    public String getPuhnro() { return puhnro; }
    public void setPuhnro(String puhnro) { this.puhnro = puhnro; }

    public String getSposti() { return sposti; }
    public void setSposti(String sposti) { this.sposti = sposti; }

    public String getSyntymapaiva() { return syntymapaiva; }
    public void setSyntymapaiva(String syntymapaiva) { this.syntymapaiva = syntymapaiva; }

    public int getKokemus() { return kokemus; }
    public void setKokemus(int kokemus) { this.kokemus = kokemus; }

    public String getLupakirja() { return lupakirja; }
    public void setLupakirja(String lupakirja) { this.lupakirja = lupakirja; }
}