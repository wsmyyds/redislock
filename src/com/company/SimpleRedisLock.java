package com.company;


import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.SetParams;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @program: redislock
 * @description:
 * @author:
 * @create:
 **/
public class SimpleRedisLock {

    /*** key*/
    private final String LOCK_KEY = "redis_lock";
    /*** 锁失效时间,避免线程异常未释放锁造成死锁,long不能转为string*/
    private final String LOCK_LEASE_TIME = "300";
    /*** 获取锁的最大等待时间*/
    private final long TIMEOUT = 1000;
    /*** lockScript的SHA1码*/
    private final String lockScriptSHA1;
    /*** unlockScript的SHA1码*/
    private final String unlockScriptSHA1;
    /**SetParams设置多个参数属性，如果使用string类型时可以直接设置key过期时间，但使用string不可重入*/
    private SetParams params = SetParams.setParams().nx().ex(Integer.parseInt(LOCK_LEASE_TIME));
    /*** 当前时间*/
    private static long currentTime;
    //不能这样获取
    //private static Jedis jedis = new JedisPool(new GenericObjectPoolConfig(),"101.43.35.219", 6379,1000,"root").getResource();
    /*** 连接池*/
    private static JedisPool JedisPool = new JedisPool(new GenericObjectPoolConfig(), "101.43.35.219", 6379, 1000, "root");

    static {
        new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                while (true) {
                    currentTime = System.currentTimeMillis();
                }
            }
        }.start();
    }

    public SimpleRedisLock() {

        //相同script会获得相同的sha1码,注意script语句空格，或者加;
        lockScriptSHA1 = JedisPool.getResource().scriptLoad(
                "local keyisexit = redis.call('HLEN',KEYS[1])" +
                        "if tonumber(keyisexit) == 0 then" +
                        "  redis.call('HSET',KEYS[1],ARGV[1],'1')" +
                        "  return redis.call('expire',KEYS[1],ARGV[2])" +
                        "elseif tonumber(keyisexit) == 1 then" +
                        "  local num = redis.call('HGET',KEYS[1],ARGV[1])" +
                        "  if num then" +
                        "   redis.call('HINCRBY',KEYS[1],ARGV[1],'1')" +
                        "   return redis.call('expire',KEYS[1],ARGV[2])" +
                        "  else" +
                        "   return 0" +
                        " end " +
                        "end"
        );

        unlockScriptSHA1 = JedisPool.getResource().scriptLoad(
                "local num = redis.call('HGET',KEYS[1],ARGV[1])"+
                        "if num then"+
                        " if tonumber(num)==1 then"+
                        " return redis.call('HDEL',KEYS[1],ARGV[1])"+
                        " elseif tonumber(num)>1 then"+
                        " return redis.call('HINCRBY',KEYS[1],ARGV[1],'-1')"+
                        " end "+
                        "else"+
                        " return 0 "+
                        "end"
        );
        //如果使用String释放锁
        //unlockScriptSHA1 = JedisPool.getResource().scriptLoad("if redis.call('get',KEYS[1]) == ARGV[1] then" + "   return redis.call('del',KEYS[1])" + "else" + "   return 0 " + "end");
//        System.out.println(lockScriptSHA1);
//        System.out.println(unlockScriptSHA1);
    }

    /**
     * @param id 同id才能解同id的锁
     * @return
     */
    public boolean lock(String id) {


        Jedis jedis = JedisPool.getResource();
        try {

            long startTime = System.currentTimeMillis();
            List<String> list = new ArrayList<>(2);
            list.add(id);
            list.add(LOCK_LEASE_TIME);
            while (true) {
                //直接set并设置过期时间
                /***String lock = jedis.set(LOCK_KEY, id, params);*/
                String lock = jedis.evalsha(lockScriptSHA1, Collections.singletonList(LOCK_KEY), list).toString();
                if ("1".equals(lock)) {
                    return true;
                }
                //持续请求，直到请求超时
                long getLockTime = currentTime - startTime;
                if (getLockTime >= TIMEOUT) {
                    return false;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        } finally {
            jedis.close();
        }

    }

    public boolean unlock(String id) {

        Jedis jedis = JedisPool.getResource();
        //String unlockScript = "if redis.call('get',KEYS[1]) == ARGV[1] then" + "   return redis.call('del',KEYS[1])" + "else" + "   return 0 " + "end";
        try {

            //String res = jedis.eval(unlockScript, Collections.singletonList(LOCK_KEY), Collections.singletonList(id)).toString();
            //eval执行lua脚本（是一个原子性操作,减少网络开销）
            String res = jedis.evalsha(unlockScriptSHA1, Collections.singletonList(LOCK_KEY), Collections.singletonList(id)).toString();
            return "1".equals(res) ? true : false;
        } finally {
            jedis.close();
        }
    }

}
//Q
//不同类线程池是共用的吗
//注意日志包
