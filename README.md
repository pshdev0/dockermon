# How to Run
* In a terminal run `docker_chs up`, or alternatively in the `docker-chs-development` repo root, run `chs-dev up`.
* In a separate terminal, navigate to the `dockermon` repo root directory, and run `mvn javafx:run`.
* Alternatively, on Mac or Linux, type `./run.sh` in the terminal (you may need to `chmod +x run.sh` first)
# Information
* Active containers are marked with a ✅, old containers with a 🛑
* Click "Clean" button to remove old containers from the list
* Click "Clear log" button to clear the selected log
* Click "Reload" to reload a selected service. Eventually the reloaded service will be picked up, just wait a while like you would if running `docker_chs reload` in the terminal.
* Any new logs entries appearing 5 seconds after any earlier log entries will be separated by a space for readability.
* Any green icons ✅ will light up for 3 seconds when the corresponding service has new logs.
* Select log text and CTRL+C to copy log text
* Click "Split" button to split the view vertically. You can CTRL + Left Click another container to view that second container.
* Use the search function to highlight matched logs in the primary container view.
# TODO
* When reloading a container, monitor the whatever-service-builder-N to check for failure, since this can cause problems with UI busy spinner if it fails.
* Maybe add Dockermon refresh button to force a logs flush?
# Troubleshooting
* If after running you see no containers in the list, try deleting the `org.fxmisc.richtext` repo in you hidden `~/.m2` folder (and other related dependencies if that doesn't work)
