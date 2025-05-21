package FPL_Code;

/**
 * luokka jossa kerätään kaikki mahdollinen tieto lentokoneesta jotta se voidaan käsitellä FlightPlanerin toimesta
 * Tallennetaan tehdyt koneet listaan tai taulukkoon yms jotta niitä voitaisiin jatkossa etsiä helposti rekisterin perusteella
 */
public class Aircraft {

    //Atribuutit

    //Perustiedot
    private String rekNro;
    private String koneTyyppi;
    private String kategoria;

    //Suorituskyky
    private int cruiseSpeed;
    private int climbRate;  //ft/min
    private int flightLevel;  //max korkeus
    private int kulutus;  //gal/h
    private int range;
    private int maxFightTime;  //jos taritsee pitää taukoja

    //Polttoaine
    private int fuelTankCapacity;
    private int usableFuel;
    private int reserveMin; //Säännösten mukainen minimipolttoainemäärä

    //Painotiedot
    private int emptyWeight;
    private int MTOW; //Maximum Take Off Weight
    private int usefulLoad; // paljonko voi kuormata
    private int payLoad;

    //Navigointivarustus
    private String transponder;
    private String GPS;
    private String radio;

    //Muodostaja
    public Aircraft() {
        rekNro = "";
        koneTyyppi = "";
        kategoria = "";

        cruiseSpeed = 0;
        climbRate = 0;
        flightLevel = 0;
        kulutus = 0;
        range = 0;
        maxFightTime = 0;

        fuelTankCapacity = 0;
        usableFuel = 0;
        reserveMin = 0;

        emptyWeight = 0;
        MTOW = 0;
        usefulLoad = 0;
        payLoad = 0;

        transponder = "";
        GPS = "";
        radio = "";
    }

    //===   SETTERIT   ======================================================================================================================================================================================================================

    public void setRekNro(String rekNro) {
        this.rekNro = rekNro;
    }

    public void setKoneTyyppi(String koneTyyppi) {
        this.koneTyyppi = koneTyyppi;
    }

    public void setKategoria(String kategoria) {
        this.kategoria = kategoria;
    }



    public void setCruiseSpeed(int cruiseSpeed) {
        this.cruiseSpeed = cruiseSpeed;
    }

    public void setClimbRate(int climbRate) {
        this.climbRate = climbRate;
    }

    public void setFlightLevel(int flightLevel) {
        this.flightLevel = flightLevel;
    }

    public void setKulutus(int kulutus) {
        this.kulutus = kulutus;
    }

    public void setRange(int range) {
        this.range = range;
    }

    public void setMaxFightTime(int maxFightTime) {
        this.maxFightTime = maxFightTime;
    }



    public void setFuelTankCapacity(int fuelTankCapacity) {
        this.fuelTankCapacity = fuelTankCapacity;
    }

    public void setUsableFuel(int usableFuel) {
        this.usableFuel = usableFuel;
    }

    public void setReserveMin(int reserveMin) {
        this.reserveMin = reserveMin;
    }



    public void setEmptyWeight(int emptyWeight) {
        this.emptyWeight = emptyWeight;
    }

    public void setMTOW(int MTOW) {
        this.MTOW = MTOW;
    }

    public void setUsefulLoad(int usefulLoad) {
        this.usefulLoad = usefulLoad;
    }

    public void setPayLoad(int payLoad) {
        this.payLoad = payLoad;
    }



    public void setTransponder(String transponder) {
        this.transponder = transponder;
    }

    public void setGPS(String GPS) {
        this.GPS = GPS;
    }

    public void setRadio(String radio) {
        this.radio = radio;
    }

    //===   GETTERIT   ===================================================================================================================================================================================================

    public String getRekNro() {
        return rekNro;
    }

    public String getKoneTyyppi() {
        return koneTyyppi;
    }

    public String getKategoria() {
        return kategoria;
    }



    public int getCruiseSpeed() {
        return cruiseSpeed;
    }

    public int getClimbRate() {
        return climbRate;
    }

    public int getFlightLevel() {
        return flightLevel;
    }

    public int getKulutus() {
        return kulutus;
    }

    public int getRange() {
        return range;
    }

    public int getMaxFightTime() {
        return maxFightTime;
    }



    public int getFuelTankCapacity() {
        return fuelTankCapacity;
    }

    public int getUsableFuel() {
        return usableFuel;
    }

    public int getReserveMin() {
        return reserveMin;
    }



    public int getEmptyWeight() {
        return emptyWeight;
    }

    public int getMTOW() {
        return MTOW;
    }

    public int getUsefulLoad() {
        return usefulLoad;
    }

    public int getPayLoad() {
        return payLoad;
    }



    public String getTransponder() {
        return transponder;
    }

    public String getGPS() {
        return GPS;
    }

    public String getRadio() {
        return radio;
    }

    @Override
    public String toString() {
        return "koneen tiedot: \n" +
                "Rekisteri tunnus: " + rekNro + "|" + " Konetyyppi: " + koneTyyppi + "|" + " Kategoria: " + kategoria +"\n" +
                "Cruising speed: " + cruiseSpeed + "|" + " Climb rate: " + climbRate + "|" + " Flight level (MAX): "+ flightLevel + "|" + " Kulutus: " + kulutus + "|" + " Range: " + range + "|" + " Maximum flight time: " + maxFightTime + "\n" +
                "Polttoaine säiliöiden maksimi kapasiteetti: " + fuelTankCapacity + "|" + " Usable fuel: " + usableFuel + "|" + " Reservivaatimus: " + reserveMin + "\n" +
                "Empty weight: " + emptyWeight + "|" + " MTOW: " + MTOW + "|" + " useful load: " + usefulLoad + "|" + " Payload: " + payLoad + "\n" +
                "Transponder: " + transponder + "|" + " GPS: " + GPS + "|" + " Radio: " + radio + "\n";
    }

    public static void main(String[] args) {
        Aircraft kone1 = new Aircraft();
        System.out.println(kone1);
        kone1.setKoneTyyppi("F18 C");
        kone1.setCruiseSpeed(750);
        kone1.setClimbRate(5000);
        kone1.setRange(850);
        kone1.setMTOW(41000);
        kone1.setUsefulLoad(5000);
        kone1.setPayLoad(5000);
        kone1.setGPS("GPS");
        System.out.println(kone1);
    }
}
