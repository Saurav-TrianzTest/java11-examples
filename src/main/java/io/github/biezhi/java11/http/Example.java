package io.github.biezhi.java11.http;

import com.google.gson.Gson;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static java.util.stream.Collectors.toList;

/**
 * Java 11 的 Http Client 示例 - Cloud-ready version
 * <p>
 * 移除了 HttpResponse.BodyHandler.asString()
 * 使用 HttpResponse.BodyHandlers.ofString() 代替功能
 * <p>
 * Cloud improvements:
 * - Externalized URLs via environment variables
 * - AWS Secrets Manager for credentials
 * - Configurable proxy settings
 * - Proper temp file cleanup with shutdown hooks
 * - Cloud storage integration for file uploads
 *
 * @author biezhi
 * @date 2018/7/10
 */
public class Example {

    // Configuration from environment variables
    private static final String UPLOAD_ENDPOINT = System.getenv().getOrDefault("UPLOAD_ENDPOINT_URL", "http://localhost:8080/upload/");
    private static final String PROXY_HOST = System.getenv().getOrDefault("PROXY_HOST", "127.0.0.1");
    private static final int PROXY_PORT = Integer.parseInt(System.getenv().getOrDefault("PROXY_PORT", "1080"));
    private static final String AWS_REGION = System.getenv().getOrDefault("AWS_REGION", "us-east-1");
    private static final String AUTH_SECRET_NAME = System.getenv().getOrDefault("AUTH_SECRET_NAME", "app/basic-auth");
    private static final String S3_BUCKET = System.getenv().getOrDefault("S3_BUCKET_NAME", "my-app-bucket");
    private static final String UPLOAD_FILE_S3_KEY = System.getenv().getOrDefault("UPLOAD_FILE_S3_KEY", "files-to-upload.txt");

    // HTTP client configuration
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(
            Long.parseLong(System.getenv().getOrDefault("HTTP_REQUEST_TIMEOUT_SECONDS", "30"))
    );

