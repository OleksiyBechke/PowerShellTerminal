package org.kpi.pattern.strategy;

import java.util.ArrayList;
import java.util.List;

public class ThemeManager {
    private static ThemeManager instance;
    private ColorTheme currentTheme;

    // Слухачі, які треба повідомити про зміну теми
    private final List<Runnable> listeners = new ArrayList<>();

    private ThemeManager() {
        this.currentTheme = new DarkTheme(); // За замовчуванням темна
    }

    public static synchronized ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }

    public ColorTheme getTheme() {
        return currentTheme;
    }

    public void setTheme(ColorTheme theme) {
        this.currentTheme = theme;
        notifyListeners();
    }

    public void toggleTheme() {
        if (currentTheme instanceof DarkTheme) {
            setTheme(new LightTheme());
        } else {
            setTheme(new DarkTheme());
        }
    }

    public void subscribe(Runnable listener) {
        listeners.add(listener);
    }

    private void notifyListeners() {
        for (Runnable listener : listeners) {
            listener.run();
        }
    }
}