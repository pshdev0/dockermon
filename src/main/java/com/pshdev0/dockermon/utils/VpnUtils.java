package com.pshdev0.dockermon.utils;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class VpnUtils {

    public static boolean isProxyActive() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", "ping -c 1 websenseproxy.internal.ch");
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            reader.readLine();

            StringBuilder output = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            return output.toString().contains("--- websenseproxy.internal.ch ping statistics ---");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public static void updateVpnStatus(Circle vpnCircle, Label vpnCircleLabel) {
        Platform.runLater(() -> {
            var status = VpnUtils.isProxyActive();

            System.out.print("Updating VPN status... ");
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
}
