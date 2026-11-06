package app.sunportal.ui.screens;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000\u0016\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0002\b\u0003\u001a\b\u0010\u0000\u001a\u00020\u0001H\u0007\u001a\b\u0010\u0002\u001a\u00020\u0001H\u0007\u001a\b\u0010\u0003\u001a\u00020\u0001H\u0007\u001a\u001c\u0010\u0004\u001a\u00020\u00012\u0012\u0010\u0005\u001a\u000e\u0012\u0004\u0012\u00020\u0007\u0012\u0004\u0012\u00020\u00010\u0006H\u0007\u001a\b\u0010\b\u001a\u00020\u0001H\u0007\u001a\b\u0010\t\u001a\u00020\u0001H\u0007\u00a8\u0006\n"}, d2 = {"BlackoutStatusCard", "", "EnergyFlowCard", "MainMetricsCard", "OverviewScreen", "onNavigate", "Lkotlin/Function1;", "", "SystemStatusCard", "WeatherCard", "app_debug"})
public final class OverviewScreenKt {
    
    /**
     * Экран обзора SunPortal приложения
     */
    @kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class})
    @androidx.compose.runtime.Composable()
    public static final void OverviewScreen(@org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onNavigate) {
    }
    
    /**
     * Карточка погоды
     */
    @androidx.compose.runtime.Composable()
    public static final void WeatherCard() {
    }
    
    /**
     * Карточка схемы потоков энергии
     */
    @androidx.compose.runtime.Composable()
    public static final void EnergyFlowCard() {
    }
    
    /**
     * Карточка статуса блекаута
     */
    @androidx.compose.runtime.Composable()
    public static final void BlackoutStatusCard() {
    }
    
    /**
     * Карточка основных показателей
     */
    @androidx.compose.runtime.Composable()
    public static final void MainMetricsCard() {
    }
    
    /**
     * Карточка статуса системы
     */
    @androidx.compose.runtime.Composable()
    public static final void SystemStatusCard() {
    }
}