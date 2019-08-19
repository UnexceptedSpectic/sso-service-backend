package dev.blep.accounts;

import dev.blep.accounts.config.SecurityProperties;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Bootstraps the application.
 */

// Enable Lombok plugin and Annotation Processors in Intellij
@Log4j2
// can't seem to authenticate spring security. disabled for now
@SpringBootApplication(exclude = { SecurityAutoConfiguration.class })
@EnableConfigurationProperties(SecurityProperties.class)
public class Application {


    /**
     * Initializes the web server.
     * @param args none
     */
    public static void main(String[] args) {
        log.info("Starting application...");
        SpringApplication.run(Application.class, args);
    }
}
