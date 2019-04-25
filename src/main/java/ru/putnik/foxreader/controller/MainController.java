package ru.putnik.foxreader.controller;

import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import ru.putnik.foxreader.ConnectionProperty;
import ru.putnik.foxreader.FLogger;
import ru.putnik.foxreader.TimeRunnable;
import ru.putnik.foxreader.TypeTreeElement;
import ru.putnik.foxreader.model.MainModel;
import ru.putnik.foxreader.model.TreeOperationsMainModel;

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
    private RequestController requestController;
    private ConnectionProperty property;
    private static Stage stage;
    private int indexRow;
    private boolean sendCustomReq=false;
    private static ArrayList<String> allNames=new ArrayList<>();
    private int numberPage=0;
    private TreeOperationsMainModel operationsMainModel;

    @FXML
    private MenuItem connectionToServerMenuItem;
    @FXML
    private MenuItem disconnectMenuItem;
    @FXML
    private Menu timeMenu;
    @FXML
    public TreeView<TypeTreeElement> treeDBTreeView;
    @FXML
    public TextArea logRequestTextArea;
    @FXML
    public TableView<List<String>> tableDBTableView;
    @FXML
    private CheckBox modeRealSQLCheckBox;
    @FXML
    private TextField textRequestTextField;
    @FXML
    private Button sendRequestButton;
    @FXML
    private Button editInWindowButton;
    @FXML
    private MenuItem addRow;
    @FXML
    private MenuItem deleteRow;
    @FXML
    private MenuItem updateTable;
    @FXML
    private MenuItem deleteAllRows;
    @FXML
    private GridPane rowGridPane;
    @FXML
    private Button saveRowButton;
    @FXML
    private Button addRowButton;
    @FXML
    private Button deleteRowButton;
    @FXML
    private Button updateButton;
    @FXML
    private Button deleteAllRowButton;
    @FXML
    private Button firstRowButton;
    @FXML
    private Button lastRowButton;
    @FXML
    private Button nextRowButton;
    @FXML
    private Button backRowButton;
    @FXML
    private Label countAllRowLabel;
    @FXML
    private TextField numberRowTextField;
    @FXML
    private Button goToRowButton;
    @Override
    public void start(Stage primaryStage) throws Exception {
        stage=primaryStage;
        Parent parent=FXMLLoader.load(Objects.requireNonNull(getClass().getClassLoader().getResource(PATH_FXML)));
        Scene scene=new Scene(parent);
        primaryStage.setScene(scene);
        primaryStage.setTitle("FoxReader");
        try {
            primaryStage.getIcons().add(new Image("icons/foxIcon.png"));
        }catch (IllegalArgumentException ex){
            System.out.println("Не обнаружена иконка программы!");
        }
        primaryStage.show();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        mainModel = new MainModel(this);

        playTimer();
        logRequestTextArea.setStyle("-fx-text-fill: green");
        tableDBTableView.setTableMenuButtonVisible(true);
        tableDBTableView.setEditable(true);
        textRequestTextField.setEditable(false);
        disconnectMenuItem.setDisable(true);

        connectionToServerMenuItem.setOnAction(event -> {
            property = connectionController.showView(stage);
            if (property != null) {
                try {
                    mainModel.initializeConnection(property);
                    mainModel.fillTree();
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Подключение установлено");
                    alert.setHeaderText("Подключение к серверу выполнено успешно!");
                    alert.setContentText("Подключение с сервером " + property.getAddress() + " на порту " + property.getPort() + " было установлено.");
                    ((Stage)alert.getDialogPane().getScene().getWindow()).getIcons().add(new Image("icons/foxIcon.png"));
                    alert.show();
                    logRequestTextArea.appendText("Success connection: " + property.createConnectionUrl() + "\n");

                    operationsMainModel=new TreeOperationsMainModel(this,mainModel);

                    textRequestTextField.setEditable(true);
                    modeRealSQLCheckBox.setDisable(false);
                    sendRequestButton.setDisable(false);
                    editInWindowButton.setDisable(false);
                    disconnectMenuItem.setDisable(false);
                } catch (SQLException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Ошибка подключения");
                    alert.setHeaderText("В процессе подключения возникла ошибка!");
                    alert.setContentText(e.getLocalizedMessage() + "\n" + "Код ошибки: " + e.getErrorCode());
                    ((Stage)alert.getDialogPane().getScene().getWindow()).getIcons().add(new Image("icons/foxIcon.png"));
                    alert.show();
                    FLogger.error("Ошибка подключения",e);
                    logRequestTextArea.appendText("Fail connection: " + property.createConnectionUrl() + "\n");
                } finally {
                    logRequestTextArea.positionCaret(0);
                }
            }
        });
        disconnectMenuItem.setOnAction(event -> {
            mainModel.disconnect();
            logRequestTextArea.appendText("Close connection: " + property.createConnectionUrl() + "\n");
            disableAllWidgets();
            rowGridPane.getChildren().clear();
            numberPage=0;
            numberRowTextField.setText(String.valueOf(0));
            tableDBTableView.getColumns().clear();
            Label l=new Label("Нет данных");
            l.setAlignment(Pos.CENTER);
            rowGridPane.add(l,0,0);
        });
        treeDBTreeView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                if (newValue.getValue().getType() == TypeTreeElement.Type.TABLE) {
                    numberPage=0;
                    numberRowTextField.setText(String.valueOf(numberPage));
                    mainModel.firstFillTable(newValue.getValue().getName(), newValue.getValue().getNameDB(), newValue.getValue().getSchema());
                    addRow.setDisable(false);
                    deleteRow.setDisable(false);
                    updateTable.setDisable(false);
                    deleteAllRows.setDisable(false);

                    saveRowButton.setDisable(false);
                    addRowButton.setDisable(false);
                    deleteRowButton.setDisable(false);
                    deleteAllRowButton.setDisable(false);
                    updateButton.setDisable(false);
                    lastRowButton.setDisable(false);
                    firstRowButton.setDisable(false);
                    nextRowButton.setDisable(false);
                    backRowButton.setDisable(false);
                    goToRowButton.setDisable(false);
                    numberRowTextField.setDisable(false);

                    MenuItem deleteTableItem=new MenuItem("Удалить таблицу");
                    deleteTableItem.setOnAction(event -> {
                        operationsMainModel.removeTable(newValue);
                    });
                    ContextMenu removeTable=new ContextMenu();
                    removeTable.getItems().add(deleteTableItem);
                    treeDBTreeView.setContextMenu(removeTable);
                }else if(newValue.getValue().getType() == TypeTreeElement.Type.TABLES){
                    MenuItem addTableItem=new MenuItem("Создать таблицу");
                    addTableItem.setOnAction(event -> {
                        operationsMainModel.addTable(newValue);
                    });
                    ContextMenu addTable=new ContextMenu();
                    addTable.getItems().add(addTableItem);
                    treeDBTreeView.setContextMenu(addTable);
                }else if(newValue.getValue().getType() == TypeTreeElement.Type.PROCEDURE){
                    MenuItem runProcedureItem=new MenuItem("Запустить хранимую процедуру");
                    MenuItem seeProcedureItem=new MenuItem("Просмотреть хранимую процедуру");
                    MenuItem deleteProcedureItem=new MenuItem("Удалить хранимую процедуру");

                    runProcedureItem.setOnAction(event -> {
                        operationsMainModel.executeProcedure(newValue);
                    });
                    seeProcedureItem.setOnAction(event -> {
                        operationsMainModel.seeProcedure(newValue);
                    });
                    deleteProcedureItem.setOnAction(event -> {
                        operationsMainModel.removeProcedure(newValue);
                    });

                    ContextMenu procedureMenu=new ContextMenu();
                    procedureMenu.getItems().add(runProcedureItem);
                    procedureMenu.getItems().add(seeProcedureItem);
                    procedureMenu.getItems().add(deleteProcedureItem);
                    treeDBTreeView.setContextMenu(procedureMenu);
                }else if(newValue.getValue().getType() == TypeTreeElement.Type.PROCEDURES){
                    MenuItem addProcedureItem=new MenuItem("Создать хранимую процедуру");
                    addProcedureItem.setOnAction(event -> {
                        operationsMainModel.addProcedure(newValue);
                    });
                    ContextMenu addProcedure=new ContextMenu();
                    addProcedure.getItems().add(addProcedureItem);
                    treeDBTreeView.setContextMenu(addProcedure);
                }else if(newValue.getValue().getType() == TypeTreeElement.Type.DATABASE){
                    MenuItem deleteDBItem=new MenuItem("Удалить базу данных");
                    deleteDBItem.setOnAction(event -> {
                        operationsMainModel.removeDB(newValue);
                    });
                    ContextMenu removeDB=new ContextMenu();
                    removeDB.getItems().add(deleteDBItem);
                    treeDBTreeView.setContextMenu(removeDB);
                }else if(newValue.getValue().getType() == TypeTreeElement.Type.SERVER){
                    MenuItem updateTree=new MenuItem("Обновить дерево иерархии");
                    MenuItem addDBItem=new MenuItem("Создать базу данных");
                    addDBItem.setOnAction(event -> {
                        operationsMainModel.addDB(newValue);
                    });
                    updateTree.setOnAction(event->mainModel.fillTree());
                    ContextMenu addDB=new ContextMenu();
                    addDB.getItems().add(updateTree);
                    addDB.getItems().add(addDBItem);
                    treeDBTreeView.setContextMenu(addDB);
                }else {
                    treeDBTreeView.setContextMenu(null);
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
            sendRequest(textRequestTextField.getText());
            mainModel.fillTree();
        });
        editInWindowButton.setOnAction(event -> {
            requestController=new RequestController(this,allNames);
            //Если открыто окно для работы с запросом, запрещено как то изменять его на главном окне
            textRequestTextField.setEditable(false);
            modeRealSQLCheckBox.setDisable(true);
            sendRequestButton.setDisable(true);
            String text=requestController.showView(this,textRequestTextField.getText());
            textRequestTextField.setText(text);
            //Когда окно для работы с запросом закрыто, снова разрешена работа на главном окне
            textRequestTextField.setEditable(true);
            modeRealSQLCheckBox.setDisable(false);
            sendRequestButton.setDisable(false);
        });
        addRow.setOnAction(event->{
            mainModel.addRow();
            addRow.setDisable(true);
            addRowButton.setDisable(true);
        });
        deleteRow.setOnAction(event -> {
            indexRow=tableDBTableView.getSelectionModel().getSelectedIndex();
            if(indexRow==-1){
                Alert alert=new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Ошибка удаления");
                alert.setHeaderText("В процессе удаления строки возникла ошибка!");
                alert.setContentText("Строка для удаления не выбрана");
                ((Stage)alert.getDialogPane().getScene().getWindow()).getIcons().add(new Image("icons/foxIcon.png"));
                alert.show();

            }else {
                mainModel.removeRow(indexRow);
                addRow.setDisable(false);
                addRowButton.setDisable(false);
            }
        });
        updateTable.setOnAction(event -> {
            mainModel.updateTable();
            deleteRow.setDisable(false);
            addRow.setDisable(false);
            deleteAllRows.setDisable(false);
            deleteRowButton.setDisable(false);
            addRowButton.setDisable(false);
            saveRowButton.setDisable(false);
            deleteAllRowButton.setDisable(false);
            sendCustomReq=false;
        });
        deleteAllRows.setOnAction(event -> {
            mainModel.deleteAllRows();
            addRow.setDisable(false);
            addRowButton.setDisable(false);
        });
        saveRowButton.setOnAction(event -> {
            mainModel.savePage();
        });
        addRowButton.setOnAction(event -> {
            mainModel.addPage();
            addRow.setDisable(true);
            addRowButton.setDisable(true);
        });
        deleteRowButton.setOnAction(event -> {
            mainModel.deletePage();
            addRow.setDisable(false);
            addRowButton.setDisable(false);
        });
        deleteAllRowButton.setOnAction(event -> {
            mainModel.deleteAllRows();
        });
        goToRowButton.setOnAction(event -> {
            if(tableDBTableView.getItems()!=null) {
                try {
                    int num = Integer.parseInt(numberRowTextField.getText());
                    if (num < 0 || num > tableDBTableView.getItems().size() - 1) {
                        throw new Exception();
                    }
                    numberPage=num;
                    mainModel.openPage(numberPage);
                } catch (Exception e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Ошибка перехода на страницу таблицы");
                    alert.setHeaderText(null);
                    alert.setContentText("Номер страницы должен быть целым неотрицательным числом меньшим количества записей");
                    ((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().add(new Image("icons/foxIcon.png"));
                    alert.show();
                    FLogger.error("Ошибка перехода на страницу таблицы",e);
                }
            }else{
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Ошибка перехода на страницу таблицы");
                alert.setHeaderText(null);
                alert.setContentText("Выбранная таблица пуста");
                ((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().add(new Image("icons/foxIcon.png"));
                alert.show();
            }
        });
        updateButton.setOnAction(event -> {
            numberPage=0;
            numberRowTextField.setText(String.valueOf(numberPage));
            mainModel.updateTable();
            mainModel.openPage(numberPage);

            deleteRowButton.setDisable(false);
            addRowButton.setDisable(false);
            saveRowButton.setDisable(false);
            deleteAllRowButton.setDisable(false);

        });
        firstRowButton.setOnAction(event -> {
            numberPage=0;
            numberRowTextField.setText(String.valueOf(numberPage));
            mainModel.openPage(numberPage);
        });
        lastRowButton.setOnAction(event -> {
            numberPage=tableDBTableView.getItems().size()-1;
            numberRowTextField.setText(String.valueOf(numberPage));
            mainModel.openPage(numberPage);
        });
        nextRowButton.setOnAction(event -> {
            if(tableDBTableView.getItems()!=null&&numberPage<tableDBTableView.getItems().size()-1){
                numberPage++;
                numberRowTextField.setText(String.valueOf(numberPage));
                mainModel.openPage(numberPage);
            }
        });
        backRowButton.setOnAction(event -> {
            if(tableDBTableView.getItems()!=null&&numberPage>0){
                numberPage--;
                numberRowTextField.setText(String.valueOf(numberPage));
                mainModel.openPage(numberPage);
            }
        });
        stage.setOnCloseRequest(event -> {
            System.exit(0);
        });
    }
    private void playTimer(){
        TimeRunnable timeRunnable = new TimeRunnable(timeMenu);
        Thread time=new Thread(timeRunnable);
        time.setDaemon(true);
        time.start();
    }
    public void sendRequest(String textReq){
        if (modeRealSQLCheckBox.isSelected()) {
            mainModel.sendRequest(textReq,mainModel.checkTypeRequest(textReq));
        } else {
            mainModel.sendFilter(textReq);
        }
        deleteRow.setDisable(true);
        addRow.setDisable(true);
        deleteAllRows.setDisable(true);
        deleteRowButton.setDisable(true);
        addRowButton.setDisable(true);
        saveRowButton.setDisable(true);
        deleteAllRowButton.setDisable(true);
        sendCustomReq=true;
    }
    private void disableAllWidgets(){
        disconnectMenuItem.setDisable(true);
        treeDBTreeView.setRoot(null);
        addRow.setDisable(true);
        deleteRow.setDisable(true);
        updateTable.setDisable(true);
        deleteAllRows.setDisable(true);
        tableDBTableView.getItems().clear();
        saveRowButton.setDisable(true);
        addRowButton.setDisable(true);
        deleteRowButton.setDisable(true);
        deleteAllRowButton.setDisable(true);
        updateButton.setDisable(true);
        lastRowButton.setDisable(true);
        firstRowButton.setDisable(true);
        nextRowButton.setDisable(true);
        backRowButton.setDisable(true);
        goToRowButton.setDisable(true);
        numberRowTextField.setDisable(true);
        textRequestTextField.setEditable(false);
        modeRealSQLCheckBox.setDisable(true);
        sendRequestButton.setDisable(true);
        editInWindowButton.setDisable(true);
        disconnectMenuItem.setDisable(true);
        countAllRowLabel.setText("Неизвестно");
    }

    public boolean isSendCustomReq() {
        return sendCustomReq;
    }
    public static void play(){
        launch();
    }
    public ArrayList<String> getAllNames() {
        return allNames;
    }

    public void setNumberPage(int numberPage) {
        this.numberPage = numberPage;
    }

    public MainModel getMainModel() {
        return mainModel;
    }

    public int getNumberPage(){
        return numberPage;
    }

    public TextField getNumberRowTextField() {
        return numberRowTextField;
    }

    public Label getCountAllRowLabel() {
        return countAllRowLabel;
    }

    public Button getAddRowButton() {
        return addRowButton;
    }

    public MenuItem getAddRow() {
        return addRow;
    }

    public GridPane getRowGridPane() {
        return rowGridPane;
    }
}
