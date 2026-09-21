package FPL_Code.Service;

import FPL_Code.Model.DTO_Boss;
import FPL_Code.Model.LentosuunnitelmaTulos;
import FPL_Code.Model.Point;
import FPL_Code.Utility.JsonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class AiService {

    private final ObjectMapper mapper = new ObjectMapper();

    public LentosuunnitelmaTulos analysoiReitti(DTO_Boss paketti) {
        // 1. Rakennetaan konteksti ja ohjeistus (Promptit) luokan sisäisenä logiikkana
        String systemPrompt = "Olet kokenut lennonopettaja. Analysoi reittidata ja palauta STRICTLY JSON-muodossa: " +
                "{'departure', 'destination', 'risk', 'safetyConcerns', 'reittiPisteet'}";

        String reittiDataJson = JsonUtils.rakennaAiPromptJson(paketti);

        System.out.println("Tekoälylle lähtevä data: " + reittiDataJson);

        // 2. TÄHÄN TULEE API-KUTSU (Mockataan toistaiseksi MVP-hengessä)

        // Simuloitu LLM-vastaus
        String llmJsonResponse = """
            {
                "departure": "%s",
                "destination": "%s",
                "risk": "MEDIUM",
                "safetyConcerns": "Sää on VFR ja reitti on selkeä. Lentäjän kokemus huomioiden suora reitti on turvallinen, mutta huomioi mahdolliset aktiiviset ilmatilat.",
                "reittiPisteet": [
                    {"pointName": "%s", "lat": %.4f, "lon": %.4f},
                    {"pointName": "%s", "lat": %.4f, "lon": %.4f}
                ]
            }
            """.formatted(
                paketti.getLahto().getPointName(),
                paketti.getKohde().getPointName(),
                paketti.getLahto().getPointName(), paketti.getLahto().getLat(), paketti.getLahto().getLon(),
                paketti.getKohde().getPointName(), paketti.getKohde().getLat(), paketti.getKohde().getLon()
        );

        // 3. Parsitaan JSON suoraan Javan olioksi
        try {
            return mapper.readValue(llmJsonResponse, LentosuunnitelmaTulos.class);
        } catch (Exception e) {
            System.err.println("Virhe tekoälyn JSON-vastauksen parsinnassa: " + e.getMessage());
            return new LentosuunnitelmaTulos(
                    paketti.getLahto().getPointName(), paketti.getKohde().getPointName(),
                    "UNKNOWN", "Analyysi epäonnistui.",
                    List.of(new Point(paketti.getLahto().getPointName(), paketti.getLahto().getLat(), paketti.getLahto().getLon()),
                            new Point(paketti.getKohde().getPointName(), paketti.getKohde().getLat(), paketti.getKohde().getLon()))
            );
        }
    }
}