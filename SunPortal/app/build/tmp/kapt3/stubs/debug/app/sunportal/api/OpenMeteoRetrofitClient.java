package app.sunportal.api;

/**
 * Retrofit клиент для Open-Meteo API
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000 \n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001b\u0010\u0007\u001a\u00020\b8FX\u0086\u0084\u0002\u00a2\u0006\f\n\u0004\b\u000b\u0010\f\u001a\u0004\b\t\u0010\n\u00a8\u0006\r"}, d2 = {"Lapp/sunportal/api/OpenMeteoRetrofitClient;", "", "()V", "BASE_URL", "", "okHttpClient", "Lokhttp3/OkHttpClient;", "openMeteoApi", "Lapp/sunportal/api/OpenMeteoApi;", "getOpenMeteoApi", "()Lapp/sunportal/api/OpenMeteoApi;", "openMeteoApi$delegate", "Lkotlin/Lazy;", "app_debug"})
public final class OpenMeteoRetrofitClient {
    @org.jetbrains.annotations.NotNull()
    private static final java.lang.String BASE_URL = "https://api.open-meteo.com/";
    @org.jetbrains.annotations.NotNull()
    private static final okhttp3.OkHttpClient okHttpClient = null;
    @org.jetbrains.annotations.NotNull()
    private static final kotlin.Lazy openMeteoApi$delegate = null;
    @org.jetbrains.annotations.NotNull()
    public static final app.sunportal.api.OpenMeteoRetrofitClient INSTANCE = null;
    
    private OpenMeteoRetrofitClient() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final app.sunportal.api.OpenMeteoApi getOpenMeteoApi() {
        return null;
    }
}