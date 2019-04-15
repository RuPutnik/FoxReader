package ru.putnik.foxreader.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.fxmisc.richtext.CodeArea;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * Создано 15.04.2019 в 14:33
 */
public class RequestController implements Initializable {
    private static final String PATH_FXML= "view/RequestView.fxml";

    private static String oldReq;
    private String request="";

    @FXML
    public CodeArea sqlReqArea;
    @FXML
    public Button handleRequestButton;
    @FXML
    public Button clearArea;
    @FXML
    public Button handleAndCloseButton;
    @FXML
    public Button saveButton;
    @FXML
    public Button notSaveButton;

    String showView(String request){
        oldReq=request;
        try {
            Stage stage=new Stage();
            Parent parent=FXMLLoader.load(Objects.requireNonNull(getClass().getClassLoader().getResource(PATH_FXML)));
            stage.setScene(new Scene(parent));
            stage.getIcons().add(new Image("icons/foxIcon.png"));
            stage.setTitle("Окно генерации");
            stage.setResizable(false);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return request;
    }
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        sqlReqArea.appendText(oldReq);
    }
}
