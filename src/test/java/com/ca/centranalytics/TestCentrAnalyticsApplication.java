package com.ca.centranalytics;

import org.springframework.boot.SpringApplication;

public class TestCentrAnalyticsApplication {

    public static void main(String[] args) {
        SpringApplication.from(CentrAnalyticsApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
