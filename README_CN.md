# EasyReader

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

[English](./README.md)

**EasyReader** 是一个轻量级的 Android TXT 电子书阅读器，支持从设备导入 TXT 文件，提供书架管理、断点阅读、深色模式、章节识别、编码选择等丰富功能。兼容 Android 4.1 (API 16) 及以上版本。

---

## 功能特点

- 📚 **书架管理** – 添加、删除书籍，支持多选批量删除
- 🔖 **断点阅读** – 自动保存阅读位置，下次打开直接跳转
- 🌙 **深色模式** – 支持深色/浅色切换
- 📖 **章节识别** – 自动识别章节
- 🔤 **编码选择** – 支持 UTF-8、GBK、GB2312、BIG5 等，独立记忆每本书的编码
- 🔎 **目录跳转** – 快速跳转到任意章节
- 📏 **字体调节** – 自由调整字体大小
- 📄 **分页阅读** – 按章节分页，大文件流畅阅读
- ⚡ **惯性滑动** – 流畅的滚动体验

---

## 截图


| 书架 | 阅读界面 | 目录跳转 |
|:---:|:---:|:---:|
| ![](screenshots/bookshelf.png) | ![](screenshots/reader.png) | ![](screenshots/toc.png) |

---

## 安装与使用

1. **下载 APK**：从 [Releases](https://github.com/yourusername/EasyReader/releases) 下载最新版本并安装。
2. **添加书籍**：点击右下角加号，选择 TXT 文件（系统文件选择器）。
3. **阅读**：点击书架上的书籍即可开始阅读。
4. **目录**：在阅读界面点击屏幕显示控制栏，点击“目录”按钮。
5. **编码切换**：如果文件乱码，点击“编码”按钮选择合适的编码。
6. **深色模式**：在书架界面右上角菜单中切换。

---

## 构建指南

1. 克隆仓库：
   ```bash
   git clone https://github.com/yourusername/EasyReader.git
2. 使用 Android Studio 打开项目。

3. 确保 build.gradle 中配置了正确的 SDK 版本（minSdk 16，targetSdk 33+）。

4. 构建并运行。

## 依赖
- AndroidX AppCompat

- Material Components

- RecyclerView

## 贡献
欢迎提交 Issue 和 Pull Request！

## 许可证
本项目基于 MIT 许可证开源。

## 作者
### 2Days

GitHub: @550Cool

Email: wohenaighs@163.com
