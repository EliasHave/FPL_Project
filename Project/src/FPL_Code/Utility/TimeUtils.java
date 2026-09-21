package FPL_Code.Utility;

import java.time.ZonedDateTime;

/**
 * Puhdas utility luokka aika sovellukselle spesifiin aikaparsintaan
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
        return ZonedDateTime.now();
    }
}
