package io.alauda.jenkins.devops.config.utils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.sf.json.JSONObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.remoting.util.Charsets;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;

/**
 * A very simple HTTP-Based client that only used to test connection availability.
 */
@SuppressFBWarnings(value = "DMI_HARDCODED_ABSOLUTE_FILENAME")
public class KubernetesConnectionTestClient {
    private static final String KUBERNETES_SERVICE_ACCOUNT_CA_CRT_PATH = "/var/run/secrets/kubernetes.io/serviceaccount/ca.crt";
    private static final String KUBERNETES_SERVICE_ACCOUNT_TOKEN_PATH = "/var/run/secrets/kubernetes.io/serviceaccount/token";
    private static final String KUBERNETES_MASTER_URL = "https://kubernetes.default.svc";

    private String serverUrl;
    private boolean skipTlsVerify;
    private String serverCertificate;
    private String token;

    public KubernetesConnectionTestClient(String serverUrl, boolean skipTlsVerify, String serverCertificate, String token) {
        this.serverUrl = serverUrl;
        this.skipTlsVerify = skipTlsVerify;
        this.serverCertificate = serverCertificate;
        this.token = token;
    }

    /**
     * Test connection availability
     *
     * @return true if the connection is available
     */
    public boolean testConnection() throws GeneralSecurityException, IOException {
        if (StringUtils.isEmpty(serverUrl)) {
            // if server url is empty, we use the default url
            serverUrl = KUBERNETES_MASTER_URL;
        }

        OkHttpClient client;

        if (skipTlsVerify) {
            client = insecureHttpClient();
        } else {
            if (StringUtils.isEmpty(serverCertificate) && Files.exists(Paths.get(KUBERNETES_SERVICE_ACCOUNT_CA_CRT_PATH))) {
                serverCertificate = KUBERNETES_SERVICE_ACCOUNT_CA_CRT_PATH;
            }
            client = customCAHttpClient(serverCertificate);
        }

        serverUrl = serverUrl.endsWith("/") ? serverUrl : serverUrl + "/";
        String wholeUrl = serverUrl + "api/v1/namespaces";


        if (StringUtils.isEmpty(token) && Files.exists(Paths.get(KUBERNETES_SERVICE_ACCOUNT_TOKEN_PATH))) {
            token = new String(Files.readAllBytes(Paths.get(KUBERNETES_SERVICE_ACCOUNT_TOKEN_PATH)), Charsets.UTF_8);
        }

        Request req = new Request.Builder()
                .url(wholeUrl)
                .addHeader("Authorization", "Bearer " + token)
                .build();

        Response res = client.newCall(req).execute();
        ResponseBody body = res.body();
        if (body == null) {
            return false;
        }

        JSONObject jsonObject;
        try {
            jsonObject = JSONObject.fromObject(body.string());
        } finally {
            body.close();
        }

        if (jsonObject == null) {
            return false;
        }

        return jsonObject.getString("kind").equals("NamespaceList");
    }

    /**
     * Create an OKHttpClient which trust the self-signed certificate.
     *
     * @return OKHttpClient
     */
    private OkHttpClient insecureHttpClient() throws KeyManagementException, NoSuchAlgorithmException {
        X509TrustManager acceptAllTrustManager = new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };

        SSLSocketFactory sslSocketFactory;

        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, new TrustManager[]{acceptAllTrustManager}, new SecureRandom());
        sslSocketFactory = sc.getSocketFactory();

        return new OkHttpClient.Builder()
                .sslSocketFactory(sslSocketFactory, acceptAllTrustManager)
                .hostnameVerifier((h, s) -> true)
                .build();
    }

    /**
     * Create a OKHttpClient which trust the custom certificate
     *
     * @param serverCertificate certificate inputted by user
     * @return OKHttpClient
     */
    private OkHttpClient customCAHttpClient(String serverCertificate) throws IOException, GeneralSecurityException {
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();

        if (StringUtils.isEmpty(serverCertificate)) {
            return clientBuilder.build();
        }

        Buffer buffer = new Buffer();
        if (Files.exists(Paths.get(serverCertificate))) {
            buffer.write(Files.readAllBytes(Paths.get(serverCertificate)));
        } else {
            buffer.writeUtf8(serverCertificate);
        }

        X509TrustManager trustManager = trustManagerForCertificates(buffer.inputStream());

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[]{trustManager}, null);

        SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

        return clientBuilder
                .sslSocketFactory(sslSocketFactory, trustManager)
                .build();

    }

    /**
     * Create trust manager which trust the certificates.
     *
     * @param in InputStream to read the certificates
     * @return TrustManager which trust the certificates
     */
    private X509TrustManager trustManagerForCertificates(InputStream in)
            throws GeneralSecurityException {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        Collection<? extends Certificate> certificates = certificateFactory.generateCertificates(in);
        if (certificates.isEmpty()) {
            throw new IllegalArgumentException("expected non-empty set of trusted certificates");
        }

        // Put the certificates a key store.
        char[] password = "password".toCharArray(); // Any password will work.
        KeyStore keyStore = newEmptyKeyStore(password);
        int index = 0;
        for (Certificate certificate : certificates) {
            String certificateAlias = Integer.toString(index++);
            keyStore.setCertificateEntry(certificateAlias, certificate);
        }

        // Use it to build an X509 trust manager.
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(
                KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, password);
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);
        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
        if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
            throw new IllegalStateException("Unexpected default trust managers:"
                    + Arrays.toString(trustManagers));
        }
        return (X509TrustManager) trustManagers[0];
    }

    private KeyStore newEmptyKeyStore(char[] password) throws GeneralSecurityException {
        try {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            InputStream in = null; // By convention, 'null' creates an empty key store.
            keyStore.load(in, password);
            return keyStore;
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }
}