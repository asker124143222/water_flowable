package com.water.flowable;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;

/**
 * @Author: xu.dm
 * @Date: 2020/10/9 11:18
 * @Version: 1.0
 * @Description: TODO
 **/
@SpringBootTest
public class testApplication {

    @Test
    void test(){
        System.out.println(Arrays.asList("kermit", "gonzo", "fozzie"));
    }
}
