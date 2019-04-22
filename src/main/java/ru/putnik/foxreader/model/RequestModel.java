package ru.putnik.foxreader.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Создано 21.04.2019 в 20:25
 */
public class RequestModel {
    public void printPlaneExecute(String request){
        /*Connection conn = getConnection();
        String showplanQuery = "SET SHOWPLAN_XML ON";
        Statement st = conn.createStatement();
        st.execute(showplanQuery);

        String actualQuery = "SELECT ATMPROFILES.TERMID FROM ATMPROFILES (NOLOCK) ";
        PreparedStatement ps=conn.prepareStatement(actualQuery);
        ps.execute();
        ResultSet rs = ps.getResultSet();
        while(rs.next())
        {
            Object object = rs.getObject(1);
            // should log the query plan
            System.out.println(object);
        }*/
    }
}
