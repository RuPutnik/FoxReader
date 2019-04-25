package ru.putnik.foxreader.model;

import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import ru.putnik.foxreader.ConnectionProperty;
import ru.putnik.foxreader.ImageLoader;
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
        new ImageLoader();
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
                ((Stage)alert.getDialogPane().getScene().getWindow()).getIcons().add(ImageLoader.getIconImage());
                alert.show();
            } catch (SQLException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Ошибка закрытия соединения");
                alert.setHeaderText("В процессе закрытия соединения возникла ошибка!");
                alert.setContentText(e.getLocalizedMessage() + "\n" + "Код ошибки: " + e.getErrorCode());
                ((Stage)alert.getDialogPane().getScene().getWindow()).getIcons().add(ImageLoader.getIconImage());
                alert.show();
                error("Ошибка при попытке отключения от сервера ",e);
            }
        }
    }
    public void fillTree(){
        if(connection!=null) {
            TreeItem<TypeTreeElement> rootItem=new TreeItem<>();
            rootItem.setValue(new TypeTreeElement(Type.SERVER,"Сервер "+property.getTypeServer()+" "+property.getAddress()+":"+property.getPort(),null,null,null));
            rootItem.setGraphic(new ImageView(ImageLoader.getServer()));
            if(getNameTableConnect(property).equals("")) {
                try {
                    ResultSet dbResultSet = connection.getMetaData().getCatalogs();
                    while (dbResultSet.next()) {
                        TreeItem<TypeTreeElement> database = new TreeItem<>();

                        database.setValue(new TypeTreeElement(Type.DATABASE,dbResultSet.getString(1),dbResultSet.getString(1),null,null));
                        database.setGraphic(new ImageView(ImageLoader.getBase()));
                        connection.setCatalog(database.getValue().getName());
                        addingTablesInTree(database);
                        addingViewsInTree(database);
                        addingProceduresInTree(database);

                        rootItem.getChildren().add(database);
                        mainController.getAllNames().add(dbResultSet.getString(1).toLowerCase());
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    error("Ошибка при генерации дерева ",e);
                }
            }else{
                TreeItem<TypeTreeElement> database = new TreeItem<>();
                database.setValue(new TypeTreeElement(Type.DATABASE,getNameTableConnect(property),getNameTableConnect(property),null,null));
                database.setGraphic(new ImageView(ImageLoader.getBase()));
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
            error("Ошибка при загрузке таблицы ",e);
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
                    ((Stage)alert.getDialogPane().getScene().getWindow()).getIcons().add(ImageLoader.getIconImage());
                    alert.show();
                    error("Ошибка обработки записи ",e);
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
                            mainController.getAddRowButton().setDisable(false);
                            mainController.getAddRow().setDisable(false);
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
                    ((Stage)alert.getDialogPane().getScene().getWindow()).getIcons().add(ImageLoader.getIconImage());
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
        mainController.getCountAllRowLabel().setText(String.valueOf(mainController.tableDBTableView.getItems().size()));
        openPage(mainController.getNumberPage());
    }
    private void createGraphicStructurePane(){
        mainController.getRowGridPane().getChildren().clear();
        mainController.getRowGridPane().setAlignment(Pos.CENTER);
        mainController.getRowGridPane().getRowConstraints().clear();
        mainController.getRowGridPane().getColumnConstraints().clear();

        ColumnConstraints constraints=new ColumnConstraints();
        ColumnConstraints constraints1=new ColumnConstraints();
        mainController.getRowGridPane().getColumnConstraints().addAll(constraints,constraints1);
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
            mainController.getRowGridPane().add(namesColumn[b],0,b);
            if(mainController.tableDBTableView.getItems().size()!=0) {
                if (fullColumnNames.get(b).split(": ")[1].equals("bit")) {
                    mainController.getRowGridPane().add(checkBoxes[c], 1, b);
                    c++;
                } else {
                    mainController.getRowGridPane().add(fields[b], 1, b);
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
                mainController.getAddRow().setDisable(false);
                mainController.getAddRowButton().setDisable(false);
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
        mainController.getNumberRowTextField().setText(String.valueOf(newNumberPage));
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
            systemTable.setValue(new TypeTreeElement(Type.TABLES,"Системные таблицы",connection.getCatalog(),null,null));
            database.getChildren().add(systemTable);
            TreeItem<TypeTreeElement> tables = new TreeItem<>();
            tables.setValue(new TypeTreeElement(Type.TABLES,"Таблицы",connection.getCatalog(),null,null));
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
                        table.setValue(new TypeTreeElement(Type.TABLE,tableResultSet.getString("TABLE_NAME"),connection.getCatalog(),tableResultSet.getString("TABLE_NAME"),tableResultSet.getString("TABLE_SCHEM")));
                        table.setGraphic(new ImageView(ImageLoader.getTable()));
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
                        table.setValue(new TypeTreeElement(Type.TABLE,tableResultSet.getString("TABLE_NAME"),connection.getCatalog(),tableResultSet.getString("TABLE_NAME"),tableResultSet.getString("TABLE_SCHEM")));
                        table.setGraphic(new ImageView(ImageLoader.getTable()));
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
                        table.setValue(new TypeTreeElement(Type.TABLE,tableResultSet.getString("TABLE_NAME"),connection.getCatalog(),tableResultSet.getString("TABLE_NAME"),tableResultSet.getString("TABLE_SCHEM")));
                        table.setGraphic(new ImageView(ImageLoader.getTable()));
                        addingComponentsOfTable(table);
                        database.getChildren().get(0).getChildren().add(table);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            error("Ошибка при добавлении таблиц в дерево ",e);
        }
    }
    private void addingComponentsOfTable(TreeItem<TypeTreeElement> table){
        TreeItem<TypeTreeElement> keys=new TreeItem<>();
        keys.setValue(new TypeTreeElement(Type.KEYS,"Ключи",table.getValue().getNameDB(),table.getValue().getNameTable(),table.getValue().getSchema()));
        keys.setGraphic(new ImageView(ImageLoader.getKeys()));
        TreeItem<TypeTreeElement> columns=new TreeItem<>();
        columns.setValue(new TypeTreeElement(Type.COLUMNS,"Столбцы",table.getValue().getNameDB(),table.getValue().getNameTable(),table.getValue().getSchema()));
        columns.setGraphic(new ImageView(ImageLoader.getColumns()));
        TreeItem<TypeTreeElement> indexes=new TreeItem<>();
        indexes.setValue(new TypeTreeElement(Type.INDEXES,"Индексы",table.getValue().getNameDB(),table.getValue().getNameTable(),table.getValue().getSchema()));
        indexes.setGraphic(new ImageView(ImageLoader.getIndexes()));
        table.getChildren().add(keys);
        table.getChildren().add(columns);
        table.getChildren().add(indexes);
        addingKeys(keys);
        addingColumns(columns);
        addingIndexes(indexes);
    }
    private void addingIndexes(TreeItem<TypeTreeElement> indexItem){
        DatabaseMetaData dbMetaData;
        try {
            dbMetaData = connection.getMetaData();
            ResultSet rs = dbMetaData.getIndexInfo(indexItem.getValue().getNameDB(), indexItem.getValue().getSchema(), indexItem.getValue().getNameTable(), false, false);
            String previousIndexName="";
            String nonUnique;
            String typeIndex="";
            boolean clustered=false;
            while (rs.next()) {
               if(rs.getString("INDEX_NAME")!=null&&!previousIndexName.equals(rs.getString("INDEX_NAME"))) {
                    TreeItem<TypeTreeElement> index = new TreeItem<>();
                    if(rs.getBoolean("NON_UNIQUE")){
                       nonUnique="Не уникальный, ";
                    }else{
                       nonUnique="";
                    }
                    switch (rs.getShort("TYPE")) {
                       case DatabaseMetaData.tableIndexClustered:
                           typeIndex="Кластеризованный";
                           clustered=true;
                           break;
                       case DatabaseMetaData.tableIndexHashed:
                           typeIndex="Хэшированный";
                           clustered=false;
                           break;
                       case DatabaseMetaData.tableIndexOther:
                           typeIndex="Некластеризованный";
                           clustered=false;
                           break;
                       case DatabaseMetaData.tableIndexStatistic:
                           typeIndex = "Статистический";
                           clustered=false;
                           break;
                   }
                    index.setValue(new TypeTreeElement(Type.INDEX, rs.getString("INDEX_NAME")+" ("+nonUnique+typeIndex+")", indexItem.getValue().getNameDB(),
                            indexItem.getValue().getNameTable(), indexItem.getValue().getSchema()));
                    if(clustered) {
                        index.setGraphic(new ImageView(ImageLoader.getPrimaryKey()));
                    }else {
                        index.setGraphic(new ImageView(ImageLoader.getIndex()));
                    }
                    indexItem.getChildren().add(index);
                    previousIndexName = rs.getString("INDEX_NAME");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            error("Ошибка при добавлении индексов в дерево ",e);
        }

    }
    private void addingKeys(TreeItem<TypeTreeElement> keysItem){
        try {
            ResultSet set=connection.getMetaData().getExportedKeys(keysItem.getValue().getNameDB(),keysItem.getValue().getSchema(),keysItem.getValue().getNameTable());
            String namePrimaryKey;
            String nameForeignKey;
            ResultSet set1=connection.getMetaData().getPrimaryKeys(keysItem.getValue().getNameDB(),keysItem.getValue().getSchema(),keysItem.getValue().getNameTable());

            while (set1.next()) {
                namePrimaryKey = set1.getString("PK_NAME");

                TreeItem<TypeTreeElement> primaryKey = new TreeItem<>();
                primaryKey.setValue(new TypeTreeElement(Type.PRIMARY_KEY, namePrimaryKey, keysItem.getValue().getNameDB(), keysItem.getValue().getNameTable(), ""));
                primaryKey.setGraphic(new ImageView(ImageLoader.getPrimaryKey()));
                keysItem.getChildren().add(primaryKey);
            }
            while (set.next()){
                nameForeignKey=set.getString("FK_NAME");
                TreeItem<TypeTreeElement> foreignKey=new TreeItem<>();
                foreignKey.setValue(new TypeTreeElement(Type.FOREIGN_KEY,nameForeignKey,keysItem.getValue().getNameDB(),keysItem.getValue().getNameTable(),""));
                foreignKey.setGraphic(new ImageView(ImageLoader.getForeignKey()));
                keysItem.getChildren().add(foreignKey);
            }
            set.close();
            set1.close();
        } catch (SQLException e) {
            e.printStackTrace();
            error("Ошибка при добавлении ключей в дерево ",e);
        }
    }
    private void addingColumns(TreeItem<TypeTreeElement> cols){
        try {
            if(!(cols.getValue().getNameTable().equals("syscollector_config_store_internal")||cols.getValue().getNameTable().equals("syspolicy_configuration_internal")||
                    cols.getValue().getNameTable().equals("sysutility_mi_smo_stage_internal")||
                    cols.getValue().getNameTable().equals("sysutility_ucp_configuration_internal"))) {
                ResultSet resSet = connection.prepareStatement("USE " + cols.getValue().getNameDB() + "; SELECT * FROM [" + cols.getValue().getNameTable() + "];").executeQuery();
                ResultSetMetaData metaData = resSet.getMetaData();
                resSet.next();
                     for(int a=1;a<=metaData.getColumnCount();a++){
                         TreeItem<TypeTreeElement> column=new TreeItem<>();
                         column.setValue(new TypeTreeElement(Type.COLUMN,metaData.getColumnName(a)+" ("+metaData.getColumnTypeName(a)+")",cols.getValue().getNameDB(),cols.getValue().getNameTable(),""));
                         column.setGraphic(new ImageView(ImageLoader.getColumn()));
                         cols.getChildren().add(column);
                     }
                resSet.close();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            error("Ошибка при добавлении столбцов в дерево ",e);
        }

    }
    private void addingProceduresInTree(TreeItem<TypeTreeElement> db){
        TreeItem<TypeTreeElement> procedures=new TreeItem<>();
        procedures.setValue(new TypeTreeElement(Type.PROCEDURES,"Хранимые процедуры",db.getValue().getNameDB(),db.getValue().getNameTable(),db.getValue().getSchema()));
        addingProcedures(procedures);
        db.getChildren().add(procedures);
    }
    private void addingProcedures(TreeItem<TypeTreeElement> db){
        try {
            DatabaseMetaData metaData=connection.getMetaData();

            ResultSet set=metaData.getProcedureColumns(db.getValue().getNameDB(),"dbo","%","%");
            String previousProcedureName="";
            while (set.next()){
                TreeItem<TypeTreeElement> procedure=new TreeItem<>();

                String procedureName=set.getString(3).split(";")[0];
                if(!procedureName.equals(previousProcedureName)) {
                    procedure.setValue(new TypeTreeElement(Type.PROCEDURE, procedureName, db.getValue().getNameDB(),db.getValue().getNameTable(), db.getValue().getSchema()));
                    procedure.setGraphic(new ImageView(ImageLoader.getProcedure()));
                    db.getChildren().add(procedure);
                    previousProcedureName=procedureName;

                }
            }
            ResultSet set1=metaData.getProcedureColumns(db.getValue().getNameDB(),"dbo","%","%");
            while (set1.next()){
                for(int a=0;a<db.getChildren().size();a++){
                    if(db.getChildren().get(a).getValue().getName().equals(set1.getString(3).split(";")[0])){
                        if(!set1.getString(4).equals("@RETURN_VALUE")) {
                            String paramType;
                            if(set1.getString(5).equals("1")){
                                paramType="Входной";
                            }else {
                                paramType="Выходной";
                            }
                            String dataParam=set1.getString(4)+" ("+set1.getString(7)+","+paramType+")   ";
                            TreeItem<TypeTreeElement> procedureParam = new TreeItem<>();
                            procedureParam.setValue(new TypeTreeElement(Type.PROCEDURE_PARAM, dataParam, db.getValue().getNameDB(), db.getValue().getNameTable(), db.getValue().getSchema()));
                            procedureParam.setGraphic(new ImageView(ImageLoader.getParam()));
                            db.getChildren().get(a).getChildren().add(procedureParam);
                        }
                    }
                }
            }
            set.close();
        } catch (SQLException e) {
            e.printStackTrace();
            error("Ошибка при добавлении процедур в дерево ",e);
        }

    }
    private void addingViewsInTree(TreeItem<TypeTreeElement> db){
        TreeItem<TypeTreeElement> views=new TreeItem<>();
        views.setValue(new TypeTreeElement(Type.VIEWS,"Представления",db.getValue().getNameDB(),db.getValue().getNameTable(),db.getValue().getSchema()));
        addingViews(views);
        db.getChildren().add(views);
    }
    private void addingViews(TreeItem<TypeTreeElement> item){
        try {
            ResultSet set=connection.getMetaData().getTables(item.getValue().getNameDB(), item.getValue().getSchema(), "%", new String[] {"VIEW"});
            TreeItem<TypeTreeElement> sysviews = new TreeItem<>();
            sysviews.setValue(new TypeTreeElement(Type.VIEWS, "Системные представления", item.getValue().getNameDB(), item.getValue().getNameTable(), item.getValue().getSchema()));
            while (set.next()){
                if(set.getString("TABLE_SCHEM").equals("dbo")) {
                    TreeItem<TypeTreeElement> view = new TreeItem<>();
                    view.setValue(new TypeTreeElement(Type.VIEW, set.getString("TABLE_NAME"), item.getValue().getNameDB(), item.getValue().getNameTable(), item.getValue().getSchema()));
                    view.setGraphic(new ImageView(ImageLoader.getView()));
                    item.getChildren().add(view);
                }else{
                    TreeItem<TypeTreeElement> view = new TreeItem<>();
                    view.setValue(new TypeTreeElement(Type.VIEW, set.getString("TABLE_NAME"), item.getValue().getNameDB(), item.getValue().getNameTable(), item.getValue().getSchema()));
                    view.setGraphic(new ImageView(ImageLoader.getView()));
                    sysviews.getChildren().add(view);
                }
            }
            item.getChildren().add(sysviews);
        } catch (SQLException e) {
            e.printStackTrace();
            error("Ошибка при добавлении отображений в дерево",e);
        }
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
                request("Запрос "+textReq);
            }
        } catch (SQLException e) {
            Alert alert=new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Ошибка выполнения запроса");
            alert.setHeaderText("При выполнении запроса возникла ошибка!");
            alert.setContentText(e.getLocalizedMessage()+"\n"+"Код ошибки: "+e.getErrorCode());
            alert.show();
            ((Stage)alert.getDialogPane().getScene().getWindow()).getIcons().add(ImageLoader.getIconImage());

            if(updateDB){
                updateTable();//Если будет ошибка, данные в графичиской части должны откатиться к реальным
            }
            success=false;
            error("Ошибка выполнения запроса "+textReq,e);
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
        mainController.getNumberRowTextField().setText(String.valueOf(mainController.getNumberPage()));
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
        ((Stage)alert.getDialogPane().getScene().getWindow()).getIcons().add(ImageLoader.getIconImage());
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

    public Connection getConnection() {
        return connection;
    }
}