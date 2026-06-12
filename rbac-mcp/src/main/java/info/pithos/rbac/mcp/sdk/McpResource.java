package info.pithos.rbac.mcp.sdk;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as an MCP resource provider.
 * TODO: replace with the actual SDK annotation when wiring io.modelcontextprotocol.sdk:mcp.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpResource {
    /** URI pattern, e.g. {@code rbac://user/context}. */
    String uri();
    String description();
}
