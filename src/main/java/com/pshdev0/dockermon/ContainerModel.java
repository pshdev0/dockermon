package com.pshdev0.dockermon;

import javafx.application.Platform;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.Stop;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.InlineCssTextArea;
import org.fxmisc.richtext.model.StyleSpan;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.pshdev0.dockermon.utils.Helper;
import org.fxmisc.richtext.model.TwoDimensional;

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
    Paint customPaint = new LinearGradient(
            0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.GREEN),
            new Stop(1, Color.BLACK)
    );
    public static final Pattern ANSI_PATTERN = Pattern.compile("\u001B\\[(\\d+)?m");

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

    void applyStyles() {
        StyleSpansBuilder<String> spansBuilder = new StyleSpansBuilder<>();
        for (StyleSpan<String> span : originalStyles) {
            spansBuilder.add(span);
        }
        this.richTextArea.setStyleSpans(0, spansBuilder.create());
    }

    void createLogProcessForContainer() {
        System.out.println("creating process for: " + getName() + " " + getId());

        var pb = new ProcessBuilder("docker", "logs", "-f", getId());
        try {
            richTextArea.clear();
            logProcess = pb.start(); // start the docker logs process

            // start the docker log pipe
            thread = new Thread(() -> {
                StringBuilder logBuffer = new StringBuilder();
                try (BufferedReader processReader = new BufferedReader(
                        new InputStreamReader(logProcess.getInputStream()))) {
                    String line;
                    while ((line = processReader.readLine()) != null) {
                        logBuffer.append(line).append("\n");

                        if (logBuffer.length() > 1024*16) {
                            updateLogs(logBuffer);
                            lastUpdateTimestamp = System.currentTimeMillis();
                        }
                        else if(!processReader.ready()) {
                            if (!logBuffer.isEmpty()) {
                                updateLogs(logBuffer);
                            }
                            lastUpdateTimestamp = System.currentTimeMillis();
                        }
                    }
                } catch (IOException e) {
                    System.out.println("! Error writing process logs to output stream");
                    e.printStackTrace();
                }
            });

            thread.start();
        } catch (IOException e) {
            System.out.println("! Error creating log process");
            e.printStackTrace();
        }
    }

    void updateLogs(StringBuilder logBuffer) {
        if(System.currentTimeMillis() - lastUpdateTimestamp > 5000) {
            logBuffer.append("\uD83C\uDF0A\n");
        }
        String remainingLogs = logBuffer.toString();
        Platform.runLater(() -> {
            int textLength = richTextArea.getLength();
            parseAnsiCodesAndApplyStyles(remainingLogs);
            var newStyles = richTextArea.getStyleSpans(textLength, richTextArea.getLength());
            newStyles.forEach(originalStyles::add);
        });
        logBuffer.setLength(0);
    }

    public void parseAnsiCodesAndApplyStyles(String line) {
        Matcher matcher = ANSI_PATTERN.matcher(line);
        int lastIndex = 0;
        String currentStyle = "-fx-fill: white;";

        var scrollY = richTextArea.getEstimatedScrollY();
        var maxScrollY = richTextArea.getTotalHeightEstimate() - richTextArea.getHeight();

        while (matcher.find()) {
            String ansiCode = matcher.group(1);

            if (matcher.start() > lastIndex) {
                richTextArea.appendText(line.substring(lastIndex, matcher.start()));
                if (!currentStyle.isEmpty()) {
                    richTextArea.setStyle(richTextArea.getLength() - (matcher.start() - lastIndex), richTextArea.getLength(), currentStyle);
                }
            }

            currentStyle = Helper.getStyleFromAnsiCode(ansiCode);
            lastIndex = matcher.end();
        }

        if (lastIndex < line.length()) {
            richTextArea.appendText(line.substring(lastIndex));
            if (!currentStyle.isEmpty()) {
                richTextArea.setStyle(richTextArea.getLength() - (line.length() - lastIndex), richTextArea.getLength(), currentStyle);
            }
        }

        // if the scroll bar is at the bottom, scroll to the end to view the new material
        if(scrollY >= maxScrollY - 10) {
            richTextArea.requestFollowCaret();
        }
    }

    private void updateCaret() {
        if(searchCarets.isEmpty()) {
            return;
        }

        int caret = searchCarets.get(currentSearchCaret);
        int currentParagraph = richTextArea.offsetToPosition(caret, TwoDimensional.Bias.Forward).getMajor();

        richTextArea.displaceCaret(caret);
        richTextArea.showParagraphAtTop(currentParagraph);
    }

    void moveCaretDown() {
        currentSearchCaret++;
        if(currentSearchCaret >= searchCarets.size()) {
            currentSearchCaret = 0;
        }
        updateCaret();
    }

    void moveCaretUp() {
        currentSearchCaret--;
        if(currentSearchCaret < 0) {
            currentSearchCaret = searchCarets.size() - 1;
        }
        updateCaret();
    }

    void search(String searchText) {
        if(searchText.trim().isEmpty()) {
            return;
        }

        System.out.println("searching for: " + searchText);

        applyStyles();

        richTextArea.setLineHighlighterFill(customPaint);
        richTextArea.setLineHighlighterOn(true);

        String content = richTextArea.getText();
        int lastIndex = 0;

        searchCarets.clear();

        // efficient case-insensitive search
        while (lastIndex <= content.length() - searchText.length()) {
            if (content.regionMatches(true, lastIndex, searchText, 0, searchText.length())) {
                int end = lastIndex + searchText.length();
                richTextArea.setStyle(lastIndex, end, "-fx-stroke: white; -rtfx-background-color: red;");
                lastIndex = end;

                searchCarets.add(lastIndex);
            } else {
                lastIndex++;
            }
        }

        if(!searchCarets.isEmpty()) {
            currentSearchCaret = searchCarets.size() - 1;
            updateCaret();
        }
    }
}
