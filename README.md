# Dependencies
The dependencies aren't in the artifactory so you'll need to install them manually.
* Download the Oracle Open Source JavaFX SDK libraries from here: https://jdk.java.net/javafx22/
* Unzip and add the `lib` folder to IntelliJ "Project Structure..." Libraries
* In "Edit Configurations" you should have an "Application" where you need to set the "Program arguments" text field to `--module-path /path/to/your/javafx-sdk/lib --add-modules=javafx.controls,javafx.fxml` (change `/path/to/your/javafx-sdk/lib` as appropriate). You'll also need to specify the Main class to run.
* You should then be able to run the JavaFX application.

For the RichTextFX console you'll need to download the following `.jar`s from Maven Central and add to the IntelliJ "Project Structure..." Libraries/Modules.
* `richtextfx-0.11.3.jar` - https://mvnrepository.com/artifact/org.fxmisc.richtext/richtextfx
* `flowless-0.7.3.jar` - https://mvnrepository.com/artifact/org.fxmisc.flowless/flowless
* `reactfx-2.0-M5.jar` - https://mvnrepository.com/artifact/org.reactfx/reactfx
* `undofx-2.1.0.jar` - https://mvnrepository.com/artifact/org.fxmisc.undo/undofx
* `wellbehavedfx-0.3.3.jar` - https://mvnrepository.com/artifact/org.fxmisc.wellbehaved/wellbehavedfx
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
* Select log text and CTRL+C to copy log text
# TODO
* Colorise log output
* Add a Refresh button to clear all processes and refresh.
* Add AWS key checking to see if they've cleared, and add an AWS button to refresh the keys
* Add VPN connection status
* Add search function
* Add Dockermon refresh button to force a logs flush

