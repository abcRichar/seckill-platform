package com.seckill.common.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Redis 常用操作封装：统一 JSON 序列化，避免 RedisTemplate 泛型序列化踩坑。
 * 底层用 StringRedisTemplate，value 是 JSON 字符串，Redis 里可直接读。
 */
@Service
@RequiredArgsConstructor
public class RedisService {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    @SneakyThrows
    public void set(String key, Object value, Duration ttl) {
        String json = value instanceof String s ? s : objectMapper.writeValueAsString(value);
        if (ttl == null) {
            redis.opsForValue().set(key, json);
        } else {
            redis.opsForValue().set(key, json, ttl);
        }
    }

    public void set(String key, Object value) {
        set(key, value, null);
    }

    public String getString(String key) {
        return redis.opsForValue().get(key);
    }

    @SneakyThrows
    public <T> T get(String key, Class<T> clazz) {
        String json = redis.opsForValue().get(key);
        if (json == null) {
            return null;
        }
        return objectMapper.readValue(json, clazz);
    }

    @SneakyThrows
    public <T> T get(String key, TypeReference<T> typeReference) {
        String json = redis.opsForValue().get(key);
        if (json == null) {
            return null;
        }
        return objectMapper.readValue(json, typeReference);
    }

    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redis.hasKey(key));
    }

    public boolean setIfAbsent(String key, String value, Duration ttl) {
        Boolean ok = redis.opsForValue().setIfAbsent(key, value, ttl);
        return Boolean.TRUE.equals(ok);
    }

    public Long increment(String key, long delta) {
        return redis.opsForValue().increment(key, delta);
    }

    public void expire(String key, Duration ttl) {
        redis.expire(key, ttl);
    }

    public void delete(String... keys) {
        if (keys != null && keys.length > 0) {
            redis.delete(List.of(keys));
        }
    }

    public void delete(Collection<String> keys) {
        if (keys != null && !keys.isEmpty()) {
            redis.delete(keys);
        }
    }

    public Set<String> members(String key) {
        Set<String> members = redis.opsForSet().members(key);
        return members == null ? Set.of() : members;
    }

    public List<String> keys(String pattern) {
        Set<String> keys = redis.keys(pattern);
        return keys == null ? new ArrayList<>() : new ArrayList<>(keys);
    }
}
