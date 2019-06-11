package io.alauda.jenkins.devops.support;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import io.kubernetes.client.ApiClient;

public interface KubernetesClusterConfigurationListener extends ExtensionPoint {
    void onConfigChange(KubernetesCluster cluster, ApiClient client);

    void onConfigError(KubernetesCluster cluster);

    static ExtensionList<KubernetesClusterConfigurationListener> all() {
        return ExtensionList.lookup(KubernetesClusterConfigurationListener.class);
    }
}
