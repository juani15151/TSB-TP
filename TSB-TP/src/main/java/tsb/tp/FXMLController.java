package tsb.tp;

import clases.TSBHashtable;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
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
    
    TSBHashtable<String, Integer> table = new TSBHashtable<>(1000);
    
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    }    

    @FXML
    private void CargarArchivo(ActionEvent event) {
        // Creamos los filtros para el FileChooser
        ExtensionFilter txtFilter = new ExtensionFilter("Text files (.txt)","*.txt");
        ExtensionFilter pdfFilter = new ExtensionFilter("PDF files (.pdf)","*.pdf");
        ExtensionFilter allFilter = new ExtensionFilter("All files (*)","*");
        // Creamos el FileChooser
        FileChooser fileChooser = new FileChooser();
        // Agregamos los filtros al FileChooser
        fileChooser.getExtensionFilters().add(txtFilter);
        fileChooser.getExtensionFilters().add(pdfFilter);
        fileChooser.getExtensionFilters().add(allFilter);
        // Ponemos titulo al FileChooser
        fileChooser.setTitle("Buscar archivo");
        // Obtenemos el archivo seleccionado del FileChooser
        try{
            File archivo = fileChooser.showOpenDialog(null);
            // Le seteamos el path al TextField de cargar archivo.
            tfArchivo.textProperty().set(archivo.getPath());
            // Cargamos la lista
            cargarLista(archivo);
        }
        catch (Exception e) {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Error al cargar el archivo");
            alert.setContentText(null);

            alert.showAndWait();
        }
    }
    
    private void mostrarPalabras(){
        // Para DEBUG solamente.
        for(Map.Entry<String, Integer> palabra : table.entrySet()){
            System.out.println("Palabra: " + palabra.getKey() + 
                    " - Repeticiones: " + palabra.getValue());
        }        
    }
    
    private void cargarLista(File file){
        try{
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line = null;
            while ((line = br.readLine()) != null) {
                String[] str = line.split(" ");
                for(int i = 0; i < str.length; i++){
                    if(!str[i].equals(" ")&&!str[i].isEmpty()){
                        // Agrega la clave a la tabla, si ya existe le suma 1.
                        // El metodo merge esta definido en Map.
                        table.merge(str[i], 1, Integer::sum);
                    }
                }
            }
        }
        catch(Exception e){
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Error al cargar la lista");
            alert.setContentText(null);

            alert.showAndWait();
        }
        mostrarPalabras();
    }

    @FXML
    private void eventOnMouseclicked(MouseEvent event) {
        try{
            if (!lstPalabras.getSelectionModel().getSelectedItem().isEmpty() && !lstPalabras.getItems().isEmpty()) {
                tfBusqueda.setText(lstPalabras.getSelectionModel().getSelectedItem().toString());
            }
        } catch (Exception e) {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Error al seleccionar una palabra");
            alert.setContentText(null);

            alert.showAndWait();
        }
    }
    private int BuscarPalabra(String palabra) {
        File file = new File(tfArchivo.getText());
        int counter = 0;
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line = null;
            while ((line = br.readLine()) != null) {
                String[] str = line.split(" ");
                for (int i = 0; i < str.length; i++) {
                    if (str[i].equals(palabra)) {
                        counter++;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("nada que hacer");
        }
        return counter;
    }
    @FXML
    private void BuscarPalabra(ActionEvent event) {
        System.out.println(table);
        try{
            tfRepeticiones.setText(table.get(tfBusqueda.getText()).toString());
        }catch (Exception e) {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("No se encontro esa palabra");
            alert.setContentText(null);

            alert.showAndWait();
        }
    }
}
