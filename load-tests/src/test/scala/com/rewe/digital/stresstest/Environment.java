package com.rewe.digital.stresstest;

public enum Environment {

    PLAY("http://localhost:9000"),
    AKKA_HTTP("http://localhost:8080"),
    FINATRA("http://localhost:9999");

    private final String url;

    private Environment(final String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return url;
    }

    public String url() { return url; }


}
