package com.pshdev0.dockermon.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class ProxyUtils {
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
}
