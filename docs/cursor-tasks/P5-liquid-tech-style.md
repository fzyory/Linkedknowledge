# P5 — 液态科技风 UI 全站改版

## 1. 目标

把整个 mindmap-ui 前端从当前样式(假设是常规样式)统一改版成**液态科技风**——玻璃质感、流光、霓虹描边、深空背景。这是横切关注点,影响所有页面。

## 2. 验收标准

- [ ] 全站背景:深空渐变(`linear-gradient(135deg, #0A0E1A 0%, #1A1F3A 100%)`)
- [ ] 所有卡片/面板:半透明深色 + `backdrop-filter: blur(20px)` + 青色描边
- [ ] 所有按钮:hover 时一道 1px 青光横扫,200ms 完成
- [ ] 主色:冷蓝青 `#00E5FF`
- [ ] 强调色:霓虹紫 `#A855F7` / 警示橙 `#FF6B35`
- [ ] 圆角统一 12px(卡片)/ 8px(按钮)
- [ ] 字体:Inter / 系统 sans-serif
- [ ] 动效:统一 `cubic-bezier(0.4, 0, 0.2, 1)`,时长 200-400ms
- [ ] 主导航栏:玻璃面板 + 流光下划线指示当前页
- [ ] 所有页面过渡不闪烁

## 3. 当前代码现状

- 前端: `C:\Users\g\Desktop\mindmap-ui`
- `src/App.vue` — 根组件,通常是布局 + 导航
- `src/style.css` 或 `src/assets/styles/` — 全局样式入口
- `src/views/` — 所有页面

## 4. 改动

### 4.1 新建全局样式:`src/assets/styles/liquid-tech.css`

```css
/* ====================================
   液态科技风 — 全局样式表
   ==================================== */

:root {
  --color-bg-dark: #0A0E1A;
  --color-bg-mid: #1A1F3A;
  --color-primary: #00E5FF;
  --color-primary-glow: rgba(0, 229, 255, 0.4);
  --color-accent: #A855F7;
  --color-warning: #FF6B35;
  --color-text: #E0E7FF;
  --color-text-dim: rgba(224, 231, 255, 0.6);

  --radius-card: 12px;
  --radius-btn: 8px;
  --ease: cubic-bezier(0.4, 0, 0.2, 1);
  --duration: 200ms;

  --shadow-glow: 0 8px 32px rgba(0, 229, 255, 0.1);
  --shadow-glow-strong: 0 0 24px rgba(0, 229, 255, 0.5);

  --glass-bg: rgba(10, 14, 26, 0.6);
  --glass-border: 1px solid rgba(0, 229, 255, 0.3);
  --glass-border-hover: 1px solid rgba(0, 229, 255, 0.6);
}

* {
  box-sizing: border-box;
}

html, body, #app {
  margin: 0;
  padding: 0;
  min-height: 100vh;
  background: linear-gradient(135deg, var(--color-bg-dark) 0%, var(--color-bg-mid) 100%);
  background-attachment: fixed;
  color: var(--color-text);
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  font-weight: 300;
  letter-spacing: 0.3px;
}

/* ---- 玻璃面板 ---- */
.glass-panel {
  background: var(--glass-bg);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: var(--glass-border);
  border-radius: var(--radius-card);
  padding: 16px;
  box-shadow: var(--shadow-glow);
  transition: all var(--duration) var(--ease);
}

.glass-panel:hover {
  border: var(--glass-border-hover);
  box-shadow: var(--shadow-glow-strong);
}

/* ---- 按钮 ---- */
.btn {
  position: relative;
  display: inline-block;
  padding: 8px 20px;
  background: rgba(0, 229, 255, 0.1);
  border: 1px solid var(--color-primary);
  color: var(--color-primary);
  border-radius: var(--radius-btn);
  font-size: 14px;
  cursor: pointer;
  overflow: hidden;
  transition: all var(--duration) var(--ease);
}

.btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(0, 229, 255, 0.4), transparent);
  transition: left var(--duration) var(--ease);
}

.btn:hover {
  background: rgba(0, 229, 255, 0.2);
  box-shadow: var(--shadow-glow-strong);
  transform: translateY(-1px);
}

.btn:hover::before {
  left: 100%;
}

.btn:active {
  transform: translateY(0);
}

.btn-accent {
  border-color: var(--color-accent);
  color: var(--color-accent);
  background: rgba(168, 85, 247, 0.1);
}

.btn-danger {
  border-color: var(--color-warning);
  color: var(--color-warning);
  background: rgba(255, 107, 53, 0.1);
}

/* ---- 输入框 ---- */
.input {
  width: 100%;
  padding: 10px 14px;
  background: rgba(10, 14, 26, 0.4);
  border: 1px solid rgba(0, 229, 255, 0.2);
  border-radius: var(--radius-btn);
  color: var(--color-text);
  font-size: 14px;
  transition: all var(--duration) var(--ease);
}

.input:focus {
  outline: none;
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(0, 229, 255, 0.15);
}

.input::placeholder {
  color: var(--color-text-dim);
}

/* ---- 标题 ---- */
h1, h2, h3 {
  color: var(--color-primary);
  font-weight: 300;
  letter-spacing: 1px;
}

/* ---- 链接 ---- */
a {
  color: var(--color-primary);
  text-decoration: none;
  transition: all var(--duration) var(--ease);
}

a:hover {
  color: var(--color-accent);
  text-shadow: 0 0 8px var(--color-primary-glow);
}

/* ---- 滚动条 ---- */
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-track {
  background: var(--color-bg-dark);
}

::-webkit-scrollbar-thumb {
  background: var(--color-primary);
  border-radius: 4px;
}

::-webkit-scrollbar-thumb:hover {
  background: var(--color-accent);
}

/* ---- 加载动效 ---- */
@keyframes pulse-glow {
  0%, 100% { box-shadow: 0 0 8px rgba(0, 229, 255, 0.3); }
  50% { box-shadow: 0 0 24px rgba(0, 229, 255, 0.7); }
}

.loading {
  animation: pulse-glow 1.5s var(--ease) infinite;
}
```

