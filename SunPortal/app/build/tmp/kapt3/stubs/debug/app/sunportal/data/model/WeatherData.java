package app.sunportal.data.model;

/**
 * Модель данных погоды
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00002\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\t\n\u0000\n\u0002\u0010\u0006\n\u0002\b\b\n\u0002\u0010\u000e\n\u0002\b/\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B\u0007\b\u0016\u00a2\u0006\u0002\u0010\u0002B]\u0012\u0006\u0010\u0003\u001a\u00020\u0004\u0012\u0006\u0010\u0005\u001a\u00020\u0006\u0012\u0006\u0010\u0007\u001a\u00020\u0006\u0012\u0006\u0010\b\u001a\u00020\u0006\u0012\u0006\u0010\t\u001a\u00020\u0006\u0012\u0006\u0010\n\u001a\u00020\u0006\u0012\u0006\u0010\u000b\u001a\u00020\u0006\u0012\u0006\u0010\f\u001a\u00020\u0006\u0012\u0006\u0010\r\u001a\u00020\u0006\u0012\u0006\u0010\u000e\u001a\u00020\u000f\u0012\u0006\u0010\u0010\u001a\u00020\u000f\u00a2\u0006\u0002\u0010\u0011J\t\u00102\u001a\u00020\u0004H\u00c6\u0003J\t\u00103\u001a\u00020\u000fH\u00c6\u0003J\t\u00104\u001a\u00020\u000fH\u00c6\u0003J\t\u00105\u001a\u00020\u0006H\u00c6\u0003J\t\u00106\u001a\u00020\u0006H\u00c6\u0003J\t\u00107\u001a\u00020\u0006H\u00c6\u0003J\t\u00108\u001a\u00020\u0006H\u00c6\u0003J\t\u00109\u001a\u00020\u0006H\u00c6\u0003J\t\u0010:\u001a\u00020\u0006H\u00c6\u0003J\t\u0010;\u001a\u00020\u0006H\u00c6\u0003J\t\u0010<\u001a\u00020\u0006H\u00c6\u0003Jw\u0010=\u001a\u00020\u00002\b\b\u0002\u0010\u0003\u001a\u00020\u00042\b\b\u0002\u0010\u0005\u001a\u00020\u00062\b\b\u0002\u0010\u0007\u001a\u00020\u00062\b\b\u0002\u0010\b\u001a\u00020\u00062\b\b\u0002\u0010\t\u001a\u00020\u00062\b\b\u0002\u0010\n\u001a\u00020\u00062\b\b\u0002\u0010\u000b\u001a\u00020\u00062\b\b\u0002\u0010\f\u001a\u00020\u00062\b\b\u0002\u0010\r\u001a\u00020\u00062\b\b\u0002\u0010\u000e\u001a\u00020\u000f2\b\b\u0002\u0010\u0010\u001a\u00020\u000fH\u00c6\u0001J\u0013\u0010>\u001a\u00020?2\b\u0010@\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010A\u001a\u00020BH\u00d6\u0001J\t\u0010C\u001a\u00020\u000fH\u00d6\u0001R\u0016\u0010\u000b\u001a\u00020\u00068\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010\u0013R\u0011\u0010\u0014\u001a\u00020\u000f8F\u00a2\u0006\u0006\u001a\u0004\b\u0015\u0010\u0016R\u0011\u0010\u0017\u001a\u00020\u000f8F\u00a2\u0006\u0006\u001a\u0004\b\u0018\u0010\u0016R\u0011\u0010\u0019\u001a\u00020\u000f8F\u00a2\u0006\u0006\u001a\u0004\b\u001a\u0010\u0016R\u0011\u0010\u001b\u001a\u00020\u000f8F\u00a2\u0006\u0006\u001a\u0004\b\u001c\u0010\u0016R\u0011\u0010\u001d\u001a\u00020\u000f8F\u00a2\u0006\u0006\u001a\u0004\b\u001e\u0010\u0016R\u0011\u0010\u001f\u001a\u00020\u000f8F\u00a2\u0006\u0006\u001a\u0004\b \u0010\u0016R\u0011\u0010!\u001a\u00020\u000f8F\u00a2\u0006\u0006\u001a\u0004\b\"\u0010\u0016R\u0016\u0010\u0007\u001a\u00020\u00068\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b#\u0010\u0013R\u0016\u0010\u000e\u001a\u00020\u000f8\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b$\u0010\u0016R\u0016\u0010\b\u001a\u00020\u00068\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b%\u0010\u0013R\u0016\u0010\u0010\u001a\u00020\u000f8\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b&\u0010\u0016R\u0016\u0010\u0005\u001a\u00020\u00068\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\'\u0010\u0013R\u0016\u0010\u0003\u001a\u00020\u00048\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b(\u0010)R\u0016\u0010\r\u001a\u00020\u00068\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b*\u0010\u0013R\u0016\u0010\f\u001a\u00020\u00068\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b+\u0010\u0013R\u0011\u0010,\u001a\u00020\u000f8F\u00a2\u0006\u0006\u001a\u0004\b-\u0010\u0016R\u0011\u0010.\u001a\u00020\u000f8F\u00a2\u0006\u0006\u001a\u0004\b/\u0010\u0016R\u0016\u0010\n\u001a\u00020\u00068\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b0\u0010\u0013R\u0016\u0010\t\u001a\u00020\u00068\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b1\u0010\u0013\u00a8\u0006D"}, d2 = {"Lapp/sunportal/data/model/WeatherData;", "", "()V", "timestamp", "", "temperature", "", "humidity", "pressure", "windSpeed", "windDirection", "cloudiness", "visibility", "uvIndex", "location", "", "source", "(JDDDDDDDDLjava/lang/String;Ljava/lang/String;)V", "getCloudiness", "()D", "formattedCloudiness", "getFormattedCloudiness", "()Ljava/lang/String;", "formattedHumidity", "getFormattedHumidity", "formattedPressure", "getFormattedPressure", "formattedTemperature", "getFormattedTemperature", "formattedUvIndex", "getFormattedUvIndex", "formattedVisibility", "getFormattedVisibility", "formattedWindSpeed", "getFormattedWindSpeed", "getHumidity", "getLocation", "getPressure", "getSource", "getTemperature", "getTimestamp", "()J", "getUvIndex", "getVisibility", "weatherDescription", "getWeatherDescription", "weatherIcon", "getWeatherIcon", "getWindDirection", "getWindSpeed", "component1", "component10", "component11", "component2", "component3", "component4", "component5", "component6", "component7", "component8", "component9", "copy", "equals", "", "other", "hashCode", "", "toString", "app_debug"})
public final class WeatherData {
    @com.google.gson.annotations.SerializedName(value = "timestamp")
    private final long timestamp = 0L;
    @com.google.gson.annotations.SerializedName(value = "temperature")
    private final double temperature = 0.0;
    @com.google.gson.annotations.SerializedName(value = "humidity")
    private final double humidity = 0.0;
    @com.google.gson.annotations.SerializedName(value = "pressure")
    private final double pressure = 0.0;
    @com.google.gson.annotations.SerializedName(value = "wind_speed")
    private final double windSpeed = 0.0;
    @com.google.gson.annotations.SerializedName(value = "wind_direction")
    private final double windDirection = 0.0;
    @com.google.gson.annotations.SerializedName(value = "cloudiness")
    private final double cloudiness = 0.0;
    @com.google.gson.annotations.SerializedName(value = "visibility")
    private final double visibility = 0.0;
    @com.google.gson.annotations.SerializedName(value = "uv_index")
    private final double uvIndex = 0.0;
    @com.google.gson.annotations.SerializedName(value = "location")
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String location = null;
    @com.google.gson.annotations.SerializedName(value = "source")
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String source = null;
    
    public WeatherData(long timestamp, double temperature, double humidity, double pressure, double windSpeed, double windDirection, double cloudiness, double visibility, double uvIndex, @org.jetbrains.annotations.NotNull()
    java.lang.String location, @org.jetbrains.annotations.NotNull()
    java.lang.String source) {
        super();
    }
    
    public final long getTimestamp() {
        return 0L;
    }
    
    public final double getTemperature() {
        return 0.0;
    }
    
    public final double getHumidity() {
        return 0.0;
    }
    
    public final double getPressure() {
        return 0.0;
    }
    
    public final double getWindSpeed() {
        return 0.0;
    }
    
    public final double getWindDirection() {
        return 0.0;
    }
    
    public final double getCloudiness() {
        return 0.0;
    }
    
    public final double getVisibility() {
        return 0.0;
    }
    
    public final double getUvIndex() {
        return 0.0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getLocation() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getSource() {
        return null;
    }
    
    public WeatherData() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getFormattedTemperature() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getFormattedHumidity() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getFormattedPressure() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getFormattedWindSpeed() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getFormattedCloudiness() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getFormattedVisibility() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getFormattedUvIndex() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getWeatherDescription() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getWeatherIcon() {
        return null;
    }
    
    public final long component1() {
        return 0L;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component10() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component11() {
        return null;
    }
    
    public final double component2() {
        return 0.0;
    }
    
    public final double component3() {
        return 0.0;
    }
    
    public final double component4() {
        return 0.0;
    }
    
    public final double component5() {
        return 0.0;
    }
    
    public final double component6() {
        return 0.0;
    }
    
    public final double component7() {
        return 0.0;
    }
    
    public final double component8() {
        return 0.0;
    }
    
    public final double component9() {
        return 0.0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final app.sunportal.data.model.WeatherData copy(long timestamp, double temperature, double humidity, double pressure, double windSpeed, double windDirection, double cloudiness, double visibility, double uvIndex, @org.jetbrains.annotations.NotNull()
    java.lang.String location, @org.jetbrains.annotations.NotNull()
    java.lang.String source) {
        return null;
    }
    
    @java.lang.Override()
    public boolean equals(@org.jetbrains.annotations.Nullable()
    java.lang.Object other) {
        return false;
    }
    
    @java.lang.Override()
    public int hashCode() {
        return 0;
    }
    
    @java.lang.Override()
    @org.jetbrains.annotations.NotNull()
    public java.lang.String toString() {
        return null;
    }
}