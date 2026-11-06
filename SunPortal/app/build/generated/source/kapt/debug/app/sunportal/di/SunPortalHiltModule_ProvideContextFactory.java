package app.sunportal.di;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class SunPortalHiltModule_ProvideContextFactory implements Factory<Context> {
  private final Provider<Context> contextProvider;

  public SunPortalHiltModule_ProvideContextFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public Context get() {
    return provideContext(contextProvider.get());
  }

  public static SunPortalHiltModule_ProvideContextFactory create(
      Provider<Context> contextProvider) {
    return new SunPortalHiltModule_ProvideContextFactory(contextProvider);
  }

  public static Context provideContext(Context context) {
    return Preconditions.checkNotNullFromProvides(SunPortalHiltModule.INSTANCE.provideContext(context));
  }
}
