package id.uvs.gateway;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "uvs.brand")
public record BrandProperties(
        String appName,
        String organizationName,
        String accentColor
) {
}
