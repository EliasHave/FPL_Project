package FPL_Code;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;

public class Feature {
    public JsonNode geometry;
    public JsonNode properties;

    public Feature(JsonNode geometry, JsonNode properties) {
        this.geometry = geometry;
        this.properties = properties;
    }


    /**
     * TODO: tämän voisi siirtää Feature Luokkaan
     * karsii parametrina tulevasta fetaure (ilmatila) oliosta turha kentät pois ja palauttaa karsitun feature olion
     * @return palauttaa uuden feature olion joka on karisittu veriso alkuperäisestä
     */
    public Feature karsiIlmatilanProperties() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode props = mapper.createObjectNode();

        Feature alkuperainen = this;

        // Nimi ja luokka
        props.put("name", alkuperainen.properties.path("name").asText(""));

        int icaoTyyppi = alkuperainen.properties.path("icaoClass").asInt(-1);
        String icaoClass = switch (icaoTyyppi) {
            case 0  -> "A";
            case 1  -> "B";
            case 2  -> "C";
            case 3  -> "D";
            case 4  -> "E";
            case 5  -> "F";
            case 6  -> "G";
            case 8  -> "Unclassified / Special Use Airspace (SUA)";
            default -> "tuntematon";
        };
        props.put("icaoClass", icaoClass);

        int tyyppi = alkuperainen.properties.path("type").asInt(-1);
        String tyyppiKirjain = switch (tyyppi) {
            case 0  -> "Other";
            case 1  -> "Restricted";
            case 2  -> "Danger";
            case 3  -> "Prohibited";
            case 4  -> "Controlled Tower Region (CTR)";
            case 5  -> "Transponder Mandatory Zone (TMZ)";
            case 6  -> "Radio Mandatory Zone (RMZ)";
            case 7  -> "Terminal Maneuvering Area (TMA)";
            case 8  -> "Temporary Reserved Area (TRA)";
            case 9  -> "Temporary Segregated Area (TSA)";
            case 10 -> "Flight Information Region (FIR)";
            case 11 -> "Upper Flight Information Region (UIR)";
            case 12 -> "Air Defense Identification Zone (ADIZ)";
            case 13 -> "Airport Traffic Zone (ATZ)";
            case 14 -> "Military Airport Traffic Zone (MATZ)";
            case 15 -> "Airway";
            case 16 -> "Military Training Route (MTR)";
            case 17 -> "Alert Area";
            case 18 -> "Warning Area";
            case 19 -> "Protected Area";
            case 20 -> "Helicopter Traffic Zone (HTZ)";
            case 21 -> "Gliding Sector";
            case 22 -> "Transponder Setting (TRP)";
            case 23 -> "Traffic Information Zone (TIZ)";
            case 24 -> "Traffic Information Area (TIA)";
            case 25 -> "Military Training Area (MTA)";
            case 26 -> "Control Area (CTA)";
            case 27 -> "ACC Sector (ACC)";
            case 28 -> "Aerial Sporting Or Recreational Activity";
            case 29 -> "Low Altitude Overflight Restriction";
            case 30 -> "Military Route (MRT)";
            case 31 -> "TSA/TRA Feeding Route (TFR)";
            case 32 -> "VFR Sector";
            case 33 -> "FIS Sector";
            case 34 -> "Lower Traffic Area (LTA)";
            case 35 -> "Upper Traffic Area (UTA)";
            case 36 -> "Military Controlled Tower Region (MCTR)";
            default -> "tuntematon";
        };
        props.put("type", tyyppiKirjain);

        // Korkeudet
        props.set("lowerLimit", muodostaKorkeusNode(alkuperainen.properties.path("lowerLimit")));
        props.set("upperLimit", muodostaKorkeusNode(alkuperainen.properties.path("upperLimit")));

        // byNotam
        if (alkuperainen.properties.path("byNotam").asBoolean(false)) {
            props.put("byNotam", true);
        }

        // Aukioloajat
        JsonNode hours = alkuperainen.properties.path("hoursOfOperation").path("operatingHours");
        if (hours.isArray()) {
            boolean kaikkiStandardia = true;
            for (JsonNode h : hours) {
                if (!h.path("startTime").asText("").equals("00:00") ||
                        !h.path("endTime").asText("").equals("00:00") ||
                        h.path("byNotam").asBoolean(false) ||
                        h.path("sunrise").asBoolean(false) ||
                        h.path("sunset").asBoolean(false) ||
                        h.path("publicHolidaysExcluded").asBoolean(false)) {
                    kaikkiStandardia = false;
                    break;
                }
            }

            if (kaikkiStandardia) {
                props.put("hoursOfOperation", "24/7");
            } else {
                ArrayNode slimmedHours = mapper.createArrayNode();
                for (JsonNode h : hours) {
                    ObjectNode d = mapper.createObjectNode();
                    d.put("dayOfWeek", h.path("dayOfWeek").asInt());
                    d.put("startTime", h.path("startTime").asText());
                    d.put("endTime", h.path("endTime").asText());
                    if (h.path("byNotam").asBoolean(false)) d.put("byNotam", true);
                    if (h.path("sunrise").asBoolean(false)) d.put("sunrise", true);
                    if (h.path("sunset").asBoolean(false)) d.put("sunset", true);
                    if (h.path("publicHolidaysExcluded").asBoolean(false)) d.put("publicHolidaysExcluded", true);
                    slimmedHours.add(d);
                }
                ObjectNode hoursWrapper = mapper.createObjectNode();
                hoursWrapper.set("operatingHours", slimmedHours);
                props.set("hoursOfOperation", hoursWrapper);
            }
        }

