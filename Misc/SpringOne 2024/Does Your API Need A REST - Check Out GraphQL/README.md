
## Reference

Based on SpringOne 2024 Talk by Dan Vega - https://www.youtube.com/watch?v=tMPC-u891XA&ab_channel=SpringDeveloper

Dan's Repo - https://github.com/danvega/graphql-store
  Forked repo - https://github.com/vrmehta93/graphql-store

# Notes

To start containers:
```
docker-compose up
```

To start springboot app:
```
mvn spring-boot:run
```

Once spring boot app starts up, access graphiql playground [here](http://localhost:8080/graphiql?path=/graphql) (http://localhost:8080/graphiql?path=/graphql)
This can be controlled by setting the value for `spring.graphql.graphiql.enabled`

