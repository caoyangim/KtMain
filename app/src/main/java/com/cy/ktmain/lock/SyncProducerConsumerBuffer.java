package com.cy.ktmain.lock;

public class SyncProducerConsumerBuffer {
    private final Object lock = new Object();
    private int count = 0;

    public void produce() throws InterruptedException {
        synchronized (lock) {
            while (count == 100) {
                lock.wait(); // 满，生产者等待
            }
            count++;
            // 唤醒所有线程。只能用 notifyAll，否则若只唤醒了同类生产者，系统可能会陷入全等死锁。
            // 但这样会把所有等待的生产者也唤醒，徒增CPU切换。
            lock.notifyAll(); 
        }
    }

    public void consume() throws InterruptedException {
        synchronized (lock) {
            while (count == 0) {
                lock.wait(); // 空，消费者等待
            }
            count--;
            lock.notifyAll(); // 无法精确定向唤醒生产者，只得全部呼唤
        }
    }
}