package io.javaoperatorsdk.tomcatoperator;

import io.fabric8.kubernetes.api.model.DoneableService;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentStatus;
import io.fabric8.kubernetes.api.model.apps.DoneableDeployment;
import io.fabric8.kubernetes.client.CustomResourceDoneable;
import io.fabric8.kubernetes.client.CustomResourceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import io.fabric8.kubernetes.client.dsl.ServiceResource;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.javaoperatorsdk.operator.api.*;
import io.javaoperatorsdk.operator.processing.KubernetesResourceUtils;
import io.javaoperatorsdk.operator.processing.dependentresource.deployment.DeleteInput;
import io.javaoperatorsdk.operator.processing.dependentresource.deployment.DeploymentDependentResource;
import io.javaoperatorsdk.operator.processing.dependentresource.deployment.DeploymentInput;
import io.javaoperatorsdk.operator.processing.event.EventSourceManager;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;

import static io.javaoperatorsdk.operator.processing.KubernetesResourceUtils.getUID;

@Controller(crdName = "tomcats.tomcatoperator.io")
public class TomcatController implements ResourceController<Tomcat> {

    public static final String TOMCAT_IMAGE_PREFIX = "tomcat:";
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final KubernetesClient kubernetesClient;

    private MixedOperation<Tomcat, CustomResourceList<Tomcat>, CustomResourceDoneable<Tomcat>, Resource<Tomcat, CustomResourceDoneable<Tomcat>>> tomcatOperations;

    private volatile DeploymentDependentResource deploymentDependentResource;

    public TomcatController(KubernetesClient client) {
        this.kubernetesClient = client;
    }

    @Override
    public void init(EventSourceManager eventSourceManager) {
        this.deploymentDependentResource = new DeploymentDependentResource(kubernetesClient);
        eventSourceManager.registerEventSource("deployment-event-source", this.deploymentDependentResource);
    }

    @Override
    public UpdateControl<Tomcat> createOrUpdateResource(Tomcat tomcat, Context<Tomcat> context) {

        var deploymentStatus = deploymentDependentResource.createOrUpdate(deploymentInput(tomcat));
        createOrUpdateService(tomcat);

        Tomcat updatedTomcat = updateTomcatStatus(tomcat, deploymentStatus.getDeployment());

        log.info("Updating status of Tomcat {} in namespace {} to {} ready replicas (event list size = {})", tomcat.getMetadata().getName(),
                tomcat.getMetadata().getNamespace(), tomcat.getStatus().getReadyReplicas(), context.getEvents().getList().size());
        return UpdateControl.updateStatusSubResource(updatedTomcat);

    }

    private DeploymentInput deploymentInput(Tomcat tomcat) {
        return new DeploymentInput(KubernetesResourceUtils.getUID(tomcat),
                tomcat.getMetadata().getName(),
                "tomcat",
                tomcat.getSpec().getVersion().toString(),
                tomcat.getMetadata().getNamespace(),
                tomcat.getSpec().getReplicas());
    }

    @Override
    public DeleteControl deleteResource(Tomcat tomcat, Context<Tomcat> context) {
        deploymentDependentResource.delete(new DeleteInput(tomcat.getMetadata().getName(),tomcat.getMetadata().getNamespace()));
        deleteService(tomcat);
        return DeleteControl.DEFAULT_DELETE;
    }

    private Tomcat updateTomcatStatus(Tomcat tomcat, Deployment deployment) {
        DeploymentStatus deploymentStatus = Objects.requireNonNullElse(deployment.getStatus(), new DeploymentStatus());
        int readyReplicas = Objects.requireNonNullElse(deploymentStatus.getReadyReplicas(), 0);
        TomcatStatus status = new TomcatStatus();
        status.setReadyReplicas(readyReplicas);
        tomcat.setStatus(status);
        return tomcat;
    }


    private Service createOrUpdateService(Tomcat tomcat) {
        Service service = loadYaml(Service.class, "service.yaml");
        service.getMetadata().setName(tomcat.getMetadata().getName());
        String ns = tomcat.getMetadata().getNamespace();
        service.getMetadata().setNamespace(ns);
        service.getSpec().getSelector().put("app", tomcat.getMetadata().getName());
        log.info("Creating or updating Service {} in {}", service.getMetadata().getName(), ns);
        return kubernetesClient.services().inNamespace(ns).createOrReplace(service);
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

    public void setTomcatOperations(MixedOperation<Tomcat, CustomResourceList<Tomcat>, CustomResourceDoneable<Tomcat>, Resource<Tomcat, CustomResourceDoneable<Tomcat>>> tomcatOperations) {
        this.tomcatOperations = tomcatOperations;
    }
}