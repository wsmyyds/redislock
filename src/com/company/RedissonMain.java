package com.company;

import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;

/**
 * @program: redislock
 * @description:
 * @author:
 * @create:
 **/
public class RedissonMain {

    public static void main(String[] args) {

        Config config = new Config();
        config.useSingleServer().setAddress("redis://101.43.35.219:6379");
        config.useSingleServer().setPassword("root");
        config.setCodec(JsonJacksonCodec.INSTANCE);
        final RedissonClient redissonClient = Redisson.create(config);
        RLock lock = redissonClient.getLock("r_lock");
        new Thread(){
            @Override
            public void run() {
                lock.lock();
            }
        }.start();
        lock.lock();
        lock.lock();
        lock.lock();
        System.out.println(lock);
        lock.unlock();
        lock.unlock();
        lock.unlock();

    }
}
