package com.ahan.ratelimiter;

import java.util.List;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class SlidingController {

    private final StringRedisTemplate redis;

    private static final String LUA = """
        local t = redis.call('TIME')
        local now=t[1]*1000 + math.floor(t[2]/1000)
        local limit = tonumber(ARGV[1])
        local window=tonumber(ARGV[2])

        redis.call('ZREMRANGEBYSCORE', KEYS[1], 0,  now-window)

        if redis.call('ZCARD', KEYS[1])<limit then
            redis.call('ZADD', KEYS[1], now, ARGV[3])
            redis.call('PEXPIRE',KEYS[1], window)
            return 1
        end
        return 0
        """;

    private final DefaultRedisScript<Long> script = new DefaultRedisScript<>(LUA, Long.class);

    public SlidingController(StringRedisTemplate redis) {
        this.redis=redis;
    }


    @GetMapping("/api/sliding")
    public ResponseEntity<String> check(@RequestParam String user){
        Long ok=redis.execute(script, List.of("sliding:"+user), "5", "60000",  UUID.randomUUID().toString());

        if (ok==1) return ResponseEntity.ok("allowed");
        return ResponseEntity.status(429).body("rejected");
    }
}