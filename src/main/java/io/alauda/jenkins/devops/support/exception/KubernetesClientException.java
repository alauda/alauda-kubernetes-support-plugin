package io.alauda.jenkins.devops.support.exception;

public class KubernetesClientException extends Exception {
    public KubernetesClientException() {
    }

    public KubernetesClientException(String message) {
        super(message);
    }

    public KubernetesClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public KubernetesClientException(Throwable cause) {
        super(cause);
    }

    public KubernetesClientException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
