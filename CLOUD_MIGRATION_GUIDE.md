# Cloud Migration Guide - AWS Deployment

This guide documents the cloud-readiness fixes applied to make this application deployable to AWS.

## Overview

All identified cloud readiness issues have been fixed to ensure the application can successfully deploy and run in AWS environments (EC2, ECS, EKS, Lambda).

## Fixes Applied

### 1. File System Dependencies (Critical)

#### Issue: Local file system write/read operations
**Files Modified:**
- `src/main/java/io/github/biezhi/java11/files/Example.java`
- `src/main/java/io/github/biezhi/java11/trywithresources/Example.java`

**Changes:**
- Replaced local file operations with AWS S3 SDK v2
- Added fallback to classpath resources for containerized deployments
- Implemented configurable storage strategy via `USE_S3_STORAGE` environment variable

**Benefits:**
- Data persists across container restarts and scaling events
- Compatible with AWS Lambda's ephemeral storage limitations
- Supports both development (classpath) and production (S3) modes

### 2. Configuration Management (High Priority)

#### Issue: Hard-coded URLs and endpoints
**File Modified:** `src/main/java/io/github/biezhi/java11/http/Example.java`

**Changes:**
- Externalized all URLs via environment variables:
  - `UPLOAD_ENDPOINT_URL`
  - `DOWNLOAD_URL`
  - `AUTH_TARGET_URL`
  - `PROXY_TARGET_URL`

**Benefits:**
- Environment-specific configuration (dev, staging, prod)
- No code changes required for different deployment targets
- Follows 12-factor app principles

### 3. Security & Authentication (High Priority)

#### Issue: Hard-coded credentials
**File Modified:** `src/main/java/io/github/biezhi/java11/http/Example.java`

**Changes:**
- Integrated AWS Secrets Manager for credential retrieval
- Removed hard-coded username/password from source code
- Added `getCredentialsFromSecretsManager()` method

**Benefits:**
- Credentials never stored in source code or containers
- Automatic secret rotation support via AWS Secrets Manager
- IAM-based access control for secrets

**Required Secret Format in AWS Secrets Manager:**
```json
{
  "username": "your-username",
  "password": "your-password"
}
```

### 4. Proxy Configuration (Medium Priority)

#### Issue: Hard-coded proxy settings
**File Modified:** `src/main/java/io/github/biezhi/java11/http/Example.java`

**Changes:**
- Externalized proxy configuration:
  - `PROXY_HOST`
  - `PROXY_PORT`

**Benefits:**
- Different proxy settings per environment
- Easy to disable proxy in AWS VPC environments

### 5. Temporary File Management (Medium Priority)

#### Issue: Temp files without cleanup strategy
**File Modified:** `src/main/java/io/github/biezhi/java11/http/Example.java`

**Changes:**
- Added shutdown hooks for automatic cleanup
- Explicit cleanup after file use
- Added timeout configuration for HTTP operations

**Benefits:**
- Prevents storage exhaustion in container environments
- AWS Lambda compatible (512MB /tmp limit)
- Proper resource cleanup on application shutdown

### 6. Logging & Monitoring (Medium Priority)

#### Issue: System.out logging
**File Modified:** `src/main/java/io/github/biezhi/java11/string/Example.java`

**Changes:**
- Replaced all `System.out.println` with SLF4J logger
- Used parameterized logging for better performance
- Added structured logging support

**Benefits:**
- Compatible with AWS CloudWatch Logs
- Proper log levels and filtering
- Better performance with lazy evaluation
- Supports JSON structured logging with appropriate appenders

## Environment Variables Reference

See `.env.template` for all required environment variables.

### Required Variables:
- `AWS_REGION` - AWS region for S3 and Secrets Manager
- `S3_BUCKET_NAME` - S3 bucket for file storage

### Optional Variables (with defaults):
- `USE_S3_STORAGE` - Enable S3 storage (default: false)
- `HTTP_REQUEST_TIMEOUT_SECONDS` - HTTP timeout (default: 30)
- `PROXY_HOST` - Proxy host (default: 127.0.0.1)
- `PROXY_PORT` - Proxy port (default: 1080)

