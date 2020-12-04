package io.javaoperatorsdk.tomcatoperator;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watcher;
import io.javaoperatorsdk.operator.processing.event.AbstractEventSource;
import io.javaoperatorsdk.operator.processing.event.internal.CustomResourceEvent;
import io.javaoperatorsdk.operator.processing.event.internal.CustomResourceEventSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static io.javaoperatorsdk.operator.processing.KubernetesResourceUtils.getUID;
import static io.javaoperatorsdk.operator.processing.KubernetesResourceUtils.getVersion;
import static java.net.HttpURLConnection.HTTP_GONE;

public class DeploymentEventSource extends AbstractEventSource implements Watcher<Deployment> {
    private final static Logger log = LoggerFactory.getLogger(DeploymentEventSource.class);

    private final KubernetesClient client;

    private final Map<String, Deployment> deploymentCache = new ConcurrentHashMap<>();

    public static DeploymentEventSource createAndRegisterWatch(KubernetesClient client) {
        DeploymentEventSource deploymentEventSource = new DeploymentEventSource(client);
        deploymentEventSource.registerWatch();
        return deploymentEventSource;
    }

    private DeploymentEventSource(KubernetesClient client) {
        this.client = client;
    }

    private void registerWatch() {
        client.apps().deployments().inAnyNamespace().withLabel("managed-by", "tomcat-operator").watch(this);
    }

    @Override
    public void eventReceived(Watcher.Action action, Deployment deployment) {
        log.info("Event received for action: {}, Deployment: {} (rr={})", action.name(), deployment.getMetadata().getName(), deployment.getStatus().getReadyReplicas());
        if (action == Action.ERROR) {
            log.warn("Skipping {} event for custom resource uid: {}, version: {}", action,
                    getUID(deployment), getVersion(deployment));
            return;
        }
        deploymentCache.put(deployment.getMetadata().getOwnerReferences().get(0).getUid(), deployment);
        eventHandler.handleEvent(new DeploymentEvent(action, deployment, this));
    }

    public Optional<Deployment> getLatestDeployment(String ownerUID) {
        return Optional.ofNullable(deploymentCache.get(ownerUID));
    }

    @Override
    public void eventSourceDeRegisteredForResource(String ownerUID) {
        deploymentCache.remove(ownerUID);
    }

    @Override
    public void onClose(KubernetesClientException e) {
        if (e == null) {
            return;
        }
        if (e.getCode() == HTTP_GONE) {
            log.warn("Received error for watch, will try to reconnect.", e);
            registerWatch();
        } else {
            // Note that this should not happen normally, since fabric8 client handles reconnect.
            // In case it tries to reconnect this method is not called.
            log.error("Unexpected error happened with watch. Will exit.", e);
            System.exit(1);
        }
    }
}
