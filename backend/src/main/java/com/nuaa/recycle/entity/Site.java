package com.nuaa.recycle.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("site")
public class Site {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String address;
    private String openTime;
    private String manager;
    private String phone;
    private String status;
}
