package org.kpi.pattern.interpreter;

import javafx.scene.paint.Color;

public class SuccessExpression implements Expression {
    @Override
    public boolean interpret(String context) {
        // Це проста евристика для заголовків PowerShell таблиць
        return context != null && context.contains("Mode") && context.contains("LastWriteTime");
    }

    @Override
    public Color getColor() {
        return Color.YELLOW; // Заголовки будуть жовтими
    }
}