package FPL_Code.Service;

//import FPL_Code.Model.FlightRequest;
import FPL_Code.Model.*;
import FPL_Code.Service.AviationData.AviationDataService;
import FPL_Code.Utility.HtmlGenerator;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Singleton joka koordinoi koko lennonsuunnittelu prosessin
 * Ei itse toteuta ilmailullista laskentaa tai tiedonhakua vaan käyttää apunaan siihen tehtyjä luokkia
 */
@Service
public class FlightPlanningService {

    private final AviationDataService aviationDataService;
    private final RouteWeatherService weatherService;
    private final SpatialFilterService spatialService;
    private final NotamService notamService;
    private final AiService aiService;

    public FlightPlanningService(AviationDataService aviationDataService,
                                 RouteWeatherService weatherService,
                                 SpatialFilterService spatialService,
                                 NotamService notamService,
                                 AiService aiService) {

        this.aviationDataService = aviationDataService;
        this.weatherService = weatherService;
        this.spatialService = spatialService;
        this.notamService = notamService;
        this.aiService = aiService;
    }

    /**
     * TÄMÄ ON SOVELLUKSEN SYDÄN. Koko lennonsuunnittelun aikajana.
     * Tämä suoritetaan alusta loppuun sille yhdelle tietylle käyttäjälle omassa säikeessään.
     */
    public String luoSuunnitelma(FlightRequest request) {

        // --- ASKEL 1: PERUSTIETOJEN KOKOAMINEN ---
        Aircraft kone = rakennaKone(request);
        Pilot pilot = rakennaPilot(request);

        // Haetaan koordinaatit välimuistista
        Point lahtoPiste = aviationDataService.etsiLentokentta(request.getLahtoKentta());
        Point kohdePiste = aviationDataService.etsiLentokentta(request.getMaaranpaa());

        // Varmistetaan, että kentät löytyivät (muuten ei voida jatkaa)
        if (lahtoPiste == null || kohdePiste == null) {
            throw new IllegalArgumentException("Lähtö- tai kohdekenttää ei löytynyt tietokannasta!");
        }

        // --- ASKEL 2: SÄÄN HAKU (Nopeusriippuvuus) ---
        List<WeatherSamplePoint> reitinSaa = weatherService.kartoitaReitinSaa(
                lahtoPiste,
                kohdePiste,
                request.getLahtoAika(),
                kone.getCruiseSpeed()
        );

        // --- ASKEL 3: GEOMETRINEN SUODATUS ---
        // Haetaan kaikki maailman data välimuistista...
        List<Feature> kaikkiIlmatilat = aviationDataService.getKaikkiIlmatilat();
        List<Feature> kaikkiNavaidit = aviationDataService.getKaikkiNavaidit();
        List<Feature> kaikkiLentokentat = aviationDataService.getKaikkiLentokentat();

        // ...ja pyydetään Geometriapalvelua karsimaan ne reitin perusteella
        List<Feature> olennaisetIlmatilat = spatialService.suodataReitinLahelta(kaikkiIlmatilat, lahtoPiste, kohdePiste, 50.0);
        List<Feature> olennaisetNavaidit = spatialService.suodataReitinLahelta(kaikkiNavaidit, lahtoPiste, kohdePiste, 50.0);
        List<Feature> olennaisetLentokentat = spatialService.suodataReitinLahelta(kaikkiLentokentat, lahtoPiste, kohdePiste, 50.0);

        // --- ASKEL 4: NOTAMIEN HAKU ---
        // Selvitetään ensin alueet (FIR)
        List<String> firKoodit = spatialService.selvitaReitinFirKoodit(kaikkiIlmatilat, lahtoPiste, kohdePiste);

        // NotamService hoitaa loput: Se hakee Lähtö, Kohde ja FIR -notamit yhdellä API-kutsulla,
        // lukee FIR-notamien Q-rivit ja heittää roskiin ne, jotka eivät osu meidän lahtoPiste->kohdePiste -reitille!
        List<Notam> relevantitNotamit = notamService.haeJaSuodataNotamit(
                lahtoPiste.getPointName(),
                kohdePiste.getPointName(),
                firKoodit,
                lahtoPiste,
                kohdePiste
        );

        // --- ASKEL 5: PAKETOINTI TEKOÄLYLLE ---
        // Kaikki kerätty data sullotaan DTO_Boss -laatikkoon
        DTO_Boss aiPaketti = new DTO_Boss(
                olennaisetIlmatilat,
                olennaisetLentokentat,
                olennaisetNavaidit,
                reitinSaa,
                relevantitNotamit,
                kone,
                pilot,
                lahtoPiste,
                kohdePiste
        );

        // --- ASKEL 6: TEKOÄLYANALYYSI ---
        LentosuunnitelmaTulos aiTulos = aiService.analysoiReitti(aiPaketti);

        // --- ASKEL 7: HTML:N GENEROINTI ---
        String valmisHtml = HtmlGenerator.generoiTulossivu(request, aiTulos, aiPaketti);

        return valmisHtml;
    }


