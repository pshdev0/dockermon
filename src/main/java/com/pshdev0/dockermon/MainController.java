package com.pshdev0.dockermon;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.AnchorPane;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Comparator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainController {

    @FXML
    Button buttonClear;
    @FXML
    Button buttonRemoveOld;
    @FXML
    TableView<ContainerModel> tableContainers;
    @FXML
    TableColumn<ContainerModel, ContainerModel> tableCol;
    @FXML
    AnchorPane logAnchor;

    ScheduledExecutorService executor;
    ObservableList<ContainerModel> containerList;

    @FXML
    public void initialize() {
        executor = Executors.newScheduledThreadPool(1);
        containerList = FXCollections.observableArrayList();
        tableContainers.setItems(containerList);

        tableCol.setCellValueFactory(row -> new SimpleObjectProperty<>(row.getValue()));
        tableCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(ContainerModel item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getCellName());

                    if(item.active) {
                        setStyle("-fx-text-fill: green;");
                    }
                    else {
                        setStyle("-fx-text-fill: red;");
                    }
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
                    tableContainers.refresh();
                }
            });
        }, 0, 1000, TimeUnit.MILLISECONDS);

        buttonRemoveOld.setOnAction(event -> {
            Platform.runLater(() -> tableContainers.setItems(containerList.filtered(c -> c.active)));
            tableContainers.refresh();
        });

        buttonClear.setOnAction(event -> Platform.runLater(() -> {
            var selectedItem = tableContainers.getSelectionModel().getSelectedItem();

            if(selectedItem != null) {
                selectedItem.textArea.clear();
            }
        }));

        tableContainers.setOnMouseClicked(event -> {
            var selectedContainer = tableContainers.getSelectionModel().getSelectedItem();

            if(selectedContainer != null) {
                logAnchor.getChildren().clear();
                logAnchor.getChildren().add(selectedContainer.textArea);
                AnchorPane.setLeftAnchor(selectedContainer.textArea, 0.0);
                AnchorPane.setRightAnchor(selectedContainer.textArea, 0.0);
                AnchorPane.setTopAnchor(selectedContainer.textArea, 0.0);
                AnchorPane.setBottomAnchor(selectedContainer.textArea, 0.0);
            }
        });

        tableCol.setSortable(false);
        tableCol.setMinWidth(350);
    }

    private void createLogProcessForContainer(ContainerModel container) {
        System.out.println("creating process for: " + container.getName() + " " + container.getId());

        var pb = new ProcessBuilder("docker", "logs", "-f", container.getId());
        try {
            // set up the pipe
            container.in = new PipedInputStream();
            container.out = new PipedOutputStream();
            container.in.connect(container.out);
            container.textArea.clear();
            container.logProcess = pb.start(); // start the docker logs process

            // start the docker log pipe
            container.thread = new Thread(() -> {
                StringBuilder logBuffer = new StringBuilder();
                try (BufferedReader processReader = new BufferedReader(
                        new InputStreamReader(container.logProcess.getInputStream()))) {
                    String line;
                    while ((line = processReader.readLine()) != null) {
                        logBuffer.append(line).append("\n");

                        if (logBuffer.length() > 1024) {
                            String logsToAppend = logBuffer.toString();
                            Platform.runLater(() -> container.textArea.appendText(logsToAppend));
                            logBuffer.setLength(0); // Clear the buffer after flushing
                        }
                    }

                    if (!logBuffer.isEmpty()) {
                        String remainingLogs = logBuffer.toString();
                        Platform.runLater(() -> container.textArea.appendText(remainingLogs));
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
