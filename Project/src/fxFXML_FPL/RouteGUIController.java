package fxFXML_FPL;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.io.IOException;

public class RouteGUIController {

    @FXML
    private Button Button_Valmis;

    @FXML
    void valmis(ActionEvent event) {
        siirry();
    }

// ======================================================================================================================================

    public void siirry() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("KoneGUIView.fxml"));
            Parent koneRoot = loader.load();

            // Hae nykyinen ikkuna
            Stage currentStage = (Stage) Button_Valmis.getScene().getWindow();

            // Luo uusi näkymä ja aseta se nykyiseen ikkunaan
            Scene koneScene = new Scene(koneRoot);
            currentStage.setScene(koneScene);
            currentStage.setTitle("Koneen tiedot");
            currentStage.show();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
