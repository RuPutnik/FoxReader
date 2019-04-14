package ru.putnik.foxreader.controller;

import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import ru.putnik.foxreader.ConnectionProperty;
import ru.putnik.foxreader.TimeRunnable;
import ru.putnik.foxreader.TypeTreeElement;
import ru.putnik.foxreader.model.MainModel;

import java.net.URL;
import java.sql.SQLException;
import java.util.*;

/**
 * Создано 09.04.2019 в 22:55
 */
public class MainController extends Application implements Initializable {
    private static final String PATH_FXML= "view/MainView.fxml";

    private MainModel mainModel;
    private ConnectionController connectionController=new ConnectionController();
    private ConnectionProperty property;
    private Stage stage;
    private int indexRow;

    @FXML
    private MenuItem connectionToServerMenuItem;
    @FXML
    private Menu timeMenu;
    @FXML
    public TreeView<TypeTreeElement> treeDBTreeView;
    @FXML
    public TextArea logRequestTextArea;
    @FXML
    public TableView<List<String>> tableDBTableView;
    @FXML
    public CheckBox modeRealSQLCheckBox;
    @FXML
    public TextField textRequestTextField;
    @FXML
    public Button sendRequestButton;
    @FXML
    public MenuItem addRow;
    @FXML
    public MenuItem deleteRow;
    @FXML
    public MenuItem updateTable;
    @Override
    public void start(Stage primaryStage) throws Exception {
        this.stage=primaryStage;
        Parent parent=FXMLLoader.load(Objects.requireNonNull(getClass().getClassLoader().getResource(PATH_FXML)));
        Scene scene=new Scene(parent);
        primaryStage.setScene(scene);
        primaryStage.setTitle("FoxReader");
        try {
            primaryStage.getIcons().add(new Image("icons/foxIcon.png"));
        }catch (IllegalArgumentException ex){
            System.out.println("Не обнаружена иконка программы!");
        }
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        mainModel = new MainModel(this);
        playTimer();
        logRequestTextArea.setStyle("-fx-text-fill: green");
        tableDBTableView.setTableMenuButtonVisible(true);
        tableDBTableView.setEditable(true);

        connectionToServerMenuItem.setOnAction(event -> {
            property = connectionController.showView(stage);
            if (property != null) {
                try {
                    mainModel.initializeConnection(property);
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Подключение установлено");
                    alert.setHeaderText("Подключение к серверу выполнено успешно!");
                    alert.setContentText("Подключение с сервером " + property.getAddress() + " на порту " + property.getPort() + " было установлено.");
                    alert.show();
                    logRequestTextArea.appendText("Success connection: " + property.createConnectionUrl() + "\n");

                    mainModel.fillTree();

                } catch (SQLException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Ошибка подключения");
                    alert.setHeaderText("В процессе подключения возникла ошибка!");
                    alert.setContentText(e.getLocalizedMessage() + "\n" + "Код ошибки: " + e.getErrorCode());
                    alert.show();
                    logRequestTextArea.appendText("Fail connection: " + property.createConnectionUrl() + "\n");
                } finally {
                    logRequestTextArea.positionCaret(0);
                }
            }
        });
        treeDBTreeView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                if (newValue.getValue().getType() == TypeTreeElement.Type.TABLE) {
                    mainModel.firstFillTable(newValue.getValue().getName(), newValue.getValue().getNameDB(), newValue.getValue().getSchema());
                    addRow.setDisable(false);
                    deleteRow.setDisable(false);
                    updateTable.setDisable(false);
                }
            }
        });
        modeRealSQLCheckBox.setOnAction(event -> {
            if (modeRealSQLCheckBox.isSelected()) {
                textRequestTextField.setPromptText("Введите SQL запрос");
            } else {
                textRequestTextField.setPromptText("Введите фильтр");
            }
        });
        sendRequestButton.setOnAction(event -> {
            if (modeRealSQLCheckBox.isSelected()) {
                mainModel.sendRequest(textRequestTextField.getText(),mainModel.checkTypeRequest(textRequestTextField.getText()));
            } else {
                mainModel.sendFilter(textRequestTextField.getText());
            }
        });
        addRow.setDisable(true);
        deleteRow.setDisable(true);
        updateTable.setDisable(true);

        addRow.setOnAction(event->{
            mainModel.addRow();
        });
        deleteRow.setOnAction(event -> {
            indexRow=tableDBTableView.getSelectionModel().getSelectedIndex();
            if(indexRow==-1){
                Alert alert=new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Ошибка удаления");
                alert.setHeaderText("В процессе удаления строки возникла ошибка!");
                alert.setContentText("Строка для удаления не выбрана");
                alert.show();
            }else {
                mainModel.removeRow(indexRow);
            }
        });
        updateTable.setOnAction(event -> {
            mainModel.updateTable();
        });
    }
    private void playTimer(){
        TimeRunnable timeRunnable = new TimeRunnable(timeMenu);
        Thread time=new Thread(timeRunnable);
        time.setDaemon(true);
        time.start();
    }
    public static void play(){
        launch();
    }
}
