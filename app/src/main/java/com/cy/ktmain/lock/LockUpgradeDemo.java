package com.cy.ktmain.lock;

/**
 * JVM 内部的对象头 (Mark Word) 升级演示
 * 无锁 (001) -> 偏向锁 (101) -> 轻量级锁 (00) -> 重量级锁 (10)
 */
public class LockUpgradeDemo {
    private final Object lockObject = new Object();

    public void demystifyUpgrade() {
        // 1. 无锁状态 [Mark Word 结尾值为 001]
        System.out.println("Object created - Unlocked status (001)");

        // 2. 偏向锁 (Biased Lock) [Mark Word 结尾 101, 记录当前 Thread ID]
        // 单个线程 (如 Thread-A) 长期不断访问该同步区
        synchronized (lockObject) {
            System.out.println("Thread-A holds - Biased to Thread-A (101)");
        }

        // 3. 升级到轻量级锁 (Lightweight Lock) [Mark Word 结尾 00, 指向栈帧 Lock Record]
        // 外部发生 Thread-B 访问相同的 lockObject，但两线程交替进行，无实质同时间并发冲突。
        // 此时 JVM 会撤销偏向锁，在 Thread-B 的栈帧开辟 Lock Record 空间，用 CAS 尝试锁。
        new Thread(() -> {
            synchronized (lockObject) {
                System.out.println("Thread-B interacts - Upgraded to Lightweight Lock (00) by CAS!");
            }
        }, "Thread-B").start();
    }
}