package com.pshdev0.dockermon;

import javafx.scene.control.TextArea;
import javafx.scene.text.Font;

import java.io.PipedInputStream;
import java.io.PipedOutputStream;

public class ContainerModel {
    String id;
    String name;
    boolean active = true;
    boolean reloading = false;
    Process logProcess;
    TextArea textArea;
    PipedOutputStream out;
    PipedInputStream in;
    Thread thread;
    long lastUpdateTimestamp = 0;

    ContainerModel(String id, String name) {
        this.id = id;
        this.name = name;
        this.textArea = new TextArea();
        this.textArea.setFont(Font.font("Courier New"));
        this.textArea.setStyle("-fx-control-inner-background: black; -fx-text-fill: #FFFFFF;");
        this.textArea.setEditable(false);
        this.textArea.setWrapText(true);
    }

    public String getCellName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
