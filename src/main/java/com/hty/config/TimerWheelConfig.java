package com.hty.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimerWheelConfig {
    @Value("${time-wheel.threadPool.corePoolSizeAuto}")
    public Boolean corePoolSizeAuto;

    @Value("${time-wheel.threadPool.corePoolSize}")
    public Integer corePoolSize;

    /***
     * 获取线程池的核心线程数
     * @return
     */
    public Integer getCoreNum(){
        if (corePoolSizeAuto){
            return Runtime.getRuntime().availableProcessors() + 1;
        }
        return corePoolSize > 0 ? corePoolSize : 1;
    }
}
