package fxFXML_FPL;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.fxml.FXMLLoader;


/**
 * @author elias 
 * @version 15.4.2025
 *
 */
public class FXML_FPLMain extends Application {
    @Override
    public void start(Stage primaryStage) {
        try {
            FXMLLoader ldr = new FXMLLoader(getClass().getResource("FXML_FPLGUIView.fxml"));
            final Pane root = ldr.load();
            //final FXML_FPLGUIController fxml_fplCtrl = (FXML_FPLGUIController)ldr.getController();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("fxml_fpl.css").toExternalForm());
            primaryStage.setScene(scene);
            primaryStage.setTitle("FXML_FPL");
            primaryStage.show();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param args Ei käytössä
     */
    public static void main(String[] args) {
        launch(args);
    }
}