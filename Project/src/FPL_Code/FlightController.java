package FPL_Code;

import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.time.Instant;

import static FPL_Code.Point.etsiKoordinaatit;
import static FPL_Code.Weather.haeSaaOlio;
import static FPL_Code.Weather.parseAjankohta;

@RestController
@RequestMapping("/api/flight")
public class FlightController {

    // Injektoidaan AviationDataService joka lataa ilmailutiedot GCS:stä Spring Bootin käynnistyessä.
    // Tämä service tarjoaa (melkein) maailmanlaajuisen ilmatila-, lentokenttä- ja navaid-datan muistista,
    // jolloin FlightPlanner voi suodattaa reitille olennaiset kohteet ilman tiedostolukuja.
    private final AviationDataService aviationDataService;

    // Muistissa oleva "varasto" valmiille suunnitelmille.
    // Avain = UUID (esim. "a3f8c2d1-..."), arvo = valmis HTML-string.
    private final Map<String, String> suunnitelmat = new ConcurrentHashMap<>();
    // Tallennetaan myös suunnitelmien luontiajat, jotta vanhentuneet suunnitelmat voidaan poistaa säännöllisesti.
    private final Map<String, Instant> luontiAjat = new ConcurrentHashMap<>();

    public FlightController(AviationDataService aviationDataService) {
        this.aviationDataService = aviationDataService;
    }


    public Map<String, String> getSuunnitelmat() {
        return suunnitelmat;
    }

    public Map<String, Instant> getLuontiAjat() {
        return luontiAjat;
    }


    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("FPL API toimii!");
    }


    @PostMapping("/suunnittele")
    public ResponseEntity<String> suunnitteleLento(@RequestBody FlightRequest request) {
        try {
            // Luodaan FlightPlanner-instanssi ja injektoidaan siihen AviationDataService.
            // Jokainen pyyntö saa oman FlightPlanner-instanssin jotta samanaikaiset
            // käyttäjät eivät sekoita toistensa tietoja (thread-safety).
            FlightPlanner planner = new FlightPlanner(aviationDataService);

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

            // Luodaan uniikki ID tälle suunnitelmalle
            String suunnitelmaId = UUID.randomUUID().toString();

            // Tallennetaan HTML muistiin tiedoston sijaan
            suunnitelmat.put(suunnitelmaId, html);
            luontiAjat.put(suunnitelmaId, Instant.now());

            // Palautetaan URL josta selain voi hakea suunnitelman
            return ResponseEntity.ok("/suunnitelma/" + suunnitelmaId);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Virhe: " + e.getMessage());
        }
    }


    @Scheduled(fixedRate = 3_600_000) // pyörii tunnin välein
    public void siivoaVanhatSuunnitelmat() {
        Instant rajapyykki = Instant.now().minus(1, ChronoUnit.HOURS); //poisetaan tuntia vanhemmat suunnitelmat
        int ennen = suunnitelmat.size();

        luontiAjat.entrySet().removeIf(e -> {
            if (e.getValue().isBefore(rajapyykki)) {
                suunnitelmat.remove(e.getKey());
                return true; // poistetaan myös luontiAjat-mapista
            }
            return false;
        });

        System.out.println("🧹 Siivous: poistettu " + (ennen - suunnitelmat.size()) + " vanhaa suunnitelmaa, jäljellä " + suunnitelmat.size());
    }
}