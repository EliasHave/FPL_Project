package FPL_Code.Service;

import FPL_Code.Model.Feature;
import FPL_Code.Model.Point;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Service
public class SpatialFilterService {

    private static final double KM_PER_DEGREE = 111.32; // Keskimääräinen etäisyys kilometreinä per asteluku maapallon pinnalla

    /**
     * Metodi joka suodattaa annetun Feature listan olioista ne pois jotka eivät geometrisesti sijaitse buffer alueella
     * joka muodostetaan lähtö ja kohde pisteen välille säteen määräämän kokoiseksi alueeksi
     * @param kaikkiFeaturet Lista kaikista Feture olioista joihin suodatus tehdään. Feature olio sisältää geometriaa joiden perusteella pystytään sanomaan jääkö se buffer alueelle vai ei
     * @param lahtoPiste Point olio joka sisältää koordinaatit ja joka toimii toisena geometrisena kiintopisteenä suodatus laskennassa
     * @param kohdePiste Point olio joka sisältää koordinaatit ja joka toimii toisena geometrisena kiintopisteenä suodatus laskennassa
     * @param puskuriKm luikuluku joka määrää kuinka suurelta alueelta suodatus tehdään. Yksikkö on KM
     * @return palauttaa uuden Feature listan koka on kopio alkuperäisestä mutta josta on karsittu bufferin ulkopuolelle jäävät esiintymät pois
     */
    public List<Feature> suodataReitinLahelta(List<Feature> kaikkiFeaturet, Point lahtoPiste, Point kohdePiste, double puskuriKm) {
        List<Feature> olennaiset = new ArrayList<>();

        // Luodaan JTS-työkalut metodin sisällä, jotta luokka pysyy täysin lankaturvallisena (Thread-Safe)
        GeometryFactory factory = new GeometryFactory();
        GeoJsonReader reader = new GeoJsonReader(factory);

        try {
            // 1. Luodaan reittiviiva (LineString)
            // HUOM: JTS ja GeoJSON käyttävät koordinaatteja muodossa (Longitude, Latitude) [X, Y]
            Coordinate[] reittiKoordinaatit = new Coordinate[]{
                    new Coordinate(lahtoPiste.getLon(), lahtoPiste.getLat()),
                    new Coordinate(kohdePiste.getLon(), kohdePiste.getLat())
            };
            LineString reittiViiva = factory.createLineString(reittiKoordinaatit);

            // 2. Luodaan puskuri (Buffer) reitin ympärille
            // Muutetaan kilometrit asteiksi JTS:ää varten
            double puskuriAsteina = puskuriKm / KM_PER_DEGREE;
            Geometry reittiPuskuri = reittiViiva.buffer(puskuriAsteina);

            // 3. Kahlataan kaikki alkiot läpi
            for (Feature f : kaikkiFeaturet) {
                if (f.geometry == null || f.geometry.isNull()) continue;

                try {
                    // Muutetaan Featuren JsonNode-geometria JTS-geometriaksi lennossa
                    Geometry kohteenGeometria = reader.read(f.geometry.toString());

                    // 4. Tarkistetaan leikkaako kohteen geometria reittipuskuria
                    if (reittiPuskuri.intersects(kohteenGeometria)) {
                        olennaiset.add(f); // Tallennetaan vain viite jos leikkaa
                    }
                } catch (Exception e) {
                    // Jos yksittäisen ilmatilan GeoJSON on viallinen, ohitetaan se vähin äänin
                }
            }
        } catch (Exception e) {
            System.err.println("Kriittinen virhe reitin suodatuksessa: " + e.getMessage());
        }

        return olennaiset;
    }


    /**
     * Metodi joka selvittää kaikki FIR koodit jotka jäävät kahden pisteen väliin
     * @param kaikkiIlmatilat Lista Feature olioita jotka sisältävät ilmatilojen geometriaa ja tietoja (tämä sisältää myös FIR ja UIR alueet)
     * @param lahtoPiste Point olio joka sisältää koordinaatit ja joka toimii toisena kiintopisteenä
     * @param kohdePiste Point olio joka sisältää koordinaatit ja joka toimii toisena kiintopisteenä
     * @return Palauttaa String listan joka sisältää kaikki FIR koodit jotka jäävät kahden pisteen väliiselle buffer alueelle
     */
    public List<String> selvitaReitinFirKoodit(List<Feature> kaikkiIlmatilat, Point lahtoPiste, Point kohdePiste) {
        java.util.Set<String> firKoodit = new java.util.HashSet<>();
        GeometryFactory factory = new GeometryFactory();
        GeoJsonReader reader = new GeoJsonReader(factory);

        try {
            Coordinate[] reittiKoordinaatit = new Coordinate[]{
                    new Coordinate(lahtoPiste.getLon(), lahtoPiste.getLat()),
                    new Coordinate(kohdePiste.getLon(), kohdePiste.getLat())
            };
            // 50 km puskuri FIR-alueiden hipomiseen
            Geometry reittiPuskuri = factory.createLineString(reittiKoordinaatit).buffer(50.0 / KM_PER_DEGREE);

            for (Feature f : kaikkiIlmatilat) {
                // OpenAIP tyypit: 2 = FIR, 3 = UIR
                int tyyppi = f.properties.path("type").asInt(-1);
                if (tyyppi != 2 && tyyppi != 3) continue;

                if (f.geometry != null && !f.geometry.isNull()) {
                    Geometry ilmatilanGeometria = reader.read(f.geometry.toString());

                    if (reittiPuskuri.intersects(ilmatilanGeometria)) {
                        // OpenAIP antaa nimen usein muodossa "EFIN FIR" tai "ESAA SWEDEN FIR"
                        String nimi = f.properties.path("name").asText("").trim().toUpperCase();

                        // Oletetaan, että ensimmäiset 4 kirjainta ovat ICAO-koodi
                        if (nimi.length() >= 4) {
                            String icao = nimi.substring(0, 4);
                            // Varmistetaan että koostuu vain kirjaimista
                            if (icao.matches("[A-Z]{4}")) {
                                firKoodit.add(icao);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Virhe FIR-koodien selvittämisessä: " + e.getMessage());
        }

        return new ArrayList<>(firKoodit);
    }

}
