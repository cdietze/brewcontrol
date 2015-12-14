build:
	cd ng && bower install
	mvn clean install

run: build
	java -agentlib:jdwp=transport=dt_socket,server=y,address=8787,suspend=n -jar target/brewcontrol-2.0-SNAPSHOT.jar server config.dev.yml

deploy: build
	./deploy.sh