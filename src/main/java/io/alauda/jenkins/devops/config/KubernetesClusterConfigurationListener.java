package io.alauda.jenkins.devops.config;

import hudson.ExtensionList;
import hudson.ExtensionPoint;

public interface KubernetesClusterConfigurationListener extends ExtensionPoint {
    void onConfigChange(KubernetesCluster cluster);


    static ExtensionList<KubernetesClusterConfigurationListener> all() {
        return ExtensionList.lookup(KubernetesClusterConfigurationListener.class);
    }
}
