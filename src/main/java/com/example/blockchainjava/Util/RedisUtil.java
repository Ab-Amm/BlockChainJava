package com.example.blockchainjava.Util;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.util.Set;

public class RedisUtil {
    private static final JedisPool jedisPool;
    private static final String BALANCE_KEY_PREFIX = "user:balance:";
    private static final String PUBLIC_KEY_BALANCE_KEY_PREFIX = "publickey:balance:";

    static {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(5);
        poolConfig.setMinIdle(1);
        jedisPool = new JedisPool(poolConfig, "localhost", 6379);
    }

    public static void setUserBalance(int userId, double balance) {
        try (Jedis jedis = jedisPool.getResource()) {
            // Update user ID based balance
            String userKey = BALANCE_KEY_PREFIX + userId;
            jedis.set(userKey, String.valueOf(balance));
            
            // Find and update public key based balance if it exists
            String publicKey = getUserPublicKey(userId);
            if (publicKey != null) {
                String publicKeyKey = PUBLIC_KEY_BALANCE_KEY_PREFIX + publicKey;
                jedis.hset(publicKeyKey, "balance", String.valueOf(balance));
            }
        }
    }

    public static Double getUserBalance(int userId) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = BALANCE_KEY_PREFIX + userId;
            String balance = jedis.get(key);
            if (balance != null) {
                return Double.parseDouble(balance);
            } else {
                // Fall back to database if Redis returns null
                // Assuming a method getBalanceFromDatabase exists
                return getBalanceFromDatabase(userId);
            }
        }
    }

    public static void setPublicKeyBalance(String publicKey, int userId, double balance) {
        try (Jedis jedis = jedisPool.getResource()) {
            // Update public key based balance
            String publicKeyKey = PUBLIC_KEY_BALANCE_KEY_PREFIX + publicKey;
            jedis.hset(publicKeyKey, "userId", String.valueOf(userId));
            jedis.hset(publicKeyKey, "balance", String.valueOf(balance));
            
            // Update user ID based balance
            String userKey = BALANCE_KEY_PREFIX + userId;
            jedis.set(userKey, String.valueOf(balance));
        }
    }

    public static Integer getUserIdByPublicKey(String publicKey) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = PUBLIC_KEY_BALANCE_KEY_PREFIX + publicKey;
            String userId = jedis.hget(key, "userId");
            return userId != null ? Integer.parseInt(userId) : null;
        }
    }

    public static Double getBalanceByPublicKey(String publicKey) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = PUBLIC_KEY_BALANCE_KEY_PREFIX + publicKey;
            String balance = jedis.hget(key, "balance");
            if (balance != null) {
                return Double.parseDouble(balance);
            } else {
                // Fall back to database if Redis returns null
                // Assuming a method getBalanceFromDatabaseByPublicKey exists
                return getBalanceFromDatabaseByPublicKey(publicKey);
            }
        }
    }

    private static String getUserPublicKey(int userId) {
        try (Jedis jedis = jedisPool.getResource()) {
            // Iterate over all keys with the public key prefix to find the matching userId
            Set<String> keys = jedis.keys(PUBLIC_KEY_BALANCE_KEY_PREFIX + "*");
            for (String key : keys) {
                String storedUserId = jedis.hget(key, "userId");
                if (storedUserId != null && storedUserId.equals(String.valueOf(userId))) {
                    return key.substring(PUBLIC_KEY_BALANCE_KEY_PREFIX.length());
                }
            }
        }
        return null;
    }

    public static void shutdown() {
        if (jedisPool != null) {
            jedisPool.close();
        }
    }

    // Assuming these methods exist to retrieve balance from database
    private static Double getBalanceFromDatabase(int userId) {
        // Implement database retrieval logic here
        return null;
    }

    private static Double getBalanceFromDatabaseByPublicKey(String publicKey) {
        // Implement database retrieval logic here
        return null;
    }
}
