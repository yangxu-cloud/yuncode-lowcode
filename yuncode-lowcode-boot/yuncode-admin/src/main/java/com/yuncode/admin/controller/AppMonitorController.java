package com.yuncode.admin.controller;

import com.yuncode.admin.app.HotAppDeployer;
import com.yuncode.common.model.util.response.Result;
import com.yuncode.system.app.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * 应用监控控制器
 * 暴露 HotAppDeployer 状态和系统监控信息，供前端运维组件展示
 */
@Slf4j
@RestController
@RequestMapping("/system/monitor")
@RequiredArgsConstructor
public class AppMonitorController {

    private final HotAppDeployer hotAppDeployer;
    private final ApplicationService applicationService;

    /**
     * 获取应用部署状态（从数据库读取应用运行/停止状态）
     */
    @GetMapping("/apps")
    public Result<Map<String, Object>> getAppStatus() {
        // 从数据库查询应用状态（运行/停止）
        long runningCount = applicationService.countByStatus(1);

        // 从 HotAppDeployer 获取安装目录下的应用列表
        List<Map<String, Object>> appList = hotAppDeployer.getAllAppsWithStatus();
        long totalDirs = appList.size();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("enabled", true);
        data.put("loadedCount", (int) Math.min(runningCount, totalDirs));
        data.put("stoppedCount", (int) Math.max(0, totalDirs - Math.min(runningCount, totalDirs)));
        data.put("totalCount", (int) totalDirs);
        data.put("appList", appList);
        return Result.success(data);
    }
}
