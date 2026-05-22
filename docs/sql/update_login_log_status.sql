-- ========================================
-- 修复登录日志状态值
-- 将旧的 status 值 0（成功）改为 1（成功），符合编程惯例
-- ========================================

USE yuncode_lowcode;

-- 查看当前状态分布
SELECT status, COUNT(*) as count, msg
FROM sys_login_log
GROUP BY status, msg
ORDER BY status;

-- 更新登录成功的记录：将 status 从 0 改为 1
UPDATE sys_login_log
SET status = 1
WHERE status = 0 AND msg = '登录成功';

-- 更新登录失败的记录：将 status 从 1 改为 0
UPDATE sys_login_log
SET status = 0
WHERE status = 1 AND msg != '登录成功';

-- 验证更新结果
SELECT status, COUNT(*) as count,
       CASE WHEN status = 1 THEN '成功' ELSE '失败' END as status_name
FROM sys_login_log
GROUP BY status
ORDER BY status;

-- ========================================
-- 说明
-- ========================================
-- 根据编程惯例：
-- 1 = 成功（SUCCESS）
-- 0 = 失败（FAIL）
--
-- 此脚本将历史数据修正为符合惯例的值
-- ========================================
