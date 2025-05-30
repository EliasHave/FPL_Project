package fxFXML_FPL;

import FPL_Code.FlightPlanner;
import FPL_Code.Pilot;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class PilotGUIController {

    @FXML
    private Button Button_Valmis;

    @FXML
    private TextArea TA_LupaKirja;

    @FXML
    private TextField TF_BD;

    @FXML
    private TextField TF_Kokemus;

    @FXML
    private TextField TF_Nimi;

    @FXML
    private TextField TF_PuhNro;

    @FXML
    private TextField TF_sPosti;

    @FXML
    void valmis(ActionEvent event) {
        muodostaPilot();
        siirry();
    }
// ======================================================================================================================================

    public void muodostaPilot() {
        Pilot p = new Pilot();
        p.setNimi(TF_Nimi.getText());
        p.setSyntymaAika(TF_BD.getText());
        p.setPuhNro(TF_PuhNro.getText());
        p.setSPosti(TF_sPosti.getText());
        p.setLupaKirjat(TA_LupaKirja.getText());
        p.setKokemus(Integer.parseInt(TF_Kokemus.getText()));

        lisaaPilotPlanneriin(p);

        System.out.println(p);
    }


    public void lisaaPilotPlanneriin(Pilot p) {
        FlightPlanner planner = FXML_FPLMain.getFlightPlanner();
        planner.setPilot(p);
    }


    public void siirry() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("SuunnitelmaGUIView.fxml"));
            Parent root = loader.load();

            // Hae nykyinen ikkuna
            Stage currentStage = (Stage) Button_Valmis.getScene().getWindow();

            //tehdään uusi scene ja laitetaan se nykyiseen ikkunaan
            Scene suunnitelmaScene = new Scene(root);
            currentStage.setScene(suunnitelmaScene);
            currentStage.setTitle("Suunnitelma tiedot");
            currentStage.show();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
