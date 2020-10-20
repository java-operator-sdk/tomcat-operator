package com.example.jaxdemo;

import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.client.CustomResourceDoneable;

public class WebappDoneable extends CustomResourceDoneable<Webapp> {
    public WebappDoneable(Webapp resource, Function<Webapp, Webapp> function) {
        super(resource, function);
    }
}
