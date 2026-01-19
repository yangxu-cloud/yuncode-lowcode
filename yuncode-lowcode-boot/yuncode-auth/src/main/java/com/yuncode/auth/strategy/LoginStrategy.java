package com.yuncode.auth.strategy;

import com.yuncode.auth.dto.LoginDTO;
import com.yuncode.auth.vo.LoginVO;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 登录策略接口
 */
public interface LoginStrategy {

    /**
     * 执行登录
     *
     * @param loginDTO 登录请求参数
     * @param request  HTTP 请求
     * @return 登录响应信息
     */
    LoginVO login(LoginDTO loginDTO, HttpServletRequest request);

    /**
     * 获取登录类型
     *
     * @return 登录类型编码
     */
    String getLoginType();
}
