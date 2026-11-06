package app.sunportal.viewmodel;

import android.app.Application;
import app.sunportal.utils.UpdateManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class UpdateViewModel_Factory implements Factory<UpdateViewModel> {
  private final Provider<Application> applicationProvider;

  private final Provider<UpdateManager> updateManagerProvider;

  public UpdateViewModel_Factory(Provider<Application> applicationProvider,
      Provider<UpdateManager> updateManagerProvider) {
    this.applicationProvider = applicationProvider;
    this.updateManagerProvider = updateManagerProvider;
  }

  @Override
  public UpdateViewModel get() {
    return newInstance(applicationProvider.get(), updateManagerProvider.get());
  }

  public static UpdateViewModel_Factory create(Provider<Application> applicationProvider,
      Provider<UpdateManager> updateManagerProvider) {
    return new UpdateViewModel_Factory(applicationProvider, updateManagerProvider);
  }

  public static UpdateViewModel newInstance(Application application, UpdateManager updateManager) {
    return new UpdateViewModel(application, updateManager);
  }
}
