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
* Switch to using `docker events --filter 'event=create' --filter 'event=destroy' --format '{{.Status}} {{.ID}} {{.Actor.Attributes.name}}'` instead of scanning `docker ps` periodically.
* For the VPN monitor, use `aws configure export-credentials --format env` and resulting `AWS_CREDENTIAL_EXPIRATION`
* When reloading a container, monitor the whatever-service-builder-N to check for failure, since this can cause problems with UI busy spinner if it fails.
* Fix clear log / search bug (clearing then searching shows up older results)
* Maybe add Dockermon refresh button to force a logs flush?
# Troubleshooting
* If after running you see no containers in the list, try deleting the `org.fxmisc.richtext` repo in you hidden `~/.m2` folder (and other related dependencies if that doesn't work)

# Adding `docker` Group Access
Dockermon monitors Docker events a Unix Domain Socket located here `/var/run/docker.sock`.

Make sure `Advanced / Allow the default Docker socket to be used` is enabled in Docker Desktop Settings.

## On Mac
Check the file permissions on `/var/run/docker.sock`:
```bash
ls -l /var/run/docker.sock
```
I see `root daemon` so you need to belong to the `daemon` group to use the Unix Socket.  Check if you're in the `daemon` group by running:
```bash
groups
```
or check if the `daemon` group exists:
```bash
dscl . -list /Groups | grep daemon
```
If the `daemon` group does not exist then you'll need to create it:
```bash
sudo dseditgroup -o create daemon
```
And then add yourself to the group:
```bash
sudo dseditgroup -o edit -a $(whoami) -t user daemon
```
Log out and log back in for the group to be applied.

## On Linux - TODO
TODO - Instead of logging out/in at the end, just run `newgrp daemon`.

## On Windows - TODO
TODO
