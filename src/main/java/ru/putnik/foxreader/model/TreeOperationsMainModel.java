package ru.putnik.foxreader.model;

import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import ru.putnik.foxreader.FLogger;
import ru.putnik.foxreader.TypeTreeElement;
import ru.putnik.foxreader.controller.MainController;

import java.sql.*;
import java.util.Arrays;
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
    public void addTable(TreeItem<TypeTreeElement> item){
        System.out.println("Создать таблицу: "+item.getParent().getValue().getName());
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
        System.out.println("Создать базу данных: "+item.getValue().getName());
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
    public void addProcedure(TreeItem<TypeTreeElement> item){
        System.out.println("Создать процедуру: "+item.getParent().getValue().getName());
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
        Alert warningDeletion=new Alert(Alert.AlertType.INFORMATION);
        warningDeletion.setTitle("Успешное удаление");
        warningDeletion.setHeaderText(null);
        warningDeletion.setContentText("Вы успешно удалили "+item.getValue().getType().toString()+" под названием "+item.getValue().getName());
        ((Stage)warningDeletion.getDialogPane().getScene().getWindow()).getIcons().add(new Image("icons/foxIcon.png"));
        warningDeletion.show();
    }
    private void showErrorDeleting(TreeItem<TypeTreeElement> item,SQLException ex){
        Alert warningDeletion=new Alert(Alert.AlertType.ERROR);
        warningDeletion.setTitle("Ошибка удаления");
        warningDeletion.setHeaderText(null);
        warningDeletion.setContentText("В процессе удаления "+item.getValue().getType().toString()+" под названием "+item.getValue().getName()+"\nвозникла ошибка: "+ex.getLocalizedMessage());
        ((Stage)warningDeletion.getDialogPane().getScene().getWindow()).getIcons().add(new Image("icons/foxIcon.png"));
        warningDeletion.show();
    }
}
