package org.kpi.pattern.strategy;

import javafx.scene.paint.Color;

public class DarkTheme implements ColorTheme {
    @Override public Color getCommandColor() { return Color.YELLOW; }
    @Override public Color getParameterColor() { return Color.GRAY; }
    @Override public Color getStringColor() { return Color.CYAN; }
    @Override public Color getPipeColor() { return Color.WHITE; }
    @Override public Color getArgumentColor() { return Color.WHITE; }

    @Override public Color getPromptColor() { return Color.LIGHTGREEN; }
    @Override public Color getErrorColor() { return Color.RED; }
    @Override public Color getBackgroundColor() { return Color.web("#1e1e1e"); } // Темно-сірий
    @Override public Color getTextColor() { return Color.LIGHTGRAY; }
    @Override public Color getCursorColor() { return Color.WHITE; }
}