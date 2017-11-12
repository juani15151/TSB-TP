package tsb.tp;

import clases.TSBHashtable;
import java.beans.EventHandler;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;


public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        load();
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/Scene.fxml"));
        Scene scene = new Scene(root);
        scene.getStylesheets().add("/styles/Styles.css");
        stage.setTitle("Trabajo Practico TSB");
        stage.setScene(scene);
        stage.show();
     
    }

    @Override
    public void stop() throws Exception {
        this.write(FXMLController.table);
        super.stop();
    }
    public void write(Object o) throws IOException {
        FileOutputStream out = new FileOutputStream("TSBHashtable.dat");
        ObjectOutputStream ois = new ObjectOutputStream(out);
        ois.writeObject(o);
        ois.close();
    }
    public void load() throws FileNotFoundException, IOException, IOException, ClassNotFoundException{
        String nameFile = "TSBHashtable.dat";
        File arch = new File(nameFile);
        if (arch.exists()) {
            FileInputStream fis = new FileInputStream("TSBHashtable.dat");
            ObjectInputStream ois = new ObjectInputStream(fis);
            FXMLController.table = (TSBHashtable)ois.readObject();
            System.out.println("");
        } else {
            throw new FileNotFoundException("No se encontró el archivo");
        }
    }

    /**
     * The main() method is ignored in correctly deployed JavaFX application.
     * main() serves only as fallback in case the application can not be
     * launched through deployment artifacts, e.g., in IDEs with limited FX
     * support. NetBeans ignores main().
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

}
