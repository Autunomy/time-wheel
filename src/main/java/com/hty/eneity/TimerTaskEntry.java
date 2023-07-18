package com.hty.eneity;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import com.hty.eneity.TimerTask;

//定时任务项
@Data
@Slf4j
public class TimerTaskEntry implements Comparable<TimerTaskEntry> {
    //所在的list集合
    volatile TimerTaskList timedTaskList;
    //前一个节点
    TimerTaskEntry next;
    //后一个节点
    TimerTaskEntry prev;
    //具体任务
    private TimerTask timerTask;
    //超时时长
    private long expireMs;

    public TimerTaskEntry(TimerTask timedTask, long expireMs) {
        this.timerTask = timedTask;
        this.expireMs = expireMs;
        this.next = null;
        this.prev = null;
    }

    //将当前节点从对应的list中删除
    void remove() {
        //首先获取到当前的list作为一个标志变量
        TimerTaskList currentList = timedTaskList;
        //使用while循环的原因是防止删除失败
        while (currentList != null) {
            currentList.remove(this);
            currentList = timedTaskList;
        }
    }

    //排序方式是 延时时长长的放在后面 延时时间短的放在前面
    @Override
    public int compareTo(TimerTaskEntry o) {
        return ((int) (this.expireMs - o.expireMs));
    }

}
