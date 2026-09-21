package FPL_Code.Service.AviationData;

import FPL_Code.Model.Feature;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
class AirspaceCache {

    private final List<Feature> ilmatilat = new ArrayList<>();


    /**
     * Metodi joka karsii ilmatilalta (Feature olio) turhat tiedot pois
     * @param raakaDataEra Karsimaton Feature lista joka sisältää tietyn erän ilmatiloja
     */
    void lisaaJaKarsiIlmatilat(List<Feature> raakaDataEra) {
        for (Feature f : raakaDataEra) {
            // Karsitaan heti, vain olennainen jää muistiin!
            ilmatilat.add(f.karsiIlmatilanProperties());
        }
    }

    /**
     * Metodi joka tyhjentää vanhat ilmatilat
     */
    void tyhjennaVanhat() {
        ilmatilat.clear();
    }


    List<Feature> getKaikkiIlmatilat() {
        return ilmatilat;
    }
}
