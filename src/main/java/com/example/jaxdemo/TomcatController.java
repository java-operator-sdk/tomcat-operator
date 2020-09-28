package com.example.jaxdemo;

import com.github.containersolutions.operator.api.Context;
import com.github.containersolutions.operator.api.Controller;
import com.github.containersolutions.operator.api.ResourceController;
import com.github.containersolutions.operator.api.UpdateControl;
import io.fabric8.kubernetes.api.model.DoneableService;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DoneableDeployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import io.fabric8.kubernetes.client.dsl.ServiceResource;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;

@Controller(customResourceClass = Tomcat.class,
        crdName = "tomcats.tomcatoperator.io")
public class TomcatController implements ResourceController<Tomcat> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final KubernetesClient kubernetesClient;

    public TomcatController(KubernetesClient client) {
        this.kubernetesClient = client;
    }

//    @Override
//    public void init(Context<Tomcat> context) {
//        for (Tomcat tomcat : context.customResourceClient().inAnyNamespace().list().getItems()) {
//            List<Deployment> deployments = context.kubernetesClient().apps().deployments().withLabel("created-by", tomcat.getMetadata().getName()).list().getItems();
//            if (deployments.size() > 0) {
//                Deployment deployment = deployments.get(0);
//                updateTomcatStatus(context, tomcat, deployment);
//
//                log.info("Attaching Watch to Deployment {}", deployment.getMetadata().getName());
//                context.kubernetesClient().apps().deployments().withName(deployment.getMetadata().getName()).watch(new Watcher<Deployment>() {
//                    @Override
//                    public void eventReceived(Action action, Deployment depl) {
//                        if (action == Action.MODIFIED) {
//                            updateTomcatStatus(context, tomcat, deployment);
//                        }
//                    }
//
//                    @Override
//                    public void onClose(KubernetesClientException cause) {
//                    }
//                });
//            } else {
//                createOrUpdateDeployment(tomcat);
//                createOrUpdateService(tomcat);
//            }
//        }
//    }

    private void updateTomcatStatus(Context<Tomcat> context, Tomcat tomcat, Deployment deployment) {
        tomcat.getStatus().setReadyReplicas(deployment.getStatus().getReadyReplicas());
        log.info("Updating status of Tomcat {} in namespace {} to {} ready replicas", tomcat.getMetadata().getName(), tomcat.getMetadata().getNamespace(), deployment.getStatus().getReadyReplicas());
//        context.customResourceClient()
//                .inNamespace(tomcat.getMetadata().getNamespace())
//                .withName(tomcat.getMetadata().getName())
//                .updateStatus(tomcat);
    }

    @Override
    public UpdateControl<Tomcat> createOrUpdateResource(Tomcat tomcat, Context<Tomcat> context) {
        createOrUpdateDeployment(tomcat);
        createOrUpdateService(tomcat);

        List<Deployment> deployments = context.kubernetesClient().apps().deployments().withLabel("created-by", tomcat.getMetadata().getName()).list().getItems();
        if (deployments.size() > 0) {
            Deployment deployment = deployments.get(0);
            updateTomcatStatus(context, tomcat, deployment);
        }

        return UpdateControl.updateCustomResource(tomcat);
    }

    @Override
    public boolean deleteResource(Tomcat tomcat, Context<Tomcat> context) {
        deleteDeployment(tomcat);
        deleteService(tomcat);
        return true;
    }

    private void createOrUpdateDeployment(Tomcat tomcat) {
        Deployment deployment = loadYaml(Deployment.class, "deployment.yaml");
        deployment.getMetadata().setName(tomcat.getMetadata().getName());
        String ns = tomcat.getMetadata().getNamespace();
        deployment.getMetadata().setNamespace(ns);
        deployment.getMetadata().getLabels().put("created-by", tomcat.getMetadata().getName());
        // set tomcat version
        deployment.getSpec().getTemplate().getSpec().getContainers().get(0).setImage("tomcat:" + tomcat.getSpec().getVersion());
        deployment.getSpec().setReplicas(tomcat.getSpec().getReplicas());
        log.info("Creating or updating Deployment {} in {}", deployment.getMetadata().getName(), ns);
        kubernetesClient.apps().deployments().inNamespace(ns).createOrReplace(deployment);
    }

    private void deleteDeployment(Tomcat tomcat) {
        log.info("Deleting Deployment {}", tomcat.getMetadata().getName());
        RollableScalableResource<Deployment, DoneableDeployment> deployment = kubernetesClient.apps().deployments()
                .inNamespace(tomcat.getMetadata().getNamespace())
                .withName(tomcat.getMetadata().getName());
        if (deployment.get() != null) {
            deployment.delete();
        }
    }

    private void createOrUpdateService(Tomcat tomcat) {
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