package com.shiguang.test;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

public class MarkdownImageBackup {

    public static void main(String[] args) {
        // 是否将本地图片上传到图床服务并替换图片路径
        boolean uploadToImageBed = false;
        // 是否将图床图片路径替换为本地图片路径
        boolean replaceImageBedWithLocal = false;

        // 指定Markdown文件所在的目录
        String markdownFolderPath = "D:\\Desktop\\test";
        // 指定图片备份的目标目录
        String outputFolderPath = "D:\\Desktop";
        // 指定图床服务的URL
        String imageUploadUrl = "https://your_img_server_path/api/v1";
        // 邮箱和密码
        String email = "your_email";
        String password = "your_password";

        try {
            // 获取授权 Token
            String authToken = getAuthToken(imageUploadUrl, email, password);
            System.out.println("authToken: " + authToken);
            // 调用备份方法
            backupImagesFromMarkdownFiles(markdownFolderPath, outputFolderPath, imageUploadUrl, authToken, uploadToImageBed, replaceImageBedWithLocal);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取授权 Token
     *
     * @param imageUploadUrl 图床服务的URL
     * @param email          邮箱
     * @param password       密码
     * @return 授权 Token
     * @throws IOException 如果发生I/O错误
     */
    private static String getAuthToken(String imageUploadUrl, String email, String password) throws IOException {
        // 创建URL对象
        URL url = new URL(imageUploadUrl + "/tokens");
        // 打开HTTP连接
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");

        // 构建请求体
        String requestBody = String.format("{\"email\":\"%s\",\"password\":\"%s\"}", email, password);

        // 发送请求
        try (OutputStream out = connection.getOutputStream();
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, "UTF-8"), true)) {
            writer.append(requestBody);
            writer.flush();
        }

        // 获取响应
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            // 解析响应，获取 token
            String token = parseTokenFromResponse(response.toString());
//            System.out.println("获取到的授权 Token: " + token);
            return "Bearer " + token;
        }
    }

    /**
     * 解析响应，获取 token
     *
     * @param response 响应内容
     * @return 授权 Token
     */
    private static String parseTokenFromResponse(String response) {
        // 假设响应格式为 {"status":true,"message":"Success","data":{"token":"your-token"}}
        int startIndex = response.indexOf("\"token\":\"") + 9;
        int endIndex = response.indexOf("\"", startIndex);
        return response.substring(startIndex, endIndex);
    }

    /**
     * 备份Markdown文件中的图片
     *
     * @param markdownFolderPath       Markdown文件所在的目录
     * @param outputFolderPath         图片备份的目标目录
     * @param imageUploadUrl           图床服务的URL
     * @param authToken                授权 Token
     * @param uploadToImageBed         是否上传到图床服务并替换本地资源图片地址
     * @param replaceImageBedWithLocal 是否将图床图片路径替换为本地图片路径
     * @throws IOException 如果发生I/O错误
     */
    public static void backupImagesFromMarkdownFiles(String markdownFolderPath, String outputFolderPath, String imageUploadUrl, String authToken, boolean uploadToImageBed, boolean replaceImageBedWithLocal) throws IOException {
        // 获取Markdown文件所在的目录
        File folder = new File(markdownFolderPath);
        // 获取目录下的所有文件
        File[] listOfFiles = folder.listFiles();

        // 如果目录下没有文件，输出提示信息并返回
        if (listOfFiles == null) {
            System.out.println("在指定的目录中找不到文件!!!");
            return;
        }

        // 遍历目录下的所有文件
        for (File file : listOfFiles) {
            String fileName = file.getName();
            // 只处理Markdown文件（以.md结尾的文件）
            if (file.isFile() && fileName.endsWith(".md")) {
                System.out.println("开始处理文件: " + fileName);
                // 处理每个Markdown文件
                processMarkdownFile(file, markdownFolderPath, outputFolderPath, imageUploadUrl, authToken, uploadToImageBed, replaceImageBedWithLocal);
            }
        }
    }

    /**
     * 处理单个Markdown文件，提取其中的图片并备份
     *
     * @param markdownFile             Markdown文件
     * @param markdownFolderPath       Markdown文件所在的目录
     * @param outputFolderPath         图片备份的目标目录
     * @param imageUploadUrl           图床服务的URL
     * @param authToken                授权 Token
     * @param uploadToImageBed         是否上传到图床服务并替换本地资源图片地址
     * @param replaceImageBedWithLocal 是否将图床图片路径替换为本地图片路径
     * @throws IOException 如果发生I/O错误
     */
    private static void processMarkdownFile(File markdownFile, String markdownFolderPath, String outputFolderPath, String imageUploadUrl, String authToken, boolean uploadToImageBed, boolean replaceImageBedWithLocal) throws IOException {
        // 获取Markdown文件的名称（去掉.md后缀）
        String fileName = markdownFile.getName().replace(".md", "");
        // 构建图片备份的目标目录路径
        Path outputDir = Paths.get(outputFolderPath, fileName);

        // 如果目标目录不存在，创建该目录
        if (!Files.exists(outputDir)) {
            System.out.println("创建目录: " + outputDir + "");
            Files.createDirectories(outputDir);
        }

        // 读取Markdown文件的内容
        String originalContent = new String(Files.readAllBytes(markdownFile.toPath()));
        String content = originalContent;
        // 定义正则表达式，匹配Markdown中的图片路径
        Pattern pattern = Pattern.compile("!\\[.*?\\]\\((.*?)\\)");
        Matcher matcher = pattern.matcher(content);

        // 创建日志文件目录
        Path logDir = Paths.get("D:\\Desktop\\logs", fileName);
        if (!Files.exists(logDir)) {
            Files.createDirectories(logDir);
        }

        // 初始化日志文件路径
        Path localToImageBedLogFile = null;
        Path imageBedToLocalLogFile = null;
        Path contentLogFile = null;
        BufferedWriter localToImageBedWriter = null;
        BufferedWriter imageBedToLocalWriter = null;
        BufferedWriter contentLogWriter = null;

        // 如果需要替换路径，创建相应的日志文件
        boolean hasLocalToImageBed = false;
        boolean hasImageBedToLocal = false;

        if (uploadToImageBed) {
            localToImageBedLogFile = logDir.resolve("local_to_imagebed.txt");
            localToImageBedWriter = Files.newBufferedWriter(localToImageBedLogFile);
            localToImageBedWriter.write("本地路径替换为图床路径:\n");
        }
        if (replaceImageBedWithLocal) {
            imageBedToLocalLogFile = logDir.resolve("imagebed_to_local.txt");
            imageBedToLocalWriter = Files.newBufferedWriter(imageBedToLocalLogFile);
            imageBedToLocalWriter.write("图床路径替换为本地路径:\n");
        }
        contentLogFile = logDir.resolve("content_log.txt");
        contentLogWriter = Files.newBufferedWriter(contentLogFile);

        // 用于存储已经替换过的图片路径
        Map<String, String> replacedImages = new HashMap<>();

        try {
            // 遍历匹配到的所有图片路径
            while (matcher.find()) {
                // 获取图片路径
                String imagePath = matcher.group(1);
                // 判断图片路径是本地资源还是图床资源
                if (imagePath.startsWith("http") || imagePath.startsWith("https")) {
                    // 下载图床资源图片
                    if (replaceImageBedWithLocal) {
                        String localImagePath = replacedImages.get(imagePath);
                        if (localImagePath == null) {
                            localImagePath = downloadImage(imagePath, outputDir);
                            replacedImages.put(imagePath, localImagePath);
                        }
                        content = content.replace(imagePath, localImagePath);
                        imageBedToLocalWriter.write(imagePath + " -> " + localImagePath + "\n");
                        hasImageBedToLocal = true;
                    } else {
                        downloadImage(imagePath, outputDir);
                    }
                } else {
                    // 复制本地资源图片到指定目录
                    copyLocalImage(imagePath, markdownFolderPath, outputDir);

                    // 上传本地资源图片到图床
                    if (uploadToImageBed) {
                        String newImagePath = replacedImages.get(imagePath);
                        if (newImagePath == null) {
                            newImagePath = uploadImageToImageBed(imagePath, markdownFolderPath, imageUploadUrl, authToken, 1);
                            if ("".equals(newImagePath)) {
                                continue;
                            }
                            replacedImages.put(imagePath, newImagePath);
                        }
                        // 替换Markdown文件中的图片路径
                        content = content.replace(imagePath, newImagePath);
                        localToImageBedWriter.write(imagePath + " -> " + newImagePath + "\n");
                        hasLocalToImageBed = true;
                    }
                }
            }

            // 将替换后的内容写入新的Markdown文件
            Path newMarkdownFile = Paths.get(outputFolderPath, markdownFile.getName());
            Files.write(newMarkdownFile, content.getBytes());
            System.out.println("新生成的Markdown文件已保存到: " + newMarkdownFile);

            // 记录替换前和替换后的文本值
            contentLogWriter.write("############################# 替换前的内容: #############################\n" + originalContent + "\n\n");
            contentLogWriter.write("############################# 替换后的内容: #############################\n" + content + "\n");
        } finally {
            // 关闭日志文件写入器
            if (localToImageBedWriter != null) {
                localToImageBedWriter.close();
                if (!hasLocalToImageBed) {
                    Files.deleteIfExists(localToImageBedLogFile);
                } else {
                    System.out.println("本地路径替换为图床路径的日志已保存到: " + localToImageBedLogFile);
                }
            }
            if (imageBedToLocalWriter != null) {
                imageBedToLocalWriter.close();
                if (!hasImageBedToLocal) {
                    Files.deleteIfExists(imageBedToLocalLogFile);
                } else {
                    System.out.println("图床路径替换为本地路径的日志已保存到: " + imageBedToLocalLogFile);
                }
            }
            if (contentLogWriter != null) {
                contentLogWriter.close();
                if (!hasLocalToImageBed && !hasImageBedToLocal) {
                    Files.deleteIfExists(contentLogFile);
                } else {
                    System.out.println("替换前和替换后的文本值已保存到: " + contentLogFile);
                }
            }
        }
    }

    /**
     * 下载图床资源图片并保存到指定目录
     *
     * @param imageUrl  图片的URL
     * @param outputDir 图片保存的目标目录
     * @return 本地图片路径
     * @throws IOException 如果发生I/O错误
     */
    private static String downloadImage(String imageUrl, Path outputDir) throws IOException {
        // 创建URL对象
        URL url = new URL(imageUrl);
        // 获取图片的文件名
        String fileName = Paths.get(url.getPath()).getFileName().toString();
        // 构建图片保存的目标路径
        Path outputPath = outputDir.resolve(fileName);

        // 下载图片并保存到目标路径
        try (InputStream in = url.openStream()) {
            Files.copy(in, outputPath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("图床图片已保存到: " + outputPath);
        }

        // 返回本地图片路径（相对路径）
        return outputDir.getFileName().toString() + File.separator + fileName;
    }

    /**
     * 复制本地资源图片到指定目录
     *
     * @param imagePath          本地图片的路径（相对路径或绝对路径）
     * @param markdownFolderPath Markdown文件所在的目录
     * @param outputDir          图片保存的目标目录
     * @throws IOException 如果发生I/O错误
     */
    private static void copyLocalImage(String imagePath, String markdownFolderPath, Path outputDir) throws IOException {
        Path sourcePath;
        // 判断图片路径是否是绝对路径
        if (Paths.get(imagePath).isAbsolute()) {
            sourcePath = Paths.get(imagePath);
        } else {
            // 拼接Markdown文件所在的目录获取图片的完整路径
            sourcePath = Paths.get(markdownFolderPath, imagePath);
        }

        // 获取图片的文件名
        String fileName = sourcePath.getFileName().toString();
        // 构建图片保存的目标路径
        Path outputPath = outputDir.resolve(fileName);

        // 复制图片到目标路径
        Files.copy(sourcePath, outputPath, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("本地资源图片已保存到: " + outputPath);
    }

    /**
     * 上传本地资源图片到图床
     *
     * @param imagePath          本地图片的路径（相对路径或绝对路径）
     * @param markdownFolderPath Markdown文件所在的目录
     * @param imageUploadUrl     图床服务的URL
     * @param authToken          授权 Token
     * @param strategyId         储存策略ID，默认值为1
     * @return 上传后的图片URL
     * @throws IOException 如果发生I/O错误
     */
    private static String uploadImageToImageBed(String imagePath, String markdownFolderPath, String imageUploadUrl, String authToken, int strategyId) throws IOException {

        if (StringUtils.isEmpty(imagePath)) {
            strategyId = 1;
        }
        Path sourcePath;
        // 判断图片路径是否是绝对路径
        if (Paths.get(imagePath).isAbsolute()) {
            sourcePath = Paths.get(imagePath);
        } else {
            // 拼接Markdown文件所在的目录获取图片的完整路径
            sourcePath = Paths.get(markdownFolderPath, imagePath);
        }

        // 获取图片的文件名
        String fileName = sourcePath.getFileName().toString();
        // 获取图片的文件类型
        String fileType = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();

        // 创建URL对象
        URL url = new URL(imageUploadUrl + "/upload");
        // 打开HTTP连接
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Authorization", authToken);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW");

        // 构建 multipart/form-data 请求体
        String boundary = "----WebKitFormBoundary7MA4YWxkTrZu0gW";
        String lineEnd = "\r\n";
        String twoHyphens = "--";

        try (OutputStream out = connection.getOutputStream();
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, "UTF-8"), true)) {
            // 添加文件部分
            writer.append(twoHyphens).append(boundary).append(lineEnd);
            writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(fileName).append("\"").append(lineEnd);
            writer.append("Content-Type: ").append(fileType).append(lineEnd);
            writer.append(lineEnd);
            writer.flush();

            // 写入文件内容
            try (InputStream in = Files.newInputStream(sourcePath)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
            out.flush();

            // 添加策略ID部分
            writer.append(lineEnd).append(twoHyphens).append(boundary).append(lineEnd);
            writer.append("Content-Disposition: form-data; name=\"strategy_id\"").append(lineEnd);
            writer.append(lineEnd);
            writer.append(String.valueOf(strategyId)).append(lineEnd);
            writer.flush();

            // 结束 multipart/form-data 请求体
            writer.append(lineEnd).append(twoHyphens).append(boundary).append(twoHyphens).append(lineEnd);
            writer.flush();
        }

        // 获取上传后的图片URL
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            // 解析响应，获取图片访问URL
            String imageUrl = parseImageUrlFromResponse(response.toString());
            System.out.println("图片上传后返回路径: " + imageUrl);
            return imageUrl;
        }
    }

    /**
     * 解析响应，获取图片访问URL或错误消息
     *
     * @param response 响应内容
     * @return 图片访问URL或错误消息
     */
    private static String parseImageUrlFromResponse(String response) {
        // 假设响应格式为 {"status":true,"message":"Success","data":{"key":"xxx","name":"xxx","pathname":"xxx","origin_name":"xxx","size":xxx,"mimetype":"xxx","extension":"xxx","md5":"xxx","sha1":"xxx","links":{"url":"xxx","thumbnail_url":"xxx"}}}
        // 或者 {"status":false,"message":"错误消息","data":{}}
        try {
            // 解析JSON响应
            JSONObject jsonResponse = new JSONObject(response);
            boolean status = jsonResponse.getBoolean("status");
            if (status) {
                // 上传成功，提取图片访问URL
                JSONObject data = jsonResponse.getJSONObject("data");
                JSONObject links = data.getJSONObject("links");
                String imageUrl = links.getString("url");
                return imageUrl;
            } else {
                // 上传失败，提取错误消息
                String errorMessage = jsonResponse.getString("message");
                System.out.println("上传失败: " + errorMessage);
                return "";
            }
        } catch (JSONException e) {
            e.printStackTrace();
            System.out.println("解析响应失败: " + e.getMessage());
            return "";
        }
    }
}