package tsb.tp;

import clases.TSBHashtable;
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
    
    TSBHashtable<String, Integer> table = new TSBHashtable<>(3, 0.2f);
    
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // TODO
    }    

    @FXML
    private void CargarArchivo(ActionEvent event) {
        // Creamos los filtros para el FileChooser
        ExtensionFilter txtFilter = new ExtensionFilter("Text files (.txt)","*.txt");
        ExtensionFilter allFilter = new ExtensionFilter("All files (*)","*");
        // Creamos el FileChooser
        FileChooser fileChooser = new FileChooser();
        // Agregamos los filtros al FileChooser
        fileChooser.getExtensionFilters().add(txtFilter);
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
    
    private void cargarHash(){
        int[] counter = new int[lstPalabras.getItems().size()];
        
        File file = new File(tfArchivo.getText());
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line = null;
            while ((line = br.readLine()) != null) {
                String[] str = line.split(" ");
                for (int i = 0; i < str.length; i++) {
                    for(int j = 0; j < counter.length; j++){
                        if (str[i].equals(lstPalabras.getItems().get(j))) {
                            counter[j]++;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("nada que hacer");
        }
        
        for(int i = 0; i < lstPalabras.getItems().size(); i++){
            table.put(lstPalabras.getItems().get(i), counter[i]);
        }
        
        /*
        for(int i = 0; i < lstPalabras.getItems().size(); i++){
            String palabra = lstPalabras.getItems().get(i);
            System.out.println("Palabra: " + palabra + " Repeticiones: " + BuscarPalabra(palabra));
            table.put(palabra, BuscarPalabra(palabra));
        }
        System.out.println(table);*/
    }
    
    private void cargarLista(File file){
        try{
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line = null;
            while ((line = br.readLine()) != null) {
                String[] str = line.split(" ");
                for(int i = 0; i < str.length; i++){
                    str[i] = checkPalabra(str[i]);
                    if( !str[i].contains("www") &&!str[i].equals(" ")&& !str[i].isEmpty() && !lstPalabras.getItems().contains(str[i])){
                        lstPalabras.getItems().add(str[i]);
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
        cargarHash();
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
    public String checkPalabra(String palabra){
        palabra = palabra.replace(".", "");
        palabra = palabra.replace(",", "");
        palabra = palabra.replace("-", "");
        palabra = palabra.replace("(", "");
        palabra = palabra.replace(")", "");
        palabra = palabra.replace(":", "");
        palabra = palabra.replace(";", "");
        palabra = palabra.replace("¿", "");
        palabra = palabra.replace("?", "");
        palabra = palabra.replace("_", "");
        palabra = palabra.replace("*", "");
        palabra = palabra.replace("¡", "");
        palabra = palabra.replace("!", "");
        palabra = palabra.replace("<", "");
        palabra = palabra.replace(">", "");
        palabra = palabra.replace("[", "");
        palabra = palabra.replace("]", "");
        palabra = palabra.replace("#", "");
        palabra = palabra.replace("@", "");
        palabra = palabra.replace("0", "");
        palabra = palabra.replace("1", "");
        palabra = palabra.replace("2", "");
        palabra = palabra.replace("3", "");
        palabra = palabra.replace("4", "");
        palabra = palabra.replace("5", "");
        palabra = palabra.replace("6", "");
        palabra = palabra.replace("7", "");
        palabra = palabra.replace("8", "");
        palabra = palabra.replace("9", "");
        return palabra;
    }
}
