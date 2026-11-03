package FPL_Code.Utility;

import java.time.ZonedDateTime;

/**
 * Puhdas utility luokka aika sovellukselle spesifiin aikaparsintaan ja laskentaan
 */
public final class TimeUtils {

    private TimeUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Parsii erilaisia ilmailun aikamuotoja (esim. 122340Z) ZonedDateTime-olioksi[cite: 17].
     */
    public static ZonedDateTime parseIlmailuAika(String ajankohtaStr) {
        // TÄNNE TULEE KOODI: Vanha Weather.parseAjankohta sisälmys[cite: 17]
        return ZonedDateTime.now(java.time.ZoneOffset.UTC);
    }

    /**
     * Laskee lennon keston (ETE) minuutteina.
     */
    public static int laskeLennonKestoMinuutteina(double etaisyysNm, int nopeusKt) {
        if (nopeusKt <= 0 || etaisyysNm <= 0) return 0;
        return (int) Math.round((etaisyysNm / nopeusKt) * 60.0);
    }


    /**
     * Laskee arvioidun saapumisajan (ETA) UTC-muodossa (String) "HH:mmZ", tai palauttaa keston, jos ETD puuttuu.
     */
    public static String laskeEta(String etdStr, double etaisyysNm, int nopeusKt) {
        int kestoMin = laskeLennonKestoMinuutteina(etaisyysNm, nopeusKt);
        if (kestoMin == 0) return "Tuntematon (puuttuva nopeus/etäisyys)";

        int hours = kestoMin / 60;
        int mins = kestoMin % 60;
        String kestoTeksti = String.format("%dh %02dmin", hours, mins);

        if (etdStr == null || etdStr.isEmpty()) {
            return "Arvioitu kesto: " + kestoTeksti;
        }

        try {
            // Parsii esim. "12:00Z" tai "1200Z"
            String puhdasEtd = etdStr.replaceAll("[^0-9]", "");
            if (puhdasEtd.length() >= 4) {
                int etdH = Integer.parseInt(puhdasEtd.substring(0, 2));
                int etdM = Integer.parseInt(puhdasEtd.substring(2, 4));

                int totalMins = etdH * 60 + etdM + kestoMin;
                int etaH = (totalMins / 60) % 24;
                int etaM = totalMins % 60;

                return String.format("%02d:%02dZ (Kesto: %s)", etaH, etaM, kestoTeksti);
            }
        } catch (Exception ignored) {}

        return "Arvioitu kesto: " + kestoTeksti;
    }
}
