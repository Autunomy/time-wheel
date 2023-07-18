package com.hty.eneity;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.DelayQueue;

//时间轮主体
@Data
@Slf4j
public class TimeWheel {

    /**
     * 基本时间跨度
     */
    private long tickMs;
    /**
     * 时间单位个数
     */
    private int wheelSize;
    /**
     * 总体时间跨度
     */
    private long interval;
    /**
     * 当前所处时间
     */
    private long currentTime;
    /**
     * 定时任务列表，每一个TimerTaskList都是时间轮中的一格
     */
    private TimerTaskList[] buckets;
    /**
     * 上层时间轮 单例的，利用双检锁来获取
     */
    private volatile TimeWheel overflowWheel;
    /**
     * 一个Timer只有一个DelayQueue,协助推进时间轮 这个队列中每一个TimerTaskList都是时间轮中的一格
     */
    private DelayQueue<TimerTaskList> delayQueue;


    public TimeWheel(long tickMs, int wheelSize, long currentTime, DelayQueue<TimerTaskList> delayQueue) {
        this.tickMs = tickMs;
        this.wheelSize = wheelSize;
        this.interval = tickMs * wheelSize;
        this.currentTime = currentTime;
        this.buckets = new TimerTaskList[wheelSize];
        this.currentTime = currentTime - (currentTime % tickMs);
        this.delayQueue = delayQueue;
        for (int i = 0; i < wheelSize; i++) {
            //创建一个时间轮中的每一格对象
            buckets[i] = new TimerTaskList();
        }
    }

    //将新的一个定时任务加入到对应的时间格中 返回false表示任务添加失败 需要立即执行 返回true表示任务成功放入定时器中
    public boolean add(TimerTaskEntry entry) {
        long expiration = entry.getExpireMs();//获取任务的超时时间
        if (expiration < tickMs + currentTime) {//如果超时时间 小于 指针指向的时间+一格的时间
            // 定时任务到期 返回false 在调用该方法的方法中需要立即执行此任务
            return false;
        } else if (expiration < currentTime + interval) { //如果超时时间 小于 指针指向的时间+整个时间轮的时间长度
            // 扔进当前时间轮的某个槽里,只有时间大于某个槽,才会放进去
            long virtualId = (expiration / tickMs);//获取格数

            //获取真实的格子位置
            int index = (int) (virtualId % wheelSize);
            //获取格子位置对应的任务集合
            TimerTaskList bucket = buckets[index];
            bucket.addTask(entry);//将任务添加到集合中
            // 设置bucket 过期时间 加if是为了防止更新失败，更新成功时setExpiration方法返回true
            if (bucket.setExpiration(virtualId * tickMs)) {
                // 设好过期时间的bucket需要入队
                delayQueue.offer(bucket);
                return true;
            }
        } else {//当前超时时间已经不能使用当前时间轮的时长来表示，需要扔到更高级的时间轮中执行
            // 当前轮不能满足,需要扔到上一轮
            TimeWheel timeWheel = getOverflowWheel();
            return timeWheel.add(entry);
        }
        return false;
    }

    //获取当前时间轮的前一个时间轮 每一个时间轮是一个单例的，这里使用双检锁来获取，保证了线程安全
    private TimeWheel getOverflowWheel() {
        if (overflowWheel == null) {//第一轮判空提高效率
            synchronized (this) {
                if (overflowWheel == null) {//第二轮判空检查是否真实为空
                    overflowWheel = new TimeWheel(interval, wheelSize, currentTime, delayQueue);
                }
            }
        }
        return overflowWheel;
    }

    /**
     * 推进所有时间轮的指针
     * @param timestamp timerTaskList的过期时间
     */
    public void advanceLock(long timestamp) {
        if (timestamp > currentTime + tickMs) {//过期时间 大于 指针指向的时间+一格时间
            //更新当前指针指向的时间 并且一定要是tickMs的整数倍
            currentTime = timestamp - (timestamp % tickMs);
            if (overflowWheel != null) {//如果当前时间轮不是精度最高的时间轮(不是最底层时间轮)
                //进行递归更底层的时间轮
                this.getOverflowWheel().advanceLock(timestamp);
            }
        }
    }

}