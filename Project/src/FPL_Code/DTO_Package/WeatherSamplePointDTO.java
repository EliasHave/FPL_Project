package FPL_Code.DTO_Package;

import FPL_Code.FlightPlanner;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WeatherSamplePointDTO {

    private double lat;
    private double lon;
    private String aika;
    private String ennusteTeksti = ""; // tähän voi myöhemmin laittaa sääkuvauksen/metarin jne.
    private Map<String, Object> forecastData;

    public double getLat() { return lat; }
    public double getLon() { return lon; }
    public String getAika() { return aika; }
    public String getEnnusteTeksti() { return ennusteTeksti; }
    public Map<String, Object> getForecastData() { return forecastData; }


    public WeatherSamplePointDTO(double lat, double lon, String aika, Map<String, Object> forecastData) {

        this.lat = lat;
        this.lon = lon;
        this.aika = aika;
        this.forecastData = forecastData;
    }


    public static List<WeatherSamplePointDTO> haeSaaPisteetDTO(List<FlightPlanner.WeatherSamplePoint> saaPisteet) {

        List<WeatherSamplePointDTO> saatDTO = new ArrayList<>();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

        for (FlightPlanner.WeatherSamplePoint sP : saaPisteet) {
           double lat = sP.getLat();
           double lon = sP.getLon();
           ZonedDateTime aikaZDT = sP.getAika();
           String aikaSTR = (aikaZDT != null) ? aikaZDT.format(fmt) : "ei tiedossa";

           Map<String, Object> forecastData = sP.getForecastData();

           WeatherSamplePointDTO sPDTO = new WeatherSamplePointDTO(lat, lon, aikaSTR, forecastData);
           saatDTO.add(sPDTO);
        }

        return saatDTO;
    }

}
