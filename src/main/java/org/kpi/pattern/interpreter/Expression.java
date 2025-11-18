package org.kpi.pattern.interpreter;

import javafx.scene.paint.Color;

public interface Expression {
    // Метод перевіряє, чи підходить текст під це правило
    boolean interpret(String context);

    // Метод повертає колір для цього правила
    Color getColor();
}