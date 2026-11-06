package app.sunportal.utils;

/**
 * Утилита для проверки разрешений
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0007\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u000e\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006J\u000e\u0010\u0007\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006J\u000e\u0010\b\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006J\u000e\u0010\t\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006J\u000e\u0010\n\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006J\u000e\u0010\u000b\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006J\u000e\u0010\f\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006\u00a8\u0006\r"}, d2 = {"Lapp/sunportal/utils/PermissionUtils;", "", "()V", "hasAllUpdatePermissions", "", "context", "Landroid/content/Context;", "hasInternetPermission", "hasNetworkStatePermission", "hasPostNotificationsPermission", "hasReadStoragePermission", "hasWriteStoragePermission", "shouldRequestUpdatePermissions", "app_debug"})
public final class PermissionUtils {
    @org.jetbrains.annotations.NotNull()
    public static final app.sunportal.utils.PermissionUtils INSTANCE = null;
    
    private PermissionUtils() {
        super();
    }
    
    /**
     * Проверка разрешения на доступ к интернету
     */
    public final boolean hasInternetPermission(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        return false;
    }
    
    /**
     * Проверка разрешения на доступ к сети
     */
    public final boolean hasNetworkStatePermission(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        return false;
    }
    
    /**
     * Проверка разрешения на запись во внешнее хранилище
     */
    public final boolean hasWriteStoragePermission(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        return false;
    }
    
    /**
     * Проверка разрешения на чтение из внешнего хранилища
     */
    public final boolean hasReadStoragePermission(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        return false;
    }
    
    /**
     * Проверка разрешения на показ уведомлений
     */
    public final boolean hasPostNotificationsPermission(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        return false;
    }
    
    /**
     * Проверка всех необходимых разрешений для обновлений
     */
    public final boolean hasAllUpdatePermissions(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        return false;
    }
    
    /**
     * Проверка, нужно ли запрашивать разрешения
     */
    public final boolean shouldRequestUpdatePermissions(@org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        return false;
    }
}