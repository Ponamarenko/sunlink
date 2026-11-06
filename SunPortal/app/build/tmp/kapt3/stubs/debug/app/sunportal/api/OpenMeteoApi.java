package app.sunportal.api;

/**
 * API сервис для Open-Meteo
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0006\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0006\bf\u0018\u00002\u00020\u0001J6\u0010\u0002\u001a\u00020\u00032\b\b\u0001\u0010\u0004\u001a\u00020\u00052\b\b\u0001\u0010\u0006\u001a\u00020\u00052\b\b\u0003\u0010\u0007\u001a\u00020\b2\b\b\u0003\u0010\t\u001a\u00020\nH\u00a7@\u00a2\u0006\u0002\u0010\u000bJJ\u0010\f\u001a\u00020\u00032\b\b\u0001\u0010\u0004\u001a\u00020\u00052\b\b\u0001\u0010\u0006\u001a\u00020\u00052\b\b\u0003\u0010\r\u001a\u00020\n2\b\b\u0003\u0010\u000e\u001a\u00020\n2\b\b\u0003\u0010\u0007\u001a\u00020\b2\b\b\u0003\u0010\t\u001a\u00020\nH\u00a7@\u00a2\u0006\u0002\u0010\u000f\u00a8\u0006\u0010"}, d2 = {"Lapp/sunportal/api/OpenMeteoApi;", "", "getCurrentWeather", "Lapp/sunportal/data/model/OpenMeteoData;", "latitude", "", "longitude", "currentWeather", "", "timezone", "", "(DDZLjava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getWeatherForecast", "hourly", "daily", "(DDLjava/lang/String;Ljava/lang/String;ZLjava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
public abstract interface OpenMeteoApi {
    
    @retrofit2.http.GET(value = "v1/forecast")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getWeatherForecast(@retrofit2.http.Query(value = "latitude")
    double latitude, @retrofit2.http.Query(value = "longitude")
    double longitude, @retrofit2.http.Query(value = "hourly")
    @org.jetbrains.annotations.NotNull()
    java.lang.String hourly, @retrofit2.http.Query(value = "daily")
    @org.jetbrains.annotations.NotNull()
    java.lang.String daily, @retrofit2.http.Query(value = "current_weather")
    boolean currentWeather, @retrofit2.http.Query(value = "timezone")
    @org.jetbrains.annotations.NotNull()
    java.lang.String timezone, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super app.sunportal.data.model.OpenMeteoData> $completion);
    
    @retrofit2.http.GET(value = "v1/forecast")
    @org.jetbrains.annotations.Nullable()
    public abstract java.lang.Object getCurrentWeather(@retrofit2.http.Query(value = "latitude")
    double latitude, @retrofit2.http.Query(value = "longitude")
    double longitude, @retrofit2.http.Query(value = "current_weather")
    boolean currentWeather, @retrofit2.http.Query(value = "timezone")
    @org.jetbrains.annotations.NotNull()
    java.lang.String timezone, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super app.sunportal.data.model.OpenMeteoData> $completion);
    
    /**
     * API сервис для Open-Meteo
     */
    @kotlin.Metadata(mv = {1, 9, 0}, k = 3, xi = 48)
    public static final class DefaultImpls {
    }
}