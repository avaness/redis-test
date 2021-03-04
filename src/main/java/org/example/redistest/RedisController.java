package org.example.redistest;

import org.apache.coyote.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@RestController
public class RedisController {

    private static final Logger log = LoggerFactory.getLogger(RedisController.class);

    @Autowired
    private RedisService redis;

    /**
     * Internal 500 handler
     */
    @ExceptionHandler({Exception.class})
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    public void handleException(Exception e) {
        log.error("log: failed to process", e);
    }

    /**
     * returns HTTP 204 on success
     */
    @PostMapping(value = "/publish", consumes = MediaType.TEXT_PLAIN_VALUE)
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    public void publish(@RequestBody String message) {
        redis.publish(message);
    }

    /**
     * @return 204 if there's no messages, otherwise 200
     */
    @GetMapping(value = "/last", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getLast() {
        String last = redis.getLast();
        if (last != null)
            return ResponseEntity.ok(last);
        return ResponseEntity.noContent().build();
    }

    /**
     * @param start - ISO 8601 formatted time
     * @param end - ISO 8601 formatted time
     * @return json array of messages withing [start-end] time bounds
     */
    @GetMapping(value = "/by-time", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<String> getByTime(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant start,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant end) {
        return redis.getByTime(start, end);
    }


}
