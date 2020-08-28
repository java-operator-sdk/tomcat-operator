package com.example.jaxdemo;

import com.github.containersolutions.operator.Operator;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.takes.facets.fork.FkRegex;
import org.takes.facets.fork.TkFork;
import org.takes.http.Exit;
import org.takes.http.FtBasic;

import java.io.IOException;

public class JAXDemoOperator {

    private static final Logger log = LoggerFactory.getLogger(JAXDemoOperator.class);

    public static void main(String[] args) throws IOException {
        log.info("Hello JAX");
        Config config = new ConfigBuilder().withNamespace(null).build();
        KubernetesClient client = new DefaultKubernetesClient(config);
        Operator operator = new Operator(client);
        operator.registerControllerForAllNamespaces(new TomcatController(client));

        new FtBasic(
        new TkFork(new FkRegex("/health", "ALL GOOD.")), 8080
        ).start(Exit.NEVER);
    }
}
