package com.cy.ktmain.lock;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class LockConditionBuffer {
    private final ReentrantLock lock = new ReentrantLock();
    // 从同一个 ReentrantLock 锁派生出两个精确条件对象
    private final Condition notFull  = lock.newCondition(); 
    private final Condition notEmpty = lock.newCondition();

    private int count = 0;

    public void produce() throws InterruptedException {
        lock.lock();
        try {
            while (count == 100) {
                notFull.await(); // 缓冲区满，【生产者】队列精准休眠挂起
            }
            count++;
            // 【精确唤醒】：只去唤醒因为没有数据而挂起的【消费者】线程
            notEmpty.signal(); 
        } finally {
            lock.unlock();
        }
    }

    public void consume() throws InterruptedException {
        lock.lock();
        try {
            while (count == 0) {
                notEmpty.await(); // 缓冲区空，【消费者】队列精准休眠挂起
            }
            count--;
            // 【精确唤醒】：只去通知因为格子满了而阻塞的【生产者】线程
            notFull.signal(); 
        } finally {
            lock.unlock();
        }
    }
}