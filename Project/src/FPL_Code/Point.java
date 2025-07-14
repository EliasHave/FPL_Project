package FPL_Code;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Point {

    public String name;
    public double lat;
    public double lon;

    public Point(String name, double lat, double lon) {
        this.name = name;
        this.lat = lat;
        this.lon = lon;
    }

    /**
     * etsii parametrina tulevan lentokentän koordinaatit
     * palauttaa uuden piste olion jonka atribuutteina nimi latitude, longitude
     * @param icao kenttä jonka koordinaatit halutaan etsiä
     */
    public static Point etsiKoordinaatit(String icao) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(Point.class.getResourceAsStream("/airports.csv")))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("id,")) continue;

                String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                if (parts.length > 5) {
                    String code = parts[1].replace("\"", "");
                    if (icao.equalsIgnoreCase(code)) {
                        String name = parts[3].replace("\"", "");
                        double lat = Double.parseDouble(parts[4]);
                        double lon = Double.parseDouble(parts[5]);

                        return new Point(name, lat, lon);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("❌ Virhe tiedoston luvussa: " + e.getMessage());
        }
        return null; // Ei löytynyt
    }


    public String getPointName() {
        return name;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }


    @Override
    public String toString() {
        return "Piste: " + name + ", lat: " + lat + ", lon: " + lon;
    }
}
