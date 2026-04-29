package FPL_Code;

import fxFXML_FPL.FXML_FPLMain;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.List;

import static FPL_Code.Point.etsiKoordinaatit;
import static FPL_Code.Weather.haeSaaOlio;
import static FPL_Code.Weather.parseAjankohta;

@RestController
@RequestMapping("/api/flight")
public class FlightController {

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("FPL API toimii!");
    }

    @PostMapping("/suunnittele")
    public ResponseEntity<String> suunnitteleLento(@RequestBody FlightRequest request) {
        try {
            FlightPlanner planner = new FlightPlanner();

            // Rakennetaan Aircraft-olio requestin tiedoista
            Aircraft kone = new Aircraft();
            kone.setRekNro(request.getRekNro());
            kone.setKoneTyyppi(request.getKoneTyyppi());
            kone.setKategoria(request.getKategoria());
            kone.setCruiseSpeed((int) request.getCruiseSpeed());
            kone.setClimbRate((int) request.getClimbRate());
            kone.setFlightLevel((int) request.getMaxAltitude());
            kone.setKulutus((int) request.getKulutus());
            kone.setRange((int) request.getRange());
            kone.setMaxFightTime((int) request.getMaxFlightTime());
            kone.setFuelTankCapacity((int) request.getTankSize());
            kone.setUsableFuel((int) request.getUsableFuel());
            kone.setReserveMin((int) request.getReserve());
            kone.setEmptyWeight((int) request.getEmptyWeight());
            kone.setMTOW((int) request.getMtow());
            kone.setUsefulLoad((int) request.getUsefulLoad());
            kone.setPayLoad((int) request.getPayLoad());
            kone.setTransponder(request.getTransponder());
            kone.setGPS(request.getGps());
            kone.setRadio(request.getRadio());

            // Rakennetaan Pilot-olio
            Pilot pilotti = new Pilot();
            pilotti.setNimi(request.getNimi());
            pilotti.setPuhNro(request.getPuhnro());
            pilotti.setSPosti(request.getSposti());
            pilotti.setSyntymaAika(request.getSyntymapaiva());
            pilotti.setKokemus(request.getKokemus());
            pilotti.setLupaKirjat(request.getLupakirja());

            // Asetetaan kone ja pilotti suunnittelijaan
            planner.setKone(kone);
            planner.setPilot(pilotti);

            ZonedDateTime lahtoZDT = parseAjankohta(request.getLahtoAika());

            Weather saaLahto = haeSaaOlio(request.getLahtoKentta(), lahtoZDT);
            // Weather saaMaaranPaa = haeSaaOlio(maaranPaa);

            // FlightPlanner planner = FXML_FPLMain.getFlightPlanner();
            planner.setSaaLahto(saaLahto);
            // planner.setSaaMaapanpaa(saaMaaranPaa);
            planner.setMaaranpaaKentta(request.getMaaranpaa());

            String notamTeksti = FPL_Code.Notam.haeNotam("paikka");

            List<Notam.NotamOlio> notamit = Notam.teeNotamOliot(notamTeksti);
            for (Notam.NotamOlio n : notamit) {
                System.out.println(n);
            }

            planner.setNotamOliot(notamit);
            planner.setNotam(notamTeksti);

            System.out.println(saaLahto);

            // Haetaan koordinaatit
            Point lahtoKoord = etsiKoordinaatit(request.getLahtoKentta());
            Point maaranpaaKoord = etsiKoordinaatit(request.getMaaranpaa());

            // Suunnitellaan reitti
            List<Point> reitti = planner.teeReitti(lahtoKoord, maaranpaaKoord);

            String html = HTMLhandler.teeHTML(planner);
            String tiedostoNimi = "suunnitelma_" + System.currentTimeMillis() + ".html";

            // Tallenna resources/static-kansioon
            Path staticDir = Paths.get(System.getProperty("user.dir"), "Project", "resources", "static");
            Files.writeString(staticDir.resolve(tiedostoNimi), html, StandardCharsets.UTF_8);

            return ResponseEntity.ok("/" + tiedostoNimi);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Virhe: " + e.getMessage());
        }
    }
}