package io.alauda.jenkins.devops.support;

import hudson.Extension;
import io.alauda.jenkins.devops.support.client.Clients;
import io.alauda.jenkins.devops.support.exception.KubernetesClientException;
import io.alauda.jenkins.devops.support.utils.SyncPluginConfigurationCompatiblilityMigrater;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import jenkins.model.GlobalConfiguration;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import org.kohsuke.stapler.DataBoundSetter;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Extension(ordinal = 203)
public class KubernetesClusterConfiguration extends GlobalConfiguration {
    private static final Logger logger = Logger.getLogger(KubernetesClusterConfiguration.class.getName());

    // We might config multiple servers in the future, so we use list to store them
    private List<KubernetesCluster> k8sClusters = new LinkedList<>();

    public static KubernetesClusterConfiguration get() {
        return GlobalConfiguration.all().get(KubernetesClusterConfiguration.class);
    }


    public KubernetesClusterConfiguration() {
        // When Jenkins is restarted, load any saved configuration from disk.
        load();
        KubernetesCluster clusterMigrateFromSync = SyncPluginConfigurationCompatiblilityMigrater.migrateConfigurationFromSyncPlugin();
        if (clusterMigrateFromSync != null) {
            clusterMigrateFromSync.setDefaultCluster(true);
            clusterMigrateFromSync.setManagerCluster(true);
            setCluster(clusterMigrateFromSync);
            return;
        }


        if (k8sClusters.size() == 0) {
            KubernetesCluster cluster = new KubernetesCluster();
            // WARN once we support multi-cluster configuration, you should change this behavior
            cluster.setDefaultCluster(true);
            cluster.setManagerCluster(true);
            setCluster(cluster);
        } else {
            triggerEvents(k8sClusters.get(0));
        }

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

        triggerEvents(cluster);
    }


    public void triggerEvents(KubernetesCluster cluster) {
        try {
            ApiClient client = Clients.createClientFromCluster(cluster);
            // If we have more clusters to config in the future, we may need to remove this.
            Configuration.setDefaultApiClient(client);

            new Thread(() -> triggerConfigChangeEvent(cluster, client)).start();
        } catch (KubernetesClientException e) {
            logger.log(Level.SEVERE, String.format("Unable to create client from cluster %s, reason %s", cluster.getMasterUrl(), e.getMessage()));
            new Thread(() -> triggerConfigErrorEvent(cluster, e)).start();
        }
    }


    private void triggerConfigChangeEvent(KubernetesCluster cluster, ApiClient client) {
        KubernetesClusterConfigurationListener
                .all()
                .forEach(listener ->
                        new Thread(() -> listener.onConfigChange(cluster, client)).start());

    }

    private void triggerConfigErrorEvent(KubernetesCluster cluster, Throwable reason) {
        KubernetesClusterConfigurationListener.all()
                .forEach(listener ->
                        listener.onConfigError(cluster, reason));
    }
}
