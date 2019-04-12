package ru.putnik.foxreader.model;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TreeItem;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import ru.putnik.foxreader.ConnectionProperty;
import ru.putnik.foxreader.TypeTreeElement;
import ru.putnik.foxreader.controller.MainController;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static ru.putnik.foxreader.TypeTreeElement.Type;

/**
 * Создано 10.04.2019 в 2:00
 */
public class MainModel {
    private Connection connection;
    private MainController mainController;
    private ConnectionProperty property;
    private String[] systemDBName={"master","msdb"};
    public MainModel(MainController controller){
        mainController=controller;
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public boolean initializeConnection(ConnectionProperty propertyConnection) throws SQLException {
        boolean success=false;
        property=propertyConnection;
        String urlConnection=propertyConnection.createConnectionUrl();
        String login=propertyConnection.getLogin();
        String password=propertyConnection.getPassword();
        DriverManager.setLoginTimeout(2);
        connection=DriverManager.getConnection(urlConnection,login,password);
        return success;
    }
    public void fillTree(){
        if(connection!=null) {

            TreeItem<TypeTreeElement> rootItem=new TreeItem<>();
            rootItem.setValue(new TypeTreeElement(Type.SERVER,"Сервер "+property.getTypeServer()+" "+property.getAddress()+":"+property.getPort()+"    ",null,null));
            rootItem.setGraphic(new ImageView(new Image("icons/server.jpg")));
            if(getNameTableConnect(property).equals("")) {
                try {
                    ResultSet dbResultSet = connection.getMetaData().getCatalogs();
                    while (dbResultSet.next()) {
                        TreeItem<TypeTreeElement> database = new TreeItem<>();
                        database.setValue(new TypeTreeElement(Type.DATABASE,dbResultSet.getString(1),dbResultSet.getString(1),null));
                        database.setGraphic(new ImageView(new Image("icons/base.jpg")));
                        connection.setCatalog(database.getValue().getName());
                        addingTablesInTree(database);

                        rootItem.getChildren().add(database);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }else{
                TreeItem<TypeTreeElement> database = new TreeItem<>();
                database.setValue(new TypeTreeElement(Type.DATABASE,getNameTableConnect(property),getNameTableConnect(property),null));
                database.setGraphic(new ImageView(new Image("icons/base.jpg")));
                addingTablesInTree(database);
                rootItem.getChildren().add(database);
            }

            rootItem.setExpanded(true);
            mainController.treeDBTreeView.setRoot(rootItem);
        }
    }
    public void firstFillTable(String nameTable,String db,String schema){
        try {
            ResultSet set=connection.getMetaData().getPrimaryKeys(db,schema,nameTable);
            String primaryKey="";

            if(set.next()) {
                primaryKey=" ORDER BY "+set.getString("COLUMN_NAME");
            }
            String request="USE "+db+" SELECT * FROM ["+nameTable+"]"+primaryKey;
            PreparedStatement statement=connection.prepareStatement(request);

            fillTable(statement);

            mainController.logRequestTextArea.appendText(request+"\n");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private void fillTable(PreparedStatement statement) throws SQLException{
            ResultSet data=statement.executeQuery();
            mainController.tableDBTableView.getColumns().clear();
            ObservableList<List<Object>> listColumns = FXCollections.observableArrayList();
            while (data.next()) {
                List<Object> list = new ArrayList<>();
                //Формируем данные (Список списков объектов)
                for (int a = 0; a < data.getMetaData().getColumnCount(); a++) {

                    list.add(data.getObject(a + 1));
                }
                listColumns.add(list);
            }
            for (int a = 0; a < data.getMetaData().getColumnCount(); a++) {
                int b = a;
                //Формируем столбец
                TableColumn<List<Object>, Object> column = new TableColumn<>(statement.getMetaData().getColumnName(a + 1));
                //Берем из общих данных отдельный список и загружаем его в столбец
                column.setCellValueFactory(value -> new SimpleObjectProperty<>(value.getValue().get(b)));
                //Загружаем столбец в таблицу
                mainController.tableDBTableView.getColumns().add(column);
            }
            mainController.tableDBTableView.setItems(listColumns);//Загружаем данные в таблицу
    }
    private void addingTablesInTree(TreeItem<TypeTreeElement> database){
        try {
            ResultSet tableResultSet = connection.getMetaData().getTables(null, null, null, null);
            TreeItem<TypeTreeElement> systemTable = new TreeItem<>();
            systemTable.setValue(new TypeTreeElement(Type.CATALOG,"Системные таблицы",connection.getCatalog(),null));
            database.getChildren().add(systemTable);
            boolean systemDB=false;
            for (String name:systemDBName){
                if(name.equals(connection.getCatalog())) {
                    systemDB = true;
                    break;
                }
                systemDB=false;
            }
            if(!systemDB) {
                while (tableResultSet.next()) {
                    if (tableResultSet.getString("TABLE_TYPE").equals("TABLE") &&
                            tableResultSet.getString("TABLE_SCHEM").equals("dbo")) {

                        TreeItem<TypeTreeElement> table = new TreeItem<>();
                        table.setValue(new TypeTreeElement(Type.TABLE,tableResultSet.getString("TABLE_NAME"),connection.getCatalog(),tableResultSet.getString("TABLE_SCHEM")));

                        table.setGraphic(new ImageView(new Image("icons/table.jpg")));
                        if (tableResultSet.getString("TABLE_NAME").equals("sysdiagrams")) {
                            database.getChildren().get(0).getChildren().add(table);
                        } else {
                            database.getChildren().add(table);
                        }
                    }
                    if (tableResultSet.getString("TABLE_TYPE").equals("SYSTEM TABLE")) {
                        TreeItem<TypeTreeElement> table = new TreeItem<>();
                        table.setValue(new TypeTreeElement(Type.TABLE,tableResultSet.getString("TABLE_NAME"),connection.getCatalog(),tableResultSet.getString("TABLE_SCHEM")));
                        table.setGraphic(new ImageView(new Image("icons/table.jpg")));
                        database.getChildren().get(0).getChildren().add(table);
                    }
                }
            }else{
                while (tableResultSet.next()) {
                    if (tableResultSet.getString("TABLE_TYPE").equals("TABLE") &&
                            tableResultSet.getString("TABLE_SCHEM").equals("dbo")) {

                        TreeItem<TypeTreeElement> table = new TreeItem<>();
                        table.setValue(new TypeTreeElement(Type.TABLE,tableResultSet.getString("TABLE_NAME"),connection.getCatalog(),tableResultSet.getString("TABLE_SCHEM")));
                        table.setGraphic(new ImageView(new Image("icons/table.jpg")));
                        database.getChildren().get(0).getChildren().add(table);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public void sendRequest(String textReq){
        try {
            fillTable(connection.prepareStatement(textReq));
        } catch (SQLException e) {
            Alert alert=new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Ошибка выполнения запроса");
            alert.setHeaderText("При выполнении запроса возникла ошибка!");
            alert.setContentText(e.getLocalizedMessage()+"\n"+"Код ошибки: "+e.getErrorCode());
            alert.show();
        }
    }
    private String getNameTableConnect(ConnectionProperty property){
        String urlCon=property.createConnectionUrl();
        boolean connectToDB=urlCon.contains("databaseName");
        String nameDB;
        if(connectToDB){
            nameDB=urlCon.split("databaseName=")[1];
        }else{
            nameDB="";
        }
        return nameDB;
    }
}
