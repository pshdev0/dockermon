# JavaFX Dependency

Since the `org.openjfx` repository was not available in the Artifactory, you need to download the `.jar` libraries from the Oracle Open Source JavaFX project:

* JavaFX libraries available here: https://jdk.java.net/javafx22/
* Unzip and add the `lib` folder to IntelliJ "Project Structure..." Libraries
* In "Edit Configurations" you should have an "Application" where you need to set the "Program arguments" text field to `--module-path /path/to/your/javafx-sdk/lib --add-modules=javafx.controls,javafx.fxml`
* You should then be able to run the JavaFX application.
# dockermon
