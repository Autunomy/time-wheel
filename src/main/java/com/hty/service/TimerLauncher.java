package com.hty.service;

import com.hty.config.ThreadPoolConfig;
import com.hty.eneity.TimeWheel;
import com.hty.eneity.TimerTaskEntry;
import com.hty.eneity.TimerTaskList;
import com.hty.eneity.TimerTask;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

//定时器的具体实现
@Data
@Slf4j
@Service
public class TimerLauncher implements Timer {

    /**
     * 底层时间轮
     */
    private TimeWheel timeWheel;
    /**
     * 一个定时器Timer只有一个延时队列
     */
    private DelayQueue<TimerTaskList> delayQueue = new DelayQueue<>();
    /**
     * 过期任务执行线程 多线程执行
     */
    private ExecutorService workerThreadPool;
    /**
     * 轮询delayQueue获取过期任务线程
     */
    private ExecutorService bossThreadPool;
    //构造方法注入
    private ThreadPoolConfig threadPoolConfig;


    public TimerLauncher(ThreadPoolConfig threadPoolConfig) {
        this.threadPoolConfig = threadPoolConfig;
        //创建时间轮链的头，每一格是1ms 格子数量为20，也就是一圈是20ms,当前指针指向的时间就是当前的系统时间 delayQueue就是延时队列
        this.timeWheel = new TimeWheel(1, 20, System.currentTimeMillis(), delayQueue);
        //任务执行线程池创建 核心数由配置文件配置
        this.workerThreadPool = Executors.newFixedThreadPool(threadPoolConfig.getCoreNum());
        //用来推动时间轮运转的线程池
        this.bossThreadPool = Executors.newFixedThreadPool(1);
        // 以1ms为单位时间推动一次时间轮
        this.bossThreadPool.submit(() -> {
            while (true) {
                this.advanceClock(1);
            }
        });
    }

    //向时间轮链中添加entry节点 这个方法主要是进行了一个封装，方便下面的add方法调用
    public void addTimerTaskEntry(TimerTaskEntry entry) {
        if (!timeWheel.add(entry)) {//如果返回了false则代表当前任务需要立即执行
            // 任务已到期
            TimerTask timerTask = entry.getTimerTask();
            log.info("=====任务:{} 已到期,准备执行============", timerTask.getDesc());
            workerThreadPool.submit(timerTask);
        }
    }

    //向时间轮链中添加任务
    @Override
    public void add(TimerTask timerTask) {
        log.info("=======添加任务开始====task:{}", timerTask.getDesc());
        TimerTaskEntry entry = new TimerTaskEntry(timerTask, timerTask.getDelayMs() + System.currentTimeMillis());
        timerTask.setTimerTaskEntry(entry);//设置自己的entry
        addTimerTaskEntry(entry);//添加entry
    }

    /**
     * 推动指针运转获取过期任务
     *
     * @param timeout 时间间隔
     * @return
     */
    @Override
    public synchronized void advanceClock(long timeout) {
        try {
            //获取到延时时间已经到达的整个任务列表
            TimerTaskList bucket = delayQueue.poll(timeout, TimeUnit.MILLISECONDS);
            if (bucket != null) {
                // 推进当前时间轮以及当前时间轮更底层的时间轮的指针
                timeWheel.advanceLock(bucket.getExpiration());
                // 执行过期任务(包含降级) this::addTimerTaskEntry就是将bucket中的所有entry都传递给addTimerTaskEntry去执行
                bucket.clear(this::addTimerTaskEntry);
            }
        } catch (InterruptedException e) {
            log.error("advanceClock error");
        }
    }

    @Override
    public int size() {
        return 10;
    }

    @Override
    public void shutdown() {
        this.bossThreadPool.shutdown();
        this.workerThreadPool.shutdown();
        this.timeWheel = null;
    }
}