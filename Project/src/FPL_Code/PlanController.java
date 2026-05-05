package FPL_Code;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class PlanController {

    private final FlightController flightController;

    public PlanController(FlightController flightController) {
        this.flightController = flightController;
    }

    @GetMapping(value = "/suunnitelma/{id}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> haeSuunnitelma(@PathVariable String id) {
        String html = flightController.getSuunnitelmat().get(id);
        if (html == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(html);
    }
}