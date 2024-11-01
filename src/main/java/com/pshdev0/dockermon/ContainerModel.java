package com.pshdev0.dockermon;

import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.InlineCssTextArea;
import org.fxmisc.richtext.model.StyleSpan;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.ArrayList;
import java.util.List;

public class ContainerModel {
    String id;
    String name;
    boolean active = true;
    boolean reloading = false;
    Process logProcess;
    InlineCssTextArea richTextArea;
    VirtualizedScrollPane<InlineCssTextArea> virtualRichTextArea;
    Thread thread;
    long lastUpdateTimestamp = 0;
    List<StyleSpan<String>> originalStyles;
    List<Integer> searchCarets;
    int currentSearchCaret;

    public ContainerModel(String id, String name) {
        this.id = id;
        this.name = name;

        this.richTextArea = new InlineCssTextArea();
        this.virtualRichTextArea = new VirtualizedScrollPane<>(richTextArea);
        this.richTextArea.setStyle("""
                -fx-background-color: black;
                -fx-font-family: 'Courier New';
                -fx-font-size: 14px;"""
        );
        this.richTextArea.setEditable(false);
        this.richTextArea.setWrapText(true);

        this.searchCarets = new ArrayList<>();
        this.originalStyles = new ArrayList<>();
        this.richTextArea.getStyleSpans(0, this.richTextArea.getLength()).forEach(this.originalStyles::add);
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

    public void applyStyles() {
        StyleSpansBuilder<String> spansBuilder = new StyleSpansBuilder<>();
        for (StyleSpan<String> span : originalStyles) {
            spansBuilder.add(span);
        }
        this.richTextArea.setStyleSpans(0, spansBuilder.create());
    }
}
