package com.water.flowable.entity;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author: xu.dm
 * @Date: 2020/9/27 15:40
 * @Version: 1.0
 * @Description: TODO
 **/
@Data
public class HolidayRequest implements Serializable {
    private String id;
    private String userName;
    private Integer nrOfHolidays;
    private String description;
    private Double fee;
}
