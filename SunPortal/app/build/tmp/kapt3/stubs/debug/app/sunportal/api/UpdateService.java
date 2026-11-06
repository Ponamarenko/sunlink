package app.sunportal.api;

/**
 * API сервис для проверки обновлений
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\b\n\u0002\b\u0007\n\u0002\u0010$\n\u0002\b\u0003\bf\u0018\u00002\u00020\u0001J6\u0010\u0002\u001a\u00020\u00032\b\b\u0001\u0010\u0004\u001a\u00020\u00052\b\b\u0001\u0010\u0006\u001a\u00020\u00072\b\b\u0001\u0010\b\u001a\u00020\u00052\b\b\u0003\u0010\t\u001a\u00020\u0005H\u00a7@\u00a2\u0006\u0002\u0010\nJ\u000e\u0010\u000b\u001a\u00020\u0003H\u00a7@\u00a2\u0006\u0002\u0010\fJ\u000e\u0010\r\u001a\u00020\u0003H\u00a7@\u00a2\u0006\u0002\u0010\fJ0\u0010\u000e\u001a\u000e\u0012\u0004\u0012\u00020\u0005\u0012\u0004\u0012\u00020\u00010\u000f2\u0014\b\u0001\u0010\u0010\u001a\u000e\u0012\u0004\u0012\u00020\u0005\u0012\u0004\u0012\u00020\u00010\u000fH\u00a7@\u00a2\u0006\u0002\u0010\u0011\u00a8\u0006\u0012"}, d2 = {"Lapp/sunportal/api/UpdateService;", "", "checkForUpdates", "Lapp/sunportal/data/model/UpdateInfo;", "currentVersion", "", "currentCode", "", "deviceId", "platform", "(Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getLatestUpdate", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getTestUpdate", "logDownload", "", "downloadInfo", "(Ljava/util/Map;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
public abstract interface UpdateService {
    
    @retrofit2.http.GET(value = "api/update/check")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object checkForUpdates(@retrofit2.http.Query(value = "current_version")
    @org.jetbrains.annotations.NotNull()
    java.lang.String currentVersion, @retrofit2.http.Query(value = "current_code")
    int currentCode, @retrofit2.http.Query(value = "device_id")
    @org.jetbrains.annotations.NotNull()
    java.lang.String deviceId, @retrofit2.http.Query(value = "platform")
    @org.jetbrains.annotations.NotNull()
    java.lang.String platform, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super app.sunportal.data.model.UpdateInfo> $completion);
    
    @retrofit2.http.GET(value = "api/update/latest")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getLatestUpdate(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super app.sunportal.data.model.UpdateInfo> $completion);
    
    @retrofit2.http.POST(value = "api/update/download")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object logDownload(@retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    java.util.Map<java.lang.String, ? extends java.lang.Object> downloadInfo, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.Map<java.lang.String, ? extends java.lang.Object>> $completion);
    
    @retrofit2.http.GET(value = "api/update/test")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getTestUpdate(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super app.sunportal.data.model.UpdateInfo> $completion);
    
    /**
     * API сервис для проверки обновлений
     */
    @kotlin.Metadata(mv = {1, 9, 0}, k = 3, xi = 48)
    public static final class DefaultImpls {
    }
}