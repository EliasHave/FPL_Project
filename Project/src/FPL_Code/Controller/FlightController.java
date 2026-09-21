package FPL_Code.Controller;

import FPL_Code.Model.FlightRequest;
import FPL_Code.Service.FlightPlanningService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton luokka joka toimii sovelluksen "pääovena"
 * Ottaa vastaan pyynnöt selaimelta ja ohjaa ne FlightPlanningServicelle
 * Ei sisällä mitään ilmailuun liittyvää laskentaa
 */
@RestController
@RequestMapping("/api/flight")
public class FlightController {

    // Injektoitu asiantuntija (Tehtaanjohtaja)
    private final FlightPlanningService flightPlanningService;

    // Yhteinen arkistokaappi valmiille HTML-suunnitelmille (Avain = UUID, Arvo = HTML)
    private final ConcurrentHashMap<String, String> valmiitSuunnitelmat = new ConcurrentHashMap<>();

    // Konstruktori-injektio (Spring Boot syöttää tämän automaattisesti)
    public FlightController(FlightPlanningService flightPlanningService) {
        this.flightPlanningService = flightPlanningService;
    }

    /**
     * Reitti, johon selaimen JavaScript lähettää tiedot kun käyttäjä painaa "Luo suunnitelma".
     */
    @PostMapping("/suunnittele")
    public ResponseEntity<String> suunnitteleLento(@RequestBody FlightRequest request) {
        try {
            // 1. Kutsutaan Päällikköä (Tämä ajaa sään, ilmatilat, NOTAMit ja Tekoälyn!)
            // Palauttaa valmiin HTML-sivun merkkijonona.
            String valmisHtml = flightPlanningService.luoSuunnitelma(request);

            // 2. Luodaan uniikki ID tälle suunnitelmalle ja tallennetaan se muistiin
            String suunnitelmaId = UUID.randomUUID().toString();
            valmiitSuunnitelmat.put(suunnitelmaId, valmisHtml);

            // 3. Palautetaan JavaScriptille URL, johon sen pitää siirtyä
            String redirectUrl = "/api/flight/tulokset/" + suunnitelmaId;
            return ResponseEntity.ok(redirectUrl);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Virhe lennon suunnittelussa: " + e.getMessage());
        }
    }


    @GetMapping(value = "/tulokset/{id}", produces = "text/html;charset=UTF-8")
    public ResponseEntity<String> naytaTulos(@PathVariable String id) {
        String html = valmiitSuunnitelmat.get(id);
        if (html == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(html);
    }

}