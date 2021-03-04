package org.example.redistest;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.util.Arrays;

/**
 * check redis service logic works with external running local redis server
 */
@Tag("integration-test")
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class RedisServiceTestIntegration {

    @Autowired
    private RedisService redis;

    @AfterEach
    public void cleanup() {
        redis.cleanupDb();
        redis.resetClock();
    }

    @Test
    public void no_message_should_be_returned_on_empty_set() {
        Assertions.assertNull(redis.getLast());
    }

    @Test
    public void publish_and_return_message_as_last() {
        String message = "message";
        redis.publish(message);
        Assertions.assertEquals(message, redis.getLast());
    }

    @Test
    public void publish_and_retrieve_two_different_last_messages() {
        redis.publish("abc");
        Assertions.assertEquals("abc", redis.getLast());
        redis.publish("def");
        Assertions.assertEquals("def", redis.getLast());
    }

    @Test
    public void publish_identical_message_twice_with_identical_times_and_return_both() {
        redis.setClock(Clock.fixed(Instant.now(), ZoneId.of("UTC")));
        redis.publish("abc");
        redis.publish("abc");
        Assertions.assertEquals("abc", redis.getLast());
    }

    @Test
    public void publish_same_message_multiple_times_with_different_clock_and_filter_part_of_them_by_time() {
        Instant instant = Instant.now().with(ChronoField.NANO_OF_SECOND, 0);
        Clock clock = Clock.fixed(instant, ZoneId.of("UTC"));
        redis.setClock(clock);
        redis.publish("abc"); //#1

        redis.setClock(Clock.offset(clock, Duration.ofNanos(1)));
        redis.publish("abc"); //#2

        redis.setClock(Clock.offset(clock, Duration.ofMillis(42)));
        redis.publish("abc"); //#3

        redis.setClock(Clock.offset(clock, Duration.ofHours(24)));
        redis.publish("abc"); //#4

        Assertions.assertEquals(
                Arrays.asList("abc", "abc", "abc"), //#4 is excluded
                redis.getByTime(
                            instant.minusMillis(1),
                            instant.plusSeconds(1))
        );
    }
}
