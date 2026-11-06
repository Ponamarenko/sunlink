package app.sunportal.data.model;

/**
 * Модель данных Open-Meteo API
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000F\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u0006\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0019\n\u0002\u0010\u000b\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0004\b\u0086\b\u0018\u00002\u00020\u0001:\u0003012B\u0007\b\u0016\u00a2\u0006\u0002\u0010\u0002BK\u0012\u0006\u0010\u0003\u001a\u00020\u0004\u0012\u0006\u0010\u0005\u001a\u00020\u0004\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\u0007\u0012\u0006\u0010\t\u001a\u00020\n\u0012\b\u0010\u000b\u001a\u0004\u0018\u00010\f\u0012\b\u0010\r\u001a\u0004\u0018\u00010\u000e\u0012\b\u0010\u000f\u001a\u0004\u0018\u00010\u0010\u00a2\u0006\u0002\u0010\u0011J\t\u0010 \u001a\u00020\u0004H\u00c6\u0003J\t\u0010!\u001a\u00020\u0004H\u00c6\u0003J\t\u0010\"\u001a\u00020\u0007H\u00c6\u0003J\t\u0010#\u001a\u00020\u0007H\u00c6\u0003J\t\u0010$\u001a\u00020\nH\u00c6\u0003J\u000b\u0010%\u001a\u0004\u0018\u00010\fH\u00c6\u0003J\u000b\u0010&\u001a\u0004\u0018\u00010\u000eH\u00c6\u0003J\u000b\u0010\'\u001a\u0004\u0018\u00010\u0010H\u00c6\u0003J_\u0010(\u001a\u00020\u00002\b\b\u0002\u0010\u0003\u001a\u00020\u00042\b\b\u0002\u0010\u0005\u001a\u00020\u00042\b\b\u0002\u0010\u0006\u001a\u00020\u00072\b\b\u0002\u0010\b\u001a\u00020\u00072\b\b\u0002\u0010\t\u001a\u00020\n2\n\b\u0002\u0010\u000b\u001a\u0004\u0018\u00010\f2\n\b\u0002\u0010\r\u001a\u0004\u0018\u00010\u000e2\n\b\u0002\u0010\u000f\u001a\u0004\u0018\u00010\u0010H\u00c6\u0001J\u0013\u0010)\u001a\u00020*2\b\u0010+\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010,\u001a\u00020\nH\u00d6\u0001J\t\u0010-\u001a\u00020\u0007H\u00d6\u0001J\u0006\u0010.\u001a\u00020/R\u0018\u0010\u000b\u001a\u0004\u0018\u00010\f8\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010\u0013R\u0018\u0010\u000f\u001a\u0004\u0018\u00010\u00108\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0014\u0010\u0015R\u0018\u0010\r\u001a\u0004\u0018\u00010\u000e8\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0016\u0010\u0017R\u0016\u0010\u0003\u001a\u00020\u00048\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0018\u0010\u0019R\u0016\u0010\u0005\u001a\u00020\u00048\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001a\u0010\u0019R\u0016\u0010\u0006\u001a\u00020\u00078\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001b\u0010\u001cR\u0016\u0010\b\u001a\u00020\u00078\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001d\u0010\u001cR\u0016\u0010\t\u001a\u00020\n8\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001e\u0010\u001f\u00a8\u00063"}, d2 = {"Lapp/sunportal/data/model/OpenMeteoData;", "", "()V", "latitude", "", "longitude", "timezone", "", "timezoneAbbreviation", "utcOffsetSeconds", "", "currentWeather", "Lapp/sunportal/data/model/OpenMeteoData$CurrentWeather;", "hourly", "Lapp/sunportal/data/model/OpenMeteoData$Hourly;", "daily", "Lapp/sunportal/data/model/OpenMeteoData$Daily;", "(DDLjava/lang/String;Ljava/lang/String;ILapp/sunportal/data/model/OpenMeteoData$CurrentWeather;Lapp/sunportal/data/model/OpenMeteoData$Hourly;Lapp/sunportal/data/model/OpenMeteoData$Daily;)V", "getCurrentWeather", "()Lapp/sunportal/data/model/OpenMeteoData$CurrentWeather;", "getDaily", "()Lapp/sunportal/data/model/OpenMeteoData$Daily;", "getHourly", "()Lapp/sunportal/data/model/OpenMeteoData$Hourly;", "getLatitude", "()D", "getLongitude", "getTimezone", "()Ljava/lang/String;", "getTimezoneAbbreviation", "getUtcOffsetSeconds", "()I", "component1", "component2", "component3", "component4", "component5", "component6", "component7", "component8", "copy", "equals", "", "other", "hashCode", "toString", "toWeatherData", "Lapp/sunportal/data/model/WeatherData;", "CurrentWeather", "Daily", "Hourly", "app_debug"})
public final class OpenMeteoData {
    @com.google.gson.annotations.SerializedName(value = "latitude")
    private final double latitude = 0.0;
    @com.google.gson.annotations.SerializedName(value = "longitude")
    private final double longitude = 0.0;
    @com.google.gson.annotations.SerializedName(value = "timezone")
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String timezone = null;
    @com.google.gson.annotations.SerializedName(value = "timezone_abbreviation")
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String timezoneAbbreviation = null;
    @com.google.gson.annotations.SerializedName(value = "utc_offset_seconds")
    private final int utcOffsetSeconds = 0;
    @com.google.gson.annotations.SerializedName(value = "current_weather")
    @org.jetbrains.annotations.Nullable()
    private final app.sunportal.data.model.OpenMeteoData.CurrentWeather currentWeather = null;
    @com.google.gson.annotations.SerializedName(value = "hourly")
    @org.jetbrains.annotations.Nullable()
    private final app.sunportal.data.model.OpenMeteoData.Hourly hourly = null;
    @com.google.gson.annotations.SerializedName(value = "daily")
    @org.jetbrains.annotations.Nullable()
    private final app.sunportal.data.model.OpenMeteoData.Daily daily = null;
    
    public OpenMeteoData(double latitude, double longitude, @org.jetbrains.annotations.NotNull()
    java.lang.String timezone, @org.jetbrains.annotations.NotNull()
    java.lang.String timezoneAbbreviation, int utcOffsetSeconds, @org.jetbrains.annotations.Nullable()
    app.sunportal.data.model.OpenMeteoData.CurrentWeather currentWeather, @org.jetbrains.annotations.Nullable()
    app.sunportal.data.model.OpenMeteoData.Hourly hourly, @org.jetbrains.annotations.Nullable()
    app.sunportal.data.model.OpenMeteoData.Daily daily) {
        super();
    }
    
    public final double getLatitude() {
        return 0.0;
    }
    
    public final double getLongitude() {
        return 0.0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getTimezone() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getTimezoneAbbreviation() {
        return null;
    }
    
    public final int getUtcOffsetSeconds() {
        return 0;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final app.sunportal.data.model.OpenMeteoData.CurrentWeather getCurrentWeather() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final app.sunportal.data.model.OpenMeteoData.Hourly getHourly() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final app.sunportal.data.model.OpenMeteoData.Daily getDaily() {
        return null;
    }
    
    public OpenMeteoData() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final app.sunportal.data.model.WeatherData toWeatherData() {
        return null;
    }
    
    public final double component1() {
        return 0.0;
    }
    
    public final double component2() {
        return 0.0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component3() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component4() {
        return null;
    }
    
    public final int component5() {
        return 0;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final app.sunportal.data.model.OpenMeteoData.CurrentWeather component6() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final app.sunportal.data.model.OpenMeteoData.Hourly component7() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final app.sunportal.data.model.OpenMeteoData.Daily component8() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final app.sunportal.data.model.OpenMeteoData copy(double latitude, double longitude, @org.jetbrains.annotations.NotNull()
    java.lang.String timezone, @org.jetbrains.annotations.NotNull()
    java.lang.String timezoneAbbreviation, int utcOffsetSeconds, @org.jetbrains.annotations.Nullable()
    app.sunportal.data.model.OpenMeteoData.CurrentWeather currentWeather, @org.jetbrains.annotations.Nullable()
    app.sunportal.data.model.OpenMeteoData.Hourly hourly, @org.jetbrains.annotations.Nullable()
    app.sunportal.data.model.OpenMeteoData.Daily daily) {
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
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0006\n\u0002\b\u0003\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0010\n\u0002\u0010\u000b\n\u0002\b\u0004\b\u0086\b\u0018\u00002\u00020\u0001B-\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0003\u0012\u0006\u0010\u0005\u001a\u00020\u0003\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\t\u00a2\u0006\u0002\u0010\nJ\t\u0010\u0013\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0014\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0015\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0016\u001a\u00020\u0007H\u00c6\u0003J\t\u0010\u0017\u001a\u00020\tH\u00c6\u0003J;\u0010\u0018\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00032\b\b\u0002\u0010\u0005\u001a\u00020\u00032\b\b\u0002\u0010\u0006\u001a\u00020\u00072\b\b\u0002\u0010\b\u001a\u00020\tH\u00c6\u0001J\u0013\u0010\u0019\u001a\u00020\u001a2\b\u0010\u001b\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\u001c\u001a\u00020\u0007H\u00d6\u0001J\t\u0010\u001d\u001a\u00020\tH\u00d6\u0001R\u0016\u0010\u0002\u001a\u00020\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\fR\u0016\u0010\b\u001a\u00020\t8\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\u000eR\u0016\u0010\u0006\u001a\u00020\u00078\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\u0010R\u0016\u0010\u0005\u001a\u00020\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\fR\u0016\u0010\u0004\u001a\u00020\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010\f\u00a8\u0006\u001e"}, d2 = {"Lapp/sunportal/data/model/OpenMeteoData$CurrentWeather;", "", "temperature", "", "windSpeed", "windDirection", "weatherCode", "", "time", "", "(DDDILjava/lang/String;)V", "getTemperature", "()D", "getTime", "()Ljava/lang/String;", "getWeatherCode", "()I", "getWindDirection", "getWindSpeed", "component1", "component2", "component3", "component4", "component5", "copy", "equals", "", "other", "hashCode", "toString", "app_debug"})
    public static final class CurrentWeather {
        @com.google.gson.annotations.SerializedName(value = "temperature")
        private final double temperature = 0.0;
        @com.google.gson.annotations.SerializedName(value = "windspeed")
        private final double windSpeed = 0.0;
        @com.google.gson.annotations.SerializedName(value = "winddirection")
        private final double windDirection = 0.0;
        @com.google.gson.annotations.SerializedName(value = "weathercode")
        private final int weatherCode = 0;
        @com.google.gson.annotations.SerializedName(value = "time")
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String time = null;
        
        public CurrentWeather(double temperature, double windSpeed, double windDirection, int weatherCode, @org.jetbrains.annotations.NotNull()
        java.lang.String time) {
            super();
        }
        
        public final double getTemperature() {
            return 0.0;
        }
        
        public final double getWindSpeed() {
            return 0.0;
        }
        
        public final double getWindDirection() {
            return 0.0;
        }
        
        public final int getWeatherCode() {
            return 0;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getTime() {
            return null;
        }
        
        public final double component1() {
            return 0.0;
        }
        
        public final double component2() {
            return 0.0;
        }
        
        public final double component3() {
            return 0.0;
        }
        
        public final int component4() {
            return 0;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component5() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final app.sunportal.data.model.OpenMeteoData.CurrentWeather copy(double temperature, double windSpeed, double windDirection, int weatherCode, @org.jetbrains.annotations.NotNull()
        java.lang.String time) {
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
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010 \n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u0006\n\u0002\b\r\n\u0002\u0010\u000b\n\u0002\b\u0004\b\u0086\b\u0018\u00002\u00020\u0001B=\u0012\f\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003\u0012\f\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00060\u0003\u0012\f\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\b0\u0003\u0012\f\u0010\t\u001a\b\u0012\u0004\u0012\u00020\b0\u0003\u00a2\u0006\u0002\u0010\nJ\u000f\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003H\u00c6\u0003J\u000f\u0010\u0011\u001a\b\u0012\u0004\u0012\u00020\u00060\u0003H\u00c6\u0003J\u000f\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\b0\u0003H\u00c6\u0003J\u000f\u0010\u0013\u001a\b\u0012\u0004\u0012\u00020\b0\u0003H\u00c6\u0003JI\u0010\u0014\u001a\u00020\u00002\u000e\b\u0002\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u00032\u000e\b\u0002\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00060\u00032\u000e\b\u0002\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\b0\u00032\u000e\b\u0002\u0010\t\u001a\b\u0012\u0004\u0012\u00020\b0\u0003H\u00c6\u0001J\u0013\u0010\u0015\u001a\u00020\u00162\b\u0010\u0017\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\u0018\u001a\u00020\u0006H\u00d6\u0001J\t\u0010\u0019\u001a\u00020\u0004H\u00d6\u0001R\u001c\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\b0\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\fR\u001c\u0010\t\u001a\b\u0012\u0004\u0012\u00020\b0\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\fR\u001c\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\fR\u001c\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00060\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\f\u00a8\u0006\u001a"}, d2 = {"Lapp/sunportal/data/model/OpenMeteoData$Daily;", "", "time", "", "", "weatherCode", "", "temperature2mMax", "", "temperature2mMin", "(Ljava/util/List;Ljava/util/List;Ljava/util/List;Ljava/util/List;)V", "getTemperature2mMax", "()Ljava/util/List;", "getTemperature2mMin", "getTime", "getWeatherCode", "component1", "component2", "component3", "component4", "copy", "equals", "", "other", "hashCode", "toString", "app_debug"})
    public static final class Daily {
        @com.google.gson.annotations.SerializedName(value = "time")
        @org.jetbrains.annotations.NotNull()
        private final java.util.List<java.lang.String> time = null;
        @com.google.gson.annotations.SerializedName(value = "weathercode")
        @org.jetbrains.annotations.NotNull()
        private final java.util.List<java.lang.Integer> weatherCode = null;
        @com.google.gson.annotations.SerializedName(value = "temperature_2m_max")
        @org.jetbrains.annotations.NotNull()
        private final java.util.List<java.lang.Double> temperature2mMax = null;
        @com.google.gson.annotations.SerializedName(value = "temperature_2m_min")
        @org.jetbrains.annotations.NotNull()
        private final java.util.List<java.lang.Double> temperature2mMin = null;
        
        public Daily(@org.jetbrains.annotations.NotNull()
        java.util.List<java.lang.String> time, @org.jetbrains.annotations.NotNull()
        java.util.List<java.lang.Integer> weatherCode, @org.jetbrains.annotations.NotNull()
        java.util.List<java.lang.Double> temperature2mMax, @org.jetbrains.annotations.NotNull()
        java.util.List<java.lang.Double> temperature2mMin) {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.List<java.lang.String> getTime() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.List<java.lang.Integer> getWeatherCode() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.List<java.lang.Double> getTemperature2mMax() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.List<java.lang.Double> getTemperature2mMin() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.List<java.lang.String> component1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.List<java.lang.Integer> component2() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.List<java.lang.Double> component3() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.List<java.lang.Double> component4() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final app.sunportal.data.model.OpenMeteoData.Daily copy(@org.jetbrains.annotations.NotNull()
        java.util.List<java.lang.String> time, @org.jetbrains.annotations.NotNull()
        java.util.List<java.lang.Integer> weatherCode, @org.jetbrains.annotations.NotNull()
        java.util.List<java.lang.Double> temperature2mMax, @org.jetbrains.annotations.NotNull()
        java.util.List<java.lang.Double> temperature2mMin) {
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
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000,\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010 \n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u0006\n\u0002\b\u0003\n\u0002\u0010\b\n\u0002\b\u000e\n\u0002\u0010\u000b\n\u0002\b\u0004\b\u0086\b\u0018\u00002\u00020\u0001BK\u0012\f\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003\u0012\f\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00060\u0003\u0012\f\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\u00060\u0003\u0012\f\u0010\b\u001a\b\u0012\u0004\u0012\u00020\u00060\u0003\u0012\f\u0010\t\u001a\b\u0012\u0004\u0012\u00020\n0\u0003\u00a2\u0006\u0002\u0010\u000bJ\u000f\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003H\u00c6\u0003J\u000f\u0010\u0013\u001a\b\u0012\u0004\u0012\u00020\u00060\u0003H\u00c6\u0003J\u000f\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u00060\u0003H\u00c6\u0003J\u000f\u0010\u0015\u001a\b\u0012\u0004\u0012\u00020\u00060\u0003H\u00c6\u0003J\u000f\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\n0\u0003H\u00c6\u0003JY\u0010\u0017\u001a\u00020\u00002\u000e\b\u0002\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u00032\u000e\b\u0002\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00060\u00032\u000e\b\u0002\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\u00060\u00032\u000e\b\u0002\u0010\b\u001a\b\u0012\u0004\u0012\u00020\u00060\u00032\u000e\b\u0002\u0010\t\u001a\b\u0012\u0004\u0012\u00020\n0\u0003H\u00c6\u0001J\u0013\u0010\u0018\u001a\u00020\u00192\b\u0010\u001a\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\u001b\u001a\u00020\nH\u00d6\u0001J\t\u0010\u001c\u001a\u00020\u0004H\u00d6\u0001R\u001c\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\u00060\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\rR\u001c\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00060\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\rR\u001c\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\rR\u001c\u0010\t\u001a\b\u0012\u0004\u0012\u00020\n0\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\rR\u001c\u0010\b\u001a\b\u0012\u0004\u0012\u00020\u00060\u00038\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\r\u00a8\u0006\u001d"}, d2 = {"Lapp/sunportal/data/model/OpenMeteoData$Hourly;", "", "time", "", "", "temperature2m", "", "relativeHumidity2m", "windSpeed10m", "weatherCode", "", "(Ljava/util/List;Ljava/util/List;Ljava/util/List;Ljava/util/List;Ljava/util/List;)V", "getRelativeHumidity2m", "()Ljava/util/List;", "getTemperature2m", "getTime", "getWeatherCode", "getWindSpeed10m", "component1", "component2", "component3", "component4", "component5", "copy", "equals", "", "other", "hashCode", "toString", "app_debug"})
    public static final class Hourly {
        @com.google.gson.annotations.SerializedName(value = "time")
        @org.jetbrains.annotations.NotNull()
        private final java.util.List<java.lang.String> time = null;
        @com.google.gson.annotations.SerializedName(value = "temperature_2m")
        @org.jetbrains.annotations.NotNull()
        private final java.util.List<java.lang.Double> temperature2m = null;
        @com.google.gson.annotations.SerializedName(value = "relativehumidity_2m")
        @org.jetbrains.annotations.NotNull()
        private final java.util.List<java.lang.Double> relativeHumidity2m = null;
        @com.google.gson.annotations.SerializedName(value = "windspeed_10m")
        @org.jetbrains.annotations.NotNull()
        private final java.util.List<java.lang.Double> windSpeed10m = null;
        @com.google.gson.annotations.SerializedName(value = "weathercode")
        @org.jetbrains.annotations.NotNull()
        private final java.util.List<java.lang.Integer> weatherCode = null;
        
        public Hourly(@org.jetbrains.annotations.NotNull()
        java.util.List<java.lang.String> time, @org.jetbrains.annotations.NotNull()
        java.util.List<java.lang.Double> temperature2m, @org.jetbrains.annotations.NotNull()
        java.util.List<java.lang.Double> relativeHumidity2m, @org.jetbrains.annotations.NotNull()
        java.util.List<java.lang.Double> windSpeed10m, @org.jetbrains.annotations.NotNull()
        java.util.List<java.lang.Integer> weatherCode) {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.List<java.lang.String> getTime() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.List<java.lang.Double> getTemperature2m() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.List<java.lang.Double> getRelativeHumidity2m() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.List<java.lang.Double> getWindSpeed10m() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.List<java.lang.Integer> getWeatherCode() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.List<java.lang.String> component1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.List<java.lang.Double> component2() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.List<java.lang.Double> component3() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.List<java.lang.Double> component4() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.List<java.lang.Integer> component5() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final app.sunportal.data.model.OpenMeteoData.Hourly copy(@org.jetbrains.annotations.NotNull()
        java.util.List<java.lang.String> time, @org.jetbrains.annotations.NotNull()
        java.util.List<java.lang.Double> temperature2m, @org.jetbrains.annotations.NotNull()
        java.util.List<java.lang.Double> relativeHumidity2m, @org.jetbrains.annotations.NotNull()
        java.util.List<java.lang.Double> windSpeed10m, @org.jetbrains.annotations.NotNull()
        java.util.List<java.lang.Integer> weatherCode) {
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
}