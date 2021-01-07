package io.alauda.jenkins.devops.support.action;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import hudson.Extension;
import hudson.model.Api;
import hudson.model.UnprotectedRootAction;
import hudson.security.ACL;
import hudson.security.ACLContext;
import hudson.security.csrf.CrumbExclusion;
import hudson.util.HttpResponses;
import hudson.util.Secret;
import io.alauda.jenkins.devops.support.KubernetesCluster;
import io.alauda.jenkins.devops.support.KubernetesClusterConfiguration;
import io.alauda.jenkins.devops.support.client.Clients;
import io.alauda.jenkins.devops.support.exception.KubernetesClientException;
import io.alauda.jenkins.devops.support.utils.CredentialsUtils;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import jenkins.model.Jenkins;
import jenkins.model.identity.IdentityRootAction;
import net.sf.json.JSONObject;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.interceptor.RequirePOST;

@Extension
public class KubernetesTokenUpdateAction implements UnprotectedRootAction {

  private static final String CREDENTIALS_ID = "jenkins-operator-token";
  private static final String URL_NAME = "clusterToken";

  public Api getApi() {
    return new Api(this);
  }

  @CheckForNull
  @Override
  public String getIconFileName() {
    return null;
  }

  @CheckForNull
  @Override
  public String getDisplayName() {
    return null;
  }

  @CheckForNull
  @Override
  public String getUrlName() {
    return URL_NAME;
  }


  @RequirePOST
  @Exported
  public HttpResponse doUpdate(StaplerRequest req, StaplerResponse resp) {
    String token = getBearerToken(req);

    // check if we can use this token to connect to the cluster defined in Jenkins
    try {
      checkClusterConnection(token);
    } catch (KubernetesClientException e) {
      return HttpResponses
          .errorJSON(String.format("Failed to update token, reason %s", e.getMessage()));
    }

    UpdateTokenResult result = new UpdateTokenResult();
    result.setJenkinsFingerprint(new IdentityRootAction().getFingerprint());
    JSONObject resultJson = JSONObject.fromObject(result);

    KubernetesClusterConfiguration globalConfiguration = KubernetesClusterConfiguration.get();
    KubernetesCluster cluster = globalConfiguration.getCluster();
    if (CREDENTIALS_ID.equals(cluster.getCredentialsId())) {
      String oldToken = "";
      try {
        oldToken = CredentialsUtils.getToken(cluster.getCredentialsId());
      } catch (GeneralSecurityException e) {
        return HttpResponses
            .errorJSON(String.format("Failed to update token, reason %s", e.getMessage()));
      }

      if (oldToken.equals(token)) {
        return HttpResponses.okJSON(resultJson);
      }
    }

    try (ACLContext ignore = ACL.as(ACL.SYSTEM)) {
      List<StringCredentials> credentialsListWithSameID = CredentialsMatchers.filter(
          CredentialsProvider
              .lookupCredentials(StringCredentials.class, Jenkins.getInstance(), ACL.SYSTEM,
                  Collections
                      .emptyList()),
          CredentialsMatchers.withId(CREDENTIALS_ID));

      SystemCredentialsProvider systemCredentialsProvider = SystemCredentialsProvider.getInstance();
      if (!CollectionUtils.isEmpty(credentialsListWithSameID)) {
        systemCredentialsProvider.getCredentials().removeAll(credentialsListWithSameID);
      }

      StringCredentials operatorCredentials = new StringCredentialsImpl(CredentialsScope.GLOBAL,
          CREDENTIALS_ID,
          "Token auto-injected by DevOps Controller for Jenkins Operator",
          Secret.fromString(token));

      systemCredentialsProvider.getCredentials().add(operatorCredentials);
      systemCredentialsProvider.save();
    } catch (IOException e) {
      return HttpResponses
          .errorJSON(String.format("Failed to update token, reason %s", e.getMessage()));
    }

    cluster.setCredentialsId(CREDENTIALS_ID);
    globalConfiguration.getDescriptor().save();
    globalConfiguration.triggerEvents(cluster);

    return HttpResponses.okJSON(resultJson);
  }


  private void checkClusterConnection(String token) throws KubernetesClientException {
    if (StringUtils.isEmpty(token)) {
      throw new KubernetesClientException(
          "Unable to initialize ApiClient to check connectivity, no token provide");
    }

    KubernetesCluster cluster = KubernetesClusterConfiguration.get().getCluster();
    if (cluster == null) {
      throw new KubernetesClientException(
          "Unable to initialize ApiClient to check connectivity, no cluster defined in Jenkins");
    }

    ApiClient client = Clients.createClientFromCluster(cluster);
    client.setApiKeyPrefix("Bearer");
    client.setApiKey(token);

    CoreV1Api api = new CoreV1Api(client);

    try {
      api.listNamespace(null, null, null, null,  null,null, null, null, null, null);
    } catch (ApiException e) {
      throw new KubernetesClientException(
          String.format("Unable to connect to cluster %s", cluster.getMasterUrl()), e);
    }
  }

  private String getBearerToken(StaplerRequest req) {
    return req.getParameter("token");
  }

  public static class UpdateTokenResult {

    private String jenkinsFingerprint;


    public String getJenkinsFingerprint() {
      return jenkinsFingerprint;
    }

    public void setJenkinsFingerprint(String jenkinsFingerprint) {
      this.jenkinsFingerprint = jenkinsFingerprint;
    }
  }

  @Extension
  public static class KubernetesTokenUpdateActionCrumbExclusion extends CrumbExclusion {

    @Override
    public boolean process(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
        throws IOException, ServletException {
      String pathInfo = req.getPathInfo();
      if (pathInfo != null && pathInfo.startsWith(getExclusionPath())) {
        chain.doFilter(req, resp);
        return true;
      }
      return false;
    }

    public String getExclusionPath() {
      return "/" + URL_NAME + "/";
    }
  }
}
