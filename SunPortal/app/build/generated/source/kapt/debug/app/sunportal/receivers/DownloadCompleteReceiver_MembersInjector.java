package app.sunportal.receivers;

import android.app.NotificationManager;
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
public final class DownloadCompleteReceiver_MembersInjector implements MembersInjector<DownloadCompleteReceiver> {
  private final Provider<NotificationManager> notificationManagerProvider;

  public DownloadCompleteReceiver_MembersInjector(
      Provider<NotificationManager> notificationManagerProvider) {
    this.notificationManagerProvider = notificationManagerProvider;
  }

  public static MembersInjector<DownloadCompleteReceiver> create(
      Provider<NotificationManager> notificationManagerProvider) {
    return new DownloadCompleteReceiver_MembersInjector(notificationManagerProvider);
  }

  @Override
  public void injectMembers(DownloadCompleteReceiver instance) {
    injectNotificationManager(instance, notificationManagerProvider.get());
  }

  @InjectedFieldSignature("app.sunportal.receivers.DownloadCompleteReceiver.notificationManager")
  public static void injectNotificationManager(DownloadCompleteReceiver instance,
      NotificationManager notificationManager) {
    instance.notificationManager = notificationManager;
  }
}
