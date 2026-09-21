package FPL_Code.Service.AviationData;

import FPL_Code.Model.Feature;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PACKAGE-PRIVATE CACHE: Vain AviationDataService voi käyttää tätä!
 * Säilyttää kaikki maailman (tai valitut softan hakemat) lentokentät valmiiksi esikarsitussa muodossa.
 */
@Component
class AirportCache {

    private final Map<String, Feature> lentokentatMap = new HashMap<>();
    private final List<Feature> lentokentat = new ArrayList<>();

    /**
     * Metodi joka karsii Lentokentältä (Feature olio) turhat tiedot pois
     * @param raakaDataEra Karsimaton Feature lista joka sisältää tietyn erän Lentokenttiä
     */
    void lisaaJaKarsiLentokentat(List<Feature> raakaDataEra) {
        for (Feature f : raakaDataEra) {
            Feature karsittu = f.karsiLentokentanProperties();
            lentokentat.add(karsittu);

            String icao = karsittu.properties.path("icaoCode").asText("").trim().toUpperCase();
            if (!icao.isEmpty()) {
                lentokentatMap.put(icao, karsittu);
            }
        }
    }

    /**
     * Metodi joka tyhjentää vanhat lentokentät
     */
    void tyhjennaVanhat() {
        lentokentatMap.clear();
        lentokentat.clear();
    }


    Feature haeIcaolla(String icao) {
        return lentokentatMap.get(icao.toUpperCase());
    }


    // Antaa Päällikölle kopion valmiiksi karsitusta datasta
    List<Feature> getKaikkiLentokentat() {
        return lentokentat;
    }
}