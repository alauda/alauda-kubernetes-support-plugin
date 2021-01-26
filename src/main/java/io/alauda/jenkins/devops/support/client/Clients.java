package io.alauda.jenkins.devops.support.client;

import io.alauda.jenkins.devops.support.KubernetesCluster;
import io.alauda.jenkins.devops.support.exception.KubernetesClientException;
import io.alauda.jenkins.devops.support.utils.CredentialsUtils;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.util.Config;
import okio.Buffer;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Clients {

    private static final Logger logger = Logger.getLogger(Clients.class.getName());

    private Clients() {
    }

    /**
     * Return a client. If client not exists, it will create a client from the cluster then return it.
     *
     * @param cluster Kubernetes cluster
     * @return Client that can connect to correspondent cluster, null if we cannot create client from cluster.
     */
    @Nonnull
    public static ApiClient createClientFromCluster(@Nonnull KubernetesCluster cluster) throws KubernetesClientException {
        ApiClient client;
        // if master url is empty, we create client from local cluster
        if (StringUtils.isEmpty(cluster.getMasterUrl())) {
            try {
                client = Config.fromCluster();
                return client;
            } catch (IOException e) {
                logger.log(Level.SEVERE, String.format("Unable to create a client from local cluster, reason %s", e.getMessage()), e);
                throw new KubernetesClientException(e);
            }
        }

        String token = "";
        try {
            if (StringUtils.isEmpty(cluster.getCredentialsId())) {
                token = getTokenFromLocalCluster();
            } else {
                token = CredentialsUtils.getToken(cluster.getCredentialsId());
            }
        } catch (GeneralSecurityException | IOException e) {
            logger.log(Level.WARNING, String.format("Unable to get token for k8s client, reason %s", e.getMessage()), e);
        }
        client = Config.fromToken(cluster.getMasterUrl(), token, !cluster.isSkipTlsVerify());

        if (!cluster.isSkipTlsVerify()) {
            try {
                Buffer buffer = new Buffer();

                if (!StringUtils.isEmpty(cluster.getServerCertificateAuthority())) {
                    if (new File(cluster.getServerCertificateAuthority()).isFile()) {
                        buffer.write(Files.readAllBytes(Paths.get(cluster.getServerCertificateAuthority())));
                    } else {
                        buffer.writeUtf8(cluster.getServerCertificateAuthority());
                    }
                } else {
                    buffer.writeUtf8(getCAFromLocalCluster());
                }
                if (buffer.size() != 0) {
                    client.setSslCaCert(buffer.inputStream());
                }
            } catch (IOException e) {
                logger.log(Level.WARNING, String.format("Unable to get ca for k8s client, reason %s", e.getMessage()), e);
            }
        }
        return client;
    }

    public static ApiClient createClientFromConfig(String masterUrl, String credentialsId, String serverCertificateAuthority, boolean skipTlsVerify) throws KubernetesClientException {
        KubernetesCluster cluster = new KubernetesCluster();
        cluster.setMasterUrl(masterUrl);
        cluster.setCredentialsId(credentialsId);
        cluster.setServerCertificateAuthority(serverCertificateAuthority);
        cluster.setSkipTlsVerify(skipTlsVerify);

        return createClientFromCluster(cluster);
    }


    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
            value = "DMI_HARDCODED_ABSOLUTE_FILENAME",
            justification = "I know what I'm doing")
    @Nonnull
    private static String getTokenFromLocalCluster() throws IOException {
        if (Files.exists(Paths.get(Config.SERVICEACCOUNT_TOKEN_PATH))) {
            return new String(Files.readAllBytes(Paths.get(Config.SERVICEACCOUNT_TOKEN_PATH)), StandardCharsets.UTF_8);
        }
        throw new FileNotFoundException(String.format("Unable to get token from %s", Config.SERVICEACCOUNT_TOKEN_PATH));
    }

    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
            value = "DMI_HARDCODED_ABSOLUTE_FILENAME",
            justification = "I know what I'm doing")
    @Nonnull
    private static String getCAFromLocalCluster() throws IOException {
        if (Files.exists(Paths.get(Config.SERVICEACCOUNT_CA_PATH))) {
            return new String(Files.readAllBytes(Paths.get(Config.SERVICEACCOUNT_CA_PATH)), StandardCharsets.UTF_8);
        }
        throw new FileNotFoundException(String.format("Unable to get CA from %s", Config.SERVICEACCOUNT_CA_PATH));
    }
}
