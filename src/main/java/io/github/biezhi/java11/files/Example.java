package io.github.biezhi.java11.files;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Files 读写文本文件 - Cloud-ready version using AWS S3
 *
 * @author biezhi
 * @date 2018/7/31
 */
public class Example {

    private static final String S3_BUCKET = System.getenv().getOrDefault("S3_BUCKET_NAME", "my-app-bucket");
    private static final String AWS_REGION = System.getenv().getOrDefault("AWS_REGION", "us-east-1");

    public static void main(String[] args) throws Exception {
        String text = "Hello biezhi.";

        // Initialize S3 client with AWS SDK v2
        S3Client s3Client = S3Client.builder()
                .region(software.amazon.awssdk.regions.Region.of(AWS_REGION))
                .build();

        String objectKey = "hello.txt";

        try {
            // 写入文本到 S3
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(S3_BUCKET)
                    .key(objectKey)
                    .contentType("text/plain")
                    .build();
            s3Client.putObject(putRequest, RequestBody.fromString(text, StandardCharsets.UTF_8));

            // 读取文本从 S3
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(S3_BUCKET)
                    .key(objectKey)
                    .build();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            s3Client.getObject(getRequest).transferTo(baos);
            String readText = baos.toString(StandardCharsets.UTF_8);
            System.out.println(text.equals(readText));

            // 删除文本从 S3
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(S3_BUCKET)
                    .key(objectKey)
                    .build();
            s3Client.deleteObject(deleteRequest);
        } finally {
            s3Client.close();
        }
    }

}