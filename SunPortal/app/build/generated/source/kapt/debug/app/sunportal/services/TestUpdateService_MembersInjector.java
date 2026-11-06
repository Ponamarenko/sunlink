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
public final class TestUpdateService_MembersInjector implements MembersInjector<TestUpdateService> {
  private final Provider<UpdateManager> updateManagerProvider;

  public TestUpdateService_MembersInjector(Provider<UpdateManager> updateManagerProvider) {
    this.updateManagerProvider = updateManagerProvider;
  }

  public static MembersInjector<TestUpdateService> create(
      Provider<UpdateManager> updateManagerProvider) {
    return new TestUpdateService_MembersInjector(updateManagerProvider);
  }

  @Override
  public void injectMembers(TestUpdateService instance) {
    injectUpdateManager(instance, updateManagerProvider.get());
  }

  @InjectedFieldSignature("app.sunportal.services.TestUpdateService.updateManager")
  public static void injectUpdateManager(TestUpdateService instance, UpdateManager updateManager) {
    instance.updateManager = updateManager;
  }
}
