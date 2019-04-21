package ru.putnik.foxreader.model;

import javafx.scene.control.TreeItem;
import ru.putnik.foxreader.TypeTreeElement;
import ru.putnik.foxreader.controller.MainController;

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
        //model.getConnection().prepareStatement("")
        System.out.println("Удалить таблицу: "+item.getValue().getName());
    }
    public void addTable(TreeItem<TypeTreeElement> item){
        System.out.println("Создать таблицу: "+item.getParent().getValue().getName());
    }
    public void removeDB(TreeItem<TypeTreeElement> item){
        System.out.println("Удалить базу данных: "+item.getValue().getName());
    }
    public void addDB(TreeItem<TypeTreeElement> item){
        System.out.println("Создать базу данных: "+item.getValue().getName());
    }
    public void removeProcedure(TreeItem<TypeTreeElement> item){
        System.out.println("Удалить процедуру: "+item.getValue().getName());
    }
    public void addProcedure(TreeItem<TypeTreeElement> item){
        System.out.println("Создать процедуру: "+item.getParent().getValue().getName());
    }
    public void executeProcedure(TreeItem<TypeTreeElement> item){
        System.out.println("Запустить процедуру: "+item.getValue().getName());
    }
}
