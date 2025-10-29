package FPL_Code;

public class Pilot {

    String nimi;
    String syntymaAika;
    String sPosti;
    String puhNro;
    String lupaKirjat;
    int kokemus;

    public Pilot() {
        String nimi = "";
        String syntymaAika = "";
        String sPosti = "";
        String puhNro = "";
        String lupaKirjat = "";
        int kokemus = 0;
    }


    public void setNimi(String nimi) {
        this.nimi = nimi;
    }

    public void setSyntymaAika(String syntymaAika) {
        this.syntymaAika = syntymaAika;
    }


    public void setSPosti(String sPosti) {
        this.sPosti = sPosti;
    }

    public void setPuhNro(String puhNro) {
        this.puhNro = puhNro;
    }

    public void setLupaKirjat(String lupaKirjat) {
        this.lupaKirjat = lupaKirjat;
    }

    public void setKokemus(int kokemus) {
        this.kokemus = kokemus;
    }



    public String getNimi() {
        return nimi;
    }

    public String getSyntymaAika() {
        return syntymaAika;
    }

    public String getSPosti() {
        return sPosti;
    }

    public String getPuhNro() {
        return puhNro;
    }

    public String getLupaKirjat() {
        return lupaKirjat;
    }

    public int getKokemus() {
        return kokemus;
    }

    @Override
    public String toString() {
        return "Pilotin tiedot: " + "\n" +
                "Nimi: " + nimi + "\n" +
                "Syntymäaika: " + syntymaAika + "\n" +
                "Säköposti: " + sPosti + "\n" +
                "Puhelin numero: " + puhNro + "\n" +
                "Lupakirjat: " + lupaKirjat + "\n" +
                "Kokemus: " + kokemus;

    }
}
