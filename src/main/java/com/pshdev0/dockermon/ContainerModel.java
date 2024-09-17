package com.pshdev0.dockermon;

import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.InlineCssTextArea;

import java.io.PipedInputStream;
import java.io.PipedOutputStream;

public class ContainerModel {
    String id;
    String name;
    boolean active = true;
    boolean reloading = false;
    Process logProcess;
    InlineCssTextArea richTextArea;
    VirtualizedScrollPane<InlineCssTextArea> virtualRichTextArea;
    PipedOutputStream out;
    PipedInputStream in;
    Thread thread;
    long lastUpdateTimestamp = 0;

    ContainerModel(String id, String name) {
        this.id = id;
        this.name = name;
        this.richTextArea = new InlineCssTextArea();
        this.virtualRichTextArea = new VirtualizedScrollPane<>(richTextArea);
        this.richTextArea.setStyle(
                "-fx-background-color: black;" +
                "-fx-font-family: 'Courier New';" +
                "-fx-font-size: 14px;"
        );
        this.richTextArea.setEditable(false);
        this.richTextArea.setWrapText(true);
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
