package info.pithos.rbac.mcp.sdk;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as an MCP tool.
 * TODO: replace with the actual SDK annotation when wiring io.modelcontextprotocol.sdk:mcp.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpTool {
    String name();
    String description();
}
