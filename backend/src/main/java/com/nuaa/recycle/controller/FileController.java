package com.nuaa.recycle.controller;

import com.nuaa.recycle.common.R;
import com.nuaa.recycle.entity.UploadFile;
import com.nuaa.recycle.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class FileController {

    @Autowired
    private FileService fileService;

    @PostMapping("/api/files")
    public R<UploadFile> upload(@RequestParam("file") MultipartFile file, @RequestParam String scene) throws Exception {
        return R.ok(fileService.upload(file, scene));
    }
}
