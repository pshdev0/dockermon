package com.pshdev0.dockermon.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AWSUtils {

    public static void scanProfiles() {
        try {
            Map<String, String> profileRegions = getAWSProfilesAndRegions();
            if (profileRegions.isEmpty()) {
                System.out.println("No profiles or regions found in ~/.aws/config");
                return;
            }

            for (Map.Entry<String, String> entry : profileRegions.entrySet()) {
                String profile = entry.getKey();
                String region = entry.getValue();

                System.out.println("Fetching token for profile: " + profile + " in region: " + region);
                String token = getAWSTokenForProfile(profile, region);
                if (token != null) {
                    System.out.println("Profile: " + profile + " - Region: " + region + " - Token: " + token);
                } else {
                    System.out.println("Failed to fetch token for profile: " + profile);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Step 1: Method to get profiles and their regions from ~/.aws/config
    public static Map<String, String> getAWSProfilesAndRegions() {
        Map<String, String> profileRegions = new HashMap<>();
        try {
            File configFile = new File(System.getProperty("user.home") + "/.aws/config");
            BufferedReader reader = new BufferedReader(new FileReader(configFile));
            String line;
            String currentProfile = null;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("[profile ")) {
                    currentProfile = line.substring(9, line.length() - 1); // Extract profile name
                } else if (line.startsWith("region") && currentProfile != null) {
                    String region = line.split("=")[1].trim(); // Extract region value
                    profileRegions.put(currentProfile, region); // Map profile to its region
                }
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return profileRegions;
    }

    // Step 2: Method to get the AWS token for a profile using ProcessBuilder
    public static String getAWSTokenForProfile(String profile, String region) {
        try {
            // Prepare AWS command
            List<String> command = new ArrayList<>();
            command.add("aws");
            command.add("--profile");
            command.add(profile);
            command.add("ecr");
            command.add("get-login-password");
            command.add("--region");
            command.add(region);

            // Run the command
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();

            // Capture the output
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
            reader.close();

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                return output.toString(); // Return the token
            } else {
                // Print error if the command failed
                System.err.println("Error: Command exited with code " + exitCode);
                return null;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
