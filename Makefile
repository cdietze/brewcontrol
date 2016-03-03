build-java:
	mvn clean install
build-ui:
	cd ui && npm run build

run-java:
	java -agentlib:jdwp=transport=dt_socket,server=y,address=8787,suspend=n -jar target/brewcontrol-2.0-SNAPSHOT.jar server config.dev.yml
run-ui:
	cd ui && npm start

deploy: build-java build-ui
	./deploy.sh