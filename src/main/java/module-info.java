module com.pshdev0.dockermon {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires java.desktop;
    requires org.fxmisc.richtext;
    requires org.fxmisc.flowless;
    requires com.fasterxml.jackson.databind;
    requires jakarta.websocket.client;

    opens com.pshdev0.dockermon to javafx.fxml;
    exports com.pshdev0.dockermon;
    exports com.pshdev0.dockermon.utils;
    opens com.pshdev0.dockermon.utils to javafx.fxml;
}