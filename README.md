# Dependencies
## Running with IntelliJ
The dependencies aren't in the artifactory so for the time-being you'll need to install them manually.
* Download the Oracle Open Source JavaFX SDK libraries from here: https://jdk.java.net/javafx22/
* Unzip and add the `lib` folder to IntelliJ "Project Structure..." `Modules` (or `Libraries`)
* In "Edit Configurations" you should have an "Application" where you need to set the "Program arguments" text field to `--module-path /path/to/your/javafx-sdk/lib --add-modules=javafx.controls,javafx.fxml` (change `/path/to/your/javafx-sdk/lib` as appropriate). You'll also need to specify the Main class to run.
* You should then be able to run the JavaFX application.

For the RichTextFX console you'll need to download the following `.jar`s from Maven Central and add to the IntelliJ "Project Structure..." `Libraries` (or `Modules`)
* `richtextfx-0.11.3.jar` - https://mvnrepository.com/artifact/org.fxmisc.richtext/richtextfx
* `flowless-0.7.3.jar` - https://mvnrepository.com/artifact/org.fxmisc.flowless/flowless
* `reactfx-2.0-M5.jar` - https://mvnrepository.com/artifact/org.reactfx/reactfx
* `undofx-2.1.0.jar` - https://mvnrepository.com/artifact/org.fxmisc.undo/undofx
* `wellbehavedfx-0.3.3.jar` - https://mvnrepository.com/artifact/org.fxmisc.wellbehaved/wellbehavedfx
## Running with Eclipse
* You'll need to add the dependencies to the `ModulePath` as for IntelliJ above
* But to run the app you may find it useful to right-click the root project `dockermon` and `Export...` then click `Java/Runnable JAR`.
* You may need to copy the `main.fxml` file into the same directory as the `MainApplication.java` file.
* You may also need to add a `/src/META-INF` folder containing a `MANIFEST.MF` file with contents:
```
Main-Class: com.pshdev0.dockermon.MainApplication
```
* Run the app with e.g. below, changing `dockermon.jar` as required.
```
java --module-path /path/to/javafx-sdk/lib --add-modules javafx.controls,javafx.fxml -jar dockermon.jar
```

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
* Click "Split" button to split the view vertically. You can CTRL + Left Click another container to view that second container.
# TODO
* Add a Refresh button to clear all processes and refresh.
* Add AWS key checking to see if they've cleared, and add an AWS button to refresh the keys
* Add VPN connection status - I think we can use `ping -c 3 websenseproxy.internal.ch`
* Add search function
* Add Dockermon refresh button to force a logs flush
* Add UI to enable/disable services, modules, development mode etc.

