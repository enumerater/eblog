# 📡 前后端接口协议 (API)

> 后端基础路径：`http://localhost:3001/api`  
> 数据格式：`JSON`  
> 字符编码：`UTF-8`

---

## 1. 文章模型 (Article)

```json
{
  "id": "string | number",
  "title": "string",
  "content": "string",
  "tags": ["string"],
  "summary": "string",
  "createdAt": "string (ISO 8601)",
  "updatedAt": "string (ISO 8601)"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | string / number | 自动 | 文章唯一标识 |
| title | string | ✅ | 文章标题 |
| content | string | ✅ | 文章内容（TipTap 富文本编辑器输出的 HTML） |
| tags | string[] | ❌ | 标签数组 |
| summary | string | ❌ | 文章摘要（如不传，前端自动截取 content 去标签后前 150 字） |
| createdAt | string | 自动 | 创建时间 |
| updatedAt | string | 自动 | 更新时间 |

---

## 2. 接口列表

### 2.1 获取所有文章

```
GET /api/articles
```

**响应 200：**
```json
[
  {
    "id": 1,
    "title": "Hello World",
    "content": "这是我的第一篇文章...",
    "tags": ["React", "前端"],
    "summary": "这是我的第一篇文章...",
    "createdAt": "2025-01-15T08:00:00.000Z",
    "updatedAt": "2025-01-15T08:00:00.000Z"
  }
]
```

---

### 2.2 获取单篇文章

```
GET /api/articles/:id
```

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| id | string / number | 文章 ID |

**响应 200：**
```json
{
  "id": 1,
  "title": "Hello World",
  "content": "这是我的第一篇文章...",
  "tags": ["React", "前端"],
  "summary": "这是我的第一篇文章...",
  "createdAt": "2025-01-15T08:00:00.000Z",
  "updatedAt": "2025-01-15T08:00:00.000Z"
}
```

**响应 404：**
```json
{
  "message": "文章不存在"
}
```

---

### 2.3 创建文章

```
POST /api/articles
Content-Type: application/json
```

**请求体：**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | string | ✅ | 文章标题 |
| content | string | ✅ | 文章内容 |
| tags | string[] | ❌ | 标签数组 |
| summary | string | ❌ | 摘要（不传则后端自动生成） |

**请求示例：**
```json
{
  "title": "我的新文章",
  "content": "这是文章的内容...",
  "tags": ["JavaScript", "教程"]
}
```

**响应 201：**
```json
{
  "id": 2,
  "title": "我的新文章",
  "content": "这是文章的内容...",
  "tags": ["JavaScript", "教程"],
  "summary": "这是文章的内容...",
  "createdAt": "2025-01-15T10:00:00.000Z",
  "updatedAt": "2025-01-15T10:00:00.000Z"
}
```

**响应 400：**
```json
{
  "message": "标题和内容不能为空"
}
```

---

### 2.4 更新文章

```
PUT /api/articles/:id
Content-Type: application/json
```

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| id | string / number | 文章 ID |

**请求体（所有字段可选，只传需要更新的字段）：**

| 字段 | 类型 | 说明 |
|------|------|------|
| title | string | 文章标题 |
| content | string | 文章内容 |
| tags | string[] | 标签数组 |
| summary | string | 摘要 |

**请求示例：**
```json
{
  "title": "更新后的标题",
  "tags": ["React", "Vite"]
}
```

**响应 200：**
```json
{
  "id": 1,
  "title": "更新后的标题",
  "content": "原文内容...",
  "tags": ["React", "Vite"],
  "summary": "原文摘要...",
  "createdAt": "2025-01-15T08:00:00.000Z",
  "updatedAt": "2025-01-15T12:00:00.000Z"
}
```

**响应 404：**
```json
{
  "message": "文章不存在"
}
```

---

### 2.5 删除文章

```
DELETE /api/articles/:id
```

**路径参数：**

| 参数 | 类型 | 说明 |
|------|------|------|
| id | string / number | 文章 ID |

**响应 200：**
```json
{
  "message": "删除成功"
}
```

**响应 404：**
```json
{
  "message": "文章不存在"
}
```

---

## 3. 错误响应格式

所有接口在出错时统一返回以下格式：

```json
{
  "message": "错误描述信息"
}
```

| HTTP 状态码 | 含义 |
|------------|------|
| 200 | 成功 |
| 201 | 创建成功 |
| 400 | 请求参数错误 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

---

## 4. 前端使用的字段

前端各页面依赖的字段如下：

| 页面 | 使用的字段 |
|------|-----------|
| 首页 (Home) | id, title, content (取前150字作摘要), tags, createdAt |
| 文章详情 (ArticleDetail) | id, title, content, tags, createdAt |
| 编辑器 (ArticleEditor) | 创建: title, content, tags / 编辑: 同上 + id |
| 管理页 (Admin) | id, title, tags, createdAt |

> **注意：** 如果后端返回的数据结构与此文档不符，前端 `src/api/index.js` 中的 `request` 函数会尝试解析 JSON，如果 HTTP 状态码非 2xx 会抛出错误。