package io.alauda.jenkins.devops.support.controller;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.informer.SharedInformerFactory;

import java.lang.reflect.Type;

public interface Controller<ApiType, ApiListType> extends ExtensionPoint {

    /**
     * Initialize all informers in this methods.
     * @param client client to connect to cluster
     * @param factory informer factory
     */
    void initialize(ApiClient client, SharedInformerFactory factory);

    /**
     * The Informer store has synced, controller can get resource from store.
     */
    void start();

    /**
     * Call when controller is shutdown
     * @param reason null if it closed by user
     */
    void shutDown(Throwable reason);

    /**
     * Whether informers synced
     * @return true if all informers related to this controller synced
     */
    boolean hasSynced();

    /**
     *
     * @return type of ApiType
     */
    Type getType();

    static ExtensionList<Controller> all() {
        return ExtensionList.lookup(Controller.class);
    }
}
