package io.alauda.jenkins.devops.config;

import hudson.Extension;
import jenkins.model.GlobalConfiguration;
import org.kohsuke.stapler.DataBoundSetter;

import java.util.LinkedList;
import java.util.List;

@Extension
public class KubernetesClusterConfiguration extends GlobalConfiguration {

    // We might config multiple servers in the future, so we use list to store them
    private List<KubernetesCluster> k8sClusters = new LinkedList<>();

    public static KubernetesClusterConfiguration get() {
        return GlobalConfiguration.all().get(KubernetesClusterConfiguration.class);
    }


    public KubernetesClusterConfiguration() {
        // When Jenkins is restarted, load any saved configuration from disk.
        load();
    }

    public KubernetesCluster getCluster() {
        if (k8sClusters == null || k8sClusters.size() == 0) {
            return null;
        }

        return k8sClusters.get(0);
    }


    /**
     * Together with {@link #getCluster()}, binds to entry in {@code config.jelly}.
     *
     * @param cluster the new value of this field
     */
    @DataBoundSetter
    public void setCluster(KubernetesCluster cluster) {
        if (k8sClusters == null) {
            k8sClusters = new LinkedList<>();
        }

        KubernetesCluster currentCluster = getCluster();
        if (currentCluster != null && currentCluster.equals(cluster)) {
            return;
        }

        k8sClusters.clear();
        k8sClusters.add(cluster);
        save();
        triggerConfigChangeEvent(cluster);
    }


    private void triggerConfigChangeEvent(KubernetesCluster cluster) {
        KubernetesClusterConfigurationListener
                .all()
                .forEach(listener ->
                        listener.onConfigChange(cluster));
    }
}
