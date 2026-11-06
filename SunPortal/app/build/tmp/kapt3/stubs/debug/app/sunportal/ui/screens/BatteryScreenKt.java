package app.sunportal.ui.screens;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000\u0014\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0002\b\u0006\u001a\u001c\u0010\u0000\u001a\u00020\u00012\u0012\u0010\u0002\u001a\u000e\u0012\u0004\u0012\u00020\u0004\u0012\u0004\u0012\u00020\u00010\u0003H\u0007\u001a\b\u0010\u0005\u001a\u00020\u0001H\u0007\u001a\b\u0010\u0006\u001a\u00020\u0001H\u0007\u001a\b\u0010\u0007\u001a\u00020\u0001H\u0007\u001a\b\u0010\b\u001a\u00020\u0001H\u0007\u001a\b\u0010\t\u001a\u00020\u0001H\u0007\u00a8\u0006\n"}, d2 = {"BatteryScreen", "", "onNavigate", "Lkotlin/Function1;", "", "BatteryStatusCard", "ChargingChartCard", "ChargingSettingsCard", "PresetsCard", "ReserveEstimationCard", "app_debug"})
public final class BatteryScreenKt {
    
    /**
     * Экран управления батареей
     */
    @kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class})
    @androidx.compose.runtime.Composable()
    public static final void BatteryScreen(@org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onNavigate) {
    }
    
    /**
     * Карточка статуса батареи
     */
    @androidx.compose.runtime.Composable()
    public static final void BatteryStatusCard() {
    }
    
    /**
     * Карточка оценки резерва
     */
    @androidx.compose.runtime.Composable()
    public static final void ReserveEstimationCard() {
    }
    
    /**
     * Карточка настроек зарядки
     */
    @androidx.compose.runtime.Composable()
    public static final void ChargingSettingsCard() {
    }
    
    /**
     * Карточка пресетов
     */
    @androidx.compose.runtime.Composable()
    public static final void PresetsCard() {
    }
    
    /**
     * Карточка графика заряда
     */
    @androidx.compose.runtime.Composable()
    public static final void ChargingChartCard() {
    }
}