### 4.2 在 `main.js` 引入

```js
import './assets/styles/liquid-tech.css'
```

### 4.3 改 `App.vue`

主导航栏改用 `.glass-panel` 类,激活项加底部青光:

```vue
<template>
  <div class="app-shell">
    <nav class="top-nav glass-panel">
      <router-link to="/" class="nav-link">首页</router-link>
      <router-link to="/graph" class="nav-link">图谱</router-link>
      <router-link to="/nodes" class="nav-link">节点</router-link>
      <router-link to="/tags" class="nav-link">标签</router-link>
    </nav>
    <main class="main-content">
      <router-view />
    </main>
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}
.top-nav {
  display: flex;
  gap: 24px;
  padding: 12px 24px;
  margin: 16px;
  border-radius: var(--radius-card);
}
.nav-link {
  position: relative;
  padding: 4px 0;
}
.nav-link.router-link-active::after {
  content: '';
  position: absolute;
  bottom: -4px;
  left: 0;
  right: 0;
  height: 2px;
  background: var(--color-primary);
  box-shadow: 0 0 8px var(--color-primary);
}
.main-content {
  flex: 1;
  padding: 16px;
}
</style>
```

### 4.4 通用改造清单(给 Cursor)

每个 view 文件(Login.vue, Register.vue, NodeList.vue, NodeDetail.vue, NodeEditor.vue 等),按以下原则改造:

| 元素 | 之前 | 现在 |
|---|---|---|
| 容器 div | 简单 div | 加 class `glass-panel` |
| 按钮 | 原生 button | 加 class `btn`(必要时 `btn-accent` / `btn-danger`) |
| 输入框 | 原生 input | 加 class `input` |
| 标题 | 默认样式 | 自动用全局 h1/h2/h3 样式 |
| 列表/卡片 | 简单 div | 包 `glass-panel` 或 `.node-card` |

## 5. 测试要求

1. 每个页面打开:背景渐变、玻璃面板、青色描边都生效
2. 鼠标 hover 按钮:有青光横扫
3. 鼠标 hover 玻璃面板:描边变亮
4. 主导航当前页:底部青光下划线
5. 浏览器开发者工具检查:`backdrop-filter: blur(20px)` 真的应用了
6. 滚动条:青色细条

## 6. 不做的事

- **不**做响应式(本期只考虑桌面端 1280+ 宽度)
- **不**做暗色/亮色切换(只有暗色)
- **不**引入 UI 组件库(Element Plus / Naive UI 等都不引入)
- **不**做主题色切换(青色定死)
- **不**做图标库(可临时用 emoji 或纯文字,后期再换 lucide-vue-next)
