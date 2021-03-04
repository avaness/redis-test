package org.example.redistest;

import io.lettuce.core.Range;
import io.lettuce.core.RedisClient;
import io.lettuce.core.ZAddArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class RedisService {

    private static final Logger log = LoggerFactory.getLogger(RedisService.class);

    private static final String SET_KEY = "redis-test-set";
    private static final String UNIQ_CNTR_KEY = "redis-test-cntr";

    private RedisClient redisClient;
    private StatefulRedisConnection<String, String> connection;
    private RedisCommands<String,String> redis;
    private final ZAddArgs nx = ZAddArgs.Builder.nx();

    @Autowired
    public RedisService(@Value("${redis.uri}") String redisUri) {
        redisClient = RedisClient.create(redisUri);
        connection = redisClient.connect();
        redis = connection.sync();
        log.info("Connected to {}", redisUri);
    }

    private Clock clock;
    {
        resetClock();
    }

    @PreDestroy
    private void free() {
        connection.close();
        redisClient.shutdown();
    }

    public void publish(String message) {
        Instant instant = clock.instant();
        Long counter = redis.incr(UNIQ_CNTR_KEY);
        if (redis.zadd(SET_KEY, nx, calcScore(instant), cryptUniqueMessage(message, counter)) != 1)
            throw new IllegalStateException("Not inserted: " + message);
    }

    public @Nullable String getLast() {
        List<String> result = redis.zrevrange(SET_KEY, 0, 0);
        if (result.size() >1)
            throw new IllegalStateException("Messages more than one: " + result);
        return result.size() == 1? decryptUniqueMessage(result.get(0)): null;
    }

    /**
     * TODO review requirements if we need including/excluding time boundaries
     * TODO review requirements if we need messages with their times (e.g. request with scores)
     */
    public List<String> getByTime(Instant start, Instant end) {
        return redis.zrevrangebyscore(SET_KEY, Range.create(calcScore(start), calcScore(end)))
                .stream().map( s-> decryptUniqueMessage(s)).collect(Collectors.toList());
    }

    Long cleanupDb() {
        redis.del(UNIQ_CNTR_KEY);
        return redis.zremrangebyrank(SET_KEY, 0, -1);
    }

    void setClock(Clock clock) {
        this.clock = clock;
    }

    void resetClock() {
        this.clock = Clock.systemUTC();
    }

    private static double calcScore(Instant instant) {
        //double can be converted from 2^53 max int without loosing precision
        //it is 20^50 seconds from 1970 epoch
        return instant.toEpochMilli();
    }
    private static String cryptUniqueMessage(String message, long counter) {
        return counter + ":" + message;
    }
    private static String decryptUniqueMessage(String s) {
        int i = s.indexOf(':');
        if (i==-1)
            throw new IllegalStateException("No ':' in message" + s);
        return s.substring(i +1);
    }
}
