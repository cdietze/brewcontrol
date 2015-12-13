build:
	cd ng && bower install
	mvn clean install

run: build
	java -jar target/brewcontrol-2.0-SNAPSHOT.jar server config.dev.yml