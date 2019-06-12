package io.alauda.jenkins.devops.support.controller;

import hudson.ExtensionList;
import hudson.ExtensionPoint;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.informer.SharedInformerFactory;

import java.lang.reflect.Type;

public interface Controller<ApiType, ApiListType> extends ExtensionPoint {

    void initialize(ApiClient client, SharedInformerFactory factory);

    void start();

    void shutDown(Throwable reason);

    boolean hasSynced();

    Type getType();

    static ExtensionList<Controller> all() {
        return ExtensionList.lookup(Controller.class);
    }
}
