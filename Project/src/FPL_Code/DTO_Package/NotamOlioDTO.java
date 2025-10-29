package FPL_Code.DTO_Package;

import FPL_Code.Notam;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class NotamOlioDTO {

    private String location;
    private String otsikko;
    private String kuvaus;
    private String voimassaAlkaen; // Stringiksi muunnettuna
    private String voimassaAsti;

    public String getLocation() {
        return location;
    }
    public String getOtsikko() {
        return otsikko;
    }
    public String getKuvaus() {
        return kuvaus;
    }
    public String getVoimassaAlkaen() {
        return voimassaAlkaen;
    }
    public String getVoimassaAsti() {
        return voimassaAsti;
    }


    public NotamOlioDTO(String location, String otsikko, String kuvaus, String voimassaAlkaen, String voimassaAsti) {
        this.location = location;
        this.otsikko = otsikko;
        this.kuvaus = kuvaus;
        this.voimassaAlkaen = voimassaAlkaen;
        this.voimassaAsti = voimassaAsti;
    }

    public static List<NotamOlioDTO> teeNotamitDTO(List<Notam.NotamOlio> notamit) {

        List<NotamOlioDTO> notamOliotDTO = new ArrayList<>();

        // meneekö notamit sekaisin järjestyksessä jos tekee näin???
        for (Notam.NotamOlio n : notamit) {
            String location = n.getLocation();
            String otsikko = n.getOtsikko();
            String kuvaus = n.getKuvaus();
            LocalDateTime voimassaAlkaen = n.getVoimassaAlkaen();
            LocalDateTime voimassaAsti = n.getVoimassaAsti();

            // null-fallbackit
            String alkaenSTR = (voimassaAlkaen != null) ? voimassaAlkaen.toString() : "ei tiedossa";
            String astiSTR   = (voimassaAsti   != null) ? voimassaAsti.toString()   : "ei tiedossa";
            NotamOlioDTO nDTO = new NotamOlioDTO(location, otsikko, kuvaus, alkaenSTR, astiSTR);
            notamOliotDTO.add(nDTO);
        }

        return notamOliotDTO;
    }

}
