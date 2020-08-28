package com.example.jaxdemo;

public class TomcatStatus {

    private String name;
    private String war;
    private Integer replicas;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getWar() {
        return war;
    }

    public void setWar(String war) {
        this.war = war;
    }

    public Integer getReplicas() { return replicas; }

    public void setReplicas(Integer replicas) {
        this.replicas = replicas;
    }
}
