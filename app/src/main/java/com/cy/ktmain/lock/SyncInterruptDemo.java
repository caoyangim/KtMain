package com.cy.ktmain.lock;

import java.util.concurrent.CountDownLatch;

public class SyncInterruptDemo {
    private final Object lock = new Object();
    private volatile boolean holdingLock;
    private volatile Thread holderThread;
    private volatile Thread waiterThread;
    private volatile Logger logger = System.out::println;

    public interface Logger {
        void log(String message);
    }

    public static void main(String[] args) throws InterruptedException {
        SyncInterruptDemo demo = new SyncInterruptDemo();
        demo.startLockContention();
        Thread.sleep(1_000);
        demo.interruptWaitingThread();
        Thread.sleep(2_000);
        demo.releaseLock();
        Thread.sleep(1_000);
        demo.stop();
    }

    public void setLogger(Logger logger) {
        this.logger = logger == null ? System.out::println : logger;
    }

    public synchronized void startLockContention() {
        stop();
        holdingLock = true;
        CountDownLatch holderAcquired = new CountDownLatch(1);
        log("创建持锁线程：进入 synchronized 后持续占有 monitor");
        holderThread = new Thread(() -> {
            synchronized (lock) {
                log("持锁线程：已获取 synchronized 锁");
                holderAcquired.countDown();
                while (holdingLock) {
                    try {
                        Thread.sleep(250);
                    } catch (InterruptedException e) {
                        log("持锁线程：收到中断，准备释放锁");
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                log("持锁线程：退出 synchronized，锁已释放");
            }
        }, "sync-lock-holder");
        holderThread.start();

        try {
            holderAcquired.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log("主线程：等待持锁线程启动时被中断");
            return;
        }

        waiterThread = new Thread(() -> {
            log("等待线程：尝试进入 synchronized");
            synchronized (lock) {
                log("等待线程：终于进入 synchronized，中断标记=" + Thread.currentThread().isInterrupted());
            }
            log("等待线程：执行结束");
        }, "sync-lock-waiter");
        waiterThread.start();
    }

    public void test() {
        waiterThread = new Thread(() -> {
            log("等待 synchronized 锁...");
            synchronized (lock) {
                // 如果另一个线程一直占有该锁，该线程将无限期死等
                // 外部发出 thread.interrupt() 也完全无法解冻此阻塞！
                log("成功获取到锁");
            }
        });
        waiterThread.start();

        // 即使执行 thread.interrupt();
        // 线程除了抛出中断标志外，依然死死等在锁同步区前，容易在死锁中卡死。
    }

    public void interruptWaitingThread() {
        Thread thread = waiterThread;
        if (thread == null || !thread.isAlive()) {
            log("等待线程：当前没有可中断的等待线程");
            return;
        }
        thread.interrupt();
        log("等待线程：已调用 interrupt()，但若正阻塞在 synchronized 入口，仍不会被唤醒");
    }

    public void releaseLock() {
        holdingLock = false;
        log("请求持锁线程释放 synchronized 锁");
    }

    public synchronized void stop() {
        holdingLock = false;
        if (holderThread != null) {
            holderThread.interrupt();
            holderThread = null;
        }
        if (waiterThread != null) {
            waiterThread.interrupt();
            waiterThread = null;
        }
    }

    public void lock(){
        Thread thread = new Thread(() -> {
            log("等待 synchronized 锁...");
            synchronized (lock) {
                int i = 0;
                while (true) {
                    try {
                        i++;
                        lock.wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    log("当前i:"+i);
                    if (i == 10){
                        Thread.currentThread().interrupt();
                    }
                }
            }
        });
        thread.start();

    }

    private void log(String message) {
        logger.log(message);
    }
}
