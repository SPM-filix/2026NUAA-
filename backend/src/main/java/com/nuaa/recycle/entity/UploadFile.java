package com.nuaa.recycle.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("upload_file")
public class UploadFile {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String url;
    private String storagePath;
    private Integer size;
    private String mime;
    private String scene;
    private String uploadedByType;
    private Long uploadedById;
    private LocalDateTime createdAt;
}
