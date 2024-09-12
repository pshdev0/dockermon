# JavaFX Dependency

Since the `org.openjfx` repository was not available in the Artifactory, you need to download the `.jar` libraries from the Oracle Open Source JavaFX project:

* JavaFX SDK libraries available here: https://jdk.java.net/javafx22/
* Unzip and add the `lib` folder to IntelliJ "Project Structure..." Libraries
* In "Edit Configurations" you should have an "Application" where you need to set the "Program arguments" text field to `--module-path /path/to/your/javafx-sdk/lib --add-modules=javafx.controls,javafx.fxml` (change `/path/to/your/javafx-sdk/lib` as appropriate). You'll also need to specify the Main class to run.
* You should then be able to run the JavaFX application.
# How to Run
* In a terminal run `docker_chs up`.
* Run Dockermon, and you should see it pick up all your services.
# Features
* Active containers are marked with a ✅, old containers with a 🛑
* Click "Clean" button to delete old (🛑) containers
* Click "Clear log" button to clear the selected log
* Click "Reload" to reload a selected service. Eventually the reloaded service will be picked up, just wait a while like you would if running `docker_chs reload` in the terminal.
* Any new logs entries appearing 5 seconds after any earlier log entries will be separated by a space for readability.
* Any green icons ✅ will light up for 3 seconds when the corresponding service has new logs.
# TODO
* Add a Refresh button to clear all processes and refresh.
* Add AWS key checking to see if they've cleared, and add an AWS button to refresh the keys
* Add VPN connection status
* Colorise log output
* Add search function
