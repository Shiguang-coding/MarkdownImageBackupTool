


# MarkdownImageBackupTool

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](https://opensource.org/licenses/MIT)

MarkdownImageBackupTool 是一个用于批量备份迁移 Markdown 文件中图片的工具。它可以将本地图片上传到图床服务并替换图片路径，或者将图床图片下载到本地并替换图片路径。生成的 Markdown 文件会直接保存到指定的目标目录，而图片则保存到以文章名称命名的文件夹内。

## 功能特性

> 本工具中使用到的图床是[蓝空图床](https://github.com/lsky-org/lsky-pro)，如使用其他图床可自行拓展

- **批量处理**：支持批量处理多个 Markdown 文件，自动备份所有文件中的图片。
- **图片上传到图床**：将本地图片上传到图床服务，并替换 Markdown 文件中的图片路径。
- **图片下载到本地**：将图床图片下载到本地，并替换 Markdown 文件中的图片路径。
- **生成新的 Markdown 文件**：生成的 Markdown 文件会直接保存到指定的目标目录，而图片则保存到以文章名称命名的文件夹内。
- **日志记录**：记录图片路径替换的日志，方便查看和追溯。
- **异常处理**：记录处理过程中出现的错误，方便问题排查和处理。

### 批量处理功能

MarkdownImageBackupTool 支持批量处理多个 Markdown 文件。它会自动扫描指定目录下的所有 Markdown 文件，并逐一处理每个文件中的图片。以下是批量处理功能的详细说明：

1. **自动扫描目录**：工具会自动扫描 `markdownFolderPath` 指定的目录，查找所有以 `.md` 结尾的 Markdown 文件。
2. **逐一处理文件**：对于每个找到的 Markdown 文件，工具会提取其中的图片路径，并根据配置进行相应的处理（上传到图床或下载到本地）。
3. **生成新的 Markdown 文件**：处理完成后，工具会生成一个新的 Markdown 文件，并将其保存到 `outputFolderPath` 指定的目录下。
4. **保存图片**：所有处理的图片会保存到以文章名称命名的文件夹内。

通过这些步骤，你可以轻松地批量备份多个 Markdown 文件中的图片。

### 日志记录功能

MarkdownImageBackupTool 提供了详细的日志记录功能，帮助你跟踪和追溯图片路径的替换过程。以下是日志记录功能的详细说明：

1. **日志目录**：日志文件会保存在 `outputFolderPath\logs` 目录下，每个 Markdown 文件的处理日志会保存在以文件名命名的子目录中。

2. **日志文件**：

   - `local_to_imagebed.txt`：记录本地图片路径替换为图床路径的日志。
   - `imagebed_to_local.txt`：记录图床图片路径替换为本地路径的日志。
   - `content_log.txt`：记录替换前和替换后的 Markdown 文件内容。
   - `absolute_to_relative.txt`: 记录本地图片绝对路径替换为本地图片相对路径的日志。
   - `error_log`: 记录处理过程中出现的错误信息。

3. **日志内容**：

   - `local_to_imagebed.txt` 、 `imagebed_to_local.txt`、`absolute_to_relative.txt` 文件中会记录每条路径替换的详细信息，格式如下：

     ```bash
     原路径 -> 新路径
     ```

   - `content_log.txt` 文件中会记录替换前和替换后的 Markdown 文件内容，方便你对比和检查。
   
   - `error_log` 文件中会记录每张图片处理失败的原因和堆栈信息。

#### 日志示例

假设你处理了一个名为 `example.md` 的 Markdown 文件，以下是日志文件的示例内容：

`local_to_imagebed.txt`

```bash
本地路径替换为图床路径:
./images/image1.png -> https://your_img_server_path/image1.png
./images/image2.png -> https://your_img_server_path/image2.png
```

`imagebed_to_local.txt`

```bash
图床路径替换为本地路径:
https://your_img_server_path/image1.png -> example/image1.png
https://your_img_server_path/image2.png -> example/image2.png
```

`content_log.txt`

```bash
############################# 替换前的内容: #############################
![Image 1](./images/image1.png)
![Image 2](./images/image2.png)

############################# 替换后的内容: #############################
![Image 1](https://your_img_server_path/image1.png)
![Image 2](https://your_img_server_path/image2.png)
```

`absolute_to_relative.txt`

```bash
本地绝对路径替换为相对路径:
C:/Users/shiguang/AppData/Roaming/Typora/typora-user-images/image-20241015141151848.png -> error-src-refspec-main-does-not-match-any\image-20241015141151848.png
C:/Users/shiguang/AppData/Roaming/Typora/typora-user-images/image-20241015141717812.png -> error-src-refspec-main-does-not-match-any\image-20241015141717812.png
```

`error_log`

```bash
处理图片 C:/Users/shiguang/AppData/Roaming/Typora/typora-user-images/image-20241015120316901.png 时发生错误: 每小时内你最多可以上传 100 张图片
java.lang.RuntimeException: 每小时内你最多可以上传 100 张图片
	at com.shiguang.test.MarkdownImageBackup.parseImageUrlFromResponse(MarkdownImageBackup.java:508)
	at com.shiguang.test.MarkdownImageBackup.uploadImageToImageBed(MarkdownImageBackup.java:480)
	at com.shiguang.test.MarkdownImageBackup.processMarkdownFile(MarkdownImageBackup.java:255)
	at com.shiguang.test.MarkdownImageBackup.backupImagesFromMarkdownFiles(MarkdownImageBackup.java:132)
	at com.shiguang.test.MarkdownImageBackup.main(MarkdownImageBackup.java:39)
```



通过这些日志文件，你可以清晰地了解每个 Markdown 文件中图片路径的替换过程，方便后续的检查和追溯。

## 安装

### 环境要求

- Java 8 或更高版本
- Maven（用于构建项目）

### 构建项目

1. 克隆仓库到本地：

   ```bash
   git clone https://github.com/Shiguang-coding/MarkdownImageBackupTool.git
   cd MarkdownImageBackupTool
   ```
   
2. 使用 Maven 构建项目：

   ```bash
   mvn clean install
   ```

### 运行项目

1. 直接运行`Main`方法

2. 或者，你可以编译并运行项目：

   ```bash
   mvn exec:java -Dexec.mainClass="com.shiguang.test.MarkdownImageBackup"
   ```

3. 或者，你可以直接运行生成的 JAR 文件：

   ```bash
   java -jar target/MarkdownImageBackupTool-1.0-SNAPSHOT.jar
   ```

## 配置

在 `MarkdownImageBackup.java` 文件中，你可以配置以下参数：

- `uploadToImageBed`：是否将本地图片上传到图床服务并替换图片路径。
- `replaceImageBedWithLocal`：是否将图床图片路径替换为本地图片路径。
- `markdownFolderPath`：Markdown 文件所在的目录。
- `outputFolderPath`：图片备份的目标目录。
- `imageUploadUrl`：图床服务的 URL。
- `email` 和 `password`：用于获取图床服务的授权 Token。

## 使用示例

假设你有一个 Markdown 文件目录 `D:\\Desktop\\test`，并且你想将图片备份到 `D:\\Desktop` 目录下。你可以按照以下步骤操作：

1. 修改 `MarkdownImageBackup.java` 文件中的配置参数：

   ```java
   String markdownFolderPath = "D:\\Desktop\\test";
   String outputFolderPath = "D:\\Desktop";
   String imageUploadUrl = "https://your_img_server_path/api/v1";
   String email = "your_email"
   String password = "your_email";
   ```

2. 运行项目

3. 检查生成的 Markdown 文件和图片是否正确备份到指定目录。

## 贡献

我们欢迎任何形式的贡献！如果你有任何建议、问题或改进，请提交 Issue 或 Pull Request。

## 许可证

本项目采用 MIT 许可证。有关更多信息，请参阅 [LICENSE](LICENSE) 文件。



