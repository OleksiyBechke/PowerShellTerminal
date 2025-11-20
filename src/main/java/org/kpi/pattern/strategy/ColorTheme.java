package org.kpi.pattern.strategy;

import javafx.scene.paint.Color;

/**
 * Інтерфейс Стратегії (Strategy): визначає набір методів для отримання кольорів.
 */
public interface ColorTheme {
    // Синтаксис
    Color getCommandColor();
    Color getParameterColor();
    Color getStringColor();
    Color getPipeColor();
    Color getArgumentColor();

    // Системні
    Color getPromptColor();
    Color getErrorColor();
    Color getBackgroundColor();
    Color getTextColor(); // Основний колір тексту
    Color getCursorColor();
}