package com.pshdev0.dockermon.utils;

import com.pshdev0.dockermon.ContainerModel;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

public class DockerUtils {

    private static DockerUtils _this;

    private DockerUtils() { }

    public static DockerUtils get() {
        if(_this == null) {
            _this = new DockerUtils();
        }
        return _this;
    }

    public List<ContainerModel> getDockerProcesses() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", "docker ps");
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            HashMap<String, String> containers = new HashMap<>();
            String line;

            reader.readLine();

            while ((line = reader.readLine()) != null) {
                String[] columns = line.split("\\s+");
                if (columns.length > 1) {
                    String containerId = columns[0];
                    String containerName = columns[columns.length - 1];
                    containers.put(containerId, containerName);
                }
            }

            var entries = new ArrayList<>(containers.entrySet());
            entries.sort(Map.Entry.comparingByValue());
            return entries.stream().map(x -> new ContainerModel(x.getKey(), x.getValue())).toList();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
