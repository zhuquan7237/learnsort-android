# 学习分类助手

学习分类助手是一个安卓优先的个人学习资料库 MVP。它把错题和知识笔记放在同一套资料体系里，支持手动整理、图片导入和可选的自定义 AI 接口。

## 当前版本

`v0.1.0` 是一个可以离线使用的本地优先 MVP：首次打开会提供演示数据，所有题目、笔记和 AI 接口设置保存在手机本地。

已实现：

- 错题库和知识库双板块切换。
- 新建、编辑、删除错题和知识笔记。
- 从系统文件选择器批量导入图片，并把图片题目放入“待整理”。
- 按关键词搜索，按掌握情况筛选错题。
- 记录学科、章节、题型、标签、错因、答案和解析。
- 一道错题一键整理成知识笔记。
- 配置 OpenAI 兼容的 `/chat/completions` 接口，为错题生成复习提纲。
- 本地 JSON 持久化，重启应用后资料仍然保留。

## 使用方式

1. 在“错题库”点击“新建题目”，或点击“导入图片”。
2. 点开题目补充答案、解析、错因和标签。
3. 点击“整理成知识笔记”把错题内容同步到知识库。
4. 在右上角“设置”中填写自定义 AI 接口地址、模型名称和 API Key。
5. 在题目详情中点击“AI复习提纲”。

接口地址可以填写服务根地址，例如 `https://example.com/v1`；应用会自动请求其 `/chat/completions` 路径。局域网 HTTP 接口也被允许用于个人测试。

## 构建

环境要求：JDK 17、Android SDK、Android API 35、Gradle Wrapper。

```powershell
./gradlew.bat assembleRelease --no-daemon
```

生成文件：`app/build/outputs/apk/release/app-release.apk`

当前 Release APK 使用本地 debug 签名，适合个人测试和 GitHub 下载体验。正式分发前应替换为自己的 release keystore。

## MVP 边界

图片导入已经可用，但本版本不会自动 OCR、自动切题或自动分类；导入后会创建待整理题目，内容由用户手动补充。AI 复习提纲依赖用户配置的兼容接口，未配置时应用仍可作为本地错题和知识库使用。

后续可以加入 OCR、批量任务重试、原图预览、复习日期、Word/PDF 导出、知识点关联题目和真正的复习音频。

## 项目结构

- `app/src/main/java/com/learnsort/app/MainActivity.kt`：MVP UI、数据模型、本地存储和 AI 请求。
- `app/src/main/AndroidManifest.xml`：应用入口、网络权限和自定义接口配置。
- `app/build.gradle.kts`：Android 构建配置。
