package com.ahan.ratelimiter;

import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class LimitController {

    private final StringRedisTemplate redis;

    private static final String LUA = """
        local q = redis.call('GET', KEYS[1])
        if not q then
            q=ARGV[1]
            redis.call('SET', KEYS[1], q, 'EX', ARGV[2])
        end
        if tonumber(q)>0 then
            redis.call('DECR',  KEYS[1])
            return 1
        end
        return 0
        """;

    private final DefaultRedisScript<Long> script=new DefaultRedisScript<>(LUA, Long.class);

    public LimitController(StringRedisTemplate redis){
        this.redis = redis;
    }


    @GetMapping("/api/check")
    public ResponseEntity<String> check(@RequestParam String user) {
        Long ok = redis.execute(script, List.of("quota:"+user),  "5", "60");

        if (ok == 1) return ResponseEntity.ok("allowed");
        return ResponseEntity.status(429).body("rejected");
    }
}