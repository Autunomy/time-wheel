package com.hty.demo;

import com.hty.eneity.TimerTask;
import com.hty.service.Timer;
import com.hty.service.TimerLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Demo1 {

    @RequestMapping("/demo1")
    public void demo1(){
//        System.out.println(threadPoolConfig.corePoolSizeAuto);
    }
}
