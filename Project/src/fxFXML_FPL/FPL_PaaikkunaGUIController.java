package fxFXML_FPL;

import fi.jyu.mit.fxgui.Dialogs;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

/**
 * @author elias
 * @version 15.4.2025
 *
 */
public class FPL_PaaikkunaGUIController {
    @FXML
    private Button Button_Laske;

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

        if (tarkista()) {
            laske();
        }
    }

//==========================================================================================================================================

    /**
     * aliohjelma joka tarkistaa onko kenttiin syötetty tietoa
     * @return palauttaa true jos kenttiin on syötetty tiedot, false jos yksikin kriittisistä kentistä on tyhjänä
     */
    public boolean tarkista() {

        if ( TA_LupaKirja.getText().isEmpty() ) {
            Dialogs.showMessageDialog("Muistithan laittaa lupakirjasi");
            return false;
        }
        if ( TF_RekNro.getText().isEmpty() ) {
            Dialogs.showMessageDialog("Muistithan laittaa koneen rakisterinumeron");
            return false;
        }
        if ( TF_LahtoAika.getText().isEmpty() ) {
            Dialogs.showMessageDialog("Muistithan laittaa lähtöajan");
            return false;
        }
        if ( TF_Maaranpaa.getText().isEmpty() ) {
            Dialogs.showMessageDialog("Muistithan laittaa määränpään");
            return false;
        }
        if ( TF_LahtoKentta.getText().isEmpty() ) {
            Dialogs.showMessageDialog("Muistithan laittaa lähtökentän");
            return false;
        }
        return true;
    }

    /**
     * aliohjelma jossa lasketaan lentosuunnitelmat annettujen tietojen perusteella
     */
    public void laske() {

    }
}