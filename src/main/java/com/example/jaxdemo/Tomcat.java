package com.example.jaxdemo;

import io.fabric8.kubernetes.client.CustomResource;

public class Tomcat extends CustomResource {

    private TomcatSpec spec;

    private TomcatStatus status;

    public TomcatSpec getSpec() {
        return spec;
    }

    public void setSpec(TomcatSpec spec) {
        this.spec = spec;
    }

    public TomcatStatus getStatus() {
        return status;
    }

    public void setStatus(TomcatStatus status) {
        this.status = status;
    }
}