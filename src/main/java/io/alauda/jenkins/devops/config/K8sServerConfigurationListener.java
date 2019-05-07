package io.alauda.jenkins.devops.config;

import hudson.ExtensionList;
import hudson.ExtensionPoint;

public interface K8sServerConfigurationListener extends ExtensionPoint {
    void onConfigChange(AlaudaDevOpsK8sServer server);


    static ExtensionList<K8sServerConfigurationListener> all() {
        return ExtensionList.lookup(K8sServerConfigurationListener.class);
    }
}
