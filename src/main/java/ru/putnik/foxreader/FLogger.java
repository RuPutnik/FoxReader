package ru.putnik.foxreader;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * Создано 18.04.2019 в 22:34
 */
public class FLogger{
    private static Logger log = LogManager.getLogger(FLogger.class);
    private FLogger(){}

    public static void error(String text){
        log.error("[FoxReader] "+text);
    }
    public static void error(String text,Throwable throwable){
        log.error("[FoxReader] "+text,throwable);
    }
    public static void info(String text){
        log.info("[FoxReader] "+text);
    }
    public static void warning(String text){
        log.warn("[FoxReader] "+text);
    }
    public static void request(String text){
        log.debug("[FoxReader] SQL Request: "+text);
    }
}
