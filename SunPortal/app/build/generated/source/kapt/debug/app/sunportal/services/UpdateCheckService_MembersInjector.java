package app.sunportal.services;

import app.sunportal.utils.UpdateManager;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class UpdateCheckService_MembersInjector implements MembersInjector<UpdateCheckService> {
  private final Provider<UpdateManager> updateManagerProvider;

  public UpdateCheckService_MembersInjector(Provider<UpdateManager> updateManagerProvider) {
    this.updateManagerProvider = updateManagerProvider;
  }

  public static MembersInjector<UpdateCheckService> create(
      Provider<UpdateManager> updateManagerProvider) {
    return new UpdateCheckService_MembersInjector(updateManagerProvider);
  }

  @Override
  public void injectMembers(UpdateCheckService instance) {
    injectUpdateManager(instance, updateManagerProvider.get());
  }

  @InjectedFieldSignature("app.sunportal.services.UpdateCheckService.updateManager")
  public static void injectUpdateManager(UpdateCheckService instance, UpdateManager updateManager) {
    instance.updateManager = updateManager;
  }
}
