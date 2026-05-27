package com.cy.ktmain.lock;

public class SyncInterruptDemo {
    private final Object lock = new Object();

    public static void main(String[] args) throws InterruptedException {

        SyncInterruptDemo demo = new SyncInterruptDemo();
        demo.lock();
        demo.test();
        Thread.sleep(20_000);
    }

    public void test() {
        Thread thread = new Thread(() -> {
            System.out.println("等待 synchronized 锁...");
            synchronized (lock) {
                // 如果另一个线程一直占有该锁，该线程将无限期死等
                // 外部发出 thread.interrupt() 也完全无法解冻此阻塞！
                System.out.println("成功获取到锁");
            }
        });
        thread.start();
        
        // 即使执行 thread.interrupt();
        // 线程除了抛出中断标志外，依然死死等在锁同步区前，容易在死锁中卡死。
    }

    void lock(){
        Thread thread = new Thread(() -> {
            System.out.println("等待 synchronized 锁...");
            synchronized (lock) {
                int i = 0;
                while (true) {
                    try {
                        i++;
                        lock.wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    System.out.println("当前i:"+i);
                    if (i == 10){
                        Thread.currentThread().interrupt();
                    }
                }
            }
        });
        thread.start();

    }
}