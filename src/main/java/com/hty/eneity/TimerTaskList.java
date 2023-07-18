package com.hty.eneity;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

//定时任务列表 每一个TimerTaskList类都是时间轮中的一格
@Data
@Slf4j
public class TimerTaskList implements Delayed {

    /**
     * TimerTaskList 环形链表使用一个虚拟根节点root 这个根结点只是标志作用，不存储任何任务
     */
    private TimerTaskEntry root = new TimerTaskEntry(null, -1);
    /**
     * 当前taskList的过期时间 也就是需要延时的时长
     */
    private AtomicLong expiration = new AtomicLong(-1L);

    {
        root.next = root;
        root.prev = root;
    }

    /***
     * 获取延时时长
     * @return
     */
    public long getExpiration() {
        return expiration.get();
    }

    /**
     * 设置bucket的过期时间,设置成功返回true
     *
     * @param expirationMs
     * @return
     */
    public boolean setExpiration(long expirationMs) {
        return expiration.getAndSet(expirationMs) != expirationMs;
    }

    /**
     * 向timerTaskList中添加任务
     * @param entry
     * @return
     */
    public boolean addTask(TimerTaskEntry entry) {
        boolean done = false;
        while (!done) {
            // 如果TimerTaskEntry已经在别的list中就先移除,同步代码块外面移除,避免死锁,一直到成功为止
            entry.remove();
            synchronized (this) {//将当前的这个list锁定，防止别的线程操作，导致链表的指针错乱
                if (entry.timedTaskList == null) {
                    // 加到链表的末尾
                    entry.timedTaskList = this;
                    TimerTaskEntry tail = root.prev;
                    entry.prev = tail;
                    entry.next = root;
                    tail.next = entry;
                    root.prev = entry;
                    done = true;
                }
            }
        }
        return true;
    }

    /**
     * 从 TimedTaskList 移除指定的 timerTaskEntry
     * @param entry 任务节点
     */
    public void remove(TimerTaskEntry entry) {
        synchronized (this) {//涉及到链表指针的变化都需要上锁
            if (entry.getTimedTaskList().equals(this)) {
                entry.next.prev = entry.prev;
                entry.prev.next = entry.next;
                entry.next = null;
                entry.prev = null;
                entry.timedTaskList = null;
            }
        }
    }

    /**
     * 移除当前list中的全部节点
     */
    public synchronized void clear(Consumer<TimerTaskEntry> entry) {
        TimerTaskEntry head = root.next;
        while (!head.equals(root)) {
            remove(head);//从当前双向循环链表中移除任务列表
            entry.accept(head);
            head = root.next;
        }
        expiration.set(-1L);
    }

    //剩余时间的计算方法
    @Override
    public long getDelay(TimeUnit unit) {
        return Math.max(0, unit.convert(expiration.get() - System.currentTimeMillis(), TimeUnit.MILLISECONDS));
    }

    //比较方法，剩余时间最少的在队头，剩余时间最多的在队尾
    @Override
    public int compareTo(Delayed o) {
        if (o instanceof TimerTaskList) {
            return Long.compare(expiration.get(), ((TimerTaskList) o).expiration.get());
        }
        return 0;
    }
}