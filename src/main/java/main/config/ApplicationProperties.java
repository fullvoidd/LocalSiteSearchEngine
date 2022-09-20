package main.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The class required to access the information contained in the configuration file.
 */
@Component
@ConfigurationProperties
@Data
public class ApplicationProperties {

    private List<CfgSite> sites;
    private int limit;

    /**
     * DTO-class required for containing information about site from configuration file.
     */
    @Data
    public static class CfgSite {

        private String url;
        private String name;
    }
}
