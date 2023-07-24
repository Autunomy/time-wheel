package com.hty.eneity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

//真正的任务
@Data
@Slf4j
@NoArgsConstructor
public class TimerTask implements Runnable {

    /**
     * 延时时长
     */
    private long delayMs;
    /**
     * 任务所在的entry
     */
    private TimerTaskEntry timerTaskEntry;

    //这部分就是真实任务信息
    private String desc;

    private String uid;

    public TimerTask(String desc, long delayMs) {
        this.desc = desc;
        this.delayMs = delayMs;
        this.timerTaskEntry = null;
    }

    //获取当前任务对应的entry对象
    public TimerTaskEntry getTimerTaskEntry() {
        return timerTaskEntry;
    }

    //设置当前任务对应的entry为传递过来的entry
    public synchronized void setTimerTaskEntry(TimerTaskEntry entry) {
        // 如果这个TimerTask已经被一个已存在的TimerTaskEntry持有,先移除一个
        if (timerTaskEntry != null && timerTaskEntry != entry) {
            timerTaskEntry.remove();
        }
        timerTaskEntry = entry;
    }

    //任务具体执行部分
    @Override
    public void run() {
        log.info("============={}任务执行", desc);
    }

}
