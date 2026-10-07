package com.ruoyi.wash.worker.service;

import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.network.domain.WashSite;
import com.ruoyi.wash.network.service.WashSiteQueryService;
import com.ruoyi.wash.worker.dto.AdminWorkerVO;
import com.ruoyi.wash.worker.mapper.WashWorkerMapper;
import com.ruoyi.wash.worker.security.WorkerTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 后台师傅管理（D-1）：覆盖运营每天会做的事，以及几个容易出事的点。
 */
@DisplayName("后台师傅管理：WashWorkerAdminService")
@ExtendWith(MockitoExtension.class)
class WashWorkerAdminServiceTest {

    @Mock private WashWorkerMapper workerMapper;
    @Mock private WashSiteQueryService siteQuery;
    @Mock private RedisCache redisCache;

    private WashWorkerAdminService service;

    private static final Long WORKER_ID = 1L;
    private static final String WORKER_NO = "W001";

    @BeforeEach
    void init() {
        service = new WashWorkerAdminService();
        inject(service, "workerMapper", workerMapper);
        inject(service, "siteQuery", siteQuery);
        inject(service, "redisCache", redisCache);
    }

    private void inject(Object target, String field, Object value) {
        try {
            var f = target.getClass().getDeclaredField(field);
            f.setAccessible(true);
            f.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private AdminWorkerVO existing() {
        AdminWorkerVO vo = new AdminWorkerVO();
        vo.setWorkerId(WORKER_ID);
        vo.setWorkerNo(WORKER_NO);
        vo.setName("张师傅");
        vo.setSiteId(10L);
        vo.setStatus("NORMAL");
        return vo;
    }

    private void stubSite() {
        WashSite site = new WashSite();
        site.setSiteId(10L);
        site.setSiteName("中央站");
        lenient().when(siteQuery.listSites()).thenReturn(List.of(site));
    }

    @Test
    @DisplayName("列表：回填站点名与中文状态名，且绝不把密码带出去")
    void list_decoratesAndHidesPassword() {
        stubSite();
        AdminWorkerVO row = existing();
        row.setPassword("dev123");
        lenient().when(workerMapper.selectAdminList(any(), any(), any(), anyInt(), anyInt())).thenReturn(List.of(row));

        List<AdminWorkerVO> rows = service.list(null, null, null, 1, 10);
        assertEquals(1, rows.size());
        assertNull(rows.get(0).getPassword(), "密码不能出现在出参里");
        assertEquals("中央站", rows.get(0).getSiteName());
        assertEquals("正常", rows.get(0).getStatusLabel());
    }

    @Test
    @DisplayName("新增：工号重复 → D4005")
    void add_duplicateWorkerNo_rejected() {
        AdminWorkerVO vo = new AdminWorkerVO();
        vo.setWorkerNo(WORKER_NO);
        vo.setName("李师傅");
        vo.setPassword("dev123");
        vo.setSiteId(10L);
        when(workerMapper.countByWorkerNo(WORKER_NO)).thenReturn(1L);

        ApiException ex = assertThrows(ApiException.class, () -> service.add(vo));
        assertEquals(ErrorCode.D4005, ex.getErrorCode());
        verify(workerMapper, never()).insertWorker(any());
    }

    @Test
    @DisplayName("新增：初始密码缺失或过短 → 拒绝")
    void add_weakPassword_rejected() {
        AdminWorkerVO ok = new AdminWorkerVO();
        ok.setWorkerNo("W002");
        ok.setName("李师傅");
        ok.setSiteId(10L);

        ok.setPassword(null);
        assertThrows(ApiException.class, () -> service.add(ok));

        ok.setPassword("123");
        assertThrows(ApiException.class, () -> service.add(ok));
        verify(workerMapper, never()).insertWorker(any());
    }

    @Test
    @DisplayName("新增：正常入参落库，且默认状态为正常")
    void add_valid_pass() {
        AdminWorkerVO vo = new AdminWorkerVO();
        vo.setWorkerNo("W002");
        vo.setName("李师傅");
        vo.setPassword("dev123");
        vo.setSiteId(10L);
        when(workerMapper.countByWorkerNo("W002")).thenReturn(0L);

        service.add(vo);
        verify(workerMapper).insertWorker(vo);
        assertEquals("NORMAL", vo.getStatus(), "未指定状态时默认正常，避免新建即被禁用");
    }

    @Test
    @DisplayName("修改：不允许改工号（工号是登录凭证）")
    void edit_changeWorkerNo_rejected() {
        when(workerMapper.selectAdminById(WORKER_ID)).thenReturn(existing());
        AdminWorkerVO incoming = new AdminWorkerVO();
        incoming.setWorkerId(WORKER_ID);
        incoming.setWorkerNo("W999");
        incoming.setName("张师傅");

        assertThrows(ApiException.class, () -> service.edit(incoming));
        verify(workerMapper, never()).updateWorker(any());
    }

    @Test
    @DisplayName("修改：改为禁用 → 立刻吊销登录态")
    void edit_disable_revokesToken() {
        when(workerMapper.selectAdminById(WORKER_ID)).thenReturn(existing());
        when(redisCache.keys(WorkerTokenService.TOKEN_PREFIX + "*")).thenReturn(List.of("worker:token:abc"));
        when(redisCache.getCacheObject("worker:token:abc")).thenReturn(WORKER_ID);

        AdminWorkerVO incoming = new AdminWorkerVO();
        incoming.setWorkerId(WORKER_ID);
        incoming.setWorkerNo(WORKER_NO);
        incoming.setName("张师傅");
        incoming.setStatus("DISABLED");
        service.edit(incoming);

        verify(redisCache).deleteObject("worker:token:abc");
    }

    @Test
    @DisplayName("修改：保持正常状态 → 不踢人下线")
    void edit_keepNormal_keepsToken() {
        when(workerMapper.selectAdminById(WORKER_ID)).thenReturn(existing());

        AdminWorkerVO incoming = new AdminWorkerVO();
        incoming.setWorkerId(WORKER_ID);
        incoming.setWorkerNo(WORKER_NO);
        incoming.setName("张师傅2");
        incoming.setStatus("NORMAL");
        service.edit(incoming);

        verify(redisCache, never()).deleteObject(anyString());
    }

    @Test
    @DisplayName("重置密码：更新密码并吊销旧 token")
    void resetPassword_updatesAndRevokes() {
        when(workerMapper.selectAdminById(WORKER_ID)).thenReturn(existing());
        when(redisCache.keys(anyString())).thenReturn(List.of("worker:token:abc"));
        when(redisCache.getCacheObject("worker:token:abc")).thenReturn(WORKER_ID);

        service.resetPassword(WORKER_ID, "newpass123");

        verify(workerMapper).updatePassword(WORKER_ID, "newpass123");
        verify(redisCache).deleteObject("worker:token:abc");
    }

    @Test
    @DisplayName("重置密码：弱密码拒绝，不动库也不踢人")
    void resetPassword_weak_rejected() {
        when(workerMapper.selectAdminById(WORKER_ID)).thenReturn(existing());
        assertThrows(ApiException.class, () -> service.resetPassword(WORKER_ID, "12"));
        verify(workerMapper, never()).updatePassword(anyLong(), anyString());
    }

    @Test
    @DisplayName("删除：逻辑删除 + 吊销登录态")
    void remove_logicalDeleteAndRevoke() {
        when(workerMapper.selectAdminById(WORKER_ID)).thenReturn(existing());
        when(redisCache.keys(anyString())).thenReturn(List.of("worker:token:abc"));
        when(redisCache.getCacheObject("worker:token:abc")).thenReturn(WORKER_ID);

        service.remove(WORKER_ID);

        verify(workerMapper).logicDelete(WORKER_ID);
        verify(redisCache).deleteObject("worker:token:abc");
    }

    @Test
    @DisplayName("不存在的师傅 → D4006")
    void require_missingWorker_D4006() {
        when(workerMapper.selectAdminById(99L)).thenReturn(null);
        ApiException ex = assertThrows(ApiException.class, () -> service.get(99L));
        assertEquals(ErrorCode.D4006, ex.getErrorCode());
    }

    @Test
    @DisplayName("吊销登录态：只删本人的 token，别人的不受影响")
    void revoke_onlyOwnTokens() {
        when(workerMapper.selectAdminById(WORKER_ID)).thenReturn(existing());
        when(redisCache.keys(anyString())).thenReturn(List.of("worker:token:mine", "worker:token:other"));
        when(redisCache.getCacheObject("worker:token:mine")).thenReturn(WORKER_ID);
        when(redisCache.getCacheObject("worker:token:other")).thenReturn(2L);

        service.resetPassword(WORKER_ID, "newpass123");

        verify(redisCache).deleteObject("worker:token:mine");
        verify(redisCache, never()).deleteObject("worker:token:other");
    }

    @Test
    @DisplayName("没有在线 token 时：keys 返回空也不报错")
    void revoke_emptyKeys_noop() {
        when(workerMapper.selectAdminById(WORKER_ID)).thenReturn(existing());
        when(redisCache.keys(anyString())).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> service.resetPassword(WORKER_ID, "newpass123"));
        verify(redisCache, never()).deleteObject(anyString());
    }
}
