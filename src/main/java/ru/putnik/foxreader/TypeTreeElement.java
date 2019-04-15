package ru.putnik.foxreader;

/**
 * Создано 10.04.2019 в 18:16
 */
public class TypeTreeElement {
    private String name;
    private Type type;
    private String nameDB;//the catalog
    private String schema;//if null then this element is database or catalog

    public TypeTreeElement(Type type, String name,String nameDB,String schema){
        this.type=type;
        this.name=name;
        this.nameDB=nameDB;
        this.schema=schema;
    }

    @Override
    public String toString() {
        return name;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public String getNameDB() {
        return nameDB;
    }

    public String getSchema() {
        return schema;
    }

    public enum Type{
        TABLE,CATALOG,DATABASE,SERVER,PRIMARY_KEY,FOREIGN_KEY,COLUMN,PROCEDURE,VIEW,KEYS,COLUMNS,PROCEDURES,VIEWS,DATA_TABLE
    }
}
