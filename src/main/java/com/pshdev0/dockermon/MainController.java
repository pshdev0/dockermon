package com.pshdev0.dockermon;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import org.fxmisc.richtext.InlineCssTextArea;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Comparator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainController {

    @FXML
    Button buttonClear;
    @FXML
    Button buttonRemoveOld;
    @FXML
    Button buttonReload;
    @FXML
    TableView<ContainerModel> tableContainers;
    @FXML
    TableColumn<ContainerModel, ContainerModel> tableCol;
    @FXML
    AnchorPane logAnchor;

    ScheduledExecutorService executor;
    ObservableList<ContainerModel> containerList;

    private static final Pattern ANSI_PATTERN = Pattern.compile("\u001B\\[(\\d+)?m");

    @FXML
    public void initialize() {
        executor = Executors.newScheduledThreadPool(1);
        containerList = FXCollections.observableArrayList();
        tableContainers.setItems(containerList);

        tableContainers.skinProperty().addListener((a, b, c) -> {
            Pane header = (Pane)tableContainers.lookup("TableHeaderRow");
            header.setVisible(false);
            header.setMinHeight(0);
            header.setMaxHeight(0);
            header.setPrefHeight(0);
        });

        tableCol.setCellValueFactory(row -> new SimpleObjectProperty<>(row.getValue()));
        tableCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(ContainerModel item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                }
                else {
                    HBox hbox = new HBox();
                    hbox.setMaxHeight(USE_COMPUTED_SIZE);
                    Label containerIdLabel = new Label(item.getId());
                    containerIdLabel.setFont(Font.font(10));

                    if(item.reloading && item.active) {
                        var indicator = new ProgressIndicator();
                        indicator.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
                        indicator.setMinWidth(30);
                        indicator.setMaxWidth(30);
                        indicator.setMaxHeight(20);
                        hbox.getChildren().add(indicator);
                        hbox.getChildren().add(new Label(" "));
                    }
                    else if(!item.active) {
                        Label prefix = new Label("\uD83D\uDED1");
                        prefix.setOpacity(0.75);
                        prefix.setFont(Font.font(18));
                        prefix.setMinWidth(30);
                        hbox.getChildren().add(prefix);
                    }
                    else {
                        Label prefix = new Label("✅");
                        if(System.currentTimeMillis() - item.lastUpdateTimestamp > 3000) {
                            prefix.setOpacity(0.5);
                        }
                        else {
                            prefix.setOpacity(1);
                        }
                        prefix.setFont(Font.font(18));
                        prefix.setMinWidth(30);
                        hbox.getChildren().add(prefix);
                    }

                    Label label = new Label(item.getCellName());
                    if(!item.active) {
                        label.setStyle("-fx-text-fill: red;"); // label.setStyle("-fx-text-fill: green;");
                    }
                    else if(item.reloading) {
                        label.setStyle("-fx-text-fill: gray;");
                    }
                    label.setFont(Font.font(18));

                    VBox vbox = new VBox();
                    vbox.getChildren().addAll(label, containerIdLabel);
                    hbox.getChildren().add(vbox);
                    setGraphic(hbox);
                }
            }
        });

        tableContainers.setRowFactory(row -> new TableRow<>() {

            @Override
            protected void updateItem(ContainerModel item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setStyle("");
                } else if (item == tableContainers.getSelectionModel().getSelectedItem()) {
                    setStyle("-fx-background-color: lightblue;");
                } else {
                    setStyle("");
                }
            }
        });

        executor.scheduleAtFixedRate(() -> {
            var list = DockerUtils.get().getDockerProcesses();

            Platform.runLater(() -> {
                var updated = false;

                // disable old containers & set up for garbage collection
                for (var container : containerList) {

                    var noneMatch = list.stream().noneMatch(x -> x.getId().equals(container.getId()));
                    container.setActive(!noneMatch);

                    if(noneMatch && container.logProcess != null) {
                        if (container.logProcess.isAlive()) {
                            System.out.println("Deleting container logs process for: " + container.getCellName() + " " + container.getId());
                            container.logProcess.destroyForcibly();
                        }
                        container.logProcess = null;
                    }

                    if(noneMatch) {
                        updated = true;
                    }
                }

                // add any new containers
                for (var container : list) {
                    boolean onList = containerList.stream().anyMatch(x -> x.getId().equals(container.getId()));

                    if (!onList) {
                        containerList.add(container);
                        createLogProcessForContainer(container);
                        updated = true;
                    }
                }

                // update ui as necessary
                if (updated) {
                    containerList.sort(Comparator
                            .comparing(ContainerModel::isActive, Comparator.reverseOrder())
                            .thenComparing(ContainerModel::getCellName));
                }

                tableContainers.refresh();
            });
        }, 0, 1000, TimeUnit.MILLISECONDS);

        buttonRemoveOld.setOnAction(event -> {
            Platform.runLater(() -> tableContainers.setItems(containerList.filtered(c -> c.active)));
            tableContainers.refresh();
        });

        buttonClear.setOnAction(event -> Platform.runLater(() -> {
            var selectedItem = tableContainers.getSelectionModel().getSelectedItem();

            if(selectedItem != null) {
//                selectedItem.textArea.clear();
                selectedItem.richTextArea.clear();
            }
        }));

        buttonReload.setOnAction(event -> {
            var selectedContainer = tableContainers.getSelectionModel().getSelectedItem();

            if (selectedContainer != null) {
                System.out.println("Reloading: " + selectedContainer.getCellName());
                selectedContainer.reloading = true;
                tableContainers.refresh();
                bashSourceAndRun("docker_chs reload " + selectedContainer.getCellName());
            }
        });

        tableContainers.setOnMouseClicked(event -> {
            var selectedContainer = tableContainers.getSelectionModel().getSelectedItem();

            if(selectedContainer != null) {
                logAnchor.getChildren().clear();
                logAnchor.getChildren().add(selectedContainer.virtualRichTextArea);
                AnchorPane.setLeftAnchor(selectedContainer.virtualRichTextArea, 0.0);
                AnchorPane.setRightAnchor(selectedContainer.virtualRichTextArea, 0.0);
                AnchorPane.setTopAnchor(selectedContainer.virtualRichTextArea, 0.0);
                AnchorPane.setBottomAnchor(selectedContainer.virtualRichTextArea, 0.0);
            }
        });

        tableCol.setSortable(false);
        tableCol.prefWidthProperty().bind(tableContainers.widthProperty().subtract(18));
    }

    private void bashSourceAndRun(String command) {
        new Thread(() -> {
            var pb = new ProcessBuilder("bash", "-c", "source ~/.bash_profile && " + command);
            try {
                Process process = pb.start();
                int exitCode = process.waitFor();  // wait for the process to finish

                Platform.runLater(() -> System.out.println("Process finished with exit code: " + exitCode));
            } catch (IOException | InterruptedException e) {
                Platform.runLater(() -> System.out.println("Error reloading: " + e.getMessage()));
            }
        }).start();
    }

    private void parseAnsiCodesAndApplyStyles(String line, InlineCssTextArea area) {
        Matcher matcher = ANSI_PATTERN.matcher(line);
        int lastIndex = 0;
        String currentStyle = "";

        while (matcher.find()) {
            String ansiCode = matcher.group(1);

            if (matcher.start() > lastIndex) {
                area.appendText(line.substring(lastIndex, matcher.start()));
                if (!currentStyle.isEmpty()) {
                    area.setStyle(area.getLength() - (matcher.start() - lastIndex), area.getLength(), currentStyle);
                }
            }

            currentStyle = getStyleFromAnsiCode(ansiCode);
            lastIndex = matcher.end();
        }

        if (lastIndex < line.length()) {
            area.appendText(line.substring(lastIndex));
            if (!currentStyle.isEmpty()) {
                area.setStyle(area.getLength() - (line.length() - lastIndex), area.getLength(), currentStyle);
            }
        }
    }

    private String getStyleFromAnsiCode(String ansiCode) {
        return switch (ansiCode) {
            case "30" -> "-fx-fill: black;";         // black
            case "31" -> "-fx-fill: red;";           // red
            case "32" -> "-fx-fill: lightgreen;";    // green
            case "33" -> "-fx-fill: yellow;";        // yellow
            case "34" -> "-fx-fill: blue;";          // blue
            case "35" -> "-fx-fill: magenta;";       // magenta
            case "36" -> "-fx-fill: cyan;";          // cyan
            case "90" -> "-fx-fill: gray;";          // bright black (gray)
            case "91" -> "-fx-fill: lightcoral;";    // bright red
            case "92" -> "-fx-fill: lightgreen;";    // bright green
            case "93" -> "-fx-fill: lightyellow;";   // bright yellow
            case "94" -> "-fx-fill: lightskyblue;";  // bright blue
            case "95" -> "-fx-fill: lightpink;";     // bright magenta
            case "96" -> "-fx-fill: lightcyan;";     // bright cyan
            case "97" -> "-fx-fill: white;";         // bright white (default)
            default -> "-fx-fill: white;";
        };
    }

    private void createLogProcessForContainer(ContainerModel container) {
        System.out.println("creating process for: " + container.getCellName() + " " + container.getId());

        var pb = new ProcessBuilder("docker", "logs", "-f", container.getId());
        try {
            // set up the pipe
            container.in = new PipedInputStream();
            container.out = new PipedOutputStream();
            container.in.connect(container.out);
            container.richTextArea.clear();
            container.logProcess = pb.start(); // start the docker logs process

            // start the docker log pipe
            container.thread = new Thread(() -> {
                StringBuilder logBuffer = new StringBuilder();
                try (BufferedReader processReader = new BufferedReader(
                        new InputStreamReader(container.logProcess.getInputStream()))) {
                    String line;
                    while ((line = processReader.readLine()) != null) {
                        logBuffer.append(line).append("\n");

                        if (logBuffer.length() > 1024*16) {
                            if(System.currentTimeMillis() - container.lastUpdateTimestamp > 5000) {
                                logBuffer.append("\uD83C\uDF0A\n");
                            }
                            String logsToAppend = logBuffer.toString();
                            Platform.runLater(() -> parseAnsiCodesAndApplyStyles(logsToAppend, container.richTextArea));
                            logBuffer.setLength(0);
                            container.lastUpdateTimestamp = System.currentTimeMillis();
                        }
                        else if(!processReader.ready()) {
                            if (!logBuffer.isEmpty()) {
                                if(System.currentTimeMillis() - container.lastUpdateTimestamp > 5000) {
                                    logBuffer.append("\uD83C\uDF0A\n");
                                }
                                String remainingLogs = logBuffer.toString();
                                Platform.runLater(() -> parseAnsiCodesAndApplyStyles(remainingLogs, container.richTextArea));
                                logBuffer.setLength(0);
                            }
                            container.lastUpdateTimestamp = System.currentTimeMillis();
                        }
                    }
                } catch (IOException e) {
                    System.out.println("! Error writing process logs to output stream");
                    e.printStackTrace();
                }
            });

            container.thread.start();
        } catch (IOException e) {
            System.out.println("! Error creating log process");
            e.printStackTrace();
        }
    }
}
