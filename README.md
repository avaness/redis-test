# Preface

## Contract

- Java 8/springboot
- spring boot REST service around redis
- REST operations `publish(String message)`, `String getLast()` and `List<String> getByTime(Instant start, Instant end)`
- `standalone REST application` -> `standalone redis server` linkage
- no redis clustering
- messages are unique and don't override each other

### KISS disclaimer  
- redis ZSETs(time, message) are used to balance `publish()/getLast()/getByTime()` execution times
- time granularity is msecs, no nanosecs.
- client app host's timer is used for message time
- 2^53 msecs can be stored in ZSET IEEE 754 double without loss, 
  571.3 years from 1970 epoch are considered acceptable bounds
- messages uniqueness are supported by `2^63-1` int prefix, for example `5867:message`
- not more than `2^63-1` publish operations allowed without db cleanup

# Usage

## Build
```bash
mvnw clean test 
```

## Integration tests

```bash
docker-compose -f docker-compose-integration.yaml up
mvnw clean verify
docker-compose -f docker-compose-integration.yaml down --rmi all
```

## Run in docker
```bash
mvnw clean package
docker-compose build
docker-compose up
```
> TODO add and url special chars encoding

# CURL

### publish(String)
```bash
curl -v --location --request POST 'localhost:8080/publish' \
--header 'Content-Type: text/plain' \
--data-raw 'abcdef'
```

### String getLast()
```bash
curl -v --location --request GET 'localhost:8080/last'
````

### List<String> getByTime(Instant, Instant)

add more messages
```bash
curl -v --location --request POST 'localhost:8080/publish' \
--header 'Content-Type: text/plain' \
--data-raw 'defghj'

curl -v --location --request POST 'localhost:8080/publish' \
--header 'Content-Type: text/plain' \
--data-raw 'abcdef'
```
query all of them
```bash
curl -v --location --request GET 'localhost:8080/by-time?start=2002-11-29T18:35:24.003Z&end=2032-11-30T18:35:24.003Z'
```
or query the same in URIEncoded form
```bash
curl -v --location --request GET 'localhost:8080/by-time?start%3D2002-11-29T18%3A35%3A24.003Z%26end%3D2032-11-30T18%3A35%3A24.003Z'
```

### Cleanup docker
```bash
docker-compose down --rmi all -v
```

## TODO Steps:

- requirements clarification
  - if we need including/excluding time boundaries
  - if we need messages with their times (e.g. request with scores)
- optimize with springboot docker layers
- use redis server TIME
