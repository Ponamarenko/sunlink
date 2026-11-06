package app.sunportal.api;

/**
 * API сервис для SunPortal приложения
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010$\n\u0002\u0010\u000e\n\u0002\b\t\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0006\bf\u0018\u00002\u00020\u0001J0\u0010\u0002\u001a\u000e\u0012\u0004\u0012\u00020\u0004\u0012\u0004\u0012\u00020\u00010\u00032\u0014\b\u0001\u0010\u0005\u001a\u000e\u0012\u0004\u0012\u00020\u0004\u0012\u0004\u0012\u00020\u00040\u0003H\u00a7@\u00a2\u0006\u0002\u0010\u0006J\u001a\u0010\u0007\u001a\u000e\u0012\u0004\u0012\u00020\u0004\u0012\u0004\u0012\u00020\u00010\u0003H\u00a7@\u00a2\u0006\u0002\u0010\bJ\u001a\u0010\t\u001a\u000e\u0012\u0004\u0012\u00020\u0004\u0012\u0004\u0012\u00020\u00010\u0003H\u00a7@\u00a2\u0006\u0002\u0010\bJ$\u0010\n\u001a\u000e\u0012\u0004\u0012\u00020\u0004\u0012\u0004\u0012\u00020\u00010\u00032\b\b\u0003\u0010\u000b\u001a\u00020\u0004H\u00a7@\u00a2\u0006\u0002\u0010\fJ\u000e\u0010\r\u001a\u00020\u000eH\u00a7@\u00a2\u0006\u0002\u0010\bJ\u000e\u0010\u000f\u001a\u00020\u000eH\u00a7@\u00a2\u0006\u0002\u0010\bJ\u001a\u0010\u0010\u001a\u000e\u0012\u0004\u0012\u00020\u0004\u0012\u0004\u0012\u00020\u00010\u0003H\u00a7@\u00a2\u0006\u0002\u0010\bJ\u001a\u0010\u0011\u001a\u000e\u0012\u0004\u0012\u00020\u0004\u0012\u0004\u0012\u00020\u00010\u0003H\u00a7@\u00a2\u0006\u0002\u0010\bJ\u001a\u0010\u0012\u001a\u000e\u0012\u0004\u0012\u00020\u0004\u0012\u0004\u0012\u00020\u00010\u0003H\u00a7@\u00a2\u0006\u0002\u0010\bJ\u000e\u0010\u0013\u001a\u00020\u0014H\u00a7@\u00a2\u0006\u0002\u0010\bJ\u001a\u0010\u0015\u001a\u000e\u0012\u0004\u0012\u00020\u0004\u0012\u0004\u0012\u00020\u00010\u0003H\u00a7@\u00a2\u0006\u0002\u0010\bJ0\u0010\u0016\u001a\u000e\u0012\u0004\u0012\u00020\u0004\u0012\u0004\u0012\u00020\u00010\u00032\u0014\b\u0001\u0010\u0017\u001a\u000e\u0012\u0004\u0012\u00020\u0004\u0012\u0004\u0012\u00020\u00010\u0003H\u00a7@\u00a2\u0006\u0002\u0010\u0006J0\u0010\u0018\u001a\u000e\u0012\u0004\u0012\u00020\u0004\u0012\u0004\u0012\u00020\u00010\u00032\u0014\b\u0001\u0010\u0017\u001a\u000e\u0012\u0004\u0012\u00020\u0004\u0012\u0004\u0012\u00020\u00010\u0003H\u00a7@\u00a2\u0006\u0002\u0010\u0006J0\u0010\u0019\u001a\u000e\u0012\u0004\u0012\u00020\u0004\u0012\u0004\u0012\u00020\u00010\u00032\u0014\b\u0001\u0010\u0017\u001a\u000e\u0012\u0004\u0012\u00020\u0004\u0012\u0004\u0012\u00020\u00010\u0003H\u00a7@\u00a2\u0006\u0002\u0010\u0006\u00a8\u0006\u001a"}, d2 = {"Lapp/sunportal/api/ApiService;", "", "connectWebSocket", "", "", "connectionParams", "(Ljava/util/Map;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getBlackoutData", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getForecastData", "getHistoryData", "period", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getInverterData", "Lapp/sunportal/data/model/InverterData;", "getInverterLiveData", "getMonthlyHistory", "getSystemStatus", "getTouData", "getWeatherData", "Lapp/sunportal/data/model/WeatherData;", "getWeeklyHistory", "updateBatterySettings", "settings", "updateInverterSettings", "updateTariffSettings", "app_debug"})
public abstract interface ApiService {
    
    @retrofit2.http.GET(value = "api/live/raw")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getInverterData(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super app.sunportal.data.model.InverterData> $completion);
    
    @retrofit2.http.GET(value = "api/live")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getInverterLiveData(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super app.sunportal.data.model.InverterData> $completion);
    
    @retrofit2.http.GET(value = "api/?action=history&period=day")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getHistoryData(@retrofit2.http.Query(value = "period")
    @org.jetbrains.annotations.NotNull()
    java.lang.String period, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.Map<java.lang.String, ? extends java.lang.Object>> $completion);
    
    @retrofit2.http.GET(value = "api/?action=history&period=week")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getWeeklyHistory(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.Map<java.lang.String, ? extends java.lang.Object>> $completion);
    
    @retrofit2.http.GET(value = "api/?action=history&period=month")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getMonthlyHistory(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.Map<java.lang.String, ? extends java.lang.Object>> $completion);
    
    @retrofit2.http.GET(value = "api/?action=blackout")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getBlackoutData(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.Map<java.lang.String, ? extends java.lang.Object>> $completion);
    
    @retrofit2.http.GET(value = "weather_read.php")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getWeatherData(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super app.sunportal.data.model.WeatherData> $completion);
    
    @retrofit2.http.GET(value = "forecast_read.php")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getForecastData(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.Map<java.lang.String, ? extends java.lang.Object>> $completion);
    
    @retrofit2.http.GET(value = "tou_read.php")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getTouData(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.Map<java.lang.String, ? extends java.lang.Object>> $completion);
    
    @retrofit2.http.GET(value = "api/status")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getSystemStatus(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.Map<java.lang.String, ? extends java.lang.Object>> $completion);
    
    @retrofit2.http.POST(value = "api/ws/connect")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object connectWebSocket(@retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    java.util.Map<java.lang.String, java.lang.String> connectionParams, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.Map<java.lang.String, ? extends java.lang.Object>> $completion);
    
    @retrofit2.http.POST(value = "api/inverter/settings")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object updateInverterSettings(@retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    java.util.Map<java.lang.String, ? extends java.lang.Object> settings, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.Map<java.lang.String, ? extends java.lang.Object>> $completion);
    
    @retrofit2.http.POST(value = "api/battery/settings")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object updateBatterySettings(@retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    java.util.Map<java.lang.String, ? extends java.lang.Object> settings, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.Map<java.lang.String, ? extends java.lang.Object>> $completion);
    
    @retrofit2.http.POST(value = "api/tariffs/settings")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object updateTariffSettings(@retrofit2.http.Body()
    @org.jetbrains.annotations.NotNull()
    java.util.Map<java.lang.String, ? extends java.lang.Object> settings, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.Map<java.lang.String, ? extends java.lang.Object>> $completion);
    
    /**
     * API сервис для SunPortal приложения
     */
    @kotlin.Metadata(mv = {1, 9, 0}, k = 3, xi = 48)
    public static final class DefaultImpls {
    }
}