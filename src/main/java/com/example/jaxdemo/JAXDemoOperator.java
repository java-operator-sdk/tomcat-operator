package com.example.jaxdemo;

import com.github.containersolutions.operator.Operator;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.runtime.StartupEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

@ApplicationScoped
public class JAXDemoOperator {

    private static final Logger log = LoggerFactory.getLogger(JAXDemoOperator.class);

    void onStart(@Observes StartupEvent ev) {

        Config config = new ConfigBuilder().withNamespace(null).build();
        KubernetesClient client = new DefaultKubernetesClient(config);
        Operator operator = new Operator(client);
        operator.registerControllerForAllNamespaces(new TomcatController(client));

    }

}