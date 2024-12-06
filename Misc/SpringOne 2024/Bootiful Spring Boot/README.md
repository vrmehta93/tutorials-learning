# Goal
Create dog adoption service

## Reference

Based on SpringOne 2024 Talk - https://www.youtube.com/watch?v=ex7rnzIMmlk&ab_channel=SpringDeveloper

Josh's repo - https://github.com/joshlong/bootiful-spring-boot-2024 
NOTE - I'm not forking the repo but instead starting from scratch since josh's repo is a little different from the code that's displayed on the talk

Martin Fowler's blog on event-driven - https://martinfowler.com/articles/201701-event-driven.html

## Links
Spring initializr - start.spring.io
Spring history game - https://springone.io/history-of-spring
Billion row challenge - https://github.com/gunnarmorling/1brc


## Notes on running the forked repo locally in VSCode devcontainer
For devcontainer selection
    Used Docker inside docker
    This allows me to install Java and Maven as "features"

To launch pgvector
```
vscode ➜ .../Misc/SpringOne 2024/Bootiful Spring Boot/service (springone-2024) $ docker-compose -f compose.yaml up
```

To test connection, used extension cweijan.vscode-postgresql-client2
    For port number, run `docker ps` and get the port number listed in terminal (NOT 5432)

To run app:
    From CLI - `./mvnw spring-boot:run` OR
    Use extension vscjava.vscode-spring-boot-dashboard

To test updating owner (from null) and testing event listener:
```
curl -d '{"name":"vm"}' -XPOST -H"content-type: application/json" http://localhost:8080/dogs/1/adoptions
```

Adding modulith's property:
```
spring.modulith.events.jdbc.schema-initialization.enabled=true
```
It automatically created a table in the DB called `event_publication`. This table has a field called `completion_date`. If I pulled the plug before the method was finished (in on() in Vet.java), the `completion_date` would be `null`
When you restart the application, it's going to replay that message before. It's going to start the method execution again on restart
NOTE - I tried this locally, and there's a graceful shutdown which gives enough time for the message to be processed. To prevent this functionality, add this property
```
spring.lifecycle.timeout-per-shutdown-phase=0s
```

For running spring boot test (`./mvnw test`), I kept getting this error:
```
java.lang.IllegalStateException: Failed to load ApplicationContext for
...
Caused by: java.net.ConnectException: Connection refused
...
```
So I had to create file `service/src/test/resources/application.properties` with postgres connection details

To enable communciation with OpenAI, add this to .devcontainer
```
"remoteEnv": {
		"OPENAI_API_KEY": "${localEnv:OPENAI_API_KEY}",
	}
```
Now you can reference that env var in `application.properties`



PICKUP - 25:25

MISCELLANEOUS ISSUES:
    Installed vmware.vscode-spring-boot extension but that did NOT turn on autocompletion





