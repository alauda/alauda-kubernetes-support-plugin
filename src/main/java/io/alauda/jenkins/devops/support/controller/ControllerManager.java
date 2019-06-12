package io.alauda.jenkins.devops.support.controller;

import hudson.Extension;
import hudson.ExtensionList;
import io.alauda.jenkins.devops.support.KubernetesCluster;
import io.alauda.jenkins.devops.support.KubernetesClusterConfigurationListener;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.informer.SharedInformerFactory;

import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Extension
public class ControllerManager implements KubernetesClusterConfigurationListener {
    private static final Logger logger = Logger.getLogger(ControllerManager.class.getName());
    private static final long DEFAULT_POLLING_RATE = 100;

    private SharedInformerFactory factory;


    public ControllerManager() {
        this.factory = new SharedInformerFactory();
    }


    // When plugin load the configuration from local, it will trigger onConfigChange event.
    // So we should initialize all controllers in there.
    // Also, when config changed, we need to stop current controllers, then restart them.
    @Override
    public void onConfigChange(KubernetesCluster cluster, ApiClient client) {
        if (factory != null) {
            factory.stopAllRegisteredInformers();
        }

        ExtensionList<Controller> controllers = Controller.all();

        logger.log(Level.FINE, "Start initialize controllers");
        controllers.forEach(c -> {
            logger.log(Level.FINE, "Initializing controller: %s ...", c.getType().getTypeName());
            c.initialize(client, factory);
        });

        logger.log(Level.FINE, "Start factories");
        factory.startAllRegisteredInformers();

        // We wait all controller synced in another thread, so we won't block other ConfigLister to receive event
        new Thread(() -> {
            try {
                waitUtilCacheSync(controllers);
                controllers.forEach(c -> {
                    logger.log(Level.FINE, "Starting worker: %s ...", c.getType().getTypeName());
                    c.start();
                });
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void onConfigError(KubernetesCluster cluster, Throwable reason) {
        ExtensionList<Controller> controllers = Controller.all();
        controllers.forEach(c -> c.shutDown(reason));
    }

    private void waitUtilCacheSync(ExtensionList<Controller> controllers) throws ExecutionException, InterruptedException {
        ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        CompletableFuture<Void> allControllerSyncedFuture = new CompletableFuture<>();

        ScheduledFuture scheduledFuture = scheduledExecutor.scheduleAtFixedRate(() -> {
            if (controllers.stream().allMatch(Controller::hasSynced)) {
                allControllerSyncedFuture.complete(null);
            }
        }, DEFAULT_POLLING_RATE, DEFAULT_POLLING_RATE, TimeUnit.MILLISECONDS);


        // When all controllers synced, we cancel the schedule task
        allControllerSyncedFuture.whenComplete((v, throwable) -> scheduledFuture.cancel(true));

        allControllerSyncedFuture.get();
    }
}
