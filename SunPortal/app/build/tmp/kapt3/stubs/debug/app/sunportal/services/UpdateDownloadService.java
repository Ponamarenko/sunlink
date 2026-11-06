package app.sunportal.services;

/**
 * Сервис для скачивания обновлений
 */
@dagger.hilt.android.AndroidEntryPoint()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000b\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\t\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\b\n\u0002\b\u0006\b\u0007\u0018\u0000 (2\u00020\u0001:\u0001(B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u0013\u001a\u00020\u0014H\u0002J\b\u0010\u0015\u001a\u00020\u0016H\u0002J\u0010\u0010\u0017\u001a\u00020\u00182\u0006\u0010\u0019\u001a\u00020\u001aH\u0002J\b\u0010\u001b\u001a\u00020\u0018H\u0002J\u0012\u0010\u001c\u001a\u0004\u0018\u00010\u001d2\u0006\u0010\u001e\u001a\u00020\u001fH\u0016J\b\u0010 \u001a\u00020\u0016H\u0016J\b\u0010!\u001a\u00020\u0016H\u0016J\"\u0010\"\u001a\u00020#2\b\u0010\u001e\u001a\u0004\u0018\u00010\u001f2\u0006\u0010$\u001a\u00020#2\u0006\u0010%\u001a\u00020#H\u0016J\u0010\u0010&\u001a\u00020\u00162\u0006\u0010\u0019\u001a\u00020\u001aH\u0002J\b\u0010\'\u001a\u00020\u0016H\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u000b\u001a\u0004\u0018\u00010\fX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001e\u0010\r\u001a\u00020\u000e8\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u000f\u0010\u0010\"\u0004\b\u0011\u0010\u0012\u00a8\u0006)"}, d2 = {"Lapp/sunportal/services/UpdateDownloadService;", "Landroid/app/Service;", "()V", "downloadCompleteReceiver", "Landroid/content/BroadcastReceiver;", "downloadId", "", "serviceJob", "Lkotlinx/coroutines/CompletableJob;", "serviceScope", "Lkotlinx/coroutines/CoroutineScope;", "updateInfo", "Lapp/sunportal/data/model/UpdateInfo;", "updateManager", "Lapp/sunportal/utils/UpdateManager;", "getUpdateManager", "()Lapp/sunportal/utils/UpdateManager;", "setUpdateManager", "(Lapp/sunportal/utils/UpdateManager;)V", "createNotification", "Landroid/app/Notification;", "createNotificationChannel", "", "getInstallPendingIntent", "Landroid/app/PendingIntent;", "apkUri", "", "getRetryPendingIntent", "onBind", "Landroid/os/IBinder;", "intent", "Landroid/content/Intent;", "onCreate", "onDestroy", "onStartCommand", "", "flags", "startId", "showDownloadCompleteNotification", "showDownloadFailedNotification", "Companion", "app_debug"})
public final class UpdateDownloadService extends android.app.Service {
    @javax.inject.Inject()
    public app.sunportal.utils.UpdateManager updateManager;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.CompletableJob serviceJob = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.CoroutineScope serviceScope = null;
    private long downloadId = -1L;
    @org.jetbrains.annotations.Nullable()
    private app.sunportal.data.model.UpdateInfo updateInfo;
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String ACTION_DOWNLOAD_UPDATE = "app.sunportal.DOWNLOAD_UPDATE";
    @org.jetbrains.annotations.NotNull()
    public static final java.lang.String EXTRA_UPDATE_INFO = "update_info";
    @org.jetbrains.annotations.NotNull()
    private final android.content.BroadcastReceiver downloadCompleteReceiver = null;
    @org.jetbrains.annotations.NotNull()
    public static final app.sunportal.services.UpdateDownloadService.Companion Companion = null;
    
    public UpdateDownloadService() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final app.sunportal.utils.UpdateManager getUpdateManager() {
        return null;
    }
    
    public final void setUpdateManager(@org.jetbrains.annotations.NotNull()
    app.sunportal.utils.UpdateManager p0) {
    }
    
    @java.lang.Override()
    public void onCreate() {
    }
    
    @java.lang.Override()
    public int onStartCommand(@org.jetbrains.annotations.Nullable()
    android.content.Intent intent, int flags, int startId) {
        return 0;
    }
    
    @java.lang.Override()
    public void onDestroy() {
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.Nullable()
    public android.os.IBinder onBind(@org.jetbrains.annotations.NotNull()
    android.content.Intent intent) {
        return null;
    }
    
    private final android.app.Notification createNotification() {
        return null;
    }
    
    private final void showDownloadCompleteNotification(java.lang.String apkUri) {
    }
    
    private final void showDownloadFailedNotification() {
    }
    
    private final android.app.PendingIntent getInstallPendingIntent(java.lang.String apkUri) {
        return null;
    }
    
    private final android.app.PendingIntent getRetryPendingIntent() {
        return null;
    }
    
    private final void createNotificationChannel() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0016\u0010\u0006\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\u000bR\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\f"}, d2 = {"Lapp/sunportal/services/UpdateDownloadService$Companion;", "", "()V", "ACTION_DOWNLOAD_UPDATE", "", "EXTRA_UPDATE_INFO", "startDownloadService", "", "context", "Landroid/content/Context;", "updateInfo", "Lapp/sunportal/data/model/UpdateInfo;", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        public final void startDownloadService(@org.jetbrains.annotations.NotNull()
        android.content.Context context, @org.jetbrains.annotations.NotNull()
        app.sunportal.data.model.UpdateInfo updateInfo) {
        }
    }
}