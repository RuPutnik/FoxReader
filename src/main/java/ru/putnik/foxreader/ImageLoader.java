package ru.putnik.foxreader;

import javafx.scene.image.Image;
/**
 * Создано 23.04.2019 в 16:35
 */
public class ImageLoader {
    private static Image[] iconViews=new Image[14];
    public ImageLoader(){
        iconViews[0]=new Image("icons/foxIcon.png");
        iconViews[1]=new Image("icons/base.png");
        iconViews[2]=new Image("icons/column.png");
        iconViews[3]=new Image("icons/columns.png");
        iconViews[4]=new Image("icons/foreign_key.png");
        iconViews[5]=new Image("icons/index.png");
        iconViews[6]=new Image("icons/indexes.png");
        iconViews[7]=new Image("icons/keys.png");
        iconViews[8]=new Image("icons/param.png");
        iconViews[9]=new Image("icons/primary_key.png");
        iconViews[10]=new Image("icons/procedure.png");
        iconViews[11]=new Image("icons/server.png");
        iconViews[12]=new Image("icons/table.png");
        iconViews[13]=new Image("icons/view.png");

    }
    public static Image getIconImage(){
        return iconViews[0];
    }
    public static Image getBase(){
        return iconViews[1];
    }
    public static Image getColumn(){
        return iconViews[2];
    }
    public static Image getColumns(){
        return iconViews[3];
    }
    public static Image getForeignKey(){
        return iconViews[4];
    }
    public static Image getIndex(){
        return iconViews[5];
    }
    public static Image getIndexes(){
        return iconViews[6];
    }
    public static Image getKeys(){
        return iconViews[7];
    }
    public static Image getParam(){
        return iconViews[8];
    }
    public static Image getPrimaryKey(){
        return iconViews[9];
    }
    public static Image getProcedure(){
        return iconViews[10];
    }
    public static Image getServer(){
        return iconViews[11];
    }
    public static Image getTable(){
        return iconViews[12];
    }
    public static Image getView(){
        return iconViews[13];
    }
}