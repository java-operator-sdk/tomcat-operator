package com.example.jaxdemo;

import com.github.containersolutions.operator.api.Context;
import com.github.containersolutions.operator.api.Controller;
import com.github.containersolutions.operator.api.ResourceController;
import com.github.containersolutions.operator.api.UpdateControl;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.Deployment;

import java.io.ByteArrayInputStream;
import java.io.StringBufferInputStream;
import java.util.List;
import java.util.Objects;

@Controller(customResourceClass = Webapp.class,
        crdName = "tomcats.tomcatoperator.io")
public class WebappController implements ResourceController<Webapp> {

    @Override
    public UpdateControl createOrUpdateResource(Webapp webapp, Context<Webapp> context) {
        if (Objects.equals(webapp.getSpec().getUrl(), webapp.getStatus().getDeployedArtifact())) {
            return UpdateControl.noUpdate();
        }

        List<Deployment> deployments = context.kubernetesClient().apps().deployments().inNamespace(webapp.getMetadata().getNamespace())
                .withLabel("created-by", webapp.getSpec().getTomcat()).list().getItems();

        if (deployments.size() > 0) {
            Deployment deployment = deployments.get(0); //there should only be 1 deployment
            List<Pod> pods = context.kubernetesClient().pods().inNamespace(webapp.getMetadata().getNamespace())
                    .withLabels(deployment.getSpec().getSelector().getMatchLabels()).list().getItems();
            for (Pod pod : pods) {
//                context.kubernetesClient().pods().inNamespace(webapp.getMetadata().getNamespace())
//                        .withName(pod.getMetadata().getName())
//                        .readingInput(new ByteArrayInputStream())
            }
        }
        return null;
    }

    @Override
    public boolean deleteResource(Webapp webapp, Context<Webapp> context) {
        return false;
    }
}
