package FPL_Code.Controller;

import FPL_Code.Model.Feature;
import FPL_Code.Service.AviationData.AviationDataService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/aviation")
public class AviationDataController {

    private final AviationDataService aviationDataService;

    public AviationDataController(AviationDataService aviationDataService) {
        this.aviationDataService = aviationDataService;
    }

    @GetMapping(value = "/all/airspaces", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getAllAirspaces() {
        return ResponseEntity.ok(kaariGeoJsoniksi(aviationDataService.getKaikkiIlmatilat()));
    }

    @GetMapping(value = "/all/airports", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getAllAirports() {
        return ResponseEntity.ok(kaariGeoJsoniksi(aviationDataService.getKaikkiLentokentat()));
    }

    @GetMapping(value = "/all/navaids", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getAllNavaids() {
        return ResponseEntity.ok(kaariGeoJsoniksi(aviationDataService.getKaikkiNavaidit()));
    }

    /**
     * APUMETODI: Käärii pelkän Javan Listan validiksi GeoJSON FeatureCollection -olioksi.
     * Spring Boot (Jackson) kääntää tämän Map-olion automaattisesti JSON-tekstiksi HTTP-vastaukseen.
     */
    private Map<String, Object> kaariGeoJsoniksi(List<Feature> features) {
        Map<String, Object> geoJson = new HashMap<>();
        geoJson.put("type", "FeatureCollection");
        geoJson.put("features", features);
        return geoJson;
    }
}