package com.company;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author
 */
public class Main {

    private static AtomicInteger atomicInteger = new AtomicInteger(1);
    private static int ticket = 100;
    static SimpleRedisLock redisLock = new SimpleRedisLock();
    static LinkedBlockingDeque blockingDeque = new LinkedBlockingDeque();

    public static void main(String[] args) {

        ThreadPoolExecutor threadPoolExecutor =
                new ThreadPoolExecutor(5, 10, 10L, SECONDS, blockingDeque, r -> {
                    //System.out.println("-----");
                    return new Thread(Thread.currentThread().getThreadGroup(), r, "thread-" + atomicInteger.getAndIncrement());
                });
        for (int i = 0; i < 10; i++) {
            threadPoolExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    while (ticket > 0) {
                        String currentThreadName = Thread.currentThread().getName();
                        if (redisLock.lock(currentThreadName)) {
                            System.out.println(currentThreadName + "获得锁");
                            ticket--;
                            System.out.println(currentThreadName + "ticket= " + ticket);
                        }
                        if (redisLock.lock(currentThreadName)) {
                            System.out.println(currentThreadName + "再次获得锁");
                            ticket--;
                            System.out.println(currentThreadName + "ticket= " + ticket);
                        }
                        if (redisLock.unlock(currentThreadName)) {
                            System.out.println(currentThreadName + "释放锁");
                        }
                        if (redisLock.unlock(currentThreadName)) {
                            System.out.println(currentThreadName + "释放锁");
                        }
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    }
                }
            });
        }


    }
}