    /**
     * Metodi joka rakentaa parametrina tulevasta FlightRequest oliosta Aircraft olion ja palauttaa tämän takaisin
     * @param req FlightRequest olio josta otetaan tiedot Aircraft olion luomista varten
     * @return Palauttaa FlightRequestin tiedoista rakennetun Aircraft olion
     */
    private Aircraft rakennaKone(FlightRequest req) {
        Aircraft kone = new Aircraft();

        // Perustiedot
        kone.setRekNro(req.getRekNro());
        kone.setKoneTyyppi(req.getKoneTyyppi());
        kone.setKategoria(req.getKategoria());

        // Suorituskyky
        kone.setCruiseSpeed((int) Math.round(req.getCruiseSpeed()));
        kone.setClimbRate((int) Math.round(req.getClimbRate()));
        kone.setFlightLevel((int) Math.round(req.getMaxAltitude())); // Maps to maxAltitude
        kone.setKulutus((int) Math.round(req.getKulutus()));
        kone.setRange((int) Math.round(req.getRange()));
        kone.setMaxFightTime((int) Math.round(req.getMaxFlightTime()));

        // Polttoaine
        kone.setFuelTankCapacity((int) Math.round(req.getTankSize())); // Maps to tankSize
        kone.setUsableFuel((int) Math.round(req.getUsableFuel()));
        kone.setReserveMin((int) Math.round(req.getReserve())); // Maps to reserve

        // Painotiedot
        kone.setEmptyWeight((int) Math.round(req.getEmptyWeight()));
        kone.setMTOW((int) Math.round(req.getMtow()));
        kone.setUsefulLoad((int) Math.round(req.getUsefulLoad()));
        kone.setPayLoad((int) Math.round(req.getPayLoad()));

        // Navigointivarustus
        kone.setTransponder(req.getTransponder());
        kone.setGPS(req.getGps());
        kone.setRadio(req.getRadio());

        return kone;
    }


    /**
     * Metodi joka rakentaa parametrina tulevasta FlightRequest oliosta Pilot olion ja palauttaa tämän takaisin
     * @param req FlightRequest olio josta otetaan tiedot Pilot olion luomista varten
     * @return Palauttaa FlightRequestin tiedoista rakennetun Pilot olion
     */
    private Pilot rakennaPilot(FlightRequest req) {
        Pilot pilotti = new Pilot();

        pilotti.setNimi(req.getNimi());
        pilotti.setSyntymaAika(req.getSyntymapaiva());
        pilotti.setSPosti(req.getSposti());
        pilotti.setPuhNro(req.getPuhnro());
        pilotti.setLupaKirjat(req.getLupakirja());
        pilotti.setKokemus(req.getKokemus());

        return pilotti;
    }
}
