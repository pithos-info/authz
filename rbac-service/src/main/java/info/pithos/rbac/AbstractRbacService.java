package info.pithos.rbac;

import info.pithos.data.relational.DataContext;
import info.pithos.data.relational.client.RelationalClient;
import info.pithos.runtime.model.protocol.Context.RequestContext;

import java.util.List;

public abstract class AbstractRbacService {

    protected final RelationalClient relationalClient;

    protected AbstractRbacService(RelationalClient relationalClient) {
        if (relationalClient == null) throw new IllegalArgumentException("relationalClient must not be null");
        this.relationalClient = relationalClient;
    }

    protected static DataContext dc(RequestContext rc) {
        return DataContext.of(rc);
    }

    protected static String authEnterpriseId(RequestContext ctx) {
        return ctx.getAuthContext().getEnterpriseId();
    }

    protected static String authUserId(RequestContext ctx) {
        return ctx.getAuthContext().getUserId();
    }

    protected static String pgArray(List<String> values) {
        if (values.isEmpty()) return "{}";
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append('"');
            sb.append(values.get(i).replace("\\", "\\\\").replace("\"", "\\\""));
            sb.append('"');
        }
        return sb.append('}').toString();
    }
}
