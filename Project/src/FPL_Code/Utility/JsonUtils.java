package FPL_Code.Utility;

// Importoi Jacksonin ObjectMapperit yms...

import FPL_Code.Model.DTO_Boss;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Puhdas utility luokka sovellukselle spesifiin Json tiedostojen parsintaan ja muodostamiseen
 */
public final class JsonUtils {

    private static final ObjectMapper mapper = new ObjectMapper();

    private JsonUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Luo tekoälylle menevän JSON-paketin kerätystä datasta
     */
    public static String rakennaAiPromptJson(DTO_Boss dtoBossData) {
        ObjectNode root = mapper.createObjectNode();

        // 1. Lentokone ja Pilotti
        if (dtoBossData.getKone() != null) {
            root.put("aircraft_type", dtoBossData.getKone().getKoneTyyppi());
        }
        if (dtoBossData.getPilot() != null) {
            root.put("pilot_experience_hours", dtoBossData.getPilot().getKokemus());
        }

        // 2. Reitti
        if (dtoBossData.getLahto() != null && dtoBossData.getKohde() != null) {
            root.put("departure_icao", dtoBossData.getLahto().getPointName());
            root.put("destination_icao", dtoBossData.getKohde().getPointName());
        }

        // 3. Yhteenvedot (Tähän voi myöhemmin lisätä for-loopit, jotka poimivat NOTAM-tekstit)
        int notamCount = (dtoBossData.getNotamit() != null) ? dtoBossData.getNotamit().size() : 0;
        int airspaceCount = (dtoBossData.getIlmatilat() != null) ? dtoBossData.getIlmatilat().size() : 0;

        root.put("notams_on_route", notamCount);
        root.put("airspaces_on_route", airspaceCount);

        return root.toString();
    }

    /**
     * Muuttaa listan Feature-olioita yhtenäiseksi GeoJSON-stringiksi.
     * Korvaa FlightPlanner.kirjoitaGeoJson() -metodin[cite: 1].
     */
    public static String rakennaGeoJsonString(java.util.List<Object> features) {
        // TÄNNE TULEE KOODI: GeoJSON feature collectionin kokoaminen[cite: 1]
        return "{\"type\":\"FeatureCollection\",\"features\":[]}";
    }
}
