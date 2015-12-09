build:
	mvn clean install
run:
	java -jar target/brewcontrol-2.0-SNAPSHOT.jar server config.dev.yml