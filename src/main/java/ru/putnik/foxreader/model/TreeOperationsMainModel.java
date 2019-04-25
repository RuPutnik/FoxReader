package ru.putnik.foxreader.model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableListBase;
import javafx.fxml.FXMLLoader;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ru.putnik.foxreader.FLogger;
import ru.putnik.foxreader.ImageLoader;
import ru.putnik.foxreader.TypeTreeElement;
import ru.putnik.foxreader.controller.MainController;

import java.sql.*;
import java.util.ArrayList;
import java.util.Optional;

import static ru.putnik.foxreader.FLogger.*;

/**
 * Создано 21.04.2019 в 18:54
 */
public class TreeOperationsMainModel {
    private MainController controller;
    private MainModel model;
    public TreeOperationsMainModel(MainController controller,MainModel model){
        this.controller=controller;
        this.model=model;
    }

    public void removeTable(TreeItem<TypeTreeElement> item){
        if(showWarningDeletion(item)){
            String req="DROP TABLE dbo."+item.getValue().getName();

            try (PreparedStatement ps = model.getConnection().prepareStatement(req)){
                ps.execute();
                item.getParent().getChildren().remove(item);
                controller.logRequestTextArea.appendText(req);
                showSuccessDeleting(item);
                FLogger.request(req);
            } catch (SQLException e) {
                showErrorDeleting(item,e);
                FLogger.error("Ошибка в процессе удаления объекта "+item.getValue().getName(),e);
            }
        }

    }
    public void addTable(TreeItem<TypeTreeElement> item) {
        String request = "USE "+item.getValue().getNameDB()+"; CREATE TABLE dbo.";

        Alert addingTable = new Alert(Alert.AlertType.CONFIRMATION);
        addingTable.setTitle("Создание таблицы");
        addingTable.setHeaderText("Создание новой таблицы");
        ArrayList<HBox> columns=new ArrayList<>();
        ArrayList<TextField> names=new ArrayList<>();
        ArrayList<ComboBox> types=new ArrayList<>();
        ArrayList<CheckBox> notNulles=new ArrayList<>();
        VBox vBox = new VBox();
        vBox.setPadding(new Insets(5, 10, 5, 10));
        vBox.setSpacing(15);
        VBox columnsListBox=new VBox();
        columnsListBox.setAlignment(Pos.CENTER);
        columnsListBox.setSpacing(5);
        HBox columnBox=new HBox();

        TextField nameColumn=new TextField();
        nameColumn.setPromptText("Имя столбца");
        names.add(nameColumn);
        ObservableList<String> list=FXCollections.observableArrayList();
        list.add("int");
        list.add("bigint");
        list.add("bit");
        list.add("float");
        list.add("date");
        list.add("time(7)");
        list.add("datetime");
        list.add("char(10)");
        list.add("nchar(10)");
        list.add("binary(50)");
        list.add("varchar(50)");
        ComboBox<String> typeCBox=new ComboBox<>(list);
        typeCBox.setValue("Тип данных");
        types.add(typeCBox);
        notNulles.add(new CheckBox("NOT NULL"));
        columnBox.setAlignment(Pos.CENTER);
        columnBox.setSpacing(5);
        columnBox.getChildren().add(names.get(0));
        columnBox.getChildren().add(types.get(0));
        columnBox.getChildren().add(notNulles.get(0));
        columns.add(columnBox);
        TextField name = new TextField();
        name.setPromptText("Введите имя новой таблицы");

        HBox primaryKeyInstall=new HBox();
        TextField nameColumnKey=new TextField();
        nameColumnKey.setPrefWidth(100);
        primaryKeyInstall.setSpacing(5);
        primaryKeyInstall.setAlignment(Pos.CENTER);
        Button newColumn=new Button("Добавить столбец");
        newColumn.setOnAction(event -> {
            HBox columnNew=new HBox();

            TextField nameNew=new TextField();
            nameNew.setPromptText("Имя столбца");
            names.add(nameNew);

            ObservableList<String> listTypes=FXCollections.observableArrayList();
            listTypes.add("int");
            listTypes.add("bigint");
            listTypes.add("bit");
            listTypes.add("float");
            listTypes.add("date");
            listTypes.add("time(7)");
            listTypes.add("datetime");
            listTypes.add("char(10)");
            listTypes.add("nchar(10)");
            listTypes.add("binary(50)");
            listTypes.add("varchar(50)");

            ComboBox<String> typeColumnCBox=new ComboBox<>(list);
            typeColumnCBox.setValue("Тип данных");
            types.add(typeColumnCBox);

            notNulles.add(new CheckBox("NOT NULL"));
            columnNew.setAlignment(Pos.CENTER);
            columnNew.setSpacing(5);
            columnNew.getChildren().add(names.get(names.size()-1));
            columnNew.getChildren().add(types.get(types.size()-1));
            columnNew.getChildren().add(notNulles.get(notNulles.size()-1));

            columns.add(columnNew);
            columnsListBox.getChildren().clear();
            for(HBox box:columns){
                columnsListBox.getChildren().add(box);
            }
            if(addingTable.getHeight()<400) {
                addingTable.setHeight(addingTable.getHeight() + 30);
            }
        });
        primaryKeyInstall.getChildren().add(newColumn);
        primaryKeyInstall.getChildren().add(new Label("Primary key:"));
        primaryKeyInstall.getChildren().add(nameColumnKey);

        vBox.getChildren().add(name);
        for(HBox box:columns){
            columnsListBox.getChildren().add(box);
        }
        vBox.getChildren().add(columnsListBox);
        vBox.getChildren().add(primaryKeyInstall);
        addingTable.getDialogPane().setContent(new ScrollPane(vBox));
        ((Stage) addingTable.getDialogPane().getScene().getWindow()).getIcons().add(new Image("icons/foxIcon.png"));
        Optional<ButtonType> optional = addingTable.showAndWait();

        if (optional.get() == ButtonType.OK) {
            request=request+name.getText();
            request=createAddTableRequest(request,names,types,notNulles,nameColumnKey.getText().trim());
            TreeItem<TypeTreeElement> newTable = new TreeItem<>();
            try {
                newTable.setValue(new TypeTreeElement(TypeTreeElement.Type.TABLE, name.getText().trim(), item.getValue().getNameDB(), name.getText().trim(), "dbo"));
                newTable.setGraphic(new ImageView(ImageLoader.getTable()));
                if (name.getText().equals("")) {
                    throw new SQLException("Не указано имя новой таблицы");
                }

                PreparedStatement ps = model.getConnection().prepareStatement(request);
                ps.execute();
                item.getChildren().add(newTable);
                showSuccessAdding(newTable);
                FLogger.request(request);
                controller.logRequestTextArea.appendText(request);
        }catch(SQLException ex){
            showErrorAdding(newTable,ex);
            FLogger.error("Ошибка в процессе создания таблицы " + item.getValue().getName(), ex);
        }
    }
    }
    private String createAddTableRequest(String startRequest,ArrayList<TextField> names,ArrayList<ComboBox> types,ArrayList<CheckBox> notNull,String primaryKey){
        startRequest=startRequest+"(";
        String primaryKeyName="";
        StringBuilder builder=new StringBuilder(startRequest);
        if(!primaryKey.trim().equals("")){
            primaryKeyName=primaryKey.trim();
        }

        for(int n=0;n<names.size();n++){
            if(!names.get(n).getText().trim().equals("")&&!types.get(n).getSelectionModel().getSelectedItem().toString().equals("Тип данных")){
                builder.append(names.get(n).getText()).append(" ").append(types.get(n).getSelectionModel().getSelectedItem().toString());
                if(names.get(n).getText().trim().equals(primaryKeyName)){
                    builder.append(" CONSTRAINT PK_").append(names.get(n).getText()).append(" PRIMARY KEY");
                }
                if(notNull.get(n).isSelected()) {
                    builder.append(" NOT NULL");
                }
                builder.append(", ");
            }
        }
        builder.setLength(builder.length()-2);

        startRequest=builder.toString();
        startRequest=startRequest+")";
        return startRequest;
    }
    public void removeDB(TreeItem<TypeTreeElement> item){
        if(showWarningDeletion(item)){
            String req="DROP DATABASE "+item.getValue().getName();

            try (PreparedStatement ps = model.getConnection().prepareStatement(req)){
                ps.execute();
                item.getParent().getChildren().remove(item);
                FLogger.request(req);
                controller.logRequestTextArea.appendText(req);
                showSuccessDeleting(item);
            } catch (SQLException e) {
                showErrorDeleting(item,e);
                FLogger.error("Ошибка в процессе удаления объекта "+item.getValue().getName(),e);
            }
        }
    }
    public void addDB(TreeItem<TypeTreeElement> item){
        String nameNewDatabase=getNameNewDB();
        if(nameNewDatabase!=null){
            String request="CREATE DATABASE "+nameNewDatabase;
            TreeItem<TypeTreeElement> newDBElement=new TreeItem<>();
            newDBElement.setValue(new TypeTreeElement(TypeTreeElement.Type.DATABASE,nameNewDatabase,nameNewDatabase,null,null));
            newDBElement.setGraphic(new ImageView(ImageLoader.getBase()));
            try (PreparedStatement ps = model.getConnection().prepareStatement(request)){
                ps.execute();
                item.getChildren().add(newDBElement);
                showSuccessAdding(newDBElement);
                controller.logRequestTextArea.appendText(request);
                FLogger.request(request);
            } catch (SQLException e) {
                showErrorAdding(newDBElement,e);
                FLogger.error("Ошибка при создании объекта "+newDBElement.getValue().getName(),e);
            }
        }

    }
    public void removeProcedure(TreeItem<TypeTreeElement> item){
        if(showWarningDeletion(item)){
            String req="DROP PROCEDURE "+item.getValue().getName();

            try (PreparedStatement ps = model.getConnection().prepareStatement(req)){
                ps.execute();
                item.getParent().getChildren().remove(item);
                controller.logRequestTextArea.appendText(req);
                FLogger.request(req);
                showSuccessDeleting(item);
            } catch (SQLException e) {
                showErrorDeleting(item,e);
                FLogger.error("Ошибка в процессе удаления объекта "+item.getValue().getName(),e);
            }
        }
    }
    public void seeProcedure(TreeItem<TypeTreeElement> item){
        String request;
        request="USE "+item.getValue().getNameDB()+";";
        request=request+"EXEC sp_helptext '"+item.getValue().getName()+"';";
        try (PreparedStatement ps = model.getConnection().prepareStatement(request)){
            ResultSet procedureCode=ps.executeQuery();
            StringBuilder code=new StringBuilder();
            while (procedureCode.next()){
                code.append(procedureCode.getString(1)).append("\n");
            }
            Alert successDeletion=new Alert(Alert.AlertType.INFORMATION);
            successDeletion.setTitle("Просмотр процедуры "+item.getValue().getName());
            successDeletion.setHeaderText("Просмотр кода хранимой процедуры");
            VBox vBox=new VBox();
            vBox.setPadding(new Insets(5,10,5,10));
            TextArea textCode=new TextArea(code.toString());
            textCode.setEditable(false);
            vBox.getChildren().add(textCode);
            successDeletion.getDialogPane().setContent(vBox);
            ((Stage)successDeletion.getDialogPane().getScene().getWindow()).getIcons().add(new Image("icons/foxIcon.png"));
            successDeletion.show();

            controller.logRequestTextArea.appendText(request);
            FLogger.request(request);
        } catch (SQLException e) {
            Alert errorRead=new Alert(Alert.AlertType.ERROR);
            errorRead.setTitle("Ошибка при просмотре процедуры");
            errorRead.setHeaderText(null);
            errorRead.setContentText("При попытке чтения кода процедуры возникла ошибка:\n"+e.getLocalizedMessage());
            ((Stage)errorRead.getDialogPane().getScene().getWindow()).getIcons().add(new Image("icons/foxIcon.png"));
            errorRead.show();
            FLogger.error("Ошибка при чтении кода процедуры "+item.getValue().getName(),e);
        }

    }
    public void addProcedure(TreeItem<TypeTreeElement> item){
        String request="";
        Alert successDeletion=new Alert(Alert.AlertType.CONFIRMATION);
        successDeletion.setTitle("Создание процедуры");
        successDeletion.setHeaderText("Запись кода хранимой процедуры");
        VBox vBox=new VBox();
        vBox.setPadding(new Insets(5,10,5,10));
        vBox.setSpacing(10);
        TextArea textCode=new TextArea();
        textCode.setPromptText("Введите код процедуры, начиная со слов CREATE PROCEDURE, используя тип dbo перед именем процедуры");
        TextField name=new TextField();
        name.setPromptText("Введите имя новой процедуры");
        vBox.getChildren().add(name);
        vBox.getChildren().add(textCode);
        successDeletion.getDialogPane().setContent(vBox);
        ((Stage)successDeletion.getDialogPane().getScene().getWindow()).getIcons().add(new Image("icons/foxIcon.png"));
        Optional<ButtonType> optional=successDeletion.showAndWait();

        if(optional.get()==ButtonType.OK) {
            TreeItem<TypeTreeElement> newProcedure = new TreeItem<>();
            newProcedure.setValue(new TypeTreeElement(TypeTreeElement.Type.PROCEDURE, name.getText().trim(), item.getValue().getNameDB(), null, null));
            newProcedure.setGraphic(new ImageView(ImageLoader.getProcedure()));
            try {
            if(name.getText().equals("")) {
                throw new SQLException("Не указано имя новой процедуры");
            }else if(textCode.getText().equals("")){
                throw new SQLException("Не указано тело новой процедуры");
            }

            request=request+textCode.getText();
            PreparedStatement ps = model.getConnection().prepareStatement(request);
                ps.execute();
                item.getChildren().add(newProcedure);
                showSuccessAdding(newProcedure);
                FLogger.request(request);
                controller.logRequestTextArea.appendText(request);
            } catch (SQLException e) {
                showErrorAdding(newProcedure, e);
                FLogger.error("Ошибка при создании процедуры " + newProcedure.getValue().getName(), e);
            }
        }

    }
    public void executeProcedure(TreeItem<TypeTreeElement> item){
        try {
            String[] parametersValues=showParameterDialog(item);
            String[] resultsValues=null;
            String[] results=null;
            int d=0,d1=0;
            int startIndexResultParam=0;

            int indexParam=0;
            if(parametersValues!=null||item.getChildren().size()==0) {
                if(item.getChildren().size()!=0&&(item.getChildren().size()-parametersValues.length>0)){
                    resultsValues=new String[item.getChildren().size()-parametersValues.length];
                    results=new String[resultsValues.length];
                    for (int m=parametersValues.length;m<item.getChildren().size();m++){
                        resultsValues[d]=item.getChildren().get(m).getValue().getName();
                        d++;
                    }
                }
                StringBuilder request = new StringBuilder("{CALL dbo.");
                request.append(item.getValue().getName());
                if(item.getChildren().size()>0) {
                    request.append("(");
                    for (int a = 0; a < item.getChildren().size(); a++) {
                        request.append("?, ");
                    }
                    request.setLength(request.length() - 2);
                    request.append(")");
                }
                request.append("}");
            CallableStatement ps = model.getConnection().prepareCall(request.toString());
            ps.setEscapeProcessing(true);
            ps.setQueryTimeout(90);
            if(parametersValues!=null) {
                for (int a = 0; a < parametersValues.length; a++) {
                    ps.setString(a + 1, parametersValues[a].split(":")[0]);
                    indexParam = a + 1;
                }
            }
            if(item.getChildren().size()!=0&&parametersValues.length!=item.getChildren().size()){
                //есть выходные параметры
                indexParam++;
                startIndexResultParam=indexParam;
                for(int n=0;n<item.getChildren().size()-parametersValues.length;n++){
                    int types=0;
                    switch (item.getChildren().get(indexParam-1).getValue().getName().split("\\(")[1].split(",")[0]){
                        case "bigint":{
                            types=Types.NUMERIC;
                            break;
                        }
                        case "bit":{
                            types=Types.SMALLINT;
                            break;
                        }
                        case "int":{
                            types=Types.INTEGER;
                            break;
                        }
                        case "float":{
                            types=Types.REAL;
                            break;
                        }
                        case "date":{
                            types=Types.DATE;
                            break;
                        }
                        case "datetime":{
                            types=Types.TIME;
                            break;
                        }
                        case "char":{
                            types=Types.VARCHAR;
                            break;
                        }
                        case "nchar":{
                            types=Types.NVARCHAR;
                            break;
                        }
                        case "binary":{
                            types=Types.VARBINARY;
                            break;
                        }
                    }
                    ps.registerOutParameter(indexParam,types);
                }
            }
            ps.execute();

                if(resultsValues!=null) {
                    for (int k=startIndexResultParam-1;k<indexParam;k++){
                        results[d1]=ps.getString(k+1);
                    }
                    showResultDialog(resultsValues,results);
                }
                controller.logRequestTextArea.appendText(request.toString());
                FLogger.request(request.toString());
                Alert alert=new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Процедура успешно выполнена");
                alert.setHeaderText(null);
                alert.setContentText("Хранимая процедура "+item.getValue().getName()+" была успешно запущена и выполнена");
                ((Stage)alert.getDialogPane().getScene().getWindow()).getIcons().add(new Image("icons/foxIcon.png"));
                alert.show();
            }else{
                Alert alert=new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Ошибка запуска процедуры");
                alert.setHeaderText(null);
                alert.setContentText("Процедура не была запущена, поскольку значения параметров не были заданы");
                ((Stage)alert.getDialogPane().getScene().getWindow()).getIcons().add(new Image("icons/foxIcon.png"));
                alert.show();
                error("Процедура не была запущена, поскольку значения параметров не были заданы");
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }
    }
    private String[] showParameterDialog(TreeItem<TypeTreeElement> item){
        if(item.getChildren().size()>0) {
            int countInputParameters = 0;
            for (int a = 0; a < item.getChildren().size(); a++) {
                if (item.getChildren().get(a).getValue().getName().contains("Входной")) {
                    countInputParameters++;
                }
            }
            String[] values = new String[countInputParameters];
            Label[] namesParams = new Label[countInputParameters];
            TextField[] valuesParams = new TextField[countInputParameters];
            for (int a = 0; a < namesParams.length; a++) {
                namesParams[a] = new Label(item.getChildren().get(a).getValue().getName());
                valuesParams[a] = new TextField();
            }

            GridPane pane = new GridPane();
            pane.setAlignment(Pos.CENTER);
            pane.setVgap(5);

            ColumnConstraints constraints = new ColumnConstraints();
            ColumnConstraints constraints1 = new ColumnConstraints();
            pane.getColumnConstraints().addAll(constraints, constraints1);
            constraints1.setHalignment(HPos.CENTER);

            for (int b = 0; b < namesParams.length; b++) {
                pane.add(namesParams[b], 0, b);
                pane.add(valuesParams[b], 1, b);
            }

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Введите значения параметров");
            alert.setHeaderText("Пожалуйста, введите значения для следующих параметров\nдля корректной работы хранимой процедуры");
            alert.getDialogPane().setContent(pane);
            ((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().add(new Image("icons/foxIcon.png"));
            Optional<ButtonType> option = alert.showAndWait();

            if (option.get() == ButtonType.OK) {
                for (int a = 0; a < values.length; a++) {
                    values[a] = valuesParams[a].getText() + ":" + namesParams[a].getText().split("\\(")[1].split(",")[0];
                }
                return values;
            } else if (option.get() == ButtonType.CANCEL) {
                return null;
            } else {
                return null;
            }
        }else
            return null;
    }
    private void showResultDialog(String[] resultsVariables, String[] values){
        GridPane pane=new GridPane();
        pane.setAlignment(Pos.CENTER);
        pane.setVgap(5);

        ColumnConstraints constraints=new ColumnConstraints();
        ColumnConstraints constraints1=new ColumnConstraints();
        pane.getColumnConstraints().addAll(constraints,constraints1);
        constraints1.setHalignment(HPos.CENTER);

        for(int b=0;b<resultsVariables.length;b++) {
            TextField value=new TextField(values[b]);
            Label resultVariable=new Label(resultsVariables[b]);
            value.setEditable(false);
            pane.add(resultVariable,0,b);
            pane.add(value, 1, b);
        }

        Alert alert=new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Результаты работы процедуры");
        alert.setHeaderText("В результае выполнения, процедура вернула следующие значения: ");
        alert.getDialogPane().setContent(pane);
        ((Stage)alert.getDialogPane().getScene().getWindow()).getIcons().add(new Image("icons/foxIcon.png"));
        alert.showAndWait();

    }
    private boolean showWarningDeletion(TreeItem<TypeTreeElement> item){
        Alert warningDeletion=new Alert(Alert.AlertType.CONFIRMATION);
        warningDeletion.setTitle("Предупреждение");
        warningDeletion.setHeaderText("Подтвердите ваше действие");
        warningDeletion.setContentText("Вы действительно хотите удалить "+item.getValue().getType().toString()+" под названием "+item.getValue().getName());
        ((Stage)warningDeletion.getDialogPane().getScene().getWindow()).getIcons().add(new Image("icons/foxIcon.png"));
        Optional<ButtonType> result=warningDeletion.showAndWait();

        if (result.get() == ButtonType.OK) {
            return true;
        } else if (result.get() == ButtonType.CANCEL) {
            return false;
        } else {
            return false;
        }
    }
    private void showSuccessDeleting(TreeItem<TypeTreeElement> item){
        Alert successDeletion=new Alert(Alert.AlertType.INFORMATION);
        successDeletion.setTitle("Успешное удаление");
        successDeletion.setHeaderText(null);
        successDeletion.setContentText("Вы успешно удалили "+item.getValue().getType().toString()+" под названием "+item.getValue().getName());
        ((Stage)successDeletion.getDialogPane().getScene().getWindow()).getIcons().add(new Image("icons/foxIcon.png"));
        successDeletion.show();
    }
    private void showErrorDeleting(TreeItem<TypeTreeElement> item,SQLException ex){
        Alert errorDeletion=new Alert(Alert.AlertType.ERROR);
        errorDeletion.setTitle("Ошибка удаления");
        errorDeletion.setHeaderText(null);
        errorDeletion.setContentText("В процессе удаления "+item.getValue().getType().toString()+" под названием "+item.getValue().getName()+"\nвозникла ошибка: "+ex.getLocalizedMessage());
        ((Stage)errorDeletion.getDialogPane().getScene().getWindow()).getIcons().add(new Image("icons/foxIcon.png"));
        errorDeletion.show();
    }
    private void showSuccessAdding(TreeItem<TypeTreeElement> item){
        Alert successAdding=new Alert(Alert.AlertType.INFORMATION);
        successAdding.setTitle("Успешное ооздание");
        successAdding.setHeaderText(null);
        successAdding.setContentText("Вы успешно создали "+item.getValue().getType().toString()+" под названием "+item.getValue().getName());
        ((Stage)successAdding.getDialogPane().getScene().getWindow()).getIcons().add(new Image("icons/foxIcon.png"));
        successAdding.show();
    }
    private void showErrorAdding(TreeItem<TypeTreeElement> item,SQLException ex){
        Alert errorAdding=new Alert(Alert.AlertType.ERROR);
        errorAdding.setTitle("Ошибка создания");
        errorAdding.setHeaderText(null);
        errorAdding.setContentText("В процессе создания "+item.getValue().getType().toString()+" под названием "+item.getValue().getName()+"\nвозникла ошибка: "+ex.getLocalizedMessage());
        ((Stage)errorAdding.getDialogPane().getScene().getWindow()).getIcons().add(new Image("icons/foxIcon.png"));
        errorAdding.show();
    }
    private String getNameNewDB(){
        String name=null;

        Alert nameDb=new Alert(Alert.AlertType.CONFIRMATION);
        nameDb.setTitle("Создание базы данных");
        nameDb.setHeaderText("Придумайте уникальное название для новой базы данных");
        ((Stage)nameDb.getDialogPane().getScene().getWindow()).getIcons().add(new Image("icons/foxIcon.png"));
        VBox box=new VBox();
        box.setPadding(new Insets(5,10,0,10));
        TextField nameDbField=new TextField();
        box.getChildren().add(nameDbField);
        nameDbField.setPromptText("Введите название");
        nameDb.getDialogPane().setContent(box);
        Optional<ButtonType> optional=nameDb.showAndWait();

        if(optional.get()==ButtonType.OK){
            name=nameDbField.getText();
        }
        return name;
    }
}
