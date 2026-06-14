package info.pithos.rbac.app.config;

import com.fasterxml.jackson.databind.JsonNode;
import info.pithos.runtime.model.config.Config.ConfigMap;
import info.pithos.runtime.model.config.Config.Configs;
import info.pithos.serde.ProtoBufSerde;

import java.io.FileReader;
import java.io.IOException;

public final class RbacConfigLoader {

    private static final int DEFAULT_HTTP_PORT = 8080;
    private static final int DEFAULT_GRPC_PORT = 9090;

    /**
     * Loads {@code RbacBootstrapConfig} from a YAML file at {@code path}.
     *
     * <p>The YAML structure mirrors the {@code ConfigMap} proto field names (camelCase).
     * A top-level {@code server} block is reserved for app-level settings and is not
     * part of the proto — it is extracted here before proto parsing:
     * <pre>
     * server:
     *   httpPort: 8080
     *   grpcPort: 9090
     * </pre>
     */
    public static RbacBootstrapConfig load(String path) {
        try (FileReader reader = new FileReader(path)) {
            JsonNode root = ProtoBufSerde.YAML_MAPPER.readTree(reader);

            int httpPort = root.path("server").path("httpPort").asInt(DEFAULT_HTTP_PORT);
            int grpcPort = root.path("server").path("grpcPort").asInt(DEFAULT_GRPC_PORT);
            boolean bypassAuth = root.path("server").path("bypassAuth").asBoolean(false);

            // Parse the whole tree into ConfigMap; the `server` block is ignored via
            // ignoringUnknownFields() inside fromJsonNode.
            ConfigMap parsed = ProtoBufSerde
                .<ConfigMap>fromJsonNode(root, ConfigMap.newBuilder())
                .getObject();

            // ServiceConfigs.getConfig() does an unconditional get(serviceName) on the
            // configs map and will NPE if the key is absent — ensure it always exists.
            String serviceName = parsed.getBootstrapConfigs().getServiceName();
            ConfigMap configMap = parsed.toBuilder()
                .putConfigs(
                    serviceName,
                    parsed.getConfigsOrDefault(serviceName, Configs.getDefaultInstance())
                )
                .build();

            return new RbacBootstrapConfig(configMap, httpPort, grpcPort, bypassAuth);

        } catch (IOException e) {
            throw new IllegalStateException("Failed to load RBAC config from " + path, e);
        }
    }

    private RbacConfigLoader() {}
}
