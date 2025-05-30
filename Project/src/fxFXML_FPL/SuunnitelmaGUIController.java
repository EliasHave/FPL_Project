package fxFXML_FPL;

import FPL_Code.FlightPlanner;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;

public class SuunnitelmaGUIController {

    FlightPlanner planner = FXML_FPLMain.getFlightPlanner();

    @FXML
    private Button Button_test;


    @FXML
    private TextArea TA_AirCraft;

    @FXML
    private TextArea TA_Notam;

    @FXML
    private TextArea TA_Pilot;

    @FXML
    private TextArea TA_Saa;


    @FXML
    void testaa(ActionEvent event) {
        kirjaaKone();
        kirjaaNotam();
        kirjaaPilot();
        kirjaaSaa();
    }


    // ==============================================================================================================================================

    public void kirjaaKone() {
        TA_AirCraft.setText(planner.getKone().toString());
    }

    public void kirjaaNotam() {
        TA_Notam.setText(planner.getNotam());
    }

    public void kirjaaPilot() {
        TA_Pilot.setText(planner.getPilot().toString());
    }

    public void kirjaaSaa() {
        StringBuilder saa = new StringBuilder();
        saa.append("Sää Lähtökohteessa:  \n" + planner.getSaaLahto().toString());
        saa.append("\n");
        saa.append("Sää määränpäässä: \n" + planner.getSaaMaapanpaa().toString());
        TA_Saa.setText(saa.toString());
    }


}