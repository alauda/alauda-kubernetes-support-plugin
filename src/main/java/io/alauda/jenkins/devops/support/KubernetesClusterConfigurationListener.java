package io.alauda.jenkins.devops.support;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import io.kubernetes.client.openapi.ApiClient;

public interface KubernetesClusterConfigurationListener extends ExtensionPoint {
    void onConfigChange(KubernetesCluster cluster, ApiClient client);

    void onConfigError(KubernetesCluster cluster, Throwable reason);

    static ExtensionList<KubernetesClusterConfigurationListener> all() {
        return ExtensionList.lookup(KubernetesClusterConfigurationListener.class);
    }
}
