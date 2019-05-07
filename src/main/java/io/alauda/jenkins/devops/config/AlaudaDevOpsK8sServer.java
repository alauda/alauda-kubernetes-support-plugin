package io.alauda.jenkins.devops.config;

import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import io.alauda.jenkins.devops.config.utils.CredentialsUtils;
import io.alauda.jenkins.devops.config.utils.KubernetesConnectionTestClient;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Objects;

public class AlaudaDevOpsK8sServer extends AbstractDescribableImpl<AlaudaDevOpsK8sServer> {
    private String serverUrl;
    private String credentialsId;
    private boolean skipTlsVerify = false;
    private String serverCertificateAuthority;


    @DataBoundConstructor
    public AlaudaDevOpsK8sServer() {
    }

    public String getServerUrl() {
        return serverUrl;
    }

    @DataBoundSetter
    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
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

    public void setServerCertificateAuthority(String serverCertificateAuthority) {
        this.serverCertificateAuthority = serverCertificateAuthority;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AlaudaDevOpsK8sServer that = (AlaudaDevOpsK8sServer) o;
        return skipTlsVerify == that.skipTlsVerify &&
                Objects.equals(serverUrl, that.serverUrl) &&
                Objects.equals(credentialsId, that.credentialsId) &&
                Objects.equals(serverCertificateAuthority, that.serverCertificateAuthority);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serverUrl, credentialsId, skipTlsVerify, serverCertificateAuthority);
    }

    @Extension
    public static class AlaudaDevOpsK8sServerDescriptor extends Descriptor<AlaudaDevOpsK8sServer> {

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


        public FormValidation doVerifyConnect(@QueryParameter String serverUrl,
                                              @QueryParameter String credentialsId,
                                              @QueryParameter String serverCertificateAuthority,
                                              @QueryParameter boolean skipTlsVerify) {
            String token;
            try {
                token = CredentialsUtils.getToken(credentialsId);
            } catch (GeneralSecurityException e) {
                return FormValidation.error(String.format("Failed to connect to API serverUrl: %s", e.getMessage()));
            }

            KubernetesConnectionTestClient testClient =
                    new KubernetesConnectionTestClient(serverUrl, skipTlsVerify, serverCertificateAuthority, token);

            try {
                if (testClient.testConnection()) {
                    return FormValidation.ok(String.format("Connect to %s success.", serverUrl));
                } else {
                    return FormValidation.error("Failed to connect to API serverUrl");
                }
            } catch (GeneralSecurityException | IOException e) {
                return FormValidation.error(String.format("Failed to connect to API serverUrl: %s", e.getMessage()));
            }
        }
    }
}
