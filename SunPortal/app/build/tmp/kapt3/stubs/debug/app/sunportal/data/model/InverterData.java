package app.sunportal.data.model;

/**
 * Модель данных инвертора Deye
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00000\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\t\n\u0000\n\u0002\u0010\u0006\n\u0002\b\u0013\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\b\n\u0002\b\'\n\u0002\u0010\u000b\n\u0002\b2\b\u0086\b\u0018\u00002\u00020\u0001B\u0007\b\u0016\u00a2\u0006\u0002\u0010\u0002B\u00bf\u0001\u0012\u0006\u0010\u0003\u001a\u00020\u0004\u0012\u0006\u0010\u0005\u001a\u00020\u0006\u0012\u0006\u0010\u0007\u001a\u00020\u0006\u0012\u0006\u0010\b\u001a\u00020\u0006\u0012\u0006\u0010\t\u001a\u00020\u0006\u0012\u0006\u0010\n\u001a\u00020\u0006\u0012\u0006\u0010\u000b\u001a\u00020\u0006\u0012\u0006\u0010\f\u001a\u00020\u0006\u0012\u0006\u0010\r\u001a\u00020\u0006\u0012\u0006\u0010\u000e\u001a\u00020\u0006\u0012\u0006\u0010\u000f\u001a\u00020\u0006\u0012\u0006\u0010\u0010\u001a\u00020\u0006\u0012\u0006\u0010\u0011\u001a\u00020\u0006\u0012\u0006\u0010\u0012\u001a\u00020\u0006\u0012\u0006\u0010\u0013\u001a\u00020\u0006\u0012\u0006\u0010\u0014\u001a\u00020\u0006\u0012\u0006\u0010\u0015\u001a\u00020\u0006\u0012\u0006\u0010\u0016\u001a\u00020\u0006\u0012\u0006\u0010\u0017\u001a\u00020\u0006\u0012\u0006\u0010\u0018\u001a\u00020\u0006\u0012\u0006\u0010\u0019\u001a\u00020\u001a\u0012\u0006\u0010\u001b\u001a\u00020\u001c\u0012\b\u0010\u001d\u001a\u0004\u0018\u00010\u001a\u00a2\u0006\u0002\u0010\u001eJ\t\u0010Z\u001a\u00020\u0004H\u00c6\u0003J\t\u0010[\u001a\u00020\u0006H\u00c6\u0003J\t\u0010\\\u001a\u00020\u0006H\u00c6\u0003J\t\u0010]\u001a\u00020\u0006H\u00c6\u0003J\t\u0010^\u001a\u00020\u0006H\u00c6\u0003J\t\u0010_\u001a\u00020\u0006H\u00c6\u0003J\t\u0010`\u001a\u00020\u0006H\u00c6\u0003J\t\u0010a\u001a\u00020\u0006H\u00c6\u0003J\t\u0010b\u001a\u00020\u0006H\u00c6\u0003J\t\u0010c\u001a\u00020\u0006H\u00c6\u0003J\t\u0010d\u001a\u00020\u0006H\u00c6\u0003J\t\u0010e\u001a\u00020\u0006H\u00c6\u0003J\t\u0010f\u001a\u00020\u0006H\u00c6\u0003J\t\u0010g\u001a\u00020\u001aH\u00c6\u0003J\t\u0010h\u001a\u00020\u001cH\u00c6\u0003J\u000b\u0010i\u001a\u0004\u0018\u00010\u001aH\u00c6\u0003J\t\u0010j\u001a\u00020\u0006H\u00c6\u0003J\t\u0010k\u001a\u00020\u0006H\u00c6\u0003J\t\u0010l\u001a\u00020\u0006H\u00c6\u0003J\t\u0010m\u001a\u00020\u0006H\u00c6\u0003J\t\u0010n\u001a\u00020\u0006H\u00c6\u0003J\t\u0010o\u001a\u00020\u0006H\u00c6\u0003J\t\u0010p\u001a\u00020\u0006H\u00c6\u0003J\u00f1\u0001\u0010q\u001a\u00020\u00002\b\b\u0002\u0010\u0003\u001a\u00020\u00042\b\b\u0002\u0010\u0005\u001a\u00020\u00062\b\b\u0002\u0010\u0007\u001a\u00020\u00062\b\b\u0002\u0010\b\u001a\u00020\u00062\b\b\u0002\u0010\t\u001a\u00020\u00062\b\b\u0002\u0010\n\u001a\u00020\u00062\b\b\u0002\u0010\u000b\u001a\u00020\u00062\b\b\u0002\u0010\f\u001a\u00020\u00062\b\b\u0002\u0010\r\u001a\u00020\u00062\b\b\u0002\u0010\u000e\u001a\u00020\u00062\b\b\u0002\u0010\u000f\u001a\u00020\u00062\b\b\u0002\u0010\u0010\u001a\u00020\u00062\b\b\u0002\u0010\u0011\u001a\u00020\u00062\b\b\u0002\u0010\u0012\u001a\u00020\u00062\b\b\u0002\u0010\u0013\u001a\u00020\u00062\b\b\u0002\u0010\u0014\u001a\u00020\u00062\b\b\u0002\u0010\u0015\u001a\u00020\u00062\b\b\u0002\u0010\u0016\u001a\u00020\u00062\b\b\u0002\u0010\u0017\u001a\u00020\u00062\b\b\u0002\u0010\u0018\u001a\u00020\u00062\b\b\u0002\u0010\u0019\u001a\u00020\u001a2\b\b\u0002\u0010\u001b\u001a\u00020\u001c2\n\b\u0002\u0010\u001d\u001a\u0004\u0018\u00010\u001aH\u00c6\u0001J\u0013\u0010r\u001a\u00020D2\b\u0010s\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010t\u001a\u00020\u001cH\u00d6\u0001J\t\u0010u\u001a\u00020\u001aH\u00d6\u0001R\u0016\u0010\u0016\u001a\u00020\u00068\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001f\u0010 R\u0016\u0010\f\u001a\u00020\u00068\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b!\u0010 R\u0016\u0010\t\u001a\u00020\u00068\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\"\u0010 R\u0016\u0010\n\u001a\u00020\u00068\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b#\u0010 R\u0016\u0010\r\u001a\u00020\u00068\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b$\u0010 R\u0016\u0010\u000b\u001a\u00020\u00068\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b%\u0010 R\u0016\u0010\u001b\u001a\u00020\u001c8\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b&\u0010\'R\u0018\u0010\u001d\u001a\u0004\u0018\u00010\u001a8\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b(\u0010)R\u0011\u0010*\u001a\u00020\u001a8F\u00a2\u0006\u0006\u001a\u0004\b+\u0010)R\u0011\u0010,\u001a\u00020\u001a8F\u00a2\u0006\u0006\u001a\u0004\b-\u0010)R\u0011\u0010.\u001a\u00020\u001a8F\u00a2\u0006\u0006\u001a\u0004\b/\u0010)R\u0011\u00100\u001a\u00020\u001a8F\u00a2\u0006\u0006\u001a\u0004\b1\u0010)R\u0011\u00102\u001a\u00020\u001a8F\u00a2\u0006\u0006\u001a\u0004\b3\u0010)R\u0011\u00104\u001a\u00020\u001a8F\u00a2\u0006\u0006\u001a\u0004\b5\u0010)R\u0011\u00106\u001a\u00020\u001a8F\u00a2\u0006\u0006\u001a\u0004\b7\u0010)R\u0011\u00108\u001a\u00020\u001a8F\u00a2\u0006\u0006\u001a\u0004\b9\u0010)R\u0011\u0010:\u001a\u00020\u001a8F\u00a2\u0006\u0006\u001a\u0004\b;\u0010)R\u0011\u0010<\u001a\u00020\u001a8F\u00a2\u0006\u0006\u001a\u0004\b=\u0010)R\u0011\u0010>\u001a\u00020\u001a8F\u00a2\u0006\u0006\u001a\u0004\b?\u0010)R\u0016\u0010\u000f\u001a\u00020\u00068\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b@\u0010 R\u0016\u0010\u0007\u001a\u00020\u00068\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\bA\u0010 R\u0016\u0010\u000e\u001a\u00020\u00068\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\bB\u0010 R\u0011\u0010C\u001a\u00020D8F\u00a2\u0006\u0006\u001a\u0004\bC\u0010ER\u0011\u0010F\u001a\u00020D8F\u00a2\u0006\u0006\u001a\u0004\bF\u0010ER\u0011\u0010G\u001a\u00020D8F\u00a2\u0006\u0006\u001a\u0004\bG\u0010ER\u0011\u0010H\u001a\u00020D8F\u00a2\u0006\u0006\u001a\u0004\bH\u0010ER\u0016\u0010\u0011\u001a\u00020\u00068\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\bI\u0010 R\u0016\u0010\b\u001a\u00020\u00068\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\bJ\u0010 R\u0016\u0010\u0010\u001a\u00020\u00068\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\bK\u0010 R\u0016\u0010\u0017\u001a\u00020\u00068\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\bL\u0010 R\u0016\u0010\u0018\u001a\u00020\u00068\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\bM\u0010 R\u0016\u0010\u0013\u001a\u00020\u00068\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\bN\u0010 R\u0016\u0010\u0005\u001a\u00020\u00068\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\bO\u0010 R\u0016\u0010\u0012\u001a\u00020\u00068\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\bP\u0010 R\u0016\u0010\u0019\u001a\u00020\u001a8\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\bQ\u0010)R\u0011\u0010R\u001a\u00020\u001a8F\u00a2\u0006\u0006\u001a\u0004\bS\u0010)R\u0011\u0010T\u001a\u00020\u001a8F\u00a2\u0006\u0006\u001a\u0004\bU\u0010)R\u0016\u0010\u0003\u001a\u00020\u00048\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\bV\u0010WR\u0016\u0010\u0015\u001a\u00020\u00068\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\bX\u0010 R\u0016\u0010\u0014\u001a\u00020\u00068\u0006X\u0087\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\bY\u0010 \u00a8\u0006v"}, d2 = {"Lapp/sunportal/data/model/InverterData;", "", "()V", "timestamp", "", "pvPower", "", "gridPower", "loadPower", "batteryPower", "batterySoc", "batteryVoltage", "batteryCurrent", "batteryTemperature", "gridVoltage", "gridCurrent", "loadVoltage", "loadCurrent", "pvVoltage", "pvCurrent", "totalEnergy", "todayEnergy", "batteryCapacity", "maxChargePower", "maxDischargePower", "status", "", "errorCode", "", "errorMessage", "(JDDDDDDDDDDDDDDDDDDDLjava/lang/String;ILjava/lang/String;)V", "getBatteryCapacity", "()D", "getBatteryCurrent", "getBatteryPower", "getBatterySoc", "getBatteryTemperature", "getBatteryVoltage", "getErrorCode", "()I", "getErrorMessage", "()Ljava/lang/String;", "formattedBatteryCapacity", "getFormattedBatteryCapacity", "formattedBatteryCurrent", "getFormattedBatteryCurrent", "formattedBatteryPower", "getFormattedBatteryPower", "formattedBatterySoc", "getFormattedBatterySoc", "formattedBatteryTemperature", "getFormattedBatteryTemperature", "formattedBatteryVoltage", "getFormattedBatteryVoltage", "formattedGridPower", "getFormattedGridPower", "formattedLoadPower", "getFormattedLoadPower", "formattedPvPower", "getFormattedPvPower", "formattedTodayEnergy", "getFormattedTodayEnergy", "formattedTotalEnergy", "getFormattedTotalEnergy", "getGridCurrent", "getGridPower", "getGridVoltage", "isCharging", "", "()Z", "isDischarging", "isGridExporting", "isGridImporting", "getLoadCurrent", "getLoadPower", "getLoadVoltage", "getMaxChargePower", "getMaxDischargePower", "getPvCurrent", "getPvPower", "getPvVoltage", "getStatus", "statusColor", "getStatusColor", "systemStatus", "getSystemStatus", "getTimestamp", "()J", "getTodayEnergy", "getTotalEnergy", "component1", "component10", "component11", "component12", "component13", "component14", "component15", "component16", "component17", "component18", "component19", "component2", "component20", "component21", "component22", "component23", "component3", "component4", "component5", "component6", "component7", "component8", "component9", "copy", "equals", "other", "hashCode", "toString", "app_debug"})
public final class InverterData {
    @com.google.gson.annotations.SerializedName(value = "timestamp")
    private final long timestamp = 0L;
    @com.google.gson.annotations.SerializedName(value = "pv_power")
    private final double pvPower = 0.0;
    @com.google.gson.annotations.SerializedName(value = "grid_power")
    private final double gridPower = 0.0;
    @com.google.gson.annotations.SerializedName(value = "load_power")
    private final double loadPower = 0.0;
    @com.google.gson.annotations.SerializedName(value = "battery_power")
    private final double batteryPower = 0.0;
    @com.google.gson.annotations.SerializedName(value = "battery_soc")
    private final double batterySoc = 0.0;
    @com.google.gson.annotations.SerializedName(value = "battery_voltage")
    private final double batteryVoltage = 0.0;
    @com.google.gson.annotations.SerializedName(value = "battery_current")
    private final double batteryCurrent = 0.0;
    @com.google.gson.annotations.SerializedName(value = "battery_temperature")
    private final double batteryTemperature = 0.0;
    @com.google.gson.annotations.SerializedName(value = "grid_voltage")
    private final double gridVoltage = 0.0;
    @com.google.gson.annotations.SerializedName(value = "grid_current")
    private final double gridCurrent = 0.0;
    @com.google.gson.annotations.SerializedName(value = "load_voltage")
    private final double loadVoltage = 0.0;
    @com.google.gson.annotations.SerializedName(value = "load_current")
    private final double loadCurrent = 0.0;
    @com.google.gson.annotations.SerializedName(value = "pv_voltage")
    private final double pvVoltage = 0.0;
    @com.google.gson.annotations.SerializedName(value = "pv_current")
    private final double pvCurrent = 0.0;
    @com.google.gson.annotations.SerializedName(value = "total_energy")
    private final double totalEnergy = 0.0;
    @com.google.gson.annotations.SerializedName(value = "today_energy")
    private final double todayEnergy = 0.0;
    @com.google.gson.annotations.SerializedName(value = "battery_capacity")
    private final double batteryCapacity = 0.0;
    @com.google.gson.annotations.SerializedName(value = "max_charge_power")
    private final double maxChargePower = 0.0;
    @com.google.gson.annotations.SerializedName(value = "max_discharge_power")
    private final double maxDischargePower = 0.0;
    @com.google.gson.annotations.SerializedName(value = "status")
    @org.jetbrains.annotations.NotNull()
    private final java.lang.String status = null;
    @com.google.gson.annotations.SerializedName(value = "error_code")
    private final int errorCode = 0;
    @com.google.gson.annotations.SerializedName(value = "error_message")
    @org.jetbrains.annotations.Nullable()
    private final java.lang.String errorMessage = null;
    
    public InverterData(long timestamp, double pvPower, double gridPower, double loadPower, double batteryPower, double batterySoc, double batteryVoltage, double batteryCurrent, double batteryTemperature, double gridVoltage, double gridCurrent, double loadVoltage, double loadCurrent, double pvVoltage, double pvCurrent, double totalEnergy, double todayEnergy, double batteryCapacity, double maxChargePower, double maxDischargePower, @org.jetbrains.annotations.NotNull()
    java.lang.String status, int errorCode, @org.jetbrains.annotations.Nullable()
    java.lang.String errorMessage) {
        super();
    }
    
    public final long getTimestamp() {
        return 0L;
    }
    
    public final double getPvPower() {
        return 0.0;
    }
    
    public final double getGridPower() {
        return 0.0;
    }
    
    public final double getLoadPower() {
        return 0.0;
    }
    
    public final double getBatteryPower() {
        return 0.0;
    }
    
    public final double getBatterySoc() {
        return 0.0;
    }
    
    public final double getBatteryVoltage() {
        return 0.0;
    }
    
    public final double getBatteryCurrent() {
        return 0.0;
    }
    
    public final double getBatteryTemperature() {
        return 0.0;
    }
    
    public final double getGridVoltage() {
        return 0.0;
    }
    
    public final double getGridCurrent() {
        return 0.0;
    }
    
    public final double getLoadVoltage() {
        return 0.0;
    }
    
    public final double getLoadCurrent() {
        return 0.0;
    }
    
    public final double getPvVoltage() {
        return 0.0;
    }
    
    public final double getPvCurrent() {
        return 0.0;
    }
    
    public final double getTotalEnergy() {
        return 0.0;
    }
    
    public final double getTodayEnergy() {
        return 0.0;
    }
    
    public final double getBatteryCapacity() {
        return 0.0;
    }
    
    public final double getMaxChargePower() {
        return 0.0;
    }
    
    public final double getMaxDischargePower() {
        return 0.0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getStatus() {
        return null;
    }
    
    public final int getErrorCode() {
        return 0;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String getErrorMessage() {
        return null;
    }
    
    public InverterData() {
        super();
    }
    
    public final boolean isCharging() {
        return false;
    }
    
    public final boolean isDischarging() {
        return false;
    }
    
    public final boolean isGridExporting() {
        return false;
    }
    
    public final boolean isGridImporting() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getFormattedPvPower() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getFormattedGridPower() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getFormattedLoadPower() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getFormattedBatteryPower() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getFormattedBatterySoc() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getFormattedBatteryVoltage() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getFormattedBatteryCurrent() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getFormattedBatteryTemperature() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getFormattedTotalEnergy() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getFormattedTodayEnergy() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getFormattedBatteryCapacity() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getSystemStatus() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String getStatusColor() {
        return null;
    }
    
    public final long component1() {
        return 0L;
    }
    
    public final double component10() {
        return 0.0;
    }
    
    public final double component11() {
        return 0.0;
    }
    
    public final double component12() {
        return 0.0;
    }
    
    public final double component13() {
        return 0.0;
    }
    
    public final double component14() {
        return 0.0;
    }
    
    public final double component15() {
        return 0.0;
    }
    
    public final double component16() {
        return 0.0;
    }
    
    public final double component17() {
        return 0.0;
    }
    
    public final double component18() {
        return 0.0;
    }
    
    public final double component19() {
        return 0.0;
    }
    
    public final double component2() {
        return 0.0;
    }
    
    public final double component20() {
        return 0.0;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String component21() {
        return null;
    }
    
    public final int component22() {
        return 0;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.String component23() {
        return null;
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
    public final app.sunportal.data.model.InverterData copy(long timestamp, double pvPower, double gridPower, double loadPower, double batteryPower, double batterySoc, double batteryVoltage, double batteryCurrent, double batteryTemperature, double gridVoltage, double gridCurrent, double loadVoltage, double loadCurrent, double pvVoltage, double pvCurrent, double totalEnergy, double todayEnergy, double batteryCapacity, double maxChargePower, double maxDischargePower, @org.jetbrains.annotations.NotNull()
    java.lang.String status, int errorCode, @org.jetbrains.annotations.Nullable()
    java.lang.String errorMessage) {
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