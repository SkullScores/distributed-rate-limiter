package com.ahan.ratelimiter;

import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class BucketController {

    private final StringRedisTemplate redis;

    private static final String LUA = """
        local t = redis.call('TIME')
        local now=t[1]*1000 + math.floor(t[2]/1000)
        local cap = tonumber(ARGV[1])
        local window=tonumber(ARGV[2])
        local rate = cap/window

        local data=redis.call('HMGET', KEYS[1], 'tokens','ts')
        local tokens = tonumber(data[1])
        local last=tonumber(data[2])

        if not tokens then
            tokens = cap
            last=now
        end

        local ok = 0
        tokens=math.min(cap, tokens + (now-last)*rate)

        if tokens>=1 then
            tokens = tokens-1
            ok=1
        end

        redis.call('HSET', KEYS[1], 'tokens', tokens,  'ts',now)
        redis.call('PEXPIRE', KEYS[1], window)
        return ok
        """;

    private final DefaultRedisScript<Long> script=new DefaultRedisScript<>(LUA, Long.class);

    public BucketController(StringRedisTemplate redis) {
        this.redis = redis;
    }


    @GetMapping("/api/bucket")
    public ResponseEntity<String> check(@RequestParam String user){
        Long ok = redis.execute(script, List.of("bucket:"+user), "5","60000");

        if (ok==1) return ResponseEntity.ok("allowed");
        return ResponseEntity.status(429).body("rejected");
    }
}