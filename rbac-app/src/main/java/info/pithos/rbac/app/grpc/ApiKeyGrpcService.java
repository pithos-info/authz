package info.pithos.rbac.app.grpc;

import com.google.inject.Inject;
import info.pithos.rbac.app.handler.ApiKeyHandlers;
import info.pithos.rbac.service.ApiKey;
import info.pithos.rbac.service.ApiKeyList;
import info.pithos.rbac.service.ApiKeyServiceGrpc;
import info.pithos.rbac.service.CreateApiKeyRequest;
import info.pithos.rbac.service.DeleteByIdRequest;
import info.pithos.rbac.service.Empty;
import info.pithos.rbac.service.GetByIdRequest;
import info.pithos.service.container.core.grpc.GrpcSupport;
import io.grpc.stub.StreamObserver;

public final class ApiKeyGrpcService extends ApiKeyServiceGrpc.ApiKeyServiceImplBase {

    private final ApiKeyHandlers.Create create;
    private final ApiKeyHandlers.Get    get;
    private final ApiKeyHandlers.Revoke revoke;
    private final ApiKeyHandlers.List   list;

    @Inject
    public ApiKeyGrpcService(
            ApiKeyHandlers.Create create,
            ApiKeyHandlers.Get    get,
            ApiKeyHandlers.Revoke revoke,
            ApiKeyHandlers.List   list) {
        this.create = create;
        this.get    = get;
        this.revoke = revoke;
        this.list   = list;
    }

    @Override
    public void create(CreateApiKeyRequest request, StreamObserver<ApiKey> responseObserver) {
        GrpcSupport.respond(create.handle(request, GrpcSupport.context()), responseObserver);
    }

    @Override
    public void get(GetByIdRequest request, StreamObserver<ApiKey> responseObserver) {
        GrpcSupport.respond(get.handle(request, GrpcSupport.context()), responseObserver);
    }

    @Override
    public void revoke(DeleteByIdRequest request, StreamObserver<Empty> responseObserver) {
        GrpcSupport.respond(revoke.handle(request, GrpcSupport.context()), responseObserver);
    }

    @Override
    public void list(Empty request, StreamObserver<ApiKeyList> responseObserver) {
        GrpcSupport.respond(list.handle(request, GrpcSupport.context()), responseObserver);
    }
}
