package edu.ngd.order.wulian;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"edu.ngd.order.wulian", "com.plc.mqtt"})
public class WulianApplication {

    public static void main(String[] args) {
        SpringApplication.run(WulianApplication.class, args);
    }
}
