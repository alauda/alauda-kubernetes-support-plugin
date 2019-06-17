package io.alauda.jenkins.devops.support;

import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.alauda.jenkins.devops.support.client.Clients;
import io.alauda.jenkins.devops.support.exception.KubernetesClientException;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.models.V1NamespaceList;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import java.util.Objects;

public class KubernetesCluster extends AbstractDescribableImpl<KubernetesCluster> {
    private String masterUrl;
    private String credentialsId;
    private boolean skipTlsVerify = false;
    private String serverCertificateAuthority;
    private boolean defaultCluster = false;
    private boolean managerCluster = false;

    @DataBoundConstructor
    public KubernetesCluster() {
    }

    public String getMasterUrl() {
        return masterUrl;
    }

    @DataBoundSetter
    public void setMasterUrl(String masterUrl) {
        this.masterUrl = masterUrl;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    @DataBoundSetter
    public void setCredentialsId(String credentialId) {
        this.credentialsId = credentialId;
    }

    public boolean isSkipTlsVerify() {
        return skipTlsVerify;
    }

    @DataBoundSetter
    public void setSkipTlsVerify(boolean skipTlsVerify) {
        this.skipTlsVerify = skipTlsVerify;
    }

    public String getServerCertificateAuthority() {
        return serverCertificateAuthority;
    }

    @DataBoundSetter
    public void setServerCertificateAuthority(String serverCertificateAuthority) {
        this.serverCertificateAuthority = serverCertificateAuthority;
    }

    public boolean isDefaultCluster() {
        return defaultCluster;
    }

    @DataBoundSetter
    public void setDefaultCluster(boolean defaultCluster) {
        this.defaultCluster = defaultCluster;
    }

    public boolean isManagerCluster() {
        return managerCluster;
    }

    @DataBoundSetter
    public void setManagerCluster(boolean managerCluster) {
        this.managerCluster = managerCluster;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KubernetesCluster that = (KubernetesCluster) o;
        return skipTlsVerify == that.skipTlsVerify &&
                Objects.equals(masterUrl, that.masterUrl) &&
                Objects.equals(credentialsId, that.credentialsId) &&
                Objects.equals(serverCertificateAuthority, that.serverCertificateAuthority);
    }

    @Override
    public int hashCode() {
        return Objects.hash(masterUrl, credentialsId, skipTlsVerify, serverCertificateAuthority);
    }

    @Extension
    public static class AlaudaDevOpsK8sServerDescriptor extends Descriptor<KubernetesCluster> {

        public ListBoxModel doFillCredentialsIdItems(@QueryParameter String credentialsId) {
            if (credentialsId == null) {
                credentialsId = "";
            }

            if (!Jenkins.getInstance().hasPermission(Jenkins.ADMINISTER)) {
                return new StandardListBoxModel()
                        .includeCurrentValue(credentialsId);
            }

            return new StandardListBoxModel()
                    .includeEmptyValue()
                    .includeAs(ACL.SYSTEM, Jenkins.getInstance(),
                            StringCredentials.class)
                    .includeCurrentValue(credentialsId);
        }


        public FormValidation doVerifyConnect(@QueryParameter String masterUrl,
                                              @QueryParameter String credentialsId,
                                              @QueryParameter String serverCertificateAuthority,
                                              @QueryParameter boolean skipTlsVerify) {

            Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);

            ApiClient testClient;
            try {
                testClient = Clients.createClientFromConfig(masterUrl, credentialsId, serverCertificateAuthority, skipTlsVerify);
            } catch (KubernetesClientException e) {
                e.printStackTrace();
                return FormValidation.error(e.getMessage());
            }

            CoreV1Api api = new CoreV1Api(testClient);
            V1NamespaceList list;
            try {
                list = api.listNamespace(null, null, null, null, null, null, null, null, null);
            } catch (ApiException e) {
                return FormValidation.error(e.getMessage());
            }

            if (list == null) {
                return FormValidation.error(String.format("Unable to connect to cluster %s", masterUrl));
            }

            return FormValidation.ok(String.format("Connect to %s succeed", masterUrl));
        }
    }
}
