package ru.putnik.foxreader.model;

import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TreeItem;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import ru.putnik.foxreader.ConnectionProperty;
import ru.putnik.foxreader.TypeTreeElement;
import ru.putnik.foxreader.controller.MainController;

import java.sql.*;
import java.util.ArrayList;
import java.util.Comparator;
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
    private String selectedDB;
    private String selectedTable;
    private String selectedSchema;
    private ArrayList<String> columnNames=new ArrayList<>();
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
            String request="USE "+db+"; SELECT * FROM ["+nameTable+"]"+primaryKey+";";
            PreparedStatement statement=connection.prepareStatement(request);

            fillTable(statement);
            selectedDB=db;
            selectedTable=nameTable;
            selectedSchema=schema;
            mainController.logRequestTextArea.appendText(request+"\n");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private void fillTable(PreparedStatement statement) throws SQLException{
        ResultSet data=statement.executeQuery();
        mainController.tableDBTableView.getColumns().clear();
        columnNames.clear();
        ObservableList<List<String>> listColumns = FXCollections.observableArrayList();
        while (data.next()) {
            List<String> list = new ArrayList<>();
            //Формируем данные (Список списков объектов)
            for (int a = 0; a < data.getMetaData().getColumnCount(); a++) {
                String valueCell=data.getString(a+1);
                if(valueCell!=null){
                    valueCell=valueCell.trim();
                    if(valueCell.equals("")){
                        valueCell="NULL";
                    }
                }
                if(valueCell==null){
                    valueCell="NULL";
                }

                list.add(valueCell);
            }
            listColumns.add(list);
        }
        for (int a = 0; a < data.getMetaData().getColumnCount(); a++) {
            int b = a;
            //Формируем столбец
            ResultSetMetaData resultSetMetaData=statement.getMetaData();
            TableColumn<List<String>, String> column = new TableColumn<>(resultSetMetaData.getColumnName(a + 1));
            columnNames.add(resultSetMetaData.getColumnName(a+1));
            //Берем из общих данных отдельный список и загружаем его в столбец
            column.setCellValueFactory(value ->new SimpleObjectProperty<>(value.getValue().get(b)));
            column.setEditable(true);
            column.setComparator(new StringIntegerComparator());
            //Либо этот вариант, либо самописные ячейки
            column.setCellFactory(TextFieldTableCell.forTableColumn());

            column.setOnEditCommit(event -> {
                updateRow(event.getTablePosition().getColumn(),event.getNewValue(),event.getRowValue());
            });
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
    private void updateRow(int columnIndex, String newValue, List<String> row){
        ArrayList<String> newRow=new ArrayList<>(row);

        StringBuilder reqUpdate= new StringBuilder("UPDATE " + selectedTable + " SET ");

        if(newValue.toLowerCase().equals("null")){
            reqUpdate.append(columnNames.get(columnIndex)).append("=").append(newValue);
        }else {
            try {
                Integer.parseInt(newValue);
                reqUpdate.append(columnNames.get(columnIndex)).append("=").append(newValue);
            } catch (NumberFormatException e) {
                reqUpdate.append(columnNames.get(columnIndex)).append("='").append(newValue).append("'");
            }
        }


        reqUpdate.append(" WHERE ");

        for(int a=0;a<columnNames.size();a++){
            if(newRow.get(a)==null||newRow.get(a).toLowerCase().equals("null")){
                reqUpdate.append(columnNames.get(a)).append(" IS NULL");
            }else {
                try {
                    Integer.parseInt(newRow.get(a));
                    reqUpdate.append(columnNames.get(a)).append("=").append(newRow.get(a));
                } catch (NumberFormatException e) {
                    reqUpdate.append(columnNames.get(a)).append("='").append(newRow.get(a)).append("'");
                }
            }
            if(a<columnNames.size()-1){
              reqUpdate.append(" AND ");
            }
        }
        reqUpdate.append(";");
        System.out.println(reqUpdate.toString());

        sendRequest(reqUpdate.toString(),true);
    }
    public boolean checkTypeRequest(String req){
        boolean isUpdate=false;

        if(req.toLowerCase().contains("update")&&req.toLowerCase().contains("set")){
            isUpdate=true;
        }

        return isUpdate;
    }
    public void sendRequest(String textReq, boolean updateDB){
        try {
            if(!textReq.trim().equals("")) {
                if(updateDB) {
                    connection.prepareStatement(textReq).executeUpdate();
                }else {
                    fillTable(connection.prepareStatement(textReq));
                }
                mainController.logRequestTextArea.appendText(textReq + "\n");
            }
        } catch (SQLException e) {
            Alert alert=new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Ошибка выполнения запроса");
            alert.setHeaderText("При выполнении запроса возникла ошибка!");
            alert.setContentText(e.getLocalizedMessage()+"\n"+"Код ошибки: "+e.getErrorCode());
            alert.show();
        }
    }
    public void sendFilter(String filter){
        if(selectedDB!=null&&!selectedDB.equals("")&&!filter.trim().equals("")){
            String sqlReq;

            sqlReq="USE "+selectedDB+";";
            sqlReq=sqlReq+" SELECT * FROM ["+selectedTable+"] WHERE ";
            sqlReq=sqlReq+filter+";";
            sendRequest(sqlReq,false);
            mainController.logRequestTextArea.appendText(sqlReq+"\n");
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
    public void removeRow(int indexRow){

    }
    public void addRow(){

    }
    public void updateTable(){
        firstFillTable(selectedTable,selectedDB,selectedSchema);
    }
    private class StringIntegerComparator implements Comparator<String>{

        @Override
        public int compare(String o1, String o2) {

            if (o1 == null && o2 == null) return 0;
            if (o1 == null) return -1;
            if (o2 == null) return 1;

            Integer i1=null;
            try{ i1=Integer.valueOf(o1); } catch(NumberFormatException ignored){}
            Integer i2=null;
            try{ i2=Integer.valueOf(o2); } catch(NumberFormatException ignored){}

            if(i1==null && i2==null) return o1.compareTo(o2);
            if(i1==null) return -1;
            if(i2==null) return 1;

            return i1-i2;
        }
    }
}
