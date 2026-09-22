package com.ruoyi.wash.order.controller;

import com.ruoyi.wash.common.api.ApiResult;
import com.ruoyi.wash.common.security.MemberContext;
import com.ruoyi.wash.order.dto.MediaVO;
import com.ruoyi.wash.order.service.WashMediaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * C 端影像。契约：openapi.yaml /api/v1/media/upload、/api/v1/media/{fileId}/raw。
 *
 * <p>读取需要登录：影像属于客户隐私（车牌、车辆外观），不能无鉴权直出。
 */
@RestController
public class WxMediaController {

    @Autowired
    private WashMediaService mediaService;

    @PostMapping("/api/v1/media/upload")
    public ApiResult<MediaVO> upload(@RequestParam("file") MultipartFile file,
                                     @RequestParam("bizType") String bizType,
                                     @RequestParam(value = "orderNo", required = false) String orderNo) {
        return ApiResult.ok(mediaService.upload(file, bizType, orderNo, MemberContext.require(), "CUSTOMER"));
    }

    @GetMapping("/api/v1/media/{fileId}/raw")
    public ResponseEntity<Resource> raw(@PathVariable String fileId) {
        Path path = mediaService.resolve(fileId);
        if (path == null || !Files.exists(path)) {
            return ResponseEntity.notFound().build();
        }
        // Content-Type 必须显式按扩展名给：默认协商会被安全过滤器预置的 application/json 顶掉，
        // 严格客户端（部分 webview）遇到 json 头不渲染图片（bug092201）
        MediaType type = MediaTypeFactory.getMediaType(path.getFileName().toString())
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok().contentType(type).body(new FileSystemResource(path.toFile()));
    }
}
