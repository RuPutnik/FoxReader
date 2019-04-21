package ru.putnik.foxreader;

/**
 * Создано 10.04.2019 в 18:16
 */
public class TypeTreeElement {
    private String name;
    private Type type;
    private String nameDB;//the catalog in jdbc
    private String schema;
    private String nameTable;

    public TypeTreeElement(Type type, String name,String nameDB,String nameTable,String schema){
        this.type=type;
        this.name=name;
        this.nameDB=nameDB;
        this.nameTable=nameTable;
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

    public String getNameTable() {
        return nameTable;
    }

    public enum Type{
        DATABASE,SERVER,  TABLES,TABLE,  KEYS,PRIMARY_KEY,FOREIGN_KEY,  COLUMNS,COLUMN,  PROCEDURES,PROCEDURE,
        VIEWS,VIEW, INDEXES,INDEX
    }
}
