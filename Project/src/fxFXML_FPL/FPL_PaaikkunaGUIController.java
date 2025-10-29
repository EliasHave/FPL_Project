package fxFXML_FPL;

import fi.jyu.mit.fxgui.Dialogs;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * @author elias
 * @version 15.4.2025
 *
 */
public class FPL_PaaikkunaGUIController {
    @FXML
    private Button Button_Aloitetaan;

    @FXML
    private TextArea TA_LupaKirja;

    @FXML
    private TextArea TA_MuutHuomiot;

    @FXML
    private TextField TF_Kokomus;

    @FXML
    private TextField TF_LahtoAika;

    @FXML
    private TextField TF_LahtoKentta;

    @FXML
    private TextField TF_Maaranpaa;

    @FXML
    private TextField TF_RekNro;

    @FXML
    void painettu(ActionEvent event) {
            aloita();
    }

//==========================================================================================================================================

    /**
     *
     */
    public void aloita() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("RouteGUIView.fxml"));
            Parent routeRoot = loader.load();

            // Hae nykyinen ikkuna
            Stage currentStage = (Stage) Button_Aloitetaan.getScene().getWindow();

            // Luo uusi näkymä ja aseta se nykyiseen ikkunaan
            Scene routeScene = new Scene(routeRoot);
            currentStage.setScene(routeScene);
            currentStage.setTitle("Reitin tiedot");
            currentStage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}