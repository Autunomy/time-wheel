package com.hty.config;


import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Configuration
public class ThreadPoolConfig {
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
