package com.example.jaxdemo;

public class TomcatStatus {

    private String war;
    private Integer version;
    private Integer replicas;

    public String getWar() {
        return war;
    }

    public void setWar(String war) {
        this.war = war;
    }

    public Integer getVersion() { return version; }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Integer getReplicas() { return replicas; }

    public void setReplicas(Integer replicas) {
        this.replicas = replicas;
    }
}
