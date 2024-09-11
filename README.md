# JavaFX Dependency

Since the `org.openjfx` repository was not available in the Artifactory, you need to download the `.jar` libraries from the Oracle Open Source JavaFX project:

* JavaFX SDK libraries available here: https://jdk.java.net/javafx22/
* Unzip and add the `lib` folder to IntelliJ "Project Structure..." Libraries
* In "Edit Configurations" you should have an "Application" where you need to set the "Program arguments" text field to `--module-path /path/to/your/javafx-sdk/lib --add-modules=javafx.controls,javafx.fxml`. You'll also need to specify the Main class to run.
* You should then be able to run the JavaFX application.
# dockermon
* Active containers appear in green, old containers in red
* Click "Clean" button to remove old (red) containers
* Click "Clear log" button to clear the selected log
* Any new logs entries appearing 5 seconds after any earlier log entries will be separated by a space for readability.
