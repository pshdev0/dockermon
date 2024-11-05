package com.pshdev0.dockermon;

import com.pshdev0.dockermon.utils.DockerUtils;
import com.pshdev0.dockermon.utils.VpnUtils;
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
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;

import java.util.Comparator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.pshdev0.dockermon.utils.Helper;

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
    @FXML
    TextField searchTextField;
    @FXML
    Button clearSearchTextButton;
    @FXML
    Button prevSearch;
    @FXML
    Button nextSearch;

    ContainerModel firstContainer = null;
    ContainerModel secondContainer = null;

    ScheduledExecutorService executor;
    ObservableList<ContainerModel> containerList;

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
                    if(item.getName().startsWith("docker-chs-development-")) {
                        labelName += item.getName().substring(23);
                    }
                    else {
                        labelName += item.getName();
                    }

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
                        container.createLogProcessForContainer();
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

        // VPN
        executor.scheduleAtFixedRate(() -> VpnUtils.updateVpnStatus(vpnCircle, vpnCircleLabel), 0, 60, TimeUnit.MINUTES);
        Tooltip.install(vpnCircle, new Tooltip("The VPN status will update every hour, or click the indicator circle to update instantly"));
        vpnCircle.setOnMouseClicked(event -> VpnUtils.updateVpnStatus(vpnCircle, vpnCircleLabel));

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

        // buttons
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
                Helper.bashSourceAndRun("docker_chs reload " + selectedContainer.getName());
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

        searchTextField.setOnAction(event -> {
            firstContainer.search(searchTextField.getText());
            if(secondContainer != null) {
                secondContainer.search(searchTextField.getText());
            }
        });

        clearSearchTextButton.setOnAction(event -> {
            searchTextField.setText("");
            firstContainer.applyStyles();
            firstContainer.richTextArea.setLineHighlighterOn(false);

            if(secondContainer != null) {
                secondContainer.applyStyles();
                secondContainer.richTextArea.setLineHighlighterOn(false);
            }
        });

        prevSearch.setOnAction(event -> {
            firstContainer.moveCaretUp();
            if(secondContainer != null) {
                secondContainer.moveCaretUp();
            }
        });
        nextSearch.setOnAction(event -> {
            firstContainer.moveCaretDown();
            if(secondContainer != null) {
                secondContainer.moveCaretDown();
            }
        });
    }
}
