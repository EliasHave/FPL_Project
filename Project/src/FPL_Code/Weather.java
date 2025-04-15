package FPL_Code;

/**
 * luokka jossa kerätään kaikki mahdollinen tieto säästä lähtökentässä, määränpäässä ja suunnitellulla reitillä
 */
public class Weather {
    private String paikka;
    private String ajankohta;
    private double temp;
    private String tuuli;
    private String sade; //todennäköisyys ja muoto
    private double humidity;
    private double kasteP;
    private String pilvet;
    private double ilmanP;
    private int nakyvyys;


    public static void main(String[] args) {
        haeSaa("EFHK");

    }

    public static void haeSaa(String paikka) {
        Weather saa = new Weather();
        
    }
}
