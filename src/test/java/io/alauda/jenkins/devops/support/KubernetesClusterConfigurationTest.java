package io.alauda.jenkins.devops.support;

import com.gargoylesoftware.htmlunit.html.HtmlCheckBoxInput;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.jvnet.hudson.test.RestartableJenkinsRule;

public class KubernetesClusterConfigurationTest {

//    @Rule
//    public RestartableJenkinsRule rr = new RestartableJenkinsRule();
//
//    @Test
//    public void uiAndStorage() {
//        final String masterUrl = "http://localhost/master";
//        final boolean defaultCluster = true;
//        final boolean managerCluster = false;
//
//        rr.then(r -> {
//            assertNull("not set initially", KubernetesClusterConfiguration.get().getCluster());
//            HtmlForm config = r.createWebClient().goTo("configure").getFormByName("config");
//            HtmlTextInput textbox = config.getInputByName("_.masterUrl");
//            textbox.setText(masterUrl);
//            HtmlCheckBoxInput checkBoxInput = config.getInputByName("_.skipTlsVerify");
//            checkBoxInput.setChecked(false);
//            config.getInputByName("_.defaultCluster").setChecked(defaultCluster);
//            config.getInputByName("_.managerCluster").setChecked(managerCluster);
//            r.submit(config);
//
//            // assert config result
//            assertClusterConfig(masterUrl, defaultCluster, managerCluster);
//        });
//        rr.then(r -> {
//            assertClusterConfig(masterUrl, defaultCluster, managerCluster);
//        });
//    }
//
//    private void assertClusterConfig(final String masterUrl, final boolean defaultCluster, final boolean managerCluster) {
//        KubernetesClusterConfiguration clusterConfig = KubernetesClusterConfiguration.get();
//        assertEquals("global config page let us edit it", masterUrl,
//                clusterConfig.getCluster().getMasterUrl());
//        assertEquals("DefaultCluster setting is not working", defaultCluster,
//                clusterConfig.getCluster().isDefaultCluster());
//        assertEquals("ManagerCluster setting is not working", managerCluster,
//                clusterConfig.getCluster().isManagerCluster());
//    }
}
