package com.pshdev0.dockermon;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.IOException;
import java.util.Objects;

public class MainApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainApplication.class.getResource("main.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1200, 800);
        stage.setTitle("\uD83D\uDC33 Dockermon \uD83D\uDDA5\uFE0F");
        stage.setScene(scene);

        Image dockIcon = ImageIO.read(Objects.requireNonNull(getClass().getResource("tray.png")));
        Taskbar taskbar = Taskbar.getTaskbar();
        taskbar.setIconImage(dockIcon);

        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
