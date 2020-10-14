package com.water.flowable;

import com.water.flowable.config.AppDispatcherServletConfiguration;
import com.water.flowable.config.ApplicationConfiguration;
import org.flowable.ui.modeler.conf.DatabaseConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * @Author: xu.dm
 * @Date: 2020/9/27 14:18
 * @Version: 1.0
 * @Description: TODO
 **/
@Import({
        ApplicationConfiguration.class,
        AppDispatcherServletConfiguration.class,
        // 引入 DatabaseConfiguration 表更新转换
        DatabaseConfiguration.class
})
@SpringBootApplication(exclude={SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class})
public class WaterApplication {
    public static void main(String[] args) {
        SpringApplication.run(WaterApplication.class,args);
    }
}
