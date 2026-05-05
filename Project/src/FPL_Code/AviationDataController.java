package FPL_Code;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/aviation")
public class AviationDataController {

    private final AviationDataService aviationDataService;

    public AviationDataController(AviationDataService aviationDataService) {
        this.aviationDataService = aviationDataService;
    }

    // Nämä kolme endpointtia palauttavat KAIKKI datat suoraan selaimelle.
    // Selain hakee näitä vasta kun käyttäjä klikkaa "Näytä kaikki" -nappia.
    // Ennen klikkausta näitä ei haeta ollenkaan -> säästää muistia ja aikaa.

    @GetMapping(value = "/all/airspaces", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getAllAirspaces() {
        return ResponseEntity.ok(aviationDataService.getIlmatilatGeoJson());
    }

    @GetMapping(value = "/all/airports", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getAllAirports() {
        return ResponseEntity.ok(aviationDataService.getLentokentatGeoJson());
    }

    @GetMapping(value = "/all/navaids", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getAllNavaids() {
        return ResponseEntity.ok(aviationDataService.getNavaiditGeoJson());
    }
}