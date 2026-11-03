package FPL_Code.Utility;

import FPL_Code.Model.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Puhdas utility-luokka valmiin HTML-suunnitelmasivun luontiin.
 * Ei sisällä liiketoimintalogiikkaa, vain datan muotoilua.
 */
public final class HtmlGenerator {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final DateTimeFormatter AIKA_FORMAATTI = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private HtmlGenerator() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Ottaa sisään kaiken käsitellyn datan ja palauttaa valmiin HTML-merkkijonon selaimelle.
     */
    public static String generoiTulossivu(FlightRequest pyynto, LentosuunnitelmaTulos aiTulos, DTO_Boss paketti) {
        try {
            InputStream inputStream = HtmlGenerator.class.getResourceAsStream("/FPL_Template2.html");
            if (inputStream == null) {
                throw new RuntimeException("Kriittinen virhe: FPL_Template2.html puuttuu luokkapolusta!");
            }
            String html = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            // Lasketaan matka ja ETA
            double matkaNm = 0;
            if (paketti.getLahto() != null && paketti.getKohde() != null) {
                matkaNm = GeoMathUtils.haversineNm(
                        paketti.getLahto().getLat(), paketti.getLahto().getLon(),
                        paketti.getKohde().getLat(), paketti.getKohde().getLon()
                );
            }
            int nopeusKt = paketti.getKone() != null ? paketti.getKone().getCruiseSpeed() : 0;

            String etdStr = (pyynto.getLahtoAika() != null && !pyynto.getLahtoAika().isEmpty()) ? pyynto.getLahtoAika() : "Ei ilmoitettu";
            String etaTeksti = TimeUtils.laskeEta(etdStr, matkaNm, nopeusKt);

            // 2. Ylätunnisteen tiedot
            html = html.replace("{{DEPARTURE}}", paketti.getLahto() != null ? paketti.getLahto().getPointName() : "N/A");
            html = html.replace("{{DESTINATION}}", paketti.getKohde() != null ? paketti.getKohde().getPointName() : "N/A");
            html = html.replace("{{ETD}}", "ETD: " + etdStr);

            // ETA + Matkan pituus
            html = html.replace("{{ETA}}", "ETA: " + etaTeksti + String.format(java.util.Locale.US, " | Etäisyys: %.1f NM", matkaNm));
            html = html.replace("{{LUOMIS_AIKA}}", "Suunnitelma luotu (UTC): " + java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC).format(AIKA_FORMAATTI));

            // 3. Tekoälyn ja sään tekstit
            html = html.replace("{{WX_LAHTO}}", muotoileSaa(paketti.getReitinSaa(), WeatherSamplePoint.PointType.DEPARTURE));
            html = html.replace("{{WX_REITTI}}", aiTulos != null && aiTulos.getSafetyConcerns() != null ? aiTulos.getSafetyConcerns() : "Ei merkittäviä reittiriskejä havaittu.");
            html = html.replace("{{WX_PAATE}}", muotoileSaa(paketti.getReitinSaa(), WeatherSamplePoint.PointType.DESTINATION));

            // 4. NOTAM-tekstit
            html = html.replace("{{NTM_LAHTO}}", muotoileNotamit(paketti.getNotamit(), Notam.NotamType.DEPARTURE));
            html = html.replace("{{NTM_REITTI}}", muotoileNotamit(paketti.getNotamit(), Notam.NotamType.EN_ROUTE));
            html = html.replace("{{NTM_PAATE}}", muotoileNotamit(paketti.getNotamit(), Notam.NotamType.DESTINATION));

            // 5. Lentokone ja Pilotti
            html = html.replace("{{AIRCRAFT}}", muotoileKone(paketti.getKone()));
            html = html.replace("{{PILOT}}", muotoilePilotti(paketti.getPilot()));

            // 6. JSON Injektiot karttaa varten
            html = html.replace("{{RELEVANT_AIRSPACES}}", listToGeoJson(paketti.getIlmatilat()));
            html = html.replace("{{RELEVANT_AIRPORTS}}", listToGeoJson(paketti.getLentokentat()));
            html = html.replace("{{RELEVANT_NAVAIDS}}", listToGeoJson(paketti.getNavaidit()));

            // 7. Reittipisteiden piirto Javascriptille
            html = html.replace("{{JS_POINTS}}", muotoileJsPisteet(aiTulos != null ? aiTulos.getReittiPisteet() : null));

            return html;

        } catch (Exception e) {
            e.printStackTrace();
            return "<html><body><h2>Kriittinen virhe HTML-generoinnissa</h2><pre>" + e.getMessage() + "</pre></body></html>";
        }
    }

    // ====================================================================================
    // PRIVATE APUMETODIT (Eristää purkulogiikan pois päämetodista)
    // ====================================================================================

    private static String muotoileSaa(List<WeatherSamplePoint> saat, WeatherSamplePoint.PointType tyyppi) {
        if (saat == null || saat.isEmpty()) return "Ei säätietoja.";

        return saat.stream()
                .filter(w -> w.getTyyppi() == tyyppi)
                .map(w -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Asema: ").append(w.getNimi() != null ? w.getNimi() : "Tuntematon").append("\n");

                    // Tuuli
                    sb.append("Tuuli: ").append(String.format("%03d/%d KT", w.getTuuliSuuntaDeg(), w.getTuuliNopeusKt()));
                    if (w.getTuuliPuuskaKt() > 0) {
                        sb.append(" (Puuskat ").append(w.getTuuliPuuskaKt()).append(" KT)");
                    }
                    sb.append("\n");

                    // Näkyvyys, Pilvet ja Ilmiöt
                    sb.append("Näkyvyys: ").append(w.getNakyvyysMetria() >= 9999 ? "10+ km" : w.getNakyvyysMetria() + " m").append("\n");

                    String pilvet = w.getPilviMaara() != null ? w.getPilviMaara() : "N/A";
                    if (w.getPilviKorkeusFt() > 0) {
                        pilvet += " @ " + w.getPilviKorkeusFt() + " ft";
                    }
                    sb.append("Pilvet: ").append(pilvet).append("\n");

                    sb.append("Sääilmiö: ").append((w.getSaaIlmio() != null && !w.getSaaIlmio().isEmpty()) ? w.getSaaIlmio() : "NSW (Ei merkittävää)").append("\n");

                    // Lämpö & QNH
                    sb.append("Lämpötila/Kastepiste: ")
                            .append(String.format(java.util.Locale.US, "%.1f°C / %.1f°C", w.getLampotilaC(), w.getKastepisteC()))
                            .append("\n");

                    sb.append("QNH (Ilmanpaine): ").append(w.getQnhHpa() > 0 ? "Q" + w.getQnhHpa() + " hPa" : "N/A").append("\n");

                    if (w.getRaakaMetar() != null && !w.getRaakaMetar().isEmpty()) {
                        sb.append("\nMETAR:\n").append(w.getRaakaMetar()).append("\n");
                    }
                    return sb.toString();
                })
                .collect(Collectors.joining("\n-------------------\n\n"));
    }

    private static String muotoileNotamit(List<Notam> notamit, Notam.NotamType tyyppi) {
        if (notamit == null || notamit.isEmpty()) return "Ei NOTAMeja.";

        String tulos = notamit.stream()
                .filter(n -> n.getTyyppi() == tyyppi)
                .map(Notam::getRaakaTeksti)
                .collect(Collectors.joining("\n\n"));

        return tulos.isEmpty() ? "Ei merkittäviä NOTAMeja tälle alueelle." : tulos;
    }

    private static String muotoileKone(Aircraft kone) {
        if (kone == null) return "Ei konetietoja.";

        return "--- PERUSTIEDOT ---\n" +
                "Rekisteritunnus: " + tarkistaString(kone.getRekNro()) + "\n" +
                "Konetyyppi: " + tarkistaString(kone.getKoneTyyppi()) + "\n" +
                "Kategoria: " + tarkistaString(kone.getKategoria()) + "\n\n" +

                "--- SUORITUSKYKY ---\n" +
                "Matkalentonopeus: " + kone.getCruiseSpeed() + " KT\n" +
                "Nousunopeus (Climb rate): " + kone.getClimbRate() + " ft/min\n" +
                "Max lentokorkeus (FL): " + kone.getFlightLevel() + "\n" +
                "Polttoaineen kulutus: " + kone.getKulutus() + " gal/h\n" +
                "Kantama (Range): " + kone.getRange() + " NM\n" +
                "Max lentoaika (Tauoton): " + kone.getMaxFightTime() + " min\n\n" +

                "--- POLTTOAINE ---\n" +
                "Tankin max kapasiteetti: " + kone.getFuelTankCapacity() + " gal\n" +
                "Käytettävissä (Usable): " + kone.getUsableFuel() + " gal\n" +
                "Reservivaatimus: " + kone.getReserveMin() + " min\n\n" +

                "--- PAINOTIEDOT ---\n" +
                "Tyhjäpaino (Empty): " + kone.getEmptyWeight() + " lbs\n" +
                "Max lentoonlähtö (MTOW): " + kone.getMTOW() + " lbs\n" +
                "Hyötykuorma (Useful Load): " + kone.getUsefulLoad() + " lbs\n" +
                "Rahtikuorma (Payload): " + kone.getPayLoad() + " lbs\n\n" +

                "--- AVIONIKKA ---\n" +
                "Transponderi: " + tarkistaString(kone.getTransponder()) + "\n" +
                "GPS: " + tarkistaString(kone.getGPS()) + "\n" +
                "Radio: " + tarkistaString(kone.getRadio());
    }

    private static String muotoilePilotti(Pilot pilot) {
        if (pilot == null) return "Ei pilottitietoja.";

        return "--- HENKILÖTIEDOT ---\n" +
                "Nimi: " + tarkistaString(pilot.getNimi()) + "\n" +
                "Syntymäaika: " + tarkistaString(pilot.getSyntymaAika()) + "\n" +
                "Sähköposti: " + tarkistaString(pilot.getSPosti()) + "\n" +
                "Puhelinnumero: " + tarkistaString(pilot.getPuhNro()) + "\n\n" +

                "--- PÄTEVYYDET ---\n" +
                "Lupakirjat/Kelpuutukset: " + tarkistaString(pilot.getLupaKirjat()) + "\n" +
                "Kokonaislentoaika: " + pilot.getKokemus() + " h";
    }

    private static String tarkistaString(String str) {
        return (str != null && !str.trim().isEmpty()) ? str : "-";
    }

    private static String muotoileJsPisteet(List<Point> pisteet) {
        if (pisteet == null || pisteet.isEmpty()) return "const points = [];";

        StringBuilder js = new StringBuilder("const points = [\n");
        for (Point p : pisteet) {
            js.append(String.format(java.util.Locale.US, "  {name: '%s', coords: [%.5f, %.5f]},\n",
                    p.getPointName(), p.getLat(), p.getLon()));
        }
        js.append("];\n");
        return js.toString();
    }

    private static String listToGeoJson(List<Feature> features) {
        if (features == null || features.isEmpty()) {
            return "{\"type\":\"FeatureCollection\",\"features\":[]}";
        }
        try {
            Map<String, Object> featureCollection = new HashMap<>();
            featureCollection.put("type", "FeatureCollection");
            featureCollection.put("features", features);
            return mapper.writeValueAsString(featureCollection);
        } catch (Exception e) {
            System.err.println("GeoJSON serialisointivirhe: " + e.getMessage());
            return "{\"type\":\"FeatureCollection\",\"features\":[]}";
        }
    }
}