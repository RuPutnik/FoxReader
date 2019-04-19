package ru.putnik.foxreader.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import ru.putnik.foxreader.ConnectionProperty;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * Создано 09.04.2019 в 23:33
 */
public class ConnectionController implements Initializable {
    @FXML
    public TextField portTextField;
    @FXML
    public ComboBox<String> typeServerComboBox;
    @FXML
    public TextField addressServerTextField;
    @FXML
    public TextField loginTextField;
    @FXML
    public TextField passwordTextField;
    @FXML
    public TextField nameDBTextField;
    @FXML
    public Button connectToServerButton;
    @FXML
    public Button cancelButton;
    @FXML
    public Button defaultDataButton;
    private static final String PATH_FXML= "view/ConnectionView.fxml";
    private static ConnectionProperty property;
    private static Stage stage;

    ConnectionProperty showView(Stage mainStage){
        stage=new Stage();
        try {
            Parent parent=FXMLLoader.load(Objects.requireNonNull(getClass().getClassLoader().getResource(PATH_FXML)));
            stage.setScene(new Scene(parent));
            stage.setResizable(false);
            stage.setTitle("Подключение к серверу");
            stage.initOwner(mainStage);
            stage.initModality(Modality.APPLICATION_MODAL);
            try {
                stage.getIcons().add(new Image("icons/foxIcon.png"));
            }catch (IllegalArgumentException ex){
                System.out.println("Не обнаружена иконка программы!");
            }
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return property;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        ObservableList<String> typesServer=FXCollections.observableArrayList();
        typesServer.add("Microsoft MS SQL");
        typesServer.add("Oracle MySQL");
        typeServerComboBox.setItems(typesServer);
        typeServerComboBox.setValue(typesServer.get(0));
        connectToServerButton.setOnAction(event -> {
            String addressServer=addressServerTextField.getText();
            String login=loginTextField.getText();
            String password=passwordTextField.getText();
            String nameDB=nameDBTextField.getText();
            String port=portTextField.getText();
            if(!login.equals("")&&!password.equals("")&&!port.equals("")) {

                try{
                    int p =Integer.parseInt(port);
                    if(p<=0) throw new NumberFormatException();
                }catch (NumberFormatException ex){
                    Alert alert=new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Ошибка подключения");
                    alert.setHeaderText("Ошибка при указании порта");
                    alert.setContentText("Номер порта должен быть целым числом больше нуля");
                    ((Stage)alert.getDialogPane().getScene().getWindow()).getIcons().add(new Image("icons/foxIcon.png"));
                    alert.show();

                }

                property = new ConnectionProperty(typeServerComboBox.getValue(),addressServer, login, password, nameDB, Integer.parseInt(port));

                Node sourceNode = (Node) event.getSource();
                Stage currentStage = (Stage) sourceNode.getScene().getWindow();
                currentStage.close();
            }else {
                String message="Для подключения необходимо указать следующие данные:\n";
                String nullData="";

                if(login.equals("")) nullData=nullData+"Логин\n";
                if(password.equals("")) nullData=nullData+"Пароль\n";
                if(port.equals("")) nullData=nullData+"Порт\n";

                Alert alert=new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Ошибка подключения");
                alert.setHeaderText("Не указаны данные для подключения");

                alert.setContentText(message+nullData);
                alert.show();
            }
        });
        defaultDataButton.setOnAction(event ->
            installDefaultSetting()
        );
        cancelButton.setOnAction(event -> {
            //ДЕЛАТЬ ВОТ ТАК
            property=null;
            Node sourceNode = (Node) event.getSource();
            Stage currentStage = (Stage) sourceNode.getScene().getWindow();
            currentStage.close();
        });
        stage.setOnCloseRequest(event -> {
            property=null;
            stage.close();
        });
    }
    private void installDefaultSetting(){
        portTextField.setText("1433");
        addressServerTextField.setText("localhost");
        loginTextField.setText("JavaConnect");
        passwordTextField.setText("123");
        typeServerComboBox.setValue(typeServerComboBox.getItems().get(0));
    }
}
