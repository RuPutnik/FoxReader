package ru.putnik.foxreader.model;

import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import ru.putnik.foxreader.ConnectionProperty;
import ru.putnik.foxreader.TypeTreeElement;
import ru.putnik.foxreader.controller.MainController;

import java.sql.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static ru.putnik.foxreader.FLogger.*;
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
    private ArrayList<String> fullColumnNames=new ArrayList<>();
    private boolean useInsert=false;
    private TextField[] fields;
    private CheckBox[] checkBoxes;
    private ArrayList<String> addedRow=new ArrayList<>();

    public MainModel(MainController controller){
        mainController=controller;
    }

    public void initializeConnection(ConnectionProperty propertyConnection) throws SQLException {
        property=propertyConnection;
        String urlConnection=propertyConnection.createConnectionUrl();
        String login=propertyConnection.getLogin();
        String password=propertyConnection.getPassword();
        DriverManager.setLoginTimeout(2);
        connection=DriverManager.getConnection(urlConnection,login,password);
    }
    public void disconnect(){
        if(connection!=null&&property!=null){
            try {
                connection.close();
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Соединение закрыто");
                alert.setHeaderText(null);
                alert.setContentText("Подключение с сервером " + property.getAddress() + " на порту " + property.getPort() + " было закрыто.");
                ((Stage)alert.getDialogPane().getScene().getWindow()).getIcons().add(new Image("icons/foxIcon.png"));
                alert.show();
            } catch (SQLException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Ошибка закрытия соединения");
                alert.setHeaderText("В процессе закрытия соединения возникла ошибка!");
                alert.setContentText(e.getLocalizedMessage() + "\n" + "Код ошибки: " + e.getErrorCode());
                ((Stage)alert.getDialogPane().getScene().getWindow()).getIcons().add(new Image("icons/foxIcon.png"));
                alert.show();
            }
        }
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
                        addingViewsInTree(database);
                        addingProceduresInTree(database);

                        rootItem.getChildren().add(database);
                        mainController.getAllNames().add(dbResultSet.getString(1).toLowerCase());
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }else{
                TreeItem<TypeTreeElement> database = new TreeItem<>();
                database.setValue(new TypeTreeElement(Type.DATABASE,getNameTableConnect(property),getNameTableConnect(property),null));
                database.setGraphic(new ImageView(new Image("icons/base.jpg")));
                addingTablesInTree(database);
                addingViewsInTree(database);
                addingProceduresInTree(database);
                rootItem.getChildren().add(database);
                mainController.getAllNames().add(getNameTableConnect(property).toLowerCase());
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
        fullColumnNames.clear();
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
            TableColumn<List<String>, String> column =
                    new TableColumn<>(resultSetMetaData.getColumnName(a + 1)+": "+resultSetMetaData.getColumnTypeName(a+1)+"  ");
            columnNames.add(resultSetMetaData.getColumnName(a+1));
            fullColumnNames.add(resultSetMetaData.getColumnName(a + 1)+": "+resultSetMetaData.getColumnTypeName(a+1));
            mainController.getAllNames().add(resultSetMetaData.getColumnName(a+1).toLowerCase());
            //Берем из общих данных отдельный список и загружаем его в столбец
            column.setCellValueFactory(value -> {
                try {
                    if (resultSetMetaData.getColumnTypeName(b + 1).equals("bit")) {
                        boolean a1 = value.getValue().get(b).equals("1");

                        HBox box=new HBox();
                        CheckBox checkBox = new CheckBox();
                        checkBox.selectedProperty().setValue(a1);
                        box.setAlignment(Pos.CENTER);
                        box.getChildren().add(checkBox);
                        checkBox.selectedProperty().addListener((ov, old_val, new_val) -> {
                            String newValue;
                            if(new_val)
                                newValue="1";
                            else
                                newValue="0";
                            updateRow(b, newValue, value.getValue());
                        });
                        //noinspection unchecked
                        return new SimpleObjectProperty(box);
                    } else {
                        return new SimpleObjectProperty<>(value.getValue().get(b));
                    }
                } catch (SQLException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Ошибка обработки записи");
                    alert.setHeaderText("В процессе обновления или загрузки записи возникла ошибка!");
                    alert.setContentText(e.getLocalizedMessage() + "\n" + "Код ошибки: " + e.getErrorCode());
                    ((Stage)alert.getDialogPane().getScene().getWindow()).getIcons().add(new Image("icons/foxIcon.png"));
                    alert.show();
                    return null;
                }
            });
            column.setEditable(true);
            column.setComparator(new StringIntegerComparator());

            if(!resultSetMetaData.getColumnTypeName(a+1).equals("bit")){
                column.setCellFactory(TextFieldTableCell.forTableColumn());
            }
            column.setOnEditCommit(event -> {
                if(!mainController.isSendCustomReq()) {
                    if (useInsert) {
                        addedRow.set(event.getTablePosition().getColumn(),event.getNewValue());
                        if(insertInto(addedRow)){
                            useInsert = false;
                            addedRow.clear();
                            openSuccessAddingAlert(mainController.tableDBTableView.getItems().size());
                            mainController.addRowButton.setDisable(false);
                            mainController.addRow.setDisable(false);
                        }else{
                            mainController.tableDBTableView.getItems().add(addedRow);
                        }
                    } else {
                        updateRow(event.getTablePosition().getColumn(), event.getNewValue(), event.getRowValue());
                    }
                }else {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Невозможно изменить занечние ячейки");
                    alert.setHeaderText("Отказано в изменении данных");
                    alert.setContentText("После применения Фильтра или SQL запроса необходимо обновить таблицу перед редактированим данных");
                    ((Stage)alert.getDialogPane().getScene().getWindow()).getIcons().add(new Image("icons/foxIcon.png"));
                    alert.show();
                }
            });
            //Загружаем столбец в таблицу
            mainController.tableDBTableView.getColumns().add(column);
        }
        mainController.tableDBTableView.setItems(listColumns);//Загружаем данные в таблицу
        fillRibbonPane();//Загружаем данные в ленточное отображение
    }
    private void fillRibbonPane(){
        createGraphicStructurePane();
        mainController.countAllRowLabel.setText(String.valueOf(mainController.tableDBTableView.getItems().size()));
        openPage(mainController.getNumberPage());
    }
    private void createGraphicStructurePane(){
        mainController.rowGridPane.getChildren().clear();
        mainController.rowGridPane.setAlignment(Pos.CENTER);
        mainController.rowGridPane.getRowConstraints().clear();
        mainController.rowGridPane.getColumnConstraints().clear();

        ColumnConstraints constraints=new ColumnConstraints();
        ColumnConstraints constraints1=new ColumnConstraints();
        mainController.rowGridPane.getColumnConstraints().addAll(constraints,constraints1);
        constraints1.setHalignment(HPos.CENTER);
        int countCheckBoxInRow=0;
        int c=0;

        for(String fullName:fullColumnNames){
            if(fullName.split(": ")[1].equals("bit"))
                countCheckBoxInRow++;
        }

        Label[] namesColumn=new Label[fullColumnNames.size()];
        fields=new TextField[fullColumnNames.size()-countCheckBoxInRow+1];
        checkBoxes=new CheckBox[countCheckBoxInRow];
        for (int a=0;a<fullColumnNames.size();a++){
            Label l=new Label(fullColumnNames.get(a));
            namesColumn[a] = l;
            if(a<fields.length) {
                TextField tf = new TextField();
                fields[a] = tf;
            }
        }
        for(int b=0;b<countCheckBoxInRow;b++){
            checkBoxes[b]=new CheckBox();
        }
        for(int b=0;b<namesColumn.length;b++) {
            mainController.rowGridPane.add(namesColumn[b],0,b);
            if(mainController.tableDBTableView.getItems().size()!=0) {
                if (fullColumnNames.get(b).split(": ")[1].equals("bit")) {
                    mainController.rowGridPane.add(checkBoxes[c], 1, b);
                    c++;
                } else {
                    mainController.rowGridPane.add(fields[b], 1, b);
                }
            }
        }
    }
    public void openPage(int numberPage){
        if(mainController.tableDBTableView.getItems().size()>0){
            int c=0;
            int d=0;
            for(int a=0;a<fullColumnNames.size();a++){
                if(!fullColumnNames.get(a).split(": ")[1].equals("bit")){
                    fields[c].setText(mainController.tableDBTableView.getItems().get(numberPage).get(a));
                    c++;
                }
            }
            for(int a=0;a<fullColumnNames.size();a++){
                if(fullColumnNames.get(a).split(": ")[1].equals("bit")){
                    boolean isSelected;
                    isSelected = mainController.tableDBTableView.getItems().get(numberPage).get(a).equals("1");
                    checkBoxes[d].setSelected(isSelected);
                    d++;
                }
            }
        }
    }
    public void savePage(){
        ArrayList<String> newRow=new ArrayList<>();
        List<String> oldRow=mainController.tableDBTableView.getItems().get(mainController.getNumberPage());
        String updateValue="";
        int updateNumberColumn=0;
        int c=0;
        int d=0;
        for (String fullColumnName : fullColumnNames) {
            if (fullColumnName.split(": ")[1].equals("bit")) {
                if (checkBoxes[c].isSelected()) {
                    newRow.add("1");
                } else {
                    newRow.add("0");
                }
                c++;
            } else {
                newRow.add(fields[d].getText());
                d++;
            }
        }
        for(int n=0;n<oldRow.size();n++){
            if(!oldRow.get(n).equals(newRow.get(n))){
                updateValue=newRow.get(n);
                updateNumberColumn=n;
                break;
            }
        }

        addedRow=newRow;
        if(useInsert){
            if(insertInto(addedRow)){
                useInsert=false;
                addedRow.clear();
                mainController.addRow.setDisable(false);
                mainController.addRowButton.setDisable(false);
                openSuccessAddingAlert(mainController.tableDBTableView.getItems().size());
            }else{
                addedRow=newRow;
                mainController.tableDBTableView.getItems().add(addedRow);
                openPage(mainController.tableDBTableView.getItems().size()-1);
            }
        }else {
            updateRow(updateNumberColumn, updateValue, oldRow);
        }
    }
    public void deletePage(){
        int newNumberPage=0;
        int oldNumberPage=mainController.getNumberPage();
        if(mainController.getNumberPage()>0){
            newNumberPage=mainController.getNumberPage()-1;
        }else{
            newNumberPage++;
        }
        mainController.setNumberPage(newNumberPage);
        mainController.numberRowTextField.setText(String.valueOf(newNumberPage));
        removeRow(oldNumberPage);
    }
    public void addPage(){
        ArrayList<String> newRow=new ArrayList<>();
        for (int a=0;a<fullColumnNames.size();a++){
            newRow.add("NULL");
        }
        int c=0,d=0;
        for(int a=0;a<fullColumnNames.size();a++){
            if(c<fields.length) {
                fields[c].setText("NULL");
                c++;
            }
        }
        for(int b=0;b<fullColumnNames.size();b++){
            if(d<checkBoxes.length) {
                checkBoxes[d].setSelected(false);
                d++;
            }
        }
        for (String fullColumnName : fullColumnNames) {
            if (fullColumnName.split(": ")[1].equals("bit")) {
                addedRow.add("0");
            } else {
                addedRow.add("NULL");
            }
        }
        mainController.tableDBTableView.getItems().add(newRow);
        useInsert=true;
    }
    private void addingTablesInTree(TreeItem<TypeTreeElement> database){
        try {
            ResultSet tableResultSet = connection.getMetaData().getTables(null, null, null, null);
            TreeItem<TypeTreeElement> systemTable = new TreeItem<>();
            systemTable.setValue(new TypeTreeElement(Type.TABLES,"Системные таблицы",connection.getCatalog(),null));
            database.getChildren().add(systemTable);
            TreeItem<TypeTreeElement> tables = new TreeItem<>();
            tables.setValue(new TypeTreeElement(Type.TABLES,"Таблицы",connection.getCatalog(),null));
            database.getChildren().add(tables);
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
                        addingComponentsOfTable(table);
                        if (tableResultSet.getString("TABLE_NAME").equals("sysdiagrams")) {
                            database.getChildren().get(0).getChildren().add(table);
                        } else {
                            database.getChildren().get(1).getChildren().add(table);
                        }
                        mainController.getAllNames().add(tableResultSet.getString("TABLE_NAME").toLowerCase());
                    }
                    if (tableResultSet.getString("TABLE_TYPE").equals("SYSTEM TABLE")) {
                        TreeItem<TypeTreeElement> table = new TreeItem<>();
                        table.setValue(new TypeTreeElement(Type.TABLE,tableResultSet.getString("TABLE_NAME"),connection.getCatalog(),tableResultSet.getString("TABLE_SCHEM")));
                        table.setGraphic(new ImageView(new Image("icons/table.jpg")));
                        addingComponentsOfTable(table);
                        database.getChildren().get(0).getChildren().add(table);
                        mainController.getAllNames().add(tableResultSet.getString("TABLE_NAME").toLowerCase());
                    }
                }
            }else{
                while (tableResultSet.next()) {
                    if (tableResultSet.getString("TABLE_TYPE").equals("TABLE") &&
                            tableResultSet.getString("TABLE_SCHEM").equals("dbo")) {

                        TreeItem<TypeTreeElement> table = new TreeItem<>();
                        table.setValue(new TypeTreeElement(Type.TABLE,tableResultSet.getString("TABLE_NAME"),connection.getCatalog(),tableResultSet.getString("TABLE_SCHEM")));
                        table.setGraphic(new ImageView(new Image("icons/table.jpg")));
                        addingComponentsOfTable(table);
                        database.getChildren().get(0).getChildren().add(table);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private void addingComponentsOfTable(TreeItem<TypeTreeElement> table){
        TreeItem<TypeTreeElement> keys=new TreeItem<>();
        keys.setValue(new TypeTreeElement(Type.KEYS,"Ключи",table.getValue().getNameDB(),table.getValue().getSchema()));
        keys.setGraphic(new ImageView("icons/keys.jpg"));
        TreeItem<TypeTreeElement> columns=new TreeItem<>();
        columns.setValue(new TypeTreeElement(Type.COLUMNS,"Столбцы",table.getValue().getNameDB(),table.getValue().getSchema()));
        columns.setGraphic(new ImageView("icons/columns.jpg"));
        table.getChildren().add(keys);
        table.getChildren().add(columns);
    }
    private void addingProceduresInTree(TreeItem<TypeTreeElement> db){
        TreeItem<TypeTreeElement> procedures=new TreeItem<>();
        procedures.setValue(new TypeTreeElement(Type.PROCEDURES,"Хранимые процедуры",db.getValue().getNameDB(),db.getValue().getSchema()));
        db.getChildren().add(procedures);
    }
    private void addingViewsInTree(TreeItem<TypeTreeElement> db){
        TreeItem<TypeTreeElement> views=new TreeItem<>();
        views.setValue(new TypeTreeElement(Type.VIEWS,"Представления",db.getValue().getNameDB(),db.getValue().getSchema()));
        db.getChildren().add(views);
    }
    private void updateRow(int columnIndex, String newValue, List<String> row){
            ArrayList<String> newRow = new ArrayList<>(row);

            StringBuilder reqUpdate = new StringBuilder("UPDATE [" + selectedTable + "] SET ");

            if (newValue.toLowerCase().equals("null")) {
                reqUpdate.append(columnNames.get(columnIndex)).append("=").append(newValue);
            } else {
                reqUpdate.append(columnNames.get(columnIndex)).append("='").append(newValue).append("'");
            }


            reqUpdate.append(" WHERE ");

            for (int a = 0; a < columnNames.size(); a++) {
                if (newRow.get(a).toLowerCase().equals("null") || newRow.get(a) == null) {
                    reqUpdate.append(columnNames.get(a)).append(" IS NULL");
                } else {
                    reqUpdate.append(columnNames.get(a)).append("='").append(newRow.get(a)).append("'");
                }
                if (a < columnNames.size() - 1) {
                    reqUpdate.append(" AND ");
                }
            }
            reqUpdate.append(";");
            sendRequest(reqUpdate.toString(), true);

    }
    private boolean insertInto(List<String> newRow){
        ArrayList<String> newRowWithoutNull=new ArrayList<>();
        ArrayList<String> requiredNamesColumn=new ArrayList<>();
        StringBuilder reqInsert;

        for(int a=0;a<newRow.size();a++){
            if(!newRow.get(a).toLowerCase().equals("null")){
                newRowWithoutNull.add(newRow.get(a));
                requiredNamesColumn.add(columnNames.get(a));
            }
        }

        if(newRowWithoutNull.size()>0) {
            reqInsert = new StringBuilder("INSERT INTO [" + selectedTable + "](");

            for (int a = 0; a < requiredNamesColumn.size(); a++) {
                reqInsert.append(requiredNamesColumn.get(a));
                if (a < requiredNamesColumn.size() - 1) {
                    reqInsert.append(", ");
                }
            }
            reqInsert.append(") VALUES(");
            for (int c = 0; c < newRowWithoutNull.size(); c++) {
                reqInsert.append("'").append(newRowWithoutNull.get(c)).append("'");
                if (c < requiredNamesColumn.size() - 1) {
                    reqInsert.append(", ");
                }
            }
            reqInsert.append(");");
        }else {
            return false;
        }
        return sendRequest(reqInsert.toString(),true);
    }
    public boolean checkTypeRequest(String req){
        boolean isUpdate=false;

        if(req.toLowerCase().contains("update")&&req.toLowerCase().contains("set")){
            isUpdate=true;
        }

        return isUpdate;
    }
    public boolean sendRequest(String textReq, boolean updateDB){
        boolean success=true;
        try {
            if(!textReq.trim().equals("")) {
                if(updateDB) {
                    connection.prepareStatement(textReq).executeUpdate();
                    updateTable();
                }else {
                    fillTable(connection.prepareStatement(textReq));
                }
                mainController.logRequestTextArea.appendText(textReq + "\n");
                request(textReq);
            }
        } catch (SQLException e) {
            Alert alert=new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Ошибка выполнения запроса");
            alert.setHeaderText("При выполнении запроса возникла ошибка!");
            alert.setContentText(e.getLocalizedMessage()+"\n"+"Код ошибки: "+e.getErrorCode());
            alert.show();
            ((Stage)alert.getDialogPane().getScene().getWindow()).getIcons().add(new Image("icons/foxIcon.png"));

            if(updateDB){
                updateTable();//Если будет ошибка, данные в графичиской части должны откатиться к реальным
            }
            success=false;
            error(textReq,e);
        }
        return success;
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
        List<String> row=mainController.tableDBTableView.getItems().get(indexRow);
        StringBuilder builderDeleteReq=new StringBuilder();
        builderDeleteReq.append("DELETE FROM [");
        builderDeleteReq.append(selectedTable);
        builderDeleteReq.append("] WHERE ");

        for(int a=0;a<columnNames.size();a++){
            if(row.get(a)==null||row.get(a).toLowerCase().equals("null")){
                builderDeleteReq.append(columnNames.get(a)).append(" IS NULL");
            }else {
                builderDeleteReq.append(columnNames.get(a)).append("='").append(row.get(a)).append("'");
            }
            if(a<columnNames.size()-1){
                builderDeleteReq.append(" AND ");
            }
        }
        builderDeleteReq.append(";");
        mainController.setNumberPage(0);
        mainController.numberRowTextField.setText(String.valueOf(mainController.getNumberPage()));
        sendRequest(builderDeleteReq.toString(),true);
    }
    public void deleteAllRows(){
        String sqlReq;
        sqlReq="TRUNCATE TABLE ["+selectedTable+"];";
        sendRequest(sqlReq,true);
        mainController.setNumberPage(0);
    }
    public void addRow(){
        ArrayList<String> newRow=new ArrayList<>();
        for (String fullColumnName1 : fullColumnNames) {
            if (fullColumnName1.split(": ")[1].equals("bit")) {
                newRow.add("0");
            } else {
                newRow.add("NULL");
            }
        }
        for (String fullColumnName : fullColumnNames) {
            if (fullColumnName.split(": ")[1].equals("bit")) {
                addedRow.add("0");
            } else {
                addedRow.add("NULL");
            }
        }
        mainController.tableDBTableView.getItems().add(newRow);
        useInsert=true;
    }
    public void updateTable(){
        firstFillTable(selectedTable,selectedDB,selectedSchema);
    }
    private void openSuccessAddingAlert(int numberRow){
        Alert alert=new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Новая запись успешно добавлена!");
        alert.setHeaderText(null);
        alert.setContentText("Запись под номером "+numberRow+" успешно добавлена в таблицу "+selectedTable);
        ((Stage)alert.getDialogPane().getScene().getWindow()).getIcons().add(new Image("icons/foxIcon.png"));
        alert.show();
    }
    private class StringIntegerComparator implements Comparator<String>{
        @Override
        public int compare(String o1, String o2) {

            if (o1 == null && o2 == null) return 0;
            if (o1 == null) return -1;
            if (o2 == null) return 1;

            Double i1=null;
            try{ i1=Double.valueOf(o1); } catch(NumberFormatException ignored){}
            Double i2=null;
            try{ i2=Double.valueOf(o2); } catch(NumberFormatException ignored){}

            if(i1==null && i2==null) return o1.compareTo(o2);
            if(i1==null) return -1;
            if(i2==null) return 1;

            double c=i1-i2;

            if(c<0){
                return -1;
            }else if(c==0) {
                return 0;
            }else{
                return 1;
            }
        }
    }
}