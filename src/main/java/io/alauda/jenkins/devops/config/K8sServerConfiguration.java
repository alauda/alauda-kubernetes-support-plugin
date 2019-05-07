package io.alauda.jenkins.devops.config;

import hudson.Extension;
import jenkins.model.GlobalConfiguration;
import org.kohsuke.stapler.DataBoundSetter;

import java.util.LinkedList;
import java.util.List;

@Extension
public class K8sServerConfiguration extends GlobalConfiguration {

    // We might config multiple servers in the future, so we use list to store them
    private List<AlaudaDevOpsK8sServer> k8sServers = new LinkedList<>();

    public static K8sServerConfiguration get() {
        return GlobalConfiguration.all().get(K8sServerConfiguration.class);
    }


    public K8sServerConfiguration() {
        // When Jenkins is restarted, load any saved configuration from disk.
        load();
    }

    public AlaudaDevOpsK8sServer getServer() {
        if (k8sServers == null || k8sServers.size() == 0) {
            return null;
        }

        return k8sServers.get(0);
    }


    /**
     * Together with {@link #getServer()}, binds to entry in {@code config.jelly}.
     *
     * @param server the new value of this field
     */
    @DataBoundSetter
    public void setServer(AlaudaDevOpsK8sServer server) {
        if (k8sServers == null) {
            k8sServers = new LinkedList<>();
        }

        AlaudaDevOpsK8sServer currentServer = getServer();
        if (currentServer != null && currentServer.equals(server)) {
            return;
        }

        k8sServers.clear();
        k8sServers.add(server);
        save();
        triggerConfigChangeEvent(server);
    }


    private void triggerConfigChangeEvent(AlaudaDevOpsK8sServer server) {
        K8sServerConfigurationListener
                .all()
                .forEach(listener ->
                        listener.onConfigChange(server));
    }
}
