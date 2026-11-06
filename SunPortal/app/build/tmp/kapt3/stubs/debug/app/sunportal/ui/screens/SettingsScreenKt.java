package app.sunportal.ui.screens;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000\u001a\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0007\u001a\b\u0010\u0000\u001a\u00020\u0001H\u0007\u001a@\u0010\u0002\u001a\u00020\u00012\u0006\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00042\u0012\u0010\u0006\u001a\u000e\u0012\u0004\u0012\u00020\u0004\u0012\u0004\u0012\u00020\u00010\u00072\u0012\u0010\b\u001a\u000e\u0012\u0004\u0012\u00020\u0004\u0012\u0004\u0012\u00020\u00010\u0007H\u0007\u001a\b\u0010\t\u001a\u00020\u0001H\u0007\u001a\b\u0010\n\u001a\u00020\u0001H\u0007\u001aT\u0010\u000b\u001a\u00020\u00012\u0006\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u00042\u0012\u0010\u0006\u001a\u000e\u0012\u0004\u0012\u00020\u0004\u0012\u0004\u0012\u00020\u00010\u00072\u0012\u0010\b\u001a\u000e\u0012\u0004\u0012\u00020\u0004\u0012\u0004\u0012\u00020\u00010\u00072\u0012\u0010\f\u001a\u000e\u0012\u0004\u0012\u00020\u0004\u0012\u0004\u0012\u00020\u00010\u0007H\u0007\u001a\b\u0010\r\u001a\u00020\u0001H\u0007\u00a8\u0006\u000e"}, d2 = {"AboutSettingsCard", "", "AppearanceSettingsCard", "currentTheme", "", "fontSize", "onThemeChange", "Lkotlin/Function1;", "onFontSizeChange", "NotificationsSettingsCard", "ServerSettingsCard", "SettingsScreen", "onNavigate", "SystemSettingsCard", "app_debug"})
public final class SettingsScreenKt {
    
    /**
     * Экран настроек приложения
     */
    @kotlin.OptIn(markerClass = {androidx.compose.material3.ExperimentalMaterial3Api.class})
    @androidx.compose.runtime.Composable()
    public static final void SettingsScreen(@org.jetbrains.annotations.NotNull()
    java.lang.String currentTheme, @org.jetbrains.annotations.NotNull()
    java.lang.String fontSize, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onThemeChange, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onFontSizeChange, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onNavigate) {
    }
    
    /**
     * Карточка настроек внешнего вида
     */
    @androidx.compose.runtime.Composable()
    public static final void AppearanceSettingsCard(@org.jetbrains.annotations.NotNull()
    java.lang.String currentTheme, @org.jetbrains.annotations.NotNull()
    java.lang.String fontSize, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onThemeChange, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function1<? super java.lang.String, kotlin.Unit> onFontSizeChange) {
    }
    
    /**
     * Карточка настроек системы
     */
    @androidx.compose.runtime.Composable()
    public static final void SystemSettingsCard() {
    }
    
    /**
     * Карточка настроек уведомлений
     */
    @androidx.compose.runtime.Composable()
    public static final void NotificationsSettingsCard() {
    }
    
    /**
     * Карточка настроек сервера
     */
    @androidx.compose.runtime.Composable()
    public static final void ServerSettingsCard() {
    }
    
    /**
     * Карточка информации о приложении
     */
    @androidx.compose.runtime.Composable()
    public static final void AboutSettingsCard() {
    }
}