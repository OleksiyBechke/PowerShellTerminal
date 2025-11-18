package org.kpi.pattern.interpreter;

import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.List;

public class SyntaxHighlighter {

    private List<Expression> rules;

    public SyntaxHighlighter() {
        rules = new ArrayList<>();
        // Додаємо наші правила в список
        rules.add(new ErrorExpression());
        rules.add(new SuccessExpression());
        // Можна додавати скільки завгодно нових правил тут
    }

    public Color determineColor(String text) {
        for (Expression rule : rules) {
            if (rule.interpret(text)) {
                return rule.getColor();
            }
        }
        return Color.LIGHTGRAY; // Колір за замовчуванням, якщо жодне правило не спрацювало
    }
}