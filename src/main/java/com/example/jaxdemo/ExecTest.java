package com.example.jaxdemo;

import com.github.containersolutions.operator.Operator;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;

public class ExecTest {

    public static void main(String[] args) {
        KubernetesClient client = new DefaultKubernetesClient();

//        client.pods().inNamespace("tomcat-demo").withName("test-tomcat1-6fd95598bf-4tk8b")
//                .exec("wget", "-O", "/data/sample.war", "http://tomcat.apache.org/tomcat-7.0-doc/appdev/sample/sample.war")
//                .getError()

        /**
         * wget
         *             - "-O"
         *             - "/data/sample.war"
         *             - http://tomcat.apache.org/tomcat-7.0-doc/appdev/sample/sample.war
         */
    }
}
