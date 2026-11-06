package app.sunportal.utils;

/**
 * Менеджер обновлений приложения
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000b\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\t\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0005\u0018\u00002\u00020\u0001B\u000f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u001c\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00120\u0011H\u0086@\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\u0004\b\u0013\u0010\u0014J\u000e\u0010\u0015\u001a\u00020\u00162\u0006\u0010\u0017\u001a\u00020\u0012J\u0014\u0010\u0018\u001a\u000e\u0012\u0004\u0012\u00020\u001a\u0012\u0004\u0012\u00020\u001b0\u0019H\u0002J\b\u0010\u001c\u001a\u00020\u001aH\u0002J\u000e\u0010\u001d\u001a\u00020\u001e2\u0006\u0010\u001f\u001a\u00020\u0016J\u001c\u0010 \u001a\b\u0012\u0004\u0012\u00020\u00120\u0011H\u0086@\u00f8\u0001\u0000\u00f8\u0001\u0001\u00a2\u0006\u0004\b!\u0010\u0014J\u0010\u0010\"\u001a\u00020#2\u0006\u0010\u0017\u001a\u00020\u0012H\u0002J\u001e\u0010$\u001a\u00020%2\u0006\u0010\u0017\u001a\u00020\u00122\u0006\u0010&\u001a\u00020\'H\u0086@\u00a2\u0006\u0002\u0010(J\u0018\u0010)\u001a\u00020%2\u0006\u0010\u001f\u001a\u00020\u00162\u0006\u0010\u0017\u001a\u00020\u0012H\u0002J\u000e\u0010*\u001a\u00020%2\u0006\u0010\u0017\u001a\u00020\u0012J\u000e\u0010+\u001a\u00020%2\u0006\u0010\u0017\u001a\u00020\u0012R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001b\u0010\u0005\u001a\u00020\u00068BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\t\u0010\n\u001a\u0004\b\u0007\u0010\bR\u001b\u0010\u000b\u001a\u00020\f8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u000f\u0010\n\u001a\u0004\b\r\u0010\u000e\u0082\u0002\u000b\n\u0002\b!\n\u0005\b\u00a1\u001e0\u0001\u00a8\u0006,"}, d2 = {"Lapp/sunportal/utils/UpdateManager;", "", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "downloadManager", "Landroid/app/DownloadManager;", "getDownloadManager", "()Landroid/app/DownloadManager;", "downloadManager$delegate", "Lkotlin/Lazy;", "notificationManager", "Landroidx/core/app/NotificationManagerCompat;", "getNotificationManager", "()Landroidx/core/app/NotificationManagerCompat;", "notificationManager$delegate", "checkForUpdates", "Lkotlin/Result;", "Lapp/sunportal/data/model/UpdateInfo;", "checkForUpdates-IoAF18A", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "downloadUpdate", "", "updateInfo", "getCurrentVersion", "Lkotlin/Pair;", "", "", "getDeviceId", "getDownloadStatus", "Landroid/app/DownloadManager$Query;", "downloadId", "getLatestUpdate", "getLatestUpdate-IoAF18A", "getUpdatePendingIntent", "Landroid/app/PendingIntent;", "logDownload", "", "success", "", "(Lapp/sunportal/data/model/UpdateInfo;ZLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "showDownloadNotification", "showForceUpdateNotification", "showUpdateNotification", "app_debug"})
public final class UpdateManager {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy downloadManager$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlin.Lazy notificationManager$delegate = null;
    
    @javax.inject.Inject()
    public UpdateManager(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        super();
    }
    
    private final android.app.DownloadManager getDownloadManager() {
        return null;
    }
    
    private final androidx.core.app.NotificationManagerCompat getNotificationManager() {
        return null;
    }
    
    /**
     * Скачивание обновления
     */
    public final long downloadUpdate(@org.jetbrains.annotations.NotNull()
    app.sunportal.data.model.UpdateInfo updateInfo) {
        return 0L;
    }
    
    /**
     * Проверка статуса скачивания
     */
    @org.jetbrains.annotations.NotNull()
    public final android.app.DownloadManager.Query getDownloadStatus(long downloadId) {
        return null;
    }
    
    /**
     * Показать уведомление о скачивании
     */
    private final void showDownloadNotification(long downloadId, app.sunportal.data.model.UpdateInfo updateInfo) {
    }
    
    /**
     * Показать уведомление об обновлении
     */
    public final void showUpdateNotification(@org.jetbrains.annotations.NotNull()
    app.sunportal.data.model.UpdateInfo updateInfo) {
    }
    
    /**
     * Показать уведомление об обязательном обновлении
     */
    public final void showForceUpdateNotification(@org.jetbrains.annotations.NotNull()
    app.sunportal.data.model.UpdateInfo updateInfo) {
    }
    
    /**
     * Получение PendingIntent для скачивания обновления
     */
    private final android.app.PendingIntent getUpdatePendingIntent(app.sunportal.data.model.UpdateInfo updateInfo) {
        return null;
    }
    
    /**
     * Получение текущей версии приложения
     */
    private final kotlin.Pair<java.lang.String, java.lang.Integer> getCurrentVersion() {
        return null;
    }
    
    /**
     * Получение ID устройства
     */
    private final java.lang.String getDeviceId() {
        return null;
    }
    
    /**
     * Логирование скачивания
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object logDownload(@org.jetbrains.annotations.NotNull()
    app.sunportal.data.model.UpdateInfo updateInfo, boolean success, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
}