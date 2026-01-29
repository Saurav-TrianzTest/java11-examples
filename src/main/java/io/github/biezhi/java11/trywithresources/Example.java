package io.github.biezhi.java11.trywithresources;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Java SE 7 引入了一个新的异常处理结构：Try-With-Resources，来自动管理资源。
 * <p>
 * 这个新的声明结构主要目的是实现"Automatic Better Resource Management"（"自动资源管理"）。
 * <p>
 * Java SE 9 将对这个声明作出一些改进来避免一些冗长写法，同时提高可读性。
 * <p>
 * Cloud-ready version using AWS S3 or classpath resources instead of local file system.
 *
 * @author biezhi
 * @date 2018/7/10
 */
public class Example {

    private static final String S3_BUCKET = System.getenv().getOrDefault("S3_BUCKET_NAME", "my-app-bucket");
    private static final String AWS_REGION = System.getenv().getOrDefault("AWS_REGION", "us-east-1");
    private static final String README_FILE_PATH = System.getenv().getOrDefault("README_FILE_PATH", "README.md");
    private static final boolean USE_S3 = Boolean.parseBoolean(System.getenv().getOrDefault("USE_S3_STORAGE", "false"));

    public static void main(String[] args) throws Exception {
        BufferedReader reader1;

        if (USE_S3) {
            // Cloud mode: Read from S3
            S3Client s3Client = S3Client.builder()
                    .region(software.amazon.awssdk.regions.Region.of(AWS_REGION))
                    .build();

            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(S3_BUCKET)
                    .key(README_FILE_PATH)
                    .build();

            ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getRequest);
            reader1 = new BufferedReader(new InputStreamReader(s3Object, StandardCharsets.UTF_8));
        } else {
            // Classpath mode: Read from classpath resources (packaged with JAR)
            InputStream resourceStream = Example.class.getClassLoader().getResourceAsStream(README_FILE_PATH);
            if (resourceStream == null) {
                throw new IllegalStateException("Resource not found: " + README_FILE_PATH + ". Please ensure the file is in the classpath or set USE_S3_STORAGE=true with valid S3_BUCKET_NAME.");
            }
            reader1 = new BufferedReader(new InputStreamReader(resourceStream, StandardCharsets.UTF_8));
        }

        try (reader1) {
            while (reader1.ready()) {
                System.out.println(reader1.readLine());
            }
        }
    }

}
