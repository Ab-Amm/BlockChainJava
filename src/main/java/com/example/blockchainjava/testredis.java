package com.example.blockchainjava;

import redis.clients.jedis.Jedis;

public class testredis {
    public static void main(String[] args) {
        // Connect to Redis running on localhost and the default port 6379
        try (Jedis jedis = new Jedis("localhost", 6379)) {
            // Test the connection
            System.out.println("Connection successful: " + jedis.ping());

            // Set and Get Example
            jedis.set("exampleKey", "Hello, Redis!");
            String value = jedis.get("exampleKey");
            System.out.println("Retrieved value: " + value);

            // Increment Example
            jedis.set("counter", "0");
            jedis.incr("counter");
            System.out.println("Counter: " + jedis.get("counter"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
