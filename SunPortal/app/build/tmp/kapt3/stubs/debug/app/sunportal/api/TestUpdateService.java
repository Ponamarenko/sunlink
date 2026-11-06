package app.sunportal.api;

/**
 * Тестовый API сервис для демонстрации обновлений
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000$\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0010$\n\u0002\u0010\u000e\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\bf\u0018\u00002\u00020\u0001J&\u0010\u0002\u001a\u001a\u0012\u0016\u0012\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00010\u00050\u00040\u0003H\u00a7@\u00a2\u0006\u0002\u0010\u0007J,\u0010\b\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\u00010\u00050\u00032\n\b\u0003\u0010\t\u001a\u0004\u0018\u00010\u0006H\u00a7@\u00a2\u0006\u0002\u0010\nJ\u000e\u0010\u000b\u001a\u00020\fH\u00a7@\u00a2\u0006\u0002\u0010\u0007\u00a8\u0006\r"}, d2 = {"Lapp/sunportal/api/TestUpdateService;", "", "getAllReleases", "Lretrofit2/Response;", "", "", "", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getLatestRelease", "currentVersion", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getTestUpdateInfo", "Lapp/sunportal/data/model/UpdateInfo;", "app_debug"})
public abstract interface TestUpdateService {
    
    @retrofit2.http.GET(value = "https://api.github.com/repos/Ponamarenko/SunPortal/releases/latest")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getLatestRelease(@retrofit2.http.Query(value = "current_version")
    @org.jetbrains.annotations.Nullable()
    java.lang.String currentVersion, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<java.util.Map<java.lang.String, java.lang.Object>>> $completion);
    
    @retrofit2.http.GET(value = "https://api.github.com/repos/Ponamarenko/SunPortal/releases")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getAllReleases(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super retrofit2.Response<java.util.List<java.util.Map<java.lang.String, java.lang.Object>>>> $completion);
    
    @retrofit2.http.GET(value = "https://raw.githubusercontent.com/Ponamarenko/SunPortal/main/test-update.json")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getTestUpdateInfo(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super app.sunportal.data.model.UpdateInfo> $completion);
    
    /**
     * Тестовый API сервис для демонстрации обновлений
     */
    @kotlin.Metadata(mv = {1, 9, 0}, k = 3, xi = 48)
    public static final class DefaultImpls {
    }
}