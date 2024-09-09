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
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
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
    ExecutorService logExecutor = Executors.newCachedThreadPool();

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
                for (var container : containerList) {

                    var noneMatch = list.stream().noneMatch(x -> x.getId().equals(container.getId()));
                    container.setActive(!noneMatch);

                    if(noneMatch && container.logProcess != null) {
                        if(container.logProcess.isAlive()) {
                            System.out.println("Deleting container logs process for: " + container.getName() + " " + container.getId());
                            container.logProcess.destroyForcibly();
                        }
                        container.logProcess = null;
                    }

                    if(noneMatch) {
                        updated = true;
                    }
                }

                for (var container : list) {
                    boolean onList = containerList.stream().anyMatch(x -> x.getId().equals(container.getId()));

                    if (!onList) {
                        containerList.add(container);
                        createLogProcessForContainer(container);
                        updated = true;
                    }
                }

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
            container.logProcess = pb.start();
            container.textArea.clear();
            logExecutor.submit(() -> captureLogs(container));
        } catch (IOException e) {
            System.out.println("! Error creating log process");
        }
    }

    private void captureLogs(ContainerModel container) {
        // Use a StringBuilder to accumulate logs
        StringBuilder logBuffer = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(container.logProcess.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logBuffer.append(line).append("\n");

                // Only update the TextArea periodically to avoid too many Platform.runLater() calls
                if (logBuffer.length() > 4096) { // Adjust size based on performance
                    final String logContent = logBuffer.toString();
                    Platform.runLater(() -> container.textArea.appendText(logContent));
                    logBuffer.setLength(0); // Clear the buffer after appending
                }
            }

            // Append any remaining logs after exiting the loop
            if (!logBuffer.isEmpty()) {
                final String logContent = logBuffer.toString();
                Platform.runLater(() -> container.textArea.appendText(logContent));
                logBuffer.setLength(0); // Clear the buffer after appending
            }
        } catch (IOException e) {
            System.out.println("! Error reading log output");
        }
    }
}
