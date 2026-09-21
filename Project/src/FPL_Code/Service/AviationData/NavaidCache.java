package FPL_Code.Service.AviationData;

import FPL_Code.Model.Feature;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
class NavaidCache {

    private final List<Feature> navaidit = new ArrayList<>();

    /**
     * Metodi joka karsii Navaidilta (Feature olio) turhat tiedot pois
     * @param raakaDataEra Karsimaton Feature lista joka sisältää tietyn erän Navaideja
     */
    void lisaaJaKarsiNavaidit(List<Feature> raakaDataEra) {
        for (Feature f : raakaDataEra) {
            // Karsitaan heti, vain olennainen jää muistiin!
            navaidit.add(f.karsiNavaidinProperties());
        }
    }

    /**
     * Metodi joka tyhjentää vanhat navaidit
     */
    void tyhjennaVanhat() {
        navaidit.clear();
    }


    List<Feature> getKaikkiNavaidit(){ return navaidit; }
}