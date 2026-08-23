package FPL_Code;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class WeatherSamplePoint {
    private double lat;
    private double lon;
    private ZonedDateTime aika;
    private String ennusteTeksti = ""; // tähän voi myöhemmin laittaa sääkuvauksen/metarin jne.
    private Map<String, Object> forecastData;

    public WeatherSamplePoint(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }

    public double getLat() { return lat; }
    public double getLon() { return lon; }
    public ZonedDateTime getAika() { return aika; }
    public String getEnnusteTeksti() { return ennusteTeksti; }

    public void setEnnusteTeksti(String teksti) {
        this.ennusteTeksti = teksti;
    }

    public void setAika(ZonedDateTime aika) {
        this.aika = aika;
    }


    public void setForecastData(Map<String, Object> data) {
        this.forecastData = data;
    }

    public Map<String, Object> getForecastData() {
        return forecastData;
    }

    @Override
    public String toString() {
        return String.format("Lat: %.5f, Lon: %.5f, Ennuste: %s", lat, lon, ennusteTeksti);
    }


    public void haeEnnuste(ObjectMapper mapper, boolean tehty) {
        WeatherSamplePoint p = this;
        try {
            double lat = p.getLat();
            double lon = p.getLon();

            // Haetaan suoraan ZonedDateTime-oliona
            ZonedDateTime aika = p.getAika(); // oletetaan että tämä ei ole null
            ZonedDateTime utc = aika.withZoneSameInstant(ZoneOffset.UTC);

            // Pyöristetään lähimpään tuntiin
            int minuutit = utc.getMinute();
            ZonedDateTime pyoristetty;
            if (minuutit >= 30) {
                // Jos yli puolen tunnin → seuraava tunti
                pyoristetty = utc.truncatedTo(ChronoUnit.HOURS).plusHours(1);
            } else {
                // Muuten → alas tuntiin
                pyoristetty = utc.truncatedTo(ChronoUnit.HOURS);
            }

            // Formatoidaan
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");
            String utcAika = pyoristetty.format(formatter);

            System.out.println("🌐 WeatherSamplePoint.getAika(): " + aika);
            System.out.println("🔄 Muunnettu UTC-aika: " + utcAika);

            // API-kutsu
            String url = String.format(
                    Locale.US,
                    "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f"
                            + "&hourly=temperature_2m,cloudcover,visibility,windspeed_10m,winddirection_10m,"
                            + "precipitation,relative_humidity_2m,pressure_msl,dew_point_2m,"
                            + "windspeed_1000hPa,winddirection_1000hPa,"
                            + "windspeed_850hPa,winddirection_850hPa,temperature_850hPa,"
                            + "windspeed_700hPa,winddirection_700hPa,temperature_700hPa,"
                            + "windspeed_500hPa,winddirection_500hPa,temperature_500hPa"
                            + "&timezone=UTC",
                    lat, lon
            );

            JsonNode root = mapper.readTree(new java.net.URL(url));
            JsonNode tuntiLista = root.path("hourly");
            JsonNode ajat = tuntiLista.path("time");

            /**
            if (!tehty) {
                System.out.println("📅 Saatavilla olevat ajat:");
                for (JsonNode a : ajat) {
                    System.out.println("  - " + a.asText());
                }
                tehty = true;   // tämä tulostaa ajat kaikile pisteille joka ei ole optimaalista
            }
             **/

            int indeksi = -1;
            for (int i = 0; i < ajat.size(); i++) {
                if (ajat.get(i).asText().equals(utcAika)) {
                    indeksi = i;
                    break;
                }
            }

            if (indeksi == -1) {
                p.setEnnusteTeksti("❌ Sääennuste puuttuu ajalle " + utcAika);
                return;
            }

            // Haetaan arvot
            double temp = tuntiLista.path("temperature_2m").get(indeksi).asDouble();
            double dew = tuntiLista.path("dew_point_2m").get(indeksi).asDouble();
            double pilvikorkeusFt = (temp - dew) * 400.0;
            int pilviFt = (int) Math.round(pilvikorkeusFt);

            double pilvisyys = tuntiLista.path("cloudcover").get(indeksi).asDouble();
            double visibility = tuntiLista.path("visibility").get(indeksi).asDouble();
            double wind = tuntiLista.path("windspeed_10m").get(indeksi).asDouble();
            double windDir = tuntiLista.path("winddirection_10m").get(indeksi).asDouble();

            double wind1000 = tuntiLista.path("windspeed_1000hPa").get(indeksi).asDouble();
            double dir1000  = tuntiLista.path("winddirection_1000hPa").get(indeksi).asDouble();

            double wind3000 = tuntiLista.path("windspeed_700hPa").get(indeksi).asDouble();
            double dir3000  = tuntiLista.path("winddirection_700hPa").get(indeksi).asDouble();
            double temp3000 = tuntiLista.path("temperature_700hPa").get(indeksi).asDouble();

            double wind6000 = tuntiLista.path("windspeed_500hPa").get(indeksi).asDouble();
            double dir6000  = tuntiLista.path("winddirection_500hPa").get(indeksi).asDouble();
            double temp6000 = tuntiLista.path("temperature_500hPa").get(indeksi).asDouble();

            double sade = tuntiLista.path("precipitation").get(indeksi).asDouble();
            double humidity = tuntiLista.path("relative_humidity_2m").get(indeksi).asDouble();
            double paine = tuntiLista.path("pressure_msl").get(indeksi).asDouble();

            // Rakennetaan strukturoitu data Map<String, Object>
            Map<String, Object> forecast = new LinkedHashMap<>();
            forecast.put("utcTime", utcAika);
            forecast.put("temperature_C", temp);
            forecast.put("dewPoint_C", dew);
            forecast.put("cloudBase_ft", pilviFt);
            forecast.put("cloudCover_pct", pilvisyys);
            forecast.put("visibility_m", visibility);
            forecast.put("wind_mps", wind);
            forecast.put("windDirection_deg", windDir);
            forecast.put("wind_1000m_mps", wind1000);
            forecast.put("windDir_1000m_deg", dir1000);
            forecast.put("wind_3000m_mps", wind3000);
            forecast.put("windDir_3000m_deg", dir3000);
            forecast.put("temp_3000m_C", temp3000);
            forecast.put("wind_6000m_mps", wind6000);
            forecast.put("windDir_6000m_deg", dir6000);
            forecast.put("temp_6000m_C", temp6000);
            forecast.put("precip_mm", sade);
            forecast.put("humidity_pct", humidity);
            forecast.put("pressure_hPa", paine);

            p.setForecastData(forecast);

            // Rakennetaan ennusteteksti
            StringBuilder sb = new StringBuilder();
            sb.append("Aika (UTC): ").append(utcAika).append("\n");
            sb.append("Lämpötila: ").append(temp).append(" °C\n");
            sb.append("Kastepiste: ").append(dew).append(" °C\n");
            sb.append("Pilvikorkeus (laskennallinen): ").append(pilviFt).append(" ft AGL\n");
            sb.append("Pilvisyys: ").append(tuntiLista.path("cloudcover").get(indeksi).asText()).append(" %\n");
            sb.append("Näkyvyys: ").append(tuntiLista.path("visibility").get(indeksi).asText()).append(" m\n");
            sb.append("Tuuli: ").append(tuntiLista.path("windspeed_10m").get(indeksi).asText()).append(" m/s\n").append(" Suunta: ").append(tuntiLista.path("winddirection_10m").get(indeksi).asText()).append(" Deg\n");
            sb.append("Sade: ").append(tuntiLista.path("precipitation").get(indeksi).asText()).append(" mm\n");
            sb.append("Ilmankosteus: ").append(tuntiLista.path("relative_humidity_2m").get(indeksi).asText()).append(" %\n");
            sb.append("Ilmanpaine: ").append(tuntiLista.path("pressure_msl").get(indeksi).asText()).append(" hPa");
            sb.append("\n--- Korkeusennusteet ---\n");
            sb.append("1000 m: Tuuli ").append(wind1000).append(" m/s, Suunta ").append(dir1000).append("°\n");
            sb.append("3000 m: Tuuli ").append(wind3000).append(" m/s, Suunta ").append(dir3000).append("°, Lämpö ").append(temp3000).append(" °C\n");
            sb.append("6000 m: Tuuli ").append(wind6000).append(" m/s, Suunta ").append(dir6000).append("°, Lämpö ").append(temp6000).append(" °C\n");


            p.setEnnusteTeksti(sb.toString());

        } catch (Exception e) {
            p.setEnnusteTeksti("⚠️ Säänhakuvirhe: " + e.getMessage());
        }
    }
}
