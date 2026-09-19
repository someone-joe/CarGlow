package com.ruoyi.wash.order.service;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.order.domain.WashMedia;
import com.ruoyi.wash.order.dto.MediaVO;
import com.ruoyi.wash.order.mapper.WashMediaMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 影像上传（B3）。
 *
 * <p>存储可切换：当前实现是本地磁盘，将来换 COS/OSS 只改 save() 一处，
 * 接口与表结构都不动（store_path 存的始终是"相对路径"）。
 *
 * <p>安全：对外只暴露 fileId；读取时由 fileId 反查库里的相对路径，再与根目录拼接，
 * 并对拼接结果做 normalize + startsWith 校验，杜绝 ../ 路径穿越。
 */
@Service
public class WashMediaService {

    private static final Logger log = LoggerFactory.getLogger(WashMediaService.class);
    private static final Set<String> BIZ_TYPES = Set.of("PARK", "PICK", "WASHED", "RETURN", "COMPARE", "VIDEO");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Autowired
    private WashMediaMapper mediaMapper;

    @Value("${wash.media.store-path:./upload/media}")
    private String storePath;

    @Value("${wash.media.max-size-mb:10}")
    private long maxSizeMb;

    /** 上传并登记。返回 MediaVO（含访问 url，前端只认 fileId） */
    public MediaVO upload(MultipartFile file, String bizType, String orderNo, Long memberId, String uploadBy) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(ErrorCode.A0001, "文件为空");
        }
        if (bizType == null || !BIZ_TYPES.contains(bizType)) {
            throw new ApiException(ErrorCode.A0001, "未知影像类型：" + bizType);
        }
        long maxBytes = maxSizeMb * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new ApiException(ErrorCode.A0001, "文件超过 " + maxSizeMb + "MB 上限");
        }

        String fileId = UUID.randomUUID().toString().replace("-", "");
        String ext = extensionOf(file.getOriginalFilename());
        // 相对路径：按天分目录，避免单目录文件过多
        String relative = LocalDate.now().format(DAY) + "/" + fileId + ext;

        save(file, relative);

        WashMedia media = new WashMedia();
        media.setFileId(fileId);
        media.setBizType(bizType);
        media.setOrderNo(orderNo);
        media.setMemberId(memberId);
        media.setFileName(file.getOriginalFilename());
        media.setStorePath(relative);
        media.setFileSize(file.getSize());
        media.setUploadBy(uploadBy);
        mediaMapper.insertMedia(media);

        log.info("[影像上传] fileId={} bizType={} orderNo={} size={}", fileId, bizType, orderNo, file.getSize());

        MediaVO vo = new MediaVO();
        vo.setFileId(fileId);
        vo.setBizType(bizType);
        vo.setUrl("/api/v1/media/" + fileId + "/raw");
        vo.setUploadTime(System.currentTimeMillis());
        return vo;
    }

    /** 按 fileId 定位真实文件；找不到返回 null（由控制器决定 404） */
    public Path resolve(String fileId) {
        WashMedia media = mediaMapper.selectByFileId(fileId);
        if (media == null) {
            return null;
        }
        Path root = Paths.get(storePath).toAbsolutePath().normalize();
        Path target = root.resolve(media.getStorePath()).normalize();
        // 路径穿越防护：解析后必须仍在根目录内
        if (!target.startsWith(root)) {
            log.warn("[影像] 非法存储路径 fileId={} path={}", fileId, media.getStorePath());
            return null;
        }
        return target;
    }

    public List<WashMedia> listByOrder(String orderNo) {
        return mediaMapper.selectByOrderNo(orderNo);
    }

    private void save(MultipartFile file, String relative) {
        try {
            Path root = Paths.get(storePath).toAbsolutePath().normalize();
            Path target = root.resolve(relative).normalize();
            Files.createDirectories(target.getParent());
            file.transferTo(target.toFile());
        } catch (IOException e) {
            log.error("[影像上传] 写盘失败 relative={}", relative, e);
            throw new ApiException(ErrorCode.C0001, "文件保存失败");
        }
    }

    /** 取扩展名（含点），取不到就用 .bin；不做内容信任，只用于落盘命名 */
    private String extensionOf(String originalName) {
        if (originalName == null || !originalName.contains(".")) {
            return ".bin";
        }
        String ext = originalName.substring(originalName.lastIndexOf('.'));
        return ext.length() > 8 ? ".bin" : ext;
    }
}
