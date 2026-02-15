# EasyReader

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

[中文版](./README_CN.md)

**EasyReader** is a lightweight Android TXT ebook reader that allows you to import TXT files from your device. It features bookshelf management, resume reading, dark mode, chapter detection, encoding selection, and more. Compatible with Android 4.0 (API 16) and above.

---

## Features

- 📚 **Bookshelf Management** – Add, delete books, and batch delete with multi-selection.
- 🔖 **Resume Reading** – Automatically saves your position and jumps back when reopened.
- 🌙 **Dark Mode** – Switch between dark and light themes, globally remembered.
- 📖 **Chapter Detection** – Automatically detects chapters like "第X章" and numeric chapters (filters years 1900-2099).
- 🔤 **Encoding Selection** – Supports UTF-8, GBK, GB2312, BIG5, etc., remembers per-book encoding.
- 🔎 **Table of Contents** – Jump to any chapter quickly.
- 📏 **Font Size Adjustment** – Freely adjust text size.
- 📄 **Paged Reading** – Pages by chapter, smooth reading for large files.
- ⚡ **Fling Scrolling** – Smooth inertial scrolling.

---

## Screenshots

*(Place your screenshots here)*

| Bookshelf | Reader | Table of Contents |
|:---:|:---:|:---:|
| ![](screenshots/bookshelf.png) | ![](screenshots/reader.png) | ![](screenshots/toc.png) |

---

## Installation & Usage

1. **Download APK**: Get the latest release from [Releases](https://github.com/550Cool/EasyReader/releases) and install.
2. **Add Books**: Tap the plus button at the bottom right, choose a TXT file via the system file picker.
3. **Read**: Tap a book on the bookshelf to start reading.
4. **Table of Contents**: Tap the screen to show controls, then tap the "Contents" button.
5. **Switch Encoding**: If the text is garbled, tap the "Encoding" button and select a proper encoding.
6. **Dark Mode**: Toggle in the menu (top right) on the bookshelf screen.

---

## Build Instructions

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/EasyReader.git
2. Open the project with Android Studio.

3. Ensure the SDK versions are set correctly in build.gradle (minSdk 16, targetSdk 33+).

4. Build and run.

## Dependencies
AndroidX AppCompat

Material Components

RecyclerView

Contributing
Issues and Pull Requests are welcome!

License
This project is open-sourced under the MIT License.

Author
2Days

GitHub: @550Cool

Email: wohenaighs@163.com