    // 同步调用 GET
    public static void syncGet(String uri) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println(response.statusCode());
        System.out.println(response.body());
    }

    // 异步调用 GET
    public static void asyncGet(String uri) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .build();

        CompletableFuture<HttpResponse<String>> responseCompletableFuture = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        responseCompletableFuture.whenComplete((resp, t) -> {
            if (t != null) {
                t.printStackTrace();
            } else {
                System.out.println(resp.body());
                System.out.println(resp.statusCode());
            }
        }).join();
    }

    // 异步调用 POST
    public static void asyncPost() throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        Gson gson = new Gson();
        Foo  foo  = new Foo();
        foo.name = "王爵nice";
        foo.url = "https://github.com/biezhi";

        String jsonBody = gson.toJson(foo);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("https://httpbin.org/post"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .whenComplete((resp, t) -> {
                    if (t != null) {
                        t.printStackTrace();
                    } else {
                        System.out.println(resp.body());
                        System.out.println(resp.statusCode());
                    }
                }).join();
    }

    // 下载文件 - Cloud-ready with proper cleanup
    public static void downloadFile() throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .build();

        String downloadUrl = System.getenv().getOrDefault("DOWNLOAD_URL", "https://labs.consol.de/");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(downloadUrl))
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();

        // Create temp file with proper cleanup strategy
        Path tempFile = Files.createTempFile("consol-labs-home", ".html");

        // Register shutdown hook for cleanup
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Files.deleteIfExists(tempFile);
            } catch (Exception e) {
                System.err.println("Failed to cleanup temp file: " + tempFile);
            }
        }));

        HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(tempFile));
        System.out.println(response.statusCode());
        System.out.println(response.body());

        // Explicit cleanup after use
        Files.deleteIfExists(tempFile);
    }

    // 上传文件 - Cloud-ready with S3 integration
    public static void uploadFile() throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .build();

        // Option 1: Upload from S3 (stream directly)
        software.amazon.awssdk.services.s3.S3Client s3Client = software.amazon.awssdk.services.s3.S3Client.builder()
                .region(software.amazon.awssdk.regions.Region.of(AWS_REGION))
                .build();

        software.amazon.awssdk.services.s3.model.GetObjectRequest getRequest =
                software.amazon.awssdk.services.s3.model.GetObjectRequest.builder()
                .bucket(S3_BUCKET)
                .key(UPLOAD_FILE_S3_KEY)
                .build();

        byte[] fileContent = s3Client.getObject(getRequest).readAllBytes();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(UPLOAD_ENDPOINT))
                .timeout(REQUEST_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofByteArray(fileContent))
                .build();

        HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
        System.out.println(response.statusCode());

        s3Client.close();
    }

    // 设置代理 - Cloud-ready with configurable proxy
    public static void proxy() throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .proxy(ProxySelector.of(new InetSocketAddress(PROXY_HOST, PROXY_PORT)))
                .connectTimeout(REQUEST_TIMEOUT)
                .build();

        String targetUrl = System.getenv().getOrDefault("PROXY_TARGET_URL", "https://www.google.com");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(targetUrl))
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(response.statusCode());
        System.out.println(response.body());
    }

    /**
     * Retrieve credentials from AWS Secrets Manager
     */
    private static class CredentialPair {
        String username;
        String password;
    }

    private static CredentialPair getCredentialsFromSecretsManager() {
        SecretsManagerClient secretsClient = SecretsManagerClient.builder()
                .region(software.amazon.awssdk.regions.Region.of(AWS_REGION))
                .build();

        GetSecretValueRequest getSecretValueRequest = GetSecretValueRequest.builder()
                .secretId(AUTH_SECRET_NAME)
                .build();

        GetSecretValueResponse getSecretValueResponse = secretsClient.getSecretValue(getSecretValueRequest);
        String secret = getSecretValueResponse.secretString();

        secretsClient.close();

        // Parse JSON secret (expected format: {"username":"xxx","password":"xxx"})
        com.google.gson.JsonObject jsonObject = new com.google.gson.JsonParser().parse(secret).getAsJsonObject();
        CredentialPair creds = new CredentialPair();
        creds.username = jsonObject.get("username").getAsString();
        creds.password = jsonObject.get("password").getAsString();

        return creds;
    }

    // basic 认证 - Cloud-ready with AWS Secrets Manager
    public static void basicAuth() throws Exception {
        // Retrieve credentials from AWS Secrets Manager instead of hardcoding
        CredentialPair credentials = getCredentialsFromSecretsManager();

        HttpClient client = HttpClient.newBuilder()
                .authenticator(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(credentials.username, credentials.password.toCharArray());
                    }
                })
                .connectTimeout(REQUEST_TIMEOUT)
                .build();

        String authUrl = System.getenv().getOrDefault("AUTH_TARGET_URL", "https://labs.consol.de");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(authUrl))
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println(response.statusCode());
        System.out.println(response.body());
    }

    // 访问 HTTP2 网址
    public static void http2() throws Exception {
        HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .version(HttpClient.Version.HTTP_2)
                .build()
                .sendAsync(HttpRequest.newBuilder()
                                .uri(new URI("https://http2.akamai.com/demo"))
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofString())
                .whenComplete((resp, t) -> {
                    if (t != null) {
                        t.printStackTrace();
                    } else {
                        System.out.println(resp.body());
                        System.out.println(resp.statusCode());
                    }
                }).join();
    }

    // 并行请求
    public void getURIs(List<URI> uris) {
        HttpClient client = HttpClient.newHttpClient();
        List<HttpRequest> requests = uris.stream()
                .map(HttpRequest::newBuilder)
                .map(HttpRequest.Builder::build)
                .collect(toList());

        CompletableFuture.allOf(requests.stream()
                .map(request -> client.sendAsync(request, HttpResponse.BodyHandlers.ofString()))
                .toArray(CompletableFuture<?>[]::new))
                .join();
    }

    public static void main(String[] args) throws Exception {
//        syncGet("https://biezhi.me");
//        asyncGet("https://biezhi.me");
        asyncPost();
//        http2();
    }

}
