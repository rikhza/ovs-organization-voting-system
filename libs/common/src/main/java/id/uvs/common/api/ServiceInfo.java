package id.uvs.common.api;

public record ServiceInfo(
        String service,
        String version,
        String status
) {
    public static ServiceInfo ready(String service) {
        return new ServiceInfo(service, "0.1.0-SNAPSHOT", "ready");
    }
}
