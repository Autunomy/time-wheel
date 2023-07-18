package com.hty.demo;

import com.hty.eneity.TimerTask;
import com.hty.service.Timer;
import com.hty.service.TimerLauncher;

public class Demo1 {
    public static void main(String[] args) {
        Timer timer = new TimerLauncher();
        timer.add(new TimerTask("(这是一个定时任务)",2000));
    }
}
