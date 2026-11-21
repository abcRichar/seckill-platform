package com.seckill.upload;

import com.seckill.common.annotation.RequirePerm;
import com.seckill.common.exception.BizException;
import com.seckill.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 图片上传：随机文件名 + 扩展名白名单 + 按日期分目录。
 * 生产环境应直接传 OSS/MinIO，不要把文件写在本机磁盘。
 */
@Slf4j
@Tag(name = "管理端-上传")
@RestController
@RequestMapping("/api/admin/upload")
@RequiredArgsConstructor
public class UploadController {

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp", "svg");

    @Value("${seckill.upload-dir}")
    private String uploadDir;

    @Operation(summary = "上传图片")
    @RequirePerm("product:edit")
    @PostMapping
    public R<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BizException.badRequest("请选择要上传的图片");
        }
        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        }
        if (!ALLOWED_EXT.contains(ext)) {
            throw BizException.badRequest("仅支持 jpg/png/gif/webp/svg 格式");
        }
        try {
            String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
            Path dir = Paths.get(uploadDir, date).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            // 只用随机名，不拼接用户文件名，避免路径穿越
            String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            file.transferTo(dir.resolve(filename).toFile());
            String url = "/uploads/" + date + "/" + filename;
            log.info("图片上传成功 {}", url);
            return R.ok(Map.of("url", url));
        } catch (Exception e) {
            log.error("图片上传失败", e);
            throw new BizException("图片上传失败");
        }
    }
}
