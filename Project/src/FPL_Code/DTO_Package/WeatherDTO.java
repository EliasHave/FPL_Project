package FPL_Code.DTO_Package;

import FPL_Code.Weather;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class WeatherDTO {

    private String paikka;
    private String ajankohta;
    private double temp;
    private String tuuli;
    private String sade; //todennäköisyys ja muoto
    private double kasteP;
    private String pilvet;
    private double ilmanP;
    private int nakyvyys;
    private String tempo;

    public String getPaikka() {
        return paikka;
    }
    public String getAjankohta() {
        return ajankohta;
    }
    public double getTemp() {
        return temp;
    }
    public String getTuuli() {
        return tuuli;
    }
    public String getSade() {
        return sade;
    }
    public double getKasteP() {
        return kasteP;
    }
    public String getPilvet() {
        return pilvet;
    }
    public double getIlmanP() {
        return ilmanP;
    }
    public int getNakyvyys() {
        return nakyvyys;
    }
    public String getTempo() {
        return tempo;
    }


    public WeatherDTO(String p, String aK, double temp, String t, String s, double kP, String pilvet,double iP, int n, String tempo) {
        this.paikka = p;
        this.ajankohta = aK;
        this.temp = temp;
        this.tuuli = t;
        this.sade = s;
        this.kasteP = kP;
        this.pilvet = pilvet;
        this.ilmanP = iP;
        this.nakyvyys = n;
        this.tempo = tempo;
    }


    /**
     * Hakee parametrina tulevan sääolion perusteella DTO version ja palauttaa sen
     * @param saa
     * @return
     */
    public static WeatherDTO haeSaaDTO(Weather saa) {

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

        String paikka = (saa.getPaikka() != null) ? saa.getPaikka() : "ei tiedossa";

        ZonedDateTime ajankohtaZDT = saa.getAjankohtaZDT();
        String ajankohtaSTR = (ajankohtaZDT != null) ? ajankohtaZDT.format(fmt) : "ei tiedossa";

        double temp = saa.getTemp();
        String tuuli = (saa.getTuuli() != null) ? saa.getTuuli() : "ei tiedossa";
        String sade = (saa.getSade() != null) ? saa.getSade() : "ei tiedossa";
        double kasteP = saa.getKasteP();
        String pilvet = (saa.getPilvet() != null) ? saa.getPilvet() : "ei tiedossa";
        double ilmanP = saa.getIlmanP();
        int nakyvyys = saa.getNakyvyys();
        String tempo = (saa.getTempo() != null) ? saa.getTempo() : "ei tiedossa";

        WeatherDTO saaDTO = new WeatherDTO(paikka, ajankohtaSTR, temp, tuuli, sade, kasteP, pilvet, ilmanP, nakyvyys, tempo);
        return saaDTO;
    }

}
