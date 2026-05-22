# 快速清除 Token - 立即执行

## 方法 1：浏览器控制台清除（最快）⭐

1. 打开浏览器访问 `http://localhost:3000`
2. 按 `F12` 打开开发者工具
3. 切换到 **Console**（控制台）标签
4. 复制粘贴以下代码并按回车：

```javascript
localStorage.clear();
console.log('✅ 已清除 localStorage');
location.reload();
```

页面会自动刷新，然后应该会跳转到登录页。

---

## 方法 2：手动删除 localStorage

1. 按 `F12` 打开开发者工具
2. 切换到 **Application**（应用程序）标签
3. 左侧找到 **Local Storage** → 点击 `http://localhost:3000`
4. 删除以下项：
   - 右键点击 `token` → Delete（清除）
   - 右键点击 `userInfo` → Delete（清除）
5. 刷新页面（`Ctrl + R`）

---

## 方法 3：使用无痕模式

- **Chrome/Edge**: 按 `Ctrl + Shift + N` 打开无痕窗口
- **Firefox**: 按 `Ctrl + Shift + P` 打开隐私窗口
- 在无痕窗口中访问 `http://localhost:3000`

无痕模式不会有任何缓存数据，会直接跳转到登录页。

---

## 验证是否清除成功

在控制台执行：

```javascript
console.log('Token:', localStorage.getItem('token'));
console.log('UserInfo:', localStorage.getItem('userInfo'));
```

如果都显示 `null`，说明清除成功。

---

## 为什么会这样？

因为之前登录成功后，token 被保存到浏览器的 localStorage 中。即使关闭浏览器再打开，localStorage 中的数据依然存在。

路由守卫检测到 localStorage 中有 token，就会认为用户已经登录，所以直接跳转到首页。

清除 token 后，路由守卫会检测到未登录状态，就会自动跳转到登录页。
