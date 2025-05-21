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

    }
}