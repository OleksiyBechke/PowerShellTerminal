package org.kpi.pattern.interpreter;

import javafx.scene.paint.Color;

public class ErrorExpression implements Expression {
    @Override
    public boolean interpret(String context) {
        // Якщо рядок починається з [ERROR] або містить слово Exception
        return context != null && (context.startsWith("[ERROR]") || context.contains("Exception"));
    }

    @Override
    public Color getColor() {
        return Color.RED;
    }
}