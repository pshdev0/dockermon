package com.pshdev0.dockermon.utils;

import javafx.application.Platform;

import java.io.IOException;

public class Helper {

    public static String getStyleFromAnsiCode(String ansiCode) {

        return switch (ansiCode) {
            case "30" -> "-fx-fill: black; -fx-font-weight: bold;";         // black
            case "31" -> "-fx-fill: red; -fx-font-weight: bold;";           // red
            case "32" -> "-fx-fill: lightgreen; -fx-font-weight: bold;";    // green
            case "33" -> "-fx-fill: yellow; -fx-font-weight: bold;";        // yellow
            case "34" -> "-fx-fill: lightblue; -fx-font-weight: bold;";     // blue
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

    public static void bashSourceAndRun(String command) {
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
}
