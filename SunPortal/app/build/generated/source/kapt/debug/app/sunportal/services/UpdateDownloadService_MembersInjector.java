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
public final class UpdateDownloadService_MembersInjector implements MembersInjector<UpdateDownloadService> {
  private final Provider<UpdateManager> updateManagerProvider;

  public UpdateDownloadService_MembersInjector(Provider<UpdateManager> updateManagerProvider) {
    this.updateManagerProvider = updateManagerProvider;
  }

  public static MembersInjector<UpdateDownloadService> create(
      Provider<UpdateManager> updateManagerProvider) {
    return new UpdateDownloadService_MembersInjector(updateManagerProvider);
  }

  @Override
  public void injectMembers(UpdateDownloadService instance) {
    injectUpdateManager(instance, updateManagerProvider.get());
  }

  @InjectedFieldSignature("app.sunportal.services.UpdateDownloadService.updateManager")
  public static void injectUpdateManager(UpdateDownloadService instance,
      UpdateManager updateManager) {
    instance.updateManager = updateManager;
  }
}
