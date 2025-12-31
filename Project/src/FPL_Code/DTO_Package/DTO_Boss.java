package FPL_Code.DTO_Package;

import FPL_Code.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class DTO_Boss {

    private List<Feature> ilmatilatDTO;
    private List<Feature> lentokentatDTO;
    private List<Feature> navaiditDTO;
    private List<WeatherSamplePointDTO> saaDTO;
    private List<NotamOlioDTO> notamitDTO;
    private Aircraft koneDTO;
    private Pilot pilotDTO;
    private WeatherDTO SaaLahtoDTO;
    private WeatherDTO saaMaaranpaaDTO;


    public DTO_Boss(List<Feature> ilmatilat, List<Feature> lentokentat, List<Feature> navaidit, List<WeatherSamplePointDTO> saa, List<NotamOlioDTO> notamit, Aircraft kone, Pilot pilot, WeatherDTO saaLahto, WeatherDTO saaMaaranpaa) {
        this.ilmatilatDTO = ilmatilat;
        this.lentokentatDTO = lentokentat;
        this.navaiditDTO = navaidit;
        this.saaDTO = saa;
        this.notamitDTO = notamit;
        this.koneDTO = kone;
        this.pilotDTO = pilot;
        this.SaaLahtoDTO = saaLahto;
        this.saaMaaranpaaDTO = saaMaaranpaa;
    }

    public List<Feature> getIlmatilatDTO() {
        return ilmatilatDTO;
    }
    public List<Feature> getLentokentatDTO() {
        return lentokentatDTO;
    }
    public List<Feature> getNavaiditDTO() {
        return navaiditDTO;
    }
    public List<WeatherSamplePointDTO> getSaaDTO() {
        return saaDTO;
    }
    public List<NotamOlioDTO> getNotamitDTO() {
        return notamitDTO;
    }
    public Aircraft getKoneDTO() {
        return koneDTO;
    }
    public Pilot getPilotDTO() {
        return pilotDTO;
    }
    public WeatherDTO getSaaLahtoDTO() {
        return SaaLahtoDTO;
    }
    public WeatherDTO getSaaMaaranpaaDTO() {
        return saaMaaranpaaDTO;
    }


    /**
     * Muotoilee DTO_Boss olion sisällön json tyyliseksi merkkijonoksi joka toimii myös tekoälyn inputtina
     * @return
     */
    public String toAiInputString() {
        ObjectMapper mapper = new ObjectMapper();

        ObjectNode root = mapper.createObjectNode();

        //Geometriat kuten ilmatilat ja lentokentätä
        ArrayNode airspaces = mapper.createArrayNode();
        if (ilmatilatDTO != null) {
            ilmatilatDTO.forEach(f -> airspaces.add(mapper.valueToTree(f)));
        }
        root.set("airspaces", airspaces);

        ArrayNode airports = mapper.createArrayNode();
        if (lentokentatDTO != null) {
            lentokentatDTO.forEach(f -> airports.add(mapper.valueToTree(f)));
        }
        root.set("airports", airports);

        ArrayNode navaids = mapper.createArrayNode();
        if (navaiditDTO != null) {
            navaiditDTO.forEach(f -> navaids.add(mapper.valueToTree(f)));
        }
        root.set("navaids", navaids);

        //Sääpisteet
        ArrayNode weatherPoints = mapper.createArrayNode();
        if (saaDTO != null) {
            saaDTO.forEach(w -> weatherPoints.add(mapper.valueToTree(w)));
        }
        root.set("weather_points", weatherPoints);

        //Notamit
        ArrayNode notams = mapper.createArrayNode();
        if (notamitDTO != null) {
            notamitDTO.forEach(n -> notams.add(mapper.valueToTree(n)));
        }
        root.set("notams", notams);


        root.set("aircraft", mapper.valueToTree(koneDTO));
        root.set("pilot", mapper.valueToTree(pilotDTO));
        root.set("departure_weather", mapper.valueToTree(SaaLahtoDTO));
        root.set("arrival_weather", mapper.valueToTree(saaMaaranpaaDTO));

        try {
            return mapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException("AI input JSON generation failed", e);
        }
    }



    /**
     * Aliohjelma joka tekee dto versiot parametrina tulevista listoista/olioista
     * Kirjoittaa myös 2 json tiedostoa datasta
     * Voisi palauttaa DTO_Boss olion soka sisältäisi tiedot noista pienemmistä asioista
      * @param ilmatilat
     * @param lentokentat
     * @param navaidit
     * @param saa
     * @param notamit
     * @param kone
     * @param pilot
     * @param saaLahto
     * @param saaMaaranpaa
     */
    public static DTO_Boss haeDTO(java.util.List<Feature> ilmatilat, List<Feature> lentokentat, List<Feature> navaidit, List<WeatherSamplePoint> saa, List< Notam.NotamOlio> notamit, Aircraft kone, Pilot pilot, Weather saaLahto, Weather saaMaaranpaa) {

        List<NotamOlioDTO> notamitDTO = NotamOlioDTO.teeNotamitDTO(notamit);
        List<WeatherSamplePointDTO> saaDTO = WeatherSamplePointDTO.haeSaaPisteetDTO(saa);
        WeatherDTO saaLahtoDTO = WeatherDTO.haeSaaDTO(saaLahto);
        WeatherDTO saaMaaranpaaDTO = WeatherDTO.haeSaaDTO(saaMaaranpaa);
        // tähän pitäisi saada käskytykset yksittäisille dto luokille tekemään dto oliot parametrina tulevista normaaleista versioista
        DTO_Boss dtoPalautus = new DTO_Boss(ilmatilat, lentokentat, navaidit, saaDTO, notamitDTO, kone, pilot, saaLahtoDTO, saaMaaranpaaDTO);

        kirjoitaGeoJson(dtoPalautus,"features.geojson");
        kirjoitaHybridJson(dtoPalautus, "flight_input.json");

        return dtoPalautus;
    }


    /**
     * metodi joka kirjoittaa geojson tiedoston kaikista lentoon liittyvistä asioista mitkä sisältävät geometriaa esim ilmatilat, lentokentät yms
     * @param filename
     */
    public static void kirjoitaGeoJson(DTO_Boss data, String filename) {
        ObjectMapper mapper = new ObjectMapper();

        ObjectNode featureCollection = mapper.createObjectNode();
        featureCollection.put("type", "FeatureCollection");

        ArrayNode features = mapper.createArrayNode();

        // lisätään ilmatilat, lentokentät, navaidit
        if (data.getIlmatilatDTO() != null) {
            data.getIlmatilatDTO().forEach(f -> features.add(mapper.valueToTree(f)));
        }
        if (data.getLentokentatDTO() != null) {
            data.getLentokentatDTO().forEach(f -> features.add(mapper.valueToTree(f)));
        }
        if (data.getNavaiditDTO() != null) {
            data.getNavaiditDTO().forEach(f -> features.add(mapper.valueToTree(f)));
        }
        if (data.getSaaDTO() != null) {
            data.getSaaDTO().forEach(f -> features.add(mapper.valueToTree(f)));
        }

        featureCollection.set("features", features);

        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(filename), featureCollection);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * metodi joka kirjoittaa yhden ison json tiedoston kaikista lentoon littyvistä parametreista ja joka myöhemmin toimii tekoälylle inputtina
     * @param filename
     */
    public static void kirjoitaHybridJson(DTO_Boss data, String filename) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();

        // Features-tyyppiset
        ArrayNode features = mapper.createArrayNode();
        if (data.getIlmatilatDTO() != null) {
            data.getIlmatilatDTO().forEach(f -> features.add(mapper.valueToTree(f)));
        }
        if (data.getLentokentatDTO() != null) {
            data.getLentokentatDTO().forEach(f -> features.add(mapper.valueToTree(f)));
        }
        if (data.getNavaiditDTO() != null) {
            data.getNavaiditDTO().forEach(f -> features.add(mapper.valueToTree(f)));
        }
        if (data.getSaaDTO() != null) {
            data.getSaaDTO().forEach(f -> features.add(mapper.valueToTree(f)));
        }
        root.set("features", features);

        // NOTAMit
        if (data.getNotamitDTO() != null) {
            root.set("notams", mapper.valueToTree(data.getNotamitDTO()));
        }

        // Aircraft ja Pilot
        if (data.getKoneDTO() != null) {
            root.set("aircraft", mapper.valueToTree(data.getKoneDTO()));
        }
        if (data.getPilotDTO() != null) {
            root.set("pilot", mapper.valueToTree(data.getPilotDTO()));
        }

        // Weather lähtö ja määränpää
        if (data.getSaaLahtoDTO() != null) {
            root.set("departure_weather", mapper.valueToTree(data.getSaaLahtoDTO()));
        }
        if (data.getSaaMaaranpaaDTO() != null) {
            root.set("arrival_weather", mapper.valueToTree(data.getSaaMaaranpaaDTO()));
        }

        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(filename), root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
