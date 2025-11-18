package org.kpi.pattern.interpreter;

import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Клас-клієнт патерну Interpreter.
 * Визначає колір тексту виводу, перевіряючи його на відповідність набору правил.
 */
public class SyntaxHighlighter {

    private final List<Expression> expressions = new ArrayList<>();

    public SyntaxHighlighter() {
        // Додаємо правила в ієрархії: Помилки мають пріоритет
        expressions.add(new ErrorExpression());    // Першим перевіряється червоний
        expressions.add(new SuccessExpression());  // Другим перевіряється жовтий
    }

    /**
     * Перевіряє рядок на відповідність усім правилам.
     * @param outputText Рядок виводу від PowerShell.
     * @return Відповідний колір або Color.LIGHTGRAY за замовчуванням.
     */
    public Color determineColor(String outputText) {
        for (Expression expr : expressions) {
            if (expr.interpret(outputText)) {
                // Як тільки правило спрацювало, повертаємо його колір
                return expr.getColor();
            }
        }
        // Якщо жодне правило не спрацювало, це звичайний текст
        return Color.LIGHTGRAY;
    }
}