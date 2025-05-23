package fxFXML_FPL;

import FPL_Code.Aircraft;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

import java.awt.event.ActionEvent;

public class KoneGUIController {

    @FXML
    private Button Button_Valmis;

    @FXML
    private TextField FT_MTOW;

    @FXML
    private TextField TF_ClimbRate;

    @FXML
    private TextField TF_CruiseSpeed;

    @FXML
    private TextField TF_EmptyWeight;

    @FXML
    private TextField TF_GPS;

    @FXML
    private TextField TF_Kategoria;

    @FXML
    private TextField TF_KoneTyyppi;

    @FXML
    private TextField TF_Kulutus;

    @FXML
    private TextField TF_MaxAltitude;

    @FXML
    private TextField TF_MaxFlightTime;

    @FXML
    private TextField TF_PayLoad;

    @FXML
    private TextField TF_Radio;

    @FXML
    private TextField TF_Range;

    @FXML
    private TextField TF_Reserve;

    @FXML
    private TextField TF_TankSize;

    @FXML
    private TextField TF_Transponder;

    @FXML
    private TextField TF_UsableFuel;

    @FXML
    private TextField TF_UsefulLoad;

    @FXML
    private TextField TF_VOR;

    @FXML
    private TextField TF_rekNro;


    @FXML
    public void Valmis(javafx.event.ActionEvent actionEvent) {
        teeOlio();
    }

    // ===================================================================================================================================================================================================

    /**
     * Tekee Aircraft olion tekstikenttiin annettujen tietojen perusteella.
     */
    public void teeOlio() {
        Aircraft kone = new Aircraft();
        kone.setRekNro(TF_rekNro.getText());
        kone.setKoneTyyppi(TF_KoneTyyppi.getText());
        kone.setKategoria(TF_Kategoria.getText());

        kone.setCruiseSpeed(Integer.parseInt(TF_CruiseSpeed.getText()));
        kone.setClimbRate(Integer.parseInt(TF_ClimbRate.getText()));
        kone.setFlightLevel(Integer.parseInt(TF_MaxAltitude.getText()));
        kone.setKulutus(Integer.parseInt(TF_Kulutus.getText()));
        kone.setRange(Integer.parseInt(TF_Range.getText()));
        kone.setMaxFightTime(Integer.parseInt(TF_MaxFlightTime.getText()));

        kone.setFuelTankCapacity(Integer.parseInt(TF_TankSize.getText()));
        kone.setUsableFuel(Integer.parseInt(TF_UsableFuel.getText()));
        kone.setReserveMin(Integer.parseInt(TF_Reserve.getText()));

        kone.setEmptyWeight(Integer.parseInt(TF_EmptyWeight.getText()));
        kone.setMTOW(Integer.parseInt(FT_MTOW.getText()));
        kone.setUsefulLoad(Integer.parseInt(TF_UsefulLoad.getText()));
        kone.setPayLoad(Integer.parseInt(TF_PayLoad.getText()));

        kone.setTransponder(TF_Transponder.getText());
        kone.setGPS(TF_GPS.getText());
        kone.setRadio(TF_Radio.getText());
    }


}
