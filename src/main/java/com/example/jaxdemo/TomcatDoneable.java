package com.example.jaxdemo;

import io.fabric8.kubernetes.api.builder.Function;
import io.fabric8.kubernetes.client.CustomResourceDoneable;

public class TomcatDoneable extends CustomResourceDoneable<Tomcat> {
    public TomcatDoneable(Tomcat resource, Function<Tomcat, Tomcat> function) {
        super(resource, function);
    }
}
