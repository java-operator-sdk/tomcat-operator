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
public class TomcatOperator {

    private static final Logger log = LoggerFactory.getLogger(TomcatOperator.class);

    void onStart(@Observes StartupEvent ev) {

        KubernetesClient client = new DefaultKubernetesClient();
        Operator operator = new Operator(client);

        TomcatController tomcatController = new TomcatController(client);
        operator.registerControllerForAllNamespaces(tomcatController);
        tomcatController.setTomcatOperations(operator.getCustomResourceClients(Tomcat.class));

        operator.registerControllerForAllNamespaces(new WebappController(client));

        log.info("Start happened");
    }

}