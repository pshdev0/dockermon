package com.pshdev0.dockermon;

import com.pshdev0.dockermon.utils.DockerUtils;
import com.pshdev0.dockermon.utils.ProxyUtils;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.paint.Color;
import org.fxmisc.richtext.InlineCssTextArea;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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
    Button buttonSplitView;
    @FXML
    TableView<ContainerModel> tableContainers;
    @FXML
    TableColumn<ContainerModel, ContainerModel> tableCol;
    @FXML
    AnchorPane logAnchor1;
    @FXML
    AnchorPane logAnchor2;
    @FXML
    SplitPane splitPane;
    @FXML
    Circle vpnCircle;
    @FXML
    Label vpnCircleLabel;

    ContainerModel firstContainer = null;
    ContainerModel secondContainer = null;

    ScheduledExecutorService executor;
    ObservableList<ContainerModel> containerList;

    private static final Pattern ANSI_PATTERN = Pattern.compile("\u001B\\[(\\d+)?m");

    @FXML
    public void initialize() {
        executor = Executors.newScheduledThreadPool(1);
        containerList = FXCollections.observableArrayList();
        tableContainers.setItems(containerList);

        splitPane.getItems().removeLast();

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

                    var labelName = "";
                    if(secondContainer != null && item == secondContainer) {
                        labelName += "\uD83D\uDD0D ";
                    }
                    labelName += item.getName();

                    Label label = new Label(labelName);
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
                } else if (item == firstContainer) {
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
                            System.out.println("Deleting container logs process for: " + container.getName() + " " + container.getId());
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
                            .thenComparing(ContainerModel::getName));
                }

                tableContainers.refresh();
            });
        }, 0, 1000, TimeUnit.MILLISECONDS);

