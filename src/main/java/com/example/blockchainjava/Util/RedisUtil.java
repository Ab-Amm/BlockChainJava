package com.example.blockchainjava.Util;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.Set;

public class RedisUtil {
    private static final JedisPool jedisPool;
    private static final String BALANCE_KEY_PREFIX = "user:balance:";

    static {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(5);
        poolConfig.setMinIdle(1);
        jedisPool = new JedisPool(poolConfig, "localhost", 6379);
    }

    public static void setUserBalance(int userId, double balance) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = BALANCE_KEY_PREFIX + userId;
            jedis.set(key, String.valueOf(balance));
        }
    }

    public static Double getUserBalance(int userId) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = BALANCE_KEY_PREFIX + userId;
            String balance = jedis.get(key);
            return balance != null ? Double.parseDouble(balance) : null;
        }
    }
    public static void shutdown() {
        if (jedisPool != null) {
            jedisPool.close();
        }
    }
}
