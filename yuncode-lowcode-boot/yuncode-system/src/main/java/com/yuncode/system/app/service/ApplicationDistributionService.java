package com.yuncode.system.app.service;

import com.yuncode.system.app.dto.DistributeResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 应用分发/部署服务
 */
public interface ApplicationDistributionService {
    DistributeResult distributeApplication(Long id, boolean includeData);
    Map<String, String> stageApplication(MultipartFile file);
    List<Map<String, String>> listStagedApplications();
    Map<String, Object> deployStagedApplication(String appId);
    boolean deleteStagedApplication(String appId);
}
