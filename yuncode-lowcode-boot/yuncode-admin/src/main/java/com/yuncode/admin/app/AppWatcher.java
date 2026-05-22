package com.yuncode.admin.app;

import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * App 变更监听器。
 * <p>
 * 监听 App 安装目录的变更（JAR 文件增删改、数据库配置、页面 CRUD、流程变动等），
 * 检测到变更时回调 {@code onAppChanged} 传入 appId。
 * </p>
 */
public interface AppWatcher extends AutoCloseable {

    /**
     * 开始监听。
     *
     * @param installDir   安装目录（包含 com.yuncode.user.apps.* 子目录）
     * @param onAppChanged 变更回调，参数为 appId（目录名）
     */
    void start(Path installDir, Consumer<String> onAppChanged);

    /** 停止监听 */
    @Override
    void close();
}
