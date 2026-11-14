package com.nuaa.recycle.service;

import com.nuaa.recycle.common.BizException;
import com.nuaa.recycle.common.UserContext;
import com.nuaa.recycle.entity.UploadFile;
import com.nuaa.recycle.mapper.UploadFileMapper;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
public class FileService {

    @Value("${upload.path:/uploads}")
    private String uploadPath;

    private static final Set<String> ALLOWED_TYPES = new HashSet<>(Arrays.asList("image/jpeg", "image/png", "image/webp"));
    private static final long MAX_SIZE = 2 * 1024 * 1024;

    @Autowired
    private UploadFileMapper uploadFileMapper;

    public UploadFile upload(MultipartFile file, String scene) throws IOException {
        if (file == null || file.isEmpty()) throw new BizException(1001, "文件不能为空");
        if (!ALLOWED_TYPES.contains(file.getContentType())) throw new BizException(4001, "文件类型不支持，仅支持 jpg/png/webp");
        if (file.getSize() > MAX_SIZE) throw new BizException(4002, "文件超过 2MB 限制");

        String ext = getExtension(file.getOriginalFilename());
        if (ext == null || (!ext.equalsIgnoreCase("jpg") && !ext.equalsIgnoreCase("jpeg") && !ext.equalsIgnoreCase("png") && !ext.equalsIgnoreCase("webp"))) {
            throw new BizException(4001, "文件类型不支持");
        }

        String dir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        Path targetDir = Paths.get(uploadPath, dir);
        if (!Files.exists(targetDir)) Files.createDirectories(targetDir);

        String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext.toLowerCase();
        Path targetPath = targetDir.resolve(filename);
        file.transferTo(targetPath.toFile());

        String url = "/uploads/" + dir.replace("/", "/") + "/" + filename;

        UploadFile uf = new UploadFile();
        uf.setUrl(url);
        uf.setStoragePath(targetPath.toString());
        uf.setSize((int) file.getSize());
        uf.setMime(file.getContentType());
        uf.setScene(scene);
        uf.setUploadedByType(UserContext.getRole());
        uf.setUploadedById(UserContext.getUserId());
        uf.setCreatedAt(LocalDateTime.now());
        uploadFileMapper.insert(uf);
        return uf;
    }

    private String getExtension(String filename) {
        if (filename == null) return null;
        int idx = filename.lastIndexOf('.');
        return idx > 0 ? filename.substring(idx + 1) : null;
    }
}