//        executor.scheduleAtFixedRate(() -> {
//            AWSUtils.scanProfiles();
//        }, 0, 30, TimeUnit.HOURS);

        executor.scheduleAtFixedRate(() -> {
            updateVpnStatus();
        }, 0, 60, TimeUnit.MINUTES);

        buttonRemoveOld.setOnAction(event -> {
            Platform.runLater(() -> tableContainers.setItems(containerList.filtered(c -> c.active)));
            tableContainers.refresh();
        });

        buttonClear.setOnAction(event -> Platform.runLater(() -> {
            var selectedItem = tableContainers.getSelectionModel().getSelectedItem();

            if(selectedItem != null) {
                selectedItem.richTextArea.clear();
            }
        }));

        buttonReload.setOnAction(event -> {
            var selectedContainer = tableContainers.getSelectionModel().getSelectedItem();

            if (selectedContainer != null) {
                System.out.println("Reloading: " + selectedContainer.getName());
                selectedContainer.reloading = true;
                tableContainers.refresh();
                bashSourceAndRun("docker_chs reload " + selectedContainer.getName());
            }
        });

        buttonSplitView.setOnAction(event -> {
            var list = splitPane.getItems();
            if(list.size() == 1) {
                list.add(logAnchor2);
            }
            else if(list.size() == 2) {
                list.removeLast();
                logAnchor2.getChildren().clear();
            }
            secondContainer = null;
            tableContainers.refresh();
        });


        tableContainers.setOnMouseClicked(event -> {
            var selectedContainer = tableContainers.getSelectionModel().getSelectedItem();

            if(selectedContainer != null) {
                if(!event.isControlDown()) {
                    logAnchor1.getChildren().clear();
                    logAnchor1.getChildren().add(selectedContainer.virtualRichTextArea);
                    AnchorPane.setLeftAnchor(selectedContainer.virtualRichTextArea, 0.0);
                    AnchorPane.setRightAnchor(selectedContainer.virtualRichTextArea, 0.0);
                    AnchorPane.setTopAnchor(selectedContainer.virtualRichTextArea, 0.0);
                    AnchorPane.setBottomAnchor(selectedContainer.virtualRichTextArea, 0.0);
                    firstContainer = selectedContainer;
                    tableContainers.refresh();
                }
                else if (selectedContainer != firstContainer && splitPane.getItems().size() > 1) {
                    logAnchor2.getChildren().clear();
                    logAnchor2.getChildren().add(selectedContainer.virtualRichTextArea);
                    AnchorPane.setLeftAnchor(selectedContainer.virtualRichTextArea, 0.0);
                    AnchorPane.setRightAnchor(selectedContainer.virtualRichTextArea, 0.0);
                    AnchorPane.setTopAnchor(selectedContainer.virtualRichTextArea, 0.0);
                    AnchorPane.setBottomAnchor(selectedContainer.virtualRichTextArea, 0.0);
                    secondContainer = selectedContainer;
                    tableContainers.refresh();
                }
            }
        });

        tableCol.setSortable(false);
        tableCol.prefWidthProperty().bind(tableContainers.widthProperty().subtract(18));

        Tooltip.install(vpnCircle, new Tooltip("The VPN status will update every hour, or click the indicator circle to update instantly"));
        vpnCircle.setOnMouseClicked(event -> updateVpnStatus());
    }

    private void updateVpnStatus() {
        Platform.runLater(() -> {
            System.out.print("Updating VPN status... ");
            var status = ProxyUtils.isProxyActive();
            if(status) {
                vpnCircle.setFill(Color.LIGHTGREEN);
                vpnCircleLabel.setText("VPN active\n@ " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
                System.out.println("active");
            }
            else {
                vpnCircle.setFill(Color.INDIANRED);
                vpnCircleLabel.setText("VPN inactive\n@ " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
                System.out.println("inactive");
            }
        });
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
        String currentStyle = "-fx-fill: white;";

        var scrollY = area.getEstimatedScrollY();
        var maxScrollY = area.getTotalHeightEstimate() - area.getHeight();

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

        // if the scroll bar is at the bottom, scroll to the end to view the new material
        if(scrollY >= maxScrollY - 10) {
            area.requestFollowCaret();
        }
    }

    private String getStyleFromAnsiCode(String ansiCode) {
        return switch (ansiCode) {
            case "30" -> "-fx-fill: black; -fx-font-weight: bold;";         // black
            case "31" -> "-fx-fill: red; -fx-font-weight: bold;";           // red
            case "32" -> "-fx-fill: lightgreen; -fx-font-weight: bold;";    // green
            case "33" -> "-fx-fill: yellow; -fx-font-weight: bold;";        // yellow
            case "34" -> "-fx-fill: lightblue; -fx-font-weight: bold;";          // blue
            case "35" -> "-fx-fill: magenta; -fx-font-weight: bold;";       // magenta
            case "36" -> "-fx-fill: cyan; -fx-font-weight: bold;";          // cyan
            case "90" -> "-fx-fill: gray; -fx-font-weight: bold;";          // bright black (gray)
            case "91" -> "-fx-fill: lightcoral; -fx-font-weight: bold;";    // bright red
            case "92" -> "-fx-fill: lightgreen; -fx-font-weight: bold;";    // bright green
            case "93" -> "-fx-fill: lightyellow; -fx-font-weight: bold;";   // bright yellow
            case "94" -> "-fx-fill: lightskyblue; -fx-font-weight: bold;";  // bright blue
            case "95" -> "-fx-fill: lightpink; -fx-font-weight: bold;";     // bright magenta
            case "96" -> "-fx-fill: lightcyan; -fx-font-weight: bold;";     // bright cyan
            case "97" -> "-fx-fill: white;";         // bright white (default)
            default -> "-fx-fill: white;";
        };
    }

    private void createLogProcessForContainer(ContainerModel container) {
        System.out.println("creating process for: " + container.getName() + " " + container.getId());

        var pb = new ProcessBuilder("docker", "logs", "-f", container.getId());
        try {
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
                            Platform.runLater(() -> {
                                parseAnsiCodesAndApplyStyles(logsToAppend, container.richTextArea);
                            });
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
