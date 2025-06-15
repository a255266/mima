# Mima - Android Password Manager

![Mima Logo](./app/src/main/res/mipmap-hdpi
/ic_launcher_round.webp) <!-- 如果你有项目logo，可以加 -->

## 项目简介

Mima 是一款基于 Android 平台的可离线密码管理应用，使用 Kotlin 和 Jetpack Compose 构建，采用 Room 作为本地数据库，内置强大的数据加密功能，支持离线数据迁移，并支持 WebDAV 云端同步，保障用户密码数据的安全与便捷管理。

---

## 主要特性

- **安全加密存储**  
  - 使用 Android Keystore 结合 AES-GCM 对本地数据库进行加密  
  - 支持自定义秘钥加密，保证跨设备数据恢复的安全性

- **离线迁移数据**  
  - 使用自定义秘钥导出文件后可在其他设备使用相同秘钥导入数据

- **本地数据库管理**  
  - 基于 Room 持久化存储密码条目  
  - 支持自定义字段

- **Jetpack Compose UI**  
  - 现代声明式 UI，响应式设计  
  - 动态搜索、分页加载，支持流畅的列表展示和交互

- **WebDAV 云同步**  
  - 支持配置 WebDAV 服务器进行云端数据同步与备份  
  - 自动检测增量更新，实现多端数据一致性
  - 支持首页下拉手动更新

- **增删改查**  
  - 支持增删改查
  - 增删改时自动上传数据到WebDAV

- **密码生成与复制**  
  - 内置密码生成器，随机生成复杂密码
  - 可配置生成密码长度
  - 一键复制密码，方便快捷

- **主题动态切换**  
  - 支持跟随系统暗色模式


---

## 技术栈

- Kotlin  
- Jetpack Compose Material3 (版本 1.3.2)  
- Room 数据库  
- Android Keystore 加密管理  
- Hilt 依赖注入  
- Paging 3 分页库
- WebDAV 云同步（dav4jvm）  
- Gson 用于 JSON 解析
