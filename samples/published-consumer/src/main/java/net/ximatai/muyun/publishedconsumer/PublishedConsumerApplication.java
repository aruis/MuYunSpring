package net.ximatai.muyun.publishedconsumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Isolated consumer used to verify Maven metadata and Starter auto-configuration. */
@SpringBootApplication
public class PublishedConsumerApplication {
    public static void main(String[] args) {
        SpringApplication.run(PublishedConsumerApplication.class, args);
    }
}
