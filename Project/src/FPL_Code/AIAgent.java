package FPL_Code;

import FPL_Code.DTO_Package.DTO_Boss;

import java.util.ArrayList;
import java.util.List;

/**
 * Tekoäly "agentti" jonka vastuulla on tekoälyltä kysyminen sekä vastauksen tulkinta ja parsiminen sekä tarkastaminen.
 */
public class AIAgent {

    private String modelName;

    public AIAgent() {
        this.modelName = "";
    }


    public List<Point> reitita(DTO_Boss flightData) {
        List<Point> reittipisteet = new ArrayList();

        String prompt = muodostaPrompt(flightData);    //muodostetaan oikeanlainen prompt tekoälylle
        String response = lahetaTekoAlylle(prompt);    //lähetetään äsken muodostettu prompt tekoälylle ja vastaanotetaan vastaus
        reittipisteet = parsiVastaus(response);        //parsitaan vastauksesta reittipisteet ja niiden perustelut ja palautetaan oikeassa järjestyksessä olevassa listassa
        // tässä voisi vielä tehdä tarkistuksen reittipisteille että onko ne järkevät
        return reittipisteet;
    }


    /**
     * Aliohjelma joka muodostaa oikeanlaisen promptin tekoälylle jossa on kaikki tarpeellinen tieto.
     * Promptissa täytyy olla lentoon liittyvät datat, oikeanlainen rooli sekä ohje tekoälylle.
     *
     * @param flightData DTO muunneltu data lennon parametreista kuten sää, ilmatilat ja koneen tiedot yms
     * @return Palauttaa valmiin string muotoisen promtin joka sisältää ohjeet, roolin ja datat selkeästi sommiteltuna.
     */
    public String muodostaPrompt(DTO_Boss flightData) {

        String prompt = """
                You are an expert flight planner and aviation safety analyst.
                Analyze the following comprehensive flight planning JSON data.
                
                Your tasks:
                1. Identify departure (ADEP) and destination (ADES) airports
                2. Analyze weather conditions (METARs, TAFs) for safety considerations
                3. Review airspace restrictions and NOTAMs along the route
                4. Evaluate fuel requirements and alternate airport suitability
                5. Check navigation aids (VOR, NDB, etc.) availability
                6. Identify any safety concerns or regulatory issues
                7. Provide a structured risk assessment
                
                Return your analysis in this JSON format:
                {
                  "departure": "ICAO code",
                  "destination": "ICAO code",
                  "weather_summary": "brief overview",
                  "safety_concerns": ["concern1", "concern2"],
                  "airspace_issues": ["issue1"],
                  "fuel_assessment": "adequate/marginal/insufficient",
                  "alternates_status": "suitable/limited/none",
                  "overall_risk": "low/medium/high",
                  "recommendations": ["recommendation1", "recommendation2"]
                }
                
                Flight data:
                """ + flightData.toAiInputString();

        System.out.println("Tässä on muodostaPrompt aliohjelmassa luotu prompt:" + "\n" + prompt + "\n" + "===============================================================================================================================");

        return prompt;
    }


    /**
     * Aliohjelma joka lähettää parametrina tulevan promptin tekoälylle ja vastaanottaa tekoälyn vastauksen joka palautetaan sellaisenaan
     *
     * @param prompt Tekoälylle lähetettävä viesti
     * @return Palauttaa tekoälyn vastauksen sille lähetettyyn viestiin
     */
    public String lahetaTekoAlylle(String prompt) {
        String response = "";
        AIClient client = new OpenAIClient(System.getenv("OPENAI_API_KEY"), "gpt-4o-mini");

        response = client.sendPrompt(prompt);
        System.out.println("\n" + "Tässä tekoälyn vastaus promptiin" + "\n" + response + "\n" + "========================================================================================================");

        return response;
    }


    /**
     * Aliohjelma joka parsii tekoälyn vastauksesta reittipisteet ja perustelut ja tekee niistä point-oliot
     * Laittaa myös valmiit point oliot listaan oikeassa järjestyksessä ja palauttaa lopuksi nämä reittipisteet
     */
    public List<Point> parsiVastaus(String response) {
        List<Point> reittipisteet = new ArrayList();

        return reittipisteet;
    }

}