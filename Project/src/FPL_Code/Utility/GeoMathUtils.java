package FPL_Code.Utility;

import FPL_Code.Model.Point;

/**
 * Puhdas utility luokka geometriseen matematiikan laskentaan
 * Tilaton, ei tallenna mitään
 */
public final class GeoMathUtils {

    // Maapallon säde
    private static final double R_KM = 6371.0;

    // Estetään luokan instansiointi (Utility-luokkia ei saa luoda new-sanalla)
    private GeoMathUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Laskee kahden pisteen välisen etäisyyden kilometreinä (Haversine-kaava).
     */
    public static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);
        return 2 * R_KM * Math.asin(Math.sqrt(a));
    }

    /**
     * Siirtää annettua koordinaattia X kilometriä tiettyyn suuntaan.
     */
    public static Point siirraKoordinaattia(double lat, double lon, double etaisyysKm, double suuntaRad) {
        double phi1 = Math.toRadians(lat);
        double lambda1 = Math.toRadians(lon);
        double angularDistance = etaisyysKm / R_KM;

        double phi2 = Math.asin(Math.sin(phi1) * Math.cos(angularDistance) +
                Math.cos(phi1) * Math.sin(angularDistance) * Math.cos(suuntaRad));

        double lambda2 = lambda1 + Math.atan2(Math.sin(suuntaRad) * Math.sin(angularDistance) * Math.cos(phi1),
                Math.cos(angularDistance) - Math.sin(phi1) * Math.sin(phi2));

        return new Point("Siirretty", Math.toDegrees(phi2), Math.toDegrees(lambda2));
    }


    public static double laskeSuuntaRad(double lat1, double lon1, double lat2, double lon2) {
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double deltaLambda = Math.toRadians(lon2 - lon1);

        double y = Math.sin(deltaLambda) * Math.cos(phi2);
        double x = Math.cos(phi1) * Math.sin(phi2) -
                Math.sin(phi1) * Math.cos(phi2) * Math.cos(deltaLambda);

        return Math.atan2(y, x);
    }

}