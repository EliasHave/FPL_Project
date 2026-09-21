package FPL_Code.Model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Point {

    public String name;
    public double lat;
    public double lon;
    public int alt;  //korkeus ft
    public String perustelu;  //tekoälyn perustelu kyseiselle reittipisteelle(esim huonon sään tai kielletyn ilmatilan väistäminen)
    public int wpNumber;  //Reittipisteen "järjestys numero"

    public Point(String name, double lat, double lon) {
        this.name = name;
        this.lat = lat;
        this.lon = lon;
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
