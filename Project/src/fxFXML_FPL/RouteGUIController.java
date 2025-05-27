package fxFXML_FPL;

import FPL_Code.Weather;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

import static FPL_Code.Weather.haeSaaOlio;

public class RouteGUIController {

    @FXML
    private Button Button_Valmis;

    @FXML
    private TextField TF_LahtoAika;

    @FXML
    private TextField TF_LahtoKentta;

    @FXML
    private TextField TF_MaaranPaa;

    @FXML
    void valmis(ActionEvent event) {
        reitti();
        siirry();
    }

// ======================================================================================================================================

    public void reitti() {
        String lahtoAika = TF_LahtoAika.getText();
        String lahtoKentta = TF_LahtoKentta.getText();
        String maaranPaa = TF_MaaranPaa.getText();

        Weather saaLahto = haeSaaOlio(lahtoKentta);

        Weather saaMaaranPaa = haeSaaOlio(maaranPaa);

        System.out.println(saaLahto);
        System.out.println(saaMaaranPaa);
    }


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
