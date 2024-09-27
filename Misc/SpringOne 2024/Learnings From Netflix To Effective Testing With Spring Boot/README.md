
## Reference

Based on SpringOne 2024 talk by Paul Bakker - https://www.youtube.com/watch?v=2bTAb-2vhBk&t=1s&ab_channel=SpringDeveloper

Paul's Repo - https://github.com/paulbakker/testing-spring-boot-presentation/tree/main
	Forked Repo - https://github.com/vrmehta93/testing-spring-boot-presentation

Complexities of project:
	GraphQL Framework
	OpenAI external component
	DB

## Execution Notes

To run postgres:
```
docker run --name postgres \
  -e POSTGRES_DB=mydatabase \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=password \
  -p 5432:5432 \
  -d postgres:15
```

To populate postgres with data:
```
# Copy the SQL files into the container
docker cp testing-spring-boot-presentation/src/test/resources/schema.sql postgres:/schema.sql
docker cp testing-spring-boot-presentation/src/test/resources/shows.sql postgres:/shows.sql

# Execute the schema.sql to create the table
docker exec -i postgres psql -U postgres -d mydatabase -f /schema.sql

# Execute the shows.sql to populate the table
docker exec -i postgres psql -U postgres -d mydatabase -f /shows.sql

# Verify the table and data
docker exec -i postgres psql -U postgres -d mydatabase -c "\dt"  # List tables
docker exec -i postgres psql -U postgres -d mydatabase -c "SELECT * FROM shows;"  # Query the shows table
```

## Notes from talk

Issues with Unit tests in `testing-spring-boot-presentation/src/test/java/com/netflix/testingdemo/LolomoDataFetcherUnitTest.java`:  
- Pure java code unit tests don't detect breaking changes such as changing query function name in `testing-spring-boot-presentation/src/main/resources/schema/schema.graphqls`
- Unit test is really just focusing on the data fetcher java code. Yes they can be nice to use (easy to use Mocks) to control the exact behavior of dependencies and get into all corner cases, especially unhappy paths. But this is NOT enough

Issues with an empty Smoke test (startup the whole app and determining if that even works) in: `testing-spring-boot-presentation/src/test/java/com/netflix/testingdemo/SmokeTest.java`
- Yes it's silly but it's required for every app at Netflix
- Because it's `@SpringBootTest`, it will figure out what components exist in my app, it'll startup the whole thing normally. If startup fails because of DI issue or in this case, GraphQL validation error, test would fail
- Takes longer to run. There's a lot of cost to running this test. However, introducing breaking change in `schema.graphqls` does fail the test
- If you introducing another bug, such as commenting out the `@DgsComponent` in `LolomoDataFetcher.java`, it does NOT detect this error. The error only shows up during runtime (null value) when making graphql query
- So it's still not ready for production. What if we add just one request inside the smoke test to know if GraphQL is working or not?

Issues with Smoke test with one request in `testing-spring-boot-presentation/src/test/java/com/netflix/testingdemo/SmokeTestWithRequest.java`:
- Still `@SpringBootTest`, so it's bootstraps the whole app, still slow and expensive
- But inject `@AutoConfigureMockMvc` - makes it easy to do an HTTP request to server that runs as part of the test without actually opening ports
- Wrap the request in `GraphqlRequest` and convert it to JSON, etc. It's not hard to do. It's a little bit more lower level details than we really care about
- The test only has one query. If you have to cover ALL of the queries in a larger app, writing it in this lower level way is doable but kind of a pain
- The other problem is that if I'm just looking at the final result of the service and maybe there's 20 components involved and a DB and an external service, etc, and all I know is that the data that I got back wasn't what I expected, where do I start debugging? That becomes difficult
- This test takes ~10 secs to run. If you have 300 tests, not a great developing experience

At Netflix, it's recommended you have one test that goes through the whole flow so you kind of know that as a black box it kind of works. So if you introduce a really bad bug with a dependency or something like that and things blow up, at least you will catch those

If you look at `SmokeTest.java` one more time:
- If you have multiple smoke tests that all have exactly the same setup, there will be context caching going on and it will re-use the same spring context and not actually restart
- But as soon as you introduce something like `@MockBean` to one of the tests, then that mechanism falls apart and it does re-initialize the context
- So only having smoke test is easy to write but very expensive from a waiting experience

Middle ground `testing-spring-boot-presentation/src/test/java/com/netflix/testingdemo/LolomoDataFetcherSpringTest.java`:
- We recommend at Netflix that 95% of tests should be written this way
- It's still `@SpringBootTest` but notice the `classes` argument
  - If it's without `classes`, it will bootstrap the whole app, everything it knows
  - If you specify `classes`, that behavior changes significantly because it's only going to bootstrap that are explicitly listed. So all we get is a app that bootstraps like core DI and 4 components. That's it
- That's a lot faster than bootstrapping the whole thing
- In `classes`, you'll notice there's `LolomoTestConfiguration.class`
  - It's another spring testing feature
  - We can use that to override beans just for testing purposes
  - In this case, we're mocking the `OpenAiService` (component that talks to OpenAI)
  - It always returns a fixed value
  - This is much faster, cheaper (not paying for a mock call), and we get a reliable list of data
  - `@TestConfiguration` is tricky to work with, it has a lot of gotchas depending on if it's on a top level class vs inner class. And behavior is kind of weird
    - This will be better in next version of spring
  
There is still one thing left to solve because I still want to send a graphql query to my service to actually test it and test all my annotations but for that to work, I need the DGS framework to be part of my framework startup. And for that we use a test slice `@EnableDgsTest`  
  - Test slide is a concept that we all over the place in Spring and at Netflix
  - We can use those to enable a specific feature in spring framework or in this case, the DGS framework
  - You can easily write those yourself
