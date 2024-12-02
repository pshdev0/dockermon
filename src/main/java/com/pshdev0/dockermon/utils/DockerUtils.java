package com.pshdev0.dockermon.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pshdev0.dockermon.ContainerModel;
import com.pshdev0.dockermon.MainController;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.TableView;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class DockerUtils {

    private static DockerUtils _this;

    private DockerUtils() { }

    public static DockerUtils get() {
        if(_this == null) {
            _this = new DockerUtils();
        }
        return _this;
    }

/*
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

 todo: findings docker events are unreliable don't know why
       unix socket would be great, but can't get it to work
       this works: curl --unix-socket /var/run/docker.sock -H "Connection: keep-alive" http://localhost/events
       BUT i have same problem as with docker events. it displays a few events and then halts output
*/

    public void startListeningToDockerEvents(ObservableList<ContainerModel> containerList, TableView<ContainerModel> table) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {

                String host = "127.0.0.1";
                int port = 2375;

                try (Socket socket = new Socket(host, port)) {
                    // Send a simple HTTP GET request
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    out.println("GET /_ping HTTP/1.1");
                    out.println("Host: localhost");
                    out.println();
                    out.flush();

                    // Read the response
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String line;
                    while ((line = in.readLine()) != null) {
                        System.out.println(line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return null;
            }
        };

        Thread thread = new Thread(task);
        thread.setDaemon(true); // ensures thread exit on application close
        thread.start();
    }

    public void startListeningToDockerEvents_old(ObservableList<ContainerModel> containerList, TableView<ContainerModel> table) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {

                ProcessBuilder processBuilder = new ProcessBuilder(
                        "docker", "events",
                        "--filter", "type=container",
                        "--filter", "event=create",
                        "--filter", "event=destroy",
                        "--format", "{{.ID}} {{.Actor.Attributes.name}}"
                );

                Process process = processBuilder.start();
                ObjectMapper objectMapper = new ObjectMapper();

                var ignoredCommands = new ArrayList<String>();
                ignoredCommands.add("exec_start");
                ignoredCommands.add("exec_die");
                ignoredCommands.add("exec_create");
                ignoredCommands.add("exec_destroy");

                boolean showJsonContainerInfoForDevelopment = true;

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {

                        System.out.println(line);

//                        Map<String, Object> event;
//                        event = objectMapper.readValue(line, new TypeReference<>() { });

//                        if(showJsonContainerInfoForDevelopment) {
//                            System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(event));
//                            System.out.println("-----------------------------------------------------------------------");
//                        }

//                        var status = (String)event.get("status");
//                        if(status == null || ignoredCommands.stream().anyMatch(status::startsWith)) {
//                            continue;
//                        }
//
//                        if (!(event.get("Actor") instanceof Map<?, ?> actor)) {
//                            continue;
//                        }
//
//                        if (!(actor.get("Attributes") instanceof Map<?, ?> attributes)) {
//                            continue;
//                        }
//
//                        String containerId = (String)event.get("id");
//                        String containerName = (String)attributes.get("name");
//
//                        if (containerName.startsWith("sha256:")) {
//                            // these types of container appear to be orchestration specific, so ignore them
//                            continue;
//                        }
//
//                        if (status.startsWith("create")) {
//                            System.out.println("create " + containerName + " : " + containerId);
//
//                            Platform.runLater(() -> {
//                                var container = new ContainerModel(containerId, containerName);
//                                containerList.add(container);
//                                container.createLogProcessForContainer();
//                                table.refresh();
//                            });
//                        }
//                        else if (status.startsWith("destroy")) {
//                            System.out.println("destroy " + containerName + " : " + containerId);
//
//                            Platform.runLater(() -> {
//                                containerList.stream().filter(c -> c.getId().equals(containerId)).forEach(c -> c.setActive(false));
//                                table.refresh();
//                            });
//                        }
                    }
                }

                System.out.println("EXITED !");

                return null;
            }
        };

        Thread thread = new Thread(task);
        thread.setDaemon(true); // ensures thread exit on application close
        thread.start();
    }
}
