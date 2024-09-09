module com.pshdev0.dockermon {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires java.desktop;

    opens com.pshdev0.dockermon to javafx.fxml;
    exports com.pshdev0.dockermon;
}