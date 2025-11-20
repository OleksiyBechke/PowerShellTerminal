package org.kpi.pattern.strategy;

import javafx.scene.paint.Color;

public class LightTheme implements ColorTheme {
    @Override public Color getCommandColor() { return Color.BLUE; }
    @Override public Color getParameterColor() { return Color.DARKGRAY; }
    @Override public Color getStringColor() { return Color.GREEN; }
    @Override public Color getPipeColor() { return Color.BLACK; }
    @Override public Color getArgumentColor() { return Color.BLACK; }

    @Override public Color getPromptColor() { return Color.DARKBLUE; }
    @Override public Color getErrorColor() { return Color.RED; }
    @Override public Color getBackgroundColor() { return Color.WHITE; }
    @Override public Color getTextColor() { return Color.BLACK; }
    @Override public Color getCursorColor() { return Color.BLACK; }
}