        return new Feature(alkuperainen.geometry, props);
    }


    private static ObjectNode muodostaKorkeusNode(JsonNode korkeus) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.put("value", korkeus.path("value").asInt(-1));

        int yksikko = korkeus.path("unit").asInt(-1);
        String yksikkoStr = switch (yksikko) {
            case 1 -> "ft MSL";
            case 6 -> "FL";
            default -> "tuntematon";
        };
        node.put("unit", yksikkoStr);

        return node;
    }


    /**
     * TODO: tämän voisi myös laittaa Feature luokkaan
     * Karsii lentokenttä-Featuresta pois tekoälyn kannalta epäolennaiset tiedot ja palauttaa uuden Feature-olion.
     */
    public Feature karsiLentokentanProperties() {
        ObjectMapper mapper = new ObjectMapper();

        Feature alkuperainen = this;

        ObjectNode slimProps = mapper.createObjectNode();
        slimProps.put("name", alkuperainen.properties.path("name").asText(""));
        slimProps.put("icaoCode", alkuperainen.properties.path("icaoCode").asText(""));
        // Lentokentän tyyppi selkokielellä
        int kenttaTyyppi = alkuperainen.properties.path("type").asInt(-1);
        String kenttaTyyppiNimi = switch (kenttaTyyppi) {
            case 0 -> "Airport (civil/military)";
            case 1 -> "Glider Site";
            case 2 -> "Civil Airfield";
            case 3 -> "International Airport";
            case 4 -> "Heliport Military";
            case 5 -> "Military Airfield";
            case 6 -> "Ultra Light Airfield";
            case 8 -> "Closed Airfield";
            case 9 -> "Airport resp. Airfield IFR";
            case 10 -> "Airfield Water";
            case 11 -> "Landing Strip";
            case 12 -> "Agricultural Landing Strip";
            case 13 -> "Altiport";
            default -> "tuntematon";
        };
        slimProps.put("type", kenttaTyyppiNimi);

        // Traffic type (0 = VFR, 1 = IFR, 2 = VFR+IFR)
        ArrayNode trafficArray = (ArrayNode) alkuperainen.properties.path("trafficType");
        List<String> liikenneTyypit = new ArrayList<>();
        for (JsonNode t : trafficArray) {
            switch (t.asInt()) {
                case 0 -> liikenneTyypit.add("VFR");
                case 1 -> liikenneTyypit.add("IFR");
                case 2 -> liikenneTyypit.add("VFR + IFR");
            }
        }
        if (!liikenneTyypit.isEmpty()) {
            slimProps.put("trafficType", String.join(", ", liikenneTyypit));
        }

        // Elevation mukaan + yksikkö selkokielellä
        JsonNode elevation = alkuperainen.properties.path("elevation");
        if (!elevation.isMissingNode()) {
            ObjectNode elev = mapper.createObjectNode();
            elev.put("value", elevation.path("value").asInt());

            int yksikko = elevation.path("unit").asInt(-1);
            String yksikkoStr = switch (yksikko) {
                case 0 -> "m MSL";
                case 1 -> "ft MSL";
                default -> "tuntematon";
            };
            elev.put("unit", yksikkoStr);

            slimProps.set("elevation", elev);
        }

        // PPR, vain jos true
        if (alkuperainen.properties.path("ppr").asBoolean(false)) {
            slimProps.put("ppr", true);
        }

        // Skydive, Winch, jne.
        if (alkuperainen.properties.path("skydiveActivity").asBoolean(false)) {
            slimProps.put("skydive", true);
        }
        if (alkuperainen.properties.path("winchOnly").asBoolean(false)) {
            slimProps.put("winchOnly", true);
        }

        // Radiotaajuudet (vain yksi tärkein)
        JsonNode freqs = alkuperainen.properties.path("frequencies");
        if (freqs.isArray() && freqs.size() > 0) {
            for (JsonNode f : freqs) {
                if (f.path("primary").asBoolean(true)) {
                    ObjectNode freq = mapper.createObjectNode();
                    freq.put("name", f.path("name").asText());
                    freq.put("value", f.path("value").asText());
                    slimProps.set("frequency", freq);
                    break;
                }
            }
        }

        // Kiitotiet
        JsonNode runways = alkuperainen.properties.path("runways");
        if (runways.isArray() && runways.size() > 0) {
            ArrayNode uusiRunwayt = mapper.createArrayNode();
            for (JsonNode rw : runways) {
                ObjectNode r = mapper.createObjectNode();
                r.put("designator", rw.path("designator").asText());
                r.put("heading", rw.path("trueHeading").asInt());

                // Pinta (yksinkertaistettu)
                int materialCode = rw.path("surface").path("mainComposite").asInt(-1);
                String pinta = switch (materialCode) {
                    case 0 -> "asfaltti";
                    case 1 -> "betoni";
                    case 2 -> "nurmi";
                    case 5 -> "sora";
                    case 12 -> "päällystetty";
                    default -> "tuntematon";
                };
                r.put("surface", pinta);

                // Mitat
                JsonNode dim = rw.path("dimension");
                ObjectNode mitat = mapper.createObjectNode();
                mitat.put("length_m", dim.path("length").path("value").asInt(-1));
                mitat.put("width_m", dim.path("width").path("value").asInt(-1));
                r.set("size", mitat);

                // Poikkeavuudet
                if (rw.path("pilotCtrlLighting").asBoolean(false)) {
                    r.put("pilotCtrlLighting", true);
                }
                if (rw.path("takeOffOnly").asBoolean(false)) {
                    r.put("takeoffOnly", true);
                }
                if (rw.path("landingOnly").asBoolean(false)) {
                    r.put("landingOnly", true);
                }

                uusiRunwayt.add(r);
            }
            slimProps.set("runways", uusiRunwayt);
        }

        return new Feature(alkuperainen.geometry, slimProps);
    }


    /**
     * TODO: tämän voisi laittaa Feature luokkaan
     * aliohjelma joka karsii navaidien properties osiosta turhat tiedot pois
     * @return palauttaa uuden karsitun feature olion
     */
    public Feature karsiNavaidinProperties() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode props = mapper.createObjectNode();

        Feature alkuperainen = this;

        // Perustiedot
        props.put("name", alkuperainen.properties.path("name").asText(""));
        props.put("identifier", alkuperainen.properties.path("identifier").asText(""));

        // Tyyppi ja selite
        int typeCode = alkuperainen.properties.path("type").asInt(-1);
        props.put("type", typeCode);

        String typeName = switch (typeCode) {
            case 0 -> "DME";
            case 1 -> "TACAN";
            case 2 -> "NDB";
            case 3 -> "VOR";
            case 4 -> "VOR-DME";
            case 5 -> "VORTAC";
            case 6 -> "DVOR";
            case 7 -> "DVOR-DME";
            case 8 -> "DVORTAC";
            default -> "tuntematon";
        };
        props.put("typeName", typeName);

        // Taajuus (frequency)
        JsonNode freq = alkuperainen.properties.path("frequency");
        if (!freq.isMissingNode()) {
            ObjectNode f = mapper.createObjectNode();
            f.put("value", freq.path("value").asText(""));

            String unitStr = switch (freq.path("unit").asInt(-1)) {
                case 1 -> "kHz";
                case 2 -> "MHz";
                default -> "tuntematon";
            };
            f.put("unit", unitStr);

            props.set("frequency", f);
        }

        // Kanava
        if (alkuperainen.properties.has("channel")) {
            props.put("channel", alkuperainen.properties.path("channel").asText());
        }

        // Korkeus
        JsonNode elevation = alkuperainen.properties.path("elevation");
        if (!elevation.isMissingNode()) {
            ObjectNode elev = mapper.createObjectNode();
            elev.put("value", elevation.path("value").asInt(-1));
            elev.put("unit", elevation.path("unit").asInt(-1));
            props.set("elevation", elev);
        }

        // Kantama (range)
        JsonNode range = alkuperainen.properties.path("range");
        if (!range.isMissingNode()) {
            ObjectNode r = mapper.createObjectNode();
            r.put("value", range.path("value").asInt(-1));

            String unitStr = switch (range.path("unit").asInt(-1)) {
                case 2 -> "NM";
                default -> "tuntematon";
            };
            r.put("unit", unitStr);

            props.set("range", r);
        }

        // HoursOfOperation – jätetään vain jos ei ole täysin oletusarvo (00:00-00:00 joka päivä)
        JsonNode hours = alkuperainen.properties.path("hoursOfOperation").path("operatingHours");
        if (hours.isArray()) {
            boolean onPoikkeavaa = false;

            for (JsonNode h : hours) {
                if (!h.path("startTime").asText().equals("00:00") ||
                        !h.path("endTime").asText().equals("00:00") ||
                        h.path("byNotam").asBoolean(false) ||
                        h.path("sunrise").asBoolean(false) ||
                        h.path("sunset").asBoolean(false) ||
                        h.path("publicHolidaysExcluded").asBoolean(false)) {
                    onPoikkeavaa = true;
                    break;
                }
            }

            if (onPoikkeavaa) {
                props.set("hoursOfOperation", hours);
            }
        }

        // (Poistetaan: _id, createdAt, updatedAt, elevationGeoid jne.)
        return new Feature(alkuperainen.geometry, props);
    }

}
