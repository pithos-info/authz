package info.pithos.rbac.app.auth;

import info.pithos.auth.OAuthClient;
import info.pithos.runtime.core.context.ApplicationContext;
import info.pithos.runtime.core.context.ServiceModule;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class BypassAuthModule extends ServiceModule {

    public BypassAuthModule(ApplicationContext context) {
        super(context);
    }

    @Override
    public boolean init() {
        this.initialized.compareAndSet(false, true);
        return this.initialized.get();
    }

    @Override
    protected void configure() {
        super.configure();
        bind(OAuthClient.class).to(BypassOAuthClient.class);
    }

    @Override
    public CompletableFuture<Boolean> start(long timeout, TimeUnit unit) {
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> shutdown(long timeout, TimeUnit unit) {
        return CompletableFuture.completedFuture(true);
    }
}
