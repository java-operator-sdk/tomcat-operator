package com.example.jaxdemo;

import com.github.containersolutions.operator.api.Context;
import com.github.containersolutions.operator.api.Controller;
import com.github.containersolutions.operator.api.ResourceController;
import com.github.containersolutions.operator.api.UpdateControl;
import io.fabric8.kubernetes.api.model.DoneablePod;
import io.fabric8.kubernetes.api.model.DoneableService;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DoneableDeployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.PodResource;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import io.fabric8.kubernetes.client.dsl.ServiceResource;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

@Controller(customResourceClass = Tomcat.class,
        crdName = "tomcats.demo.example.com")
public class TomcatController implements ResourceController<Tomcat> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final KubernetesClient kubernetesClient;

    public TomcatController(KubernetesClient client) {
        this.kubernetesClient = client;
    }

    @Override
    public UpdateControl<Tomcat> createOrUpdateResource(Tomcat tomcat, Context<Tomcat> context) {
        createOrUpdateDeployment(tomcat);
        createOrUpdateService(tomcat);

        return UpdateControl.updateCustomResource(tomcat);
    }

    @Override
    public boolean deleteResource(Tomcat tomcat, Context<Tomcat> context) {
        deleteDeployment(tomcat);
        deleteService(tomcat);
        return true;
    }

    private void createOrUpdateDeployment (Tomcat tomcat) {
        Deployment deployment = loadYaml(Deployment.class, "deployment.yaml");
        deployment.getMetadata().setName(tomcat.getMetadata().getName());
        String ns = tomcat.getMetadata().getNamespace();
        deployment.getMetadata().setNamespace(ns);
        // set tomcat version
        deployment.getSpec().getTemplate().getSpec().getContainers().get(0).setImage("tomcat:"+tomcat.getSpec().getVersion());
        // set war
        deployment.getSpec().getTemplate().getSpec().getInitContainers().get(0)
                .setCommand(Arrays.asList("wget", "-O", "/data/ROOT.war", tomcat.getSpec().getWar()));
        deployment.getSpec().setReplicas(tomcat.getSpec().getReplicas());
        log.info("Creating or updating Deployment {} in {}", deployment.getMetadata().getName(), ns);
        kubernetesClient.apps().deployments().inNamespace(ns).createOrReplace(deployment);
    }

    private void deleteDeployment (Tomcat tomcat) {
        log.info("Deleting Deployment {}", tomcat.getMetadata().getName());
        RollableScalableResource<Deployment, DoneableDeployment> deployment = kubernetesClient.apps().deployments()
                .inNamespace(tomcat.getMetadata().getNamespace())
                .withName(tomcat.getMetadata().getName());
        if (deployment.get() != null) {
            deployment.delete();
        }
    }

    private void createOrUpdateService (Tomcat tomcat) {
        Service service = loadYaml(Service.class, "service.yaml");
        service.getMetadata().setName(tomcat.getMetadata().getName());
        String ns = tomcat.getMetadata().getNamespace();
        service.getMetadata().setNamespace(ns);
        log.info("Creating or updating Service {} in {}", service.getMetadata().getName(), ns);
        kubernetesClient.services().inNamespace(ns).createOrReplace(service);
    }

    private void deleteService(Tomcat tomcat) {
        log.info("Deleting Service {}", tomcat.getMetadata().getName());
        ServiceResource<Service, DoneableService> service = kubernetesClient.services()
                .inNamespace(tomcat.getMetadata().getNamespace())
                .withName(tomcat.getMetadata().getName());
        if (service.get() != null) {
            service.delete();
        }
    }

    private <T> T loadYaml(Class<T> clazz, String yaml) {
        try (InputStream is = getClass().getResourceAsStream(yaml)) {
            return Serialization.unmarshal(is, clazz);
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot find yaml on classpath: " + yaml);
        }
    }
}