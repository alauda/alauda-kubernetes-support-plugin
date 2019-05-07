package io.alauda.jenkins.devops.config;

import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.jvnet.hudson.test.RestartableJenkinsRule;

public class K8sServerConfigurationTest {

    @Rule
    public RestartableJenkinsRule rr = new RestartableJenkinsRule();

    @Test
    public void uiAndStorage() {
        rr.then(r -> {
            assertNull("not set initially", K8sServerConfiguration.get().getServer());
            HtmlForm config = r.createWebClient().goTo("configure").getFormByName("config");
            HtmlTextInput textbox = config.getInputByName("_.serverUrl");
            textbox.setText("http://test");
            r.submit(config);
            assertEquals("global config page let us edit it", "http://test", K8sServerConfiguration.get().getServer().getServerUrl());
        });
        rr.then(r -> {
            assertEquals("still there after restart of Jenkins", "http://test", K8sServerConfiguration.get().getServer().getServerUrl());
        });
    }

}
