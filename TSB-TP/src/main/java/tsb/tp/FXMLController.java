package tsb.tp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

public class FXMLController implements Initializable {
    
    @FXML
    private Label label;
    @FXML
    private Button btCargarArchivo;
    @FXML
    private ListView<String> lstPalabras;
    @FXML
    private TextField tfArchivo;
    @FXML
    private TextField tfBusqueda;
    @FXML
    private Button btBuscar;
    @FXML
    private TextField tfRepeticiones;
    
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    }    

    @FXML
    private void CargarArchivo(ActionEvent event) {
        ExtensionFilter txtFilter = new ExtensionFilter("Text files (.txt)","*.txt");
        ExtensionFilter pdfFilter = new ExtensionFilter("PDF files (.pdf)","*.pdf");
        ExtensionFilter allFilter = new ExtensionFilter("All files (*)","*");
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(txtFilter);
        fileChooser.getExtensionFilters().add(pdfFilter);
        fileChooser.getExtensionFilters().add(allFilter);
        fileChooser.setTitle("Buscar archivo");
        File archivo = fileChooser.showOpenDialog(null);
        tfArchivo.textProperty().set(archivo.getPath());
        cargarLista(archivo);
    }
    
    private void cargarLista(File file){
        try{
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line = null;
            while ((line = br.readLine()) != null) {
                String[] str = line.split(" ");
                for(int i = 0; i < str.length; i++){
                    if(!str[i].equals(" ")&&!str[i].isEmpty() && !lstPalabras.getItems().contains(str[i])){
                        lstPalabras.getItems().add(str[i]);
                    }
                }
            }
        }
        catch(Exception e){
            System.out.println("nada que hacer");
        }
    }

    @FXML
    private void eventOnMouseclicked(MouseEvent event) {
        try{
            if (!lstPalabras.getSelectionModel().getSelectedItem().toString().isEmpty() && !lstPalabras.getItems().isEmpty()) {
                tfBusqueda.setText(lstPalabras.getSelectionModel().getSelectedItem().toString());
            }
        } catch(Exception e){}
    }

    @FXML
    private void BuscarPalabra(ActionEvent event) {
        
    }
}