The `@ImportAutoConfiguration` inside `@EnableDgsTest` imports relevant auto configuration classes automatically and added to the test anytime you use this test slice  
Now you can send real graphql query using the DGS framework - it goes through the DGS query executor, data fetcher and all the other code that are part of this test  
The other nice thing is because we're bootstrapping a very small part of the app, this runs super quick. It runs as quick as a unit test. In fact, we call these things at Netflix just "unit tests"  

Let's look at `testTop10UnsupportedCountry()` inside that test
- If a country is not supported, the data fetcher throws this exception `Top10NotAvailableException(countryCode);`
- There's a `@ControllerAdvice` that catches this exception in `CustomDgsExceptionHandler.java` and converts that into a message for graphql
- If you introduce a bug in that controller advice by changing method param from `Top10NotAvailableException` to `IllegalArgumentException`, so it's no longer catching the exception that I want to catch, and if you run the test, the test blows up and gives an error message

So the nice thing is we have few components working together and we can see the interaction between those components and testing things that are not really just one class and you wouldn't catch those things with a like a pure unit test. But if you put them all together, that's where all the magic happens

We still have a problem. I have mentioned Postgres DB. Just now, I just mocked the `ShowsRepository` but we haven't tested any of the SQL so far  
Is it a good idea to mock out your DB?
- Maybe sometimes if I want to test very specific corner cases and I have other tests that cover my SQL
- But in this case, it would be more useful if the test would include the call to the DB because why not
  - That brings us to a problem because how do we run a DB?
  - So far on my demo machine, I'm running this postgres docker container and that's why we have a DB
  - There's many problems with this approach
    1. The first problem I can't actually run docker this way on a Jenkins host, at least not in at Netflix because there's like containers and containers and all that sorts of issues. So I can't run this same docker container when running on CI
	2. Another problem is that if I'm running and just re-using the same thing with all of my tests and also using it for local testing and development just in browser, I probably will introduce new data to the DB, the next time I run the test, the test will fail over because it gets different data. Now you have to figure out if you broke the data, did you introduce a bug, etc. so just re-using an existing DB either in docker or the real thing is problematic, just not a good solution

So if you stop the container, the smoke test would fail. This is something we have to solve for. And the way we're going to solve that is we're going to use test containers
- Test containers are also something that we rely on heavily at Netflix for testing
- Test container is a java library that gives you a java API to basically start docker containers as part of your tests and the lifecycle of those containers will be controlled by your tests
- So your test will start a docker container, in this case postgres, but we also use for things like Kafka, AWS SQS and S3 and many other things
- As part of your test, test container will be started. You can insert test data specifically for your test and the container will be gone
- "Didn't you say you can't run docker containers on CI?" True. However, test containers also comes with a paid feature, that we do pay for, which is test container cloud. And with test containers cloud, the actual docker containers will be on a remote host in the cloud and it will be port forwarding to and from those containers so that your test actually talk to that thing
- That means you don't have to run docker on the actual host that you're on
- It also solves the issue that if I'm on Mac with apple silicon, many of the existing containers for docker are not optimized for this hardware architecture and they run super slow compared to running it on Linux machine, for example. With test container cloud you don't have that problem

What do we have to do to use test containers?
- `LolomoDataFetcherSpringTcTest.java` - this is almost the same test
- I have a new annotation `@EnableDatabaseTest`. Again, this is a test slice that I defined
- This is a bit of a mouthful because I need a whole bunch of annoations to configure Spring Data JPA but that is why you want to put this on a meta-annotation. So that you only write this once for your app and then all your tests can re-use this
- For `@AutoConfigureTestDatabase`, this is really important, I explicitly tell it to NOT start an in-memory DB. The default behavior in spring is that it will start H2 and that's kind of the traditional approach of testing DBs if you don't have test containers
  - The problem with something like H2 is it's a different SQL dialect than postgres
  - If I go to `ShowsRepository`, I'm using a native query. And this is postgres specific SQL
  - Now the traditional thinking about it is - don't use postgres specific SQL, you just use the common SQL dialect and then you get to do JPA and it abstracts it even further. But DBs are really powerful and even in AI space, postgres has a lot of functionality specifically for that but it's not standard SQL. So actually you do want to use your DB to it's fullest extent and I do want to use this native keyword because it's really useful. So I want ot use that during my test as well
- So in `EnableDatabaseTest.java`, it has all the standard Spring annotations. And there's one annotation that does an import of a test config that I created - `@Import(PostgresTestContainerConfig.class)`
  - This is where the real magic with test containers happens

If you run multiple tests that uses the postgres container, it will keep that container around

The important thing is it's really easy to write these tests and they run really fast. And with test container, I can test with real components that I rely on

You might be wondering, in Spring we also have these annotations like `@WebMvcTest` and that's actually the same thing as a `@SpringBootTest(classes = ...` with some test slice included. So it's the same thing  
I find that these provided annotations are often a little bit limiting and often you want a little bit more and then you have to fall back to `@SpringBootTest(classes=...` with some test slices but technically they're the same thing. And if they work for you, just use them. They're convenient

What tests should you write?
- Most of your tests should be `@SpringBootTest(classes = ...` given the test slices that we have
- You want at least one smoke test. And if it's a GraphQL or REST service, you probably should do that one request so that you know that at least things sort of work at a HTTP level
- Maybe you have some unit tests if you have some more complex business logic that doesn't use any framework stuff. It's fine. Unit tests are not bad, just usually not sufficient




