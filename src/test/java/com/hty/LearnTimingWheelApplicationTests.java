package com.hty;

import com.hty.demo.Demo1;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class LearnTimingWheelApplicationTests {
    @Autowired
    Demo1 demo1;
    @Test
    void contextLoads() {
        demo1.demo1();
    }

}