## AWS Resources Required

### 1. S3 Bucket
```bash
aws s3 mb s3://my-app-bucket --region us-east-1
```

### 2. AWS Secrets Manager Secret
```bash
aws secretsmanager create-secret \
  --name app/basic-auth \
  --secret-string '{"username":"myuser","password":"mypass"}' \
  --region us-east-1
```

### 3. IAM Permissions
The application requires the following IAM permissions:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:GetObject",
        "s3:PutObject",
        "s3:DeleteObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::my-app-bucket",
        "arn:aws:s3:::my-app-bucket/*"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "secretsmanager:GetSecretValue"
      ],
      "Resource": "arn:aws:secretsmanager:us-east-1:*:secret:app/basic-auth-*"
    }
  ]
}
```

## Dependencies Required

Add the following dependencies to your `pom.xml`:

```xml
<!-- AWS SDK v2 for S3 -->
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>s3</artifactId>
    <version>2.20.0</version>
</dependency>

<!-- AWS SDK v2 for Secrets Manager -->
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>secretsmanager</artifactId>
    <version>2.20.0</version>
</dependency>

<!-- SLF4J API -->
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
    <version>2.0.9</version>
</dependency>

<!-- Logback for structured logging -->
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>1.4.11</version>
</dependency>

<!-- Optional: Logback JSON encoder for CloudWatch -->
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

## Deployment Options

### Option 1: AWS ECS/EKS (Recommended)
- Package as executable JAR with embedded server
- Use container with environment variables
- Mount IAM role for AWS service access

### Option 2: AWS EC2
- Deploy JAR directly on EC2 instance
- Configure environment variables in systemd service
- Attach IAM instance profile

### Option 3: AWS Lambda
- Limited support due to temp file operations
- Ensure operations complete within 15-minute timeout
- Monitor /tmp storage usage (512MB limit)

## Testing the Migration

### 1. Local Testing with LocalStack
```bash
# Start LocalStack for local AWS service emulation
docker run -d -p 4566:4566 localstack/localstack

# Configure environment to use LocalStack
export AWS_ENDPOINT_URL=http://localhost:4566
export AWS_REGION=us-east-1
export S3_BUCKET_NAME=test-bucket
```

### 2. AWS Environment Testing
```bash
# Set environment variables
export AWS_REGION=us-east-1
export S3_BUCKET_NAME=my-app-bucket
export USE_S3_STORAGE=true
export AUTH_SECRET_NAME=app/basic-auth

# Run application
java -jar target/application.jar
```

## Monitoring and Observability

### CloudWatch Logs
- Configure JSON logging with logback-json-encoder
- Set up log group per environment
- Enable CloudWatch Logs Insights for querying

### CloudWatch Metrics
- Monitor S3 operation latency
- Track HTTP request timeouts
- Alert on authentication failures

### AWS X-Ray (Recommended)
- Add X-Ray SDK for distributed tracing
- Track cross-service requests
- Identify performance bottlenecks

## Security Best Practices

1. **Never commit .env file** - Added to .gitignore
2. **Use IAM roles** - Avoid access keys in environment
3. **Rotate secrets** - Enable automatic rotation in Secrets Manager
4. **Encrypt S3 bucket** - Enable default encryption
5. **VPC endpoints** - Use VPC endpoints for S3 and Secrets Manager

## Rollback Plan

If issues occur:
1. Original code backed up with git history
2. Environment variables have sensible defaults
3. Graceful fallback to classpath resources when S3 unavailable
4. All changes are backward compatible with local development

## Next Steps

1. **Add health check endpoint** - For ALB/ELB integration
2. **Implement circuit breakers** - For S3 and Secrets Manager calls
3. **Add retry logic** - For transient AWS service failures
4. **Configure CloudWatch alarms** - For critical errors
5. **Set up CI/CD pipeline** - Automated deployment to AWS

## Support

For issues or questions:
- Check CloudWatch Logs for application errors
- Review IAM permissions for AWS service access
- Verify environment variable configuration
- Ensure AWS resources are in the correct region
