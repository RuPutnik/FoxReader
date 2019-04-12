package ru.putnik.foxreader;

import javafx.application.Platform;
import javafx.scene.control.Menu;
import javafx.stage.Stage;

import javax.swing.*;
import java.util.GregorianCalendar;

/**
 * Создано 15.07.15
 */
public class TimeRunnable implements Runnable {
    private Menu timeMenu;

    public TimeRunnable(Menu timeMenu) {
        this.timeMenu = timeMenu;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(1000);
                GregorianCalendar calendar = new GregorianCalendar();
                Platform.runLater(() -> timeMenu.setText(calendar.getTime().toString()));
            } catch (InterruptedException ignore) {}
        }
    }
}
