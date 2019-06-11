package io.alauda.jenkins.devops.support;

import com.gargoylesoftware.htmlunit.html.HtmlCheckBoxInput;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.jvnet.hudson.test.RestartableJenkinsRule;

public class KubernetesClusterConfigurationTest {

    @Rule
    public RestartableJenkinsRule rr = new RestartableJenkinsRule();

    @Test
    public void uiAndStorage() {
        rr.then(r -> {
            assertNull("not set initially", KubernetesClusterConfiguration.get().getCluster());
            HtmlForm config = r.createWebClient().goTo("configure").getFormByName("config");
            HtmlTextInput textbox = config.getInputByName("_.masterUrl");
            textbox.setText("http://test");
            HtmlCheckBoxInput checkBoxInput = config.getInputByName("_.skipTlsVerify");
            checkBoxInput.setChecked(false);
            r.submit(config);
            assertEquals("global config page let us edit it", "http://test", KubernetesClusterConfiguration.get().getCluster().getMasterUrl());
        });
        rr.then(r -> assertEquals("still there after restart of Jenkins", "http://test", KubernetesClusterConfiguration.get().getCluster().getMasterUrl()));
    }

}
