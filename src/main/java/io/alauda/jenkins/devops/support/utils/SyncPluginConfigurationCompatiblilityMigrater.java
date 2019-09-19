package io.alauda.jenkins.devops.support.utils;

import io.alauda.jenkins.devops.support.KubernetesCluster;
import jenkins.model.Jenkins;
import org.apache.commons.io.output.FileWriterWithEncoding;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SyncPluginConfigurationCompatiblilityMigrater {
    private static final Logger logger = Logger.getLogger(SyncPluginConfigurationCompatiblilityMigrater.class.getName());
    private static final String SYNC_PLUGIN_CONFIGURATION_FILE_NAME = "io.alauda.jenkins.devops.sync.AlaudaSyncGlobalConfiguration.xml";


    public static KubernetesCluster migrateConfigurationFromSyncPlugin() {
        File configFile = new File(Jenkins.getInstance().getRootDir(), SYNC_PLUGIN_CONFIGURATION_FILE_NAME);
        if (!configFile.exists() || !configFile.isFile()) {
            return null;
        }

        try {
            Document configDocument = readFileToXMLDocument(configFile);
            Element documentElement = configDocument.getDocumentElement();
            if (documentElement == null) {
                return null;
            }

            KubernetesCluster cluster = new KubernetesCluster();
            NodeList trustCertsNodeList = configDocument.getElementsByTagName("trustCerts");
            if (trustCertsNodeList == null || trustCertsNodeList.getLength() == 0) {
                return null;
            }
            String trustCertsStr = trustCertsNodeList.item(0).getTextContent();
            if ("true".equals(trustCertsStr) || "false".equals(trustCertsStr)) {
                cluster.setSkipTlsVerify(Boolean.parseBoolean(trustCertsStr));
            } else {
                return null;
            }
            removeNodeList(trustCertsNodeList);

            NodeList serverNodeList = configDocument.getElementsByTagName("server");
            if (serverNodeList == null || serverNodeList.getLength() == 0) {
                return null;
            }
            cluster.setMasterUrl(serverNodeList.item(0).getTextContent());
            removeNodeList(serverNodeList);

            NodeList credentialsNodeList = configDocument.getElementsByTagName("credentialsId");
            if (credentialsNodeList != null && credentialsNodeList.getLength() > 0) {
                cluster.setCredentialsId(credentialsNodeList.item(0).getTextContent());
                removeNodeList(credentialsNodeList);
            }

            writeXMLDocumentToFile(configDocument, configFile);
            return cluster;
        } catch (ParserConfigurationException | IOException | TransformerException | SAXException e) {
            logger.log(Level.FINE, String.format("Unable to migrate configuration from old sync plugin, will skip it, reason: %s", e.getMessage()), e);
        }
        return null;
    }

    private static Document readFileToXMLDocument(File xml) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        return builder.parse(xml);
    }

    private static void writeXMLDocumentToFile(Document document, File xml) throws TransformerException, IOException {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        StreamResult result = new StreamResult(new FileWriterWithEncoding(xml, StandardCharsets.UTF_8));
        DOMSource source = new DOMSource(document);

        transformer.transform(source, result);
    }

    private static void removeNodeList(NodeList nodeList) {
        if (nodeList == null) {
            return;
        }
        for (int i = 0; i < nodeList.getLength(); i++) {
            nodeList.item(i).getParentNode().removeChild(nodeList.item(i));
        }
    }
}
