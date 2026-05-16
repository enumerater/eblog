# 📝 eblog

一个基于 **React + Spring Boot** 的全栈博客系统，支持富文本编辑、文章管理、草稿保存和 JWT 认证。

---

## 🏗️ 技术栈

| 层级 | 技术 |
|------|------|
| **前端** | React 19 + Vite 8 + React Router 7 |
| **富文本编辑器** | TipTap (基于 ProseMirror) |
| **后端** | Spring Boot 3.5 + Java 17 |
| **ORM** | MyBatis 3 |
| **数据库** | MySQL |
| **认证** | JWT (jjwt) |
| **代码高亮** | highlight.js + lowlight |

---

## 📁 项目结构

```
eblog/
├── src/                          # 前端源码
│   ├── api/index.js              # API 请求封装
│   ├── components/               # 通用组件
│   │   ├── ArticleCard.jsx       # 文章卡片
│   │   ├── Navbar.jsx            # 导航栏
│   │   ├── Footer.jsx            # 页脚
│   │   └── editor/               # 富文本编辑器组件
│   │       ├── RichEditor.jsx    # 编辑器主体
│   │       ├── BubbleMenu.jsx    # 气泡菜单
│   │       ├── SlashCommands.jsx # 斜杠命令菜单
│   │       └── CodeBlockView.jsx # 代码块视图
│   ├── pages/                    # 页面
│   │   ├── Home.jsx              # 首页（文章列表）
│   │   ├── ArticleDetail.jsx     # 文章详情
│   │   ├── ArticleEditor.jsx     # 文章编辑/创建
│   │   ├── Admin.jsx             # 管理后台
│   │   └── Login.jsx             # 登录页
│   ├── App.jsx                   # 路由配置
│   └── main.jsx                  # 入口
│
├── back/                         # 后端源码
│   └── src/main/java/com/enumerate/back/
│       ├── Controller/           # 控制器层
│       │   ├── ArticleController.java  # 文章 CRUD
│       │   ├── AuthController.java     # 登录认证
│       │   └── DraftController.java    # 草稿管理
│       ├── Service/              # 业务逻辑层
│       ├── Mapper/               # MyBatis 数据访问
│       ├── Entity/               # 实体类
│       ├── DTO/                  # 数据传输对象
│       └── Config/               # 配置（拦截器、CORS）
│
├── API.md                        # 前后端接口协议文档
├── package.json                  # 前端依赖
└── back/pom.xml                  # 后端依赖
```

---

## 🚀 快速开始

### 前置要求

- Node.js >= 18
- Java 17+
- Maven
- MySQL

### 1. 克隆项目

```bash
git clone <repo-url>
cd eblog
```

### 2. 启动后端

```bash
cd back
# 配置数据库连接（编辑 application.properties）
# 然后启动
mvn spring-boot:run
```

后端默认运行在 `http://localhost:3001`。

### 3. 启动前端

```bash
# 在项目根目录
npm install
npm run dev
```

前端默认运行在 `http://localhost:5173`。

---

## 🔌 API 概览

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/articles` | 获取所有文章 |
| GET | `/api/articles/:id` | 获取单篇文章 |
| POST | `/api/articles` | 创建文章 |
| PUT | `/api/articles/:id` | 更新文章 |
| DELETE | `/api/articles/:id` | 删除文章 |
| POST | `/api/auth/login` | 管理员登录 |
| GET | `/api/drafts` | 获取草稿列表 |
| POST | `/api/drafts` | 保存草稿 |
| DELETE | `/api/drafts/:id` | 删除草稿 |

详细接口文档见 [API.md](./API.md)。

---

## ✨ 功能特性

- **富文本编辑** — 基于 TipTap，支持标题、加粗、斜体、下划线、代码块、表格、图片、链接、文本对齐等
- **斜杠命令菜单** — 输入 `/` 快速插入各种内容块
- **代码高亮** — 支持多种编程语言的语法高亮
- **文章管理** — 创建、编辑、删除文章
- **草稿保存** — 编辑过程中自动保存草稿
- **JWT 认证** — 管理员登录保护管理后台
- **响应式布局** — 适配桌面和移动端

---

## 📄 许可证

MIT