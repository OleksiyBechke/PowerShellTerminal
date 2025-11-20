package org.kpi.service.syntax;

import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import org.kpi.pattern.strategy.ColorTheme;
import org.kpi.pattern.strategy.ThemeManager;
import org.kpi.service.syntax.PowerShellSyntax;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

public class SyntaxTokenizer {

    private static final Font FONT = Font.font("Consolas", 14);

    /**
     * Перетворює рядок команди на список кольорових Text-нод
     */
    public List<Text> tokenize(String text, boolean initialExpectCommand) {
        List<Text> nodes = new ArrayList<>();
        Matcher matcher = PowerShellSyntax.TOKEN_PATTERN.matcher(text);

        // ОТРИМУЄМО ПОТОЧНУ ТЕМУ
        ColorTheme theme = ThemeManager.getInstance().getTheme();

        int lastEnd = 0;
        boolean expectingCommand = initialExpectCommand;

        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                // Пробіли використовують колір аргументів (або background, але краще аргументів)
                nodes.add(createColoredText(text.substring(lastEnd, matcher.start()), theme.getArgumentColor()));
            }

            Color color = theme.getArgumentColor(); // Дефолтний колір з теми

            if (matcher.group(1) != null) { // String
                color = theme.getStringColor(); // <-- ТЕМА
                if (expectingCommand) expectingCommand = true; else expectingCommand = false;

            } else if (matcher.group(2) != null) { // Parameter
                color = theme.getParameterColor(); // <-- ТЕМА
                expectingCommand = false;

            } else if (matcher.group(3) != null) { // Pipe
                color = theme.getPipeColor(); // <-- ТЕМА
                expectingCommand = true;

            } else if (matcher.group(4) != null) { // Generic Word
                if (expectingCommand) {
                    color = theme.getCommandColor(); // <-- ТЕМА
                    expectingCommand = false;
                } else {
                    color = theme.getArgumentColor(); // <-- ТЕМА
                }
            }

            nodes.add(createColoredText(matcher.group(), color));
            lastEnd = matcher.end();
        }

        if (lastEnd < text.length()) {
            // Хвіст теж беремо з теми
            nodes.add(createColoredText(text.substring(lastEnd), theme.getArgumentColor()));
        }

        return nodes;
    }

    private Text createColoredText(String content, Color color) {
        Text textNode = new Text(content);
        textNode.setFill(color);
        textNode.setFont(FONT);
        return textNode;
    }
}