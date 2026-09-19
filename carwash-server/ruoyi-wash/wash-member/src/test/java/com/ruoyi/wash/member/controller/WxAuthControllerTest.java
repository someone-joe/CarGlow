package com.ruoyi.wash.member.controller;

import com.ruoyi.wash.common.api.ApiException;
import com.ruoyi.wash.common.api.ApiResult;
import com.ruoyi.wash.common.api.ErrorCode;
import com.ruoyi.wash.member.dto.LoginVO;
import com.ruoyi.wash.member.service.WashAuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * 接入层测试：C 端登录接口（WxAuthController.login）。
 *
 * <p>纯 Mockito 单测：覆盖正常登录（dev 模式任意 code 返回 token）与 code 缺失/空白两种非法入参。
 */
@DisplayName("C 端登录")
@ExtendWith(MockitoExtension.class)
class WxAuthControllerTest {

    @Mock
    private WashAuthService authService;

    @InjectMocks
    private WxAuthController controller;

    @Test
    @DisplayName("code 正常 → 返回 0 与 LoginVO（本地 dev 模式任意 code 可登录）")
    void loginSuccess() {
        LoginVO vo = new LoginVO();
        vo.setToken("tok-123");
        when(authService.login("any-code")).thenReturn(vo);

        ApiResult<LoginVO> result = controller.login(new WxAuthController.LoginRequest("any-code", null));

        assertEquals(0, result.getCode());
        assertSame(vo, result.getData());
    }

    @Test
    @DisplayName("code 为空白 → A0001")
    void loginBlankCode() {
        ApiException e = assertThrows(ApiException.class,
                () -> controller.login(new WxAuthController.LoginRequest("   ", null)));
        assertEquals(ErrorCode.A0001, e.getErrorCode());
    }

    @Test
    @DisplayName("code 为 null → A0001")
    void loginNullCode() {
        ApiException e = assertThrows(ApiException.class,
                () -> controller.login(new WxAuthController.LoginRequest(null, null)));
        assertEquals(ErrorCode.A0001, e.getErrorCode());
    }
}
