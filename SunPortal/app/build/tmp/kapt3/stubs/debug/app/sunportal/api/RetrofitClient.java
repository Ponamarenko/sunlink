package app.sunportal.api;

/**
 * Retrofit клиент для SunPortal API
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00006\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u000e\u0010\u0014\u001a\u00020\u00152\u0006\u0010\u0016\u001a\u00020\u0004R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u001b\u0010\u0005\u001a\u00020\u00068FX\u0086\u0084\u0002\u00a2\u0006\f\n\u0004\b\t\u0010\n\u001a\u0004\b\u0007\u0010\bR\u000e\u0010\u000b\u001a\u00020\fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\r\u001a\u00020\u000eX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001b\u0010\u000f\u001a\u00020\u00108FX\u0086\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0013\u0010\n\u001a\u0004\b\u0011\u0010\u0012\u00a8\u0006\u0017"}, d2 = {"Lapp/sunportal/api/RetrofitClient;", "", "()V", "BASE_URL", "", "apiService", "Lapp/sunportal/api/ApiService;", "getApiService", "()Lapp/sunportal/api/ApiService;", "apiService$delegate", "Lkotlin/Lazy;", "loggingInterceptor", "Lokhttp3/logging/HttpLoggingInterceptor;", "okHttpClient", "Lokhttp3/OkHttpClient;", "updateService", "Lapp/sunportal/api/UpdateService;", "getUpdateService", "()Lapp/sunportal/api/UpdateService;", "updateService$delegate", "createRetrofit", "Lretrofit2/Retrofit;", "baseUrl", "app_debug"})
public final class RetrofitClient {
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String BASE_URL = "http://k4a.pp.ua:8000/";
    @org.jetbrains.annotations.NotNull()
    private static final okhttp3.logging.HttpLoggingInterceptor loggingInterceptor = null;
    @org.jetbrains.annotations.NotNull()
    private static final okhttp3.OkHttpClient okHttpClient = null;
    @org.jetbrains.annotations.NotNull()
    private static final kotlin.Lazy apiService$delegate = null;
    @org.jetbrains.annotations.NotNull()
    private static final kotlin.Lazy updateService$delegate = null;
    @org.jetbrains.annotations.NotNull()
    public static final app.sunportal.api.RetrofitClient INSTANCE = null;
    
    private RetrofitClient() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final app.sunportal.api.ApiService getApiService() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final app.sunportal.api.UpdateService getUpdateService() {
        return null;
    }
    
    /**
     * Создание Retrofit с кастомным URL
     */
    @org.jetbrains.annotations.NotNull()
    public final retrofit2.Retrofit createRetrofit(@org.jetbrains.annotations.NotNull()
    java.lang.String baseUrl) {
        return null;
    }
}