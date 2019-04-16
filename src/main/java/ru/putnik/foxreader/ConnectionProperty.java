package ru.putnik.foxreader;

import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * Создано 10.04.2019 в 0:16
 */
public class ConnectionProperty {
    private String typeServer;
    private String patternConnection;
    private String login;
    private String password;
    private String nameDB;
    private int port;
    private String address;

    public ConnectionProperty(String typeServer,String address,String login,String password,String nameDB,int port){
        this.typeServer=typeServer;
        this.address=address;
        this.login=login;
        this.password=password;
        this.nameDB=nameDB;
        this.port=port;

        if(typeServer.equals("Microsoft MS SQL")){
            patternConnection="jdbc:sqlserver:";
        }else if(typeServer.equals("Oracle MySQL")){
            patternConnection="jdbc:mysql:";
        }else{
            Alert alert=new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Ошибка подключения");
            alert.setHeaderText("Не установлен протокол подключения");
            alert.setContentText("Для данного типа сервера не удалось найти протокол подключения. Подключение недоступно.");
            ((Stage)alert.getDialogPane().getScene().getWindow()).getIcons().add(new Image("icons/foxIcon.png"));
            alert.show();
        }
    }
    public String createConnectionUrl(){
        String url;
        url=patternConnection+"//"+address+":"+port+";";

        if(!nameDB.equals("")){
            url=url+"databaseName="+nameDB;
        }

        return url;
    }

    public String getTypeServer() {
        return typeServer;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }
}
