package org.kpi.service.syntax;

import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
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

        int lastEnd = 0;
        boolean expectingCommand = initialExpectCommand;

        while (matcher.find()) {
            // 1. Додаємо пробіли або звичайний текст між токенами
            if (matcher.start() > lastEnd) {
                nodes.add(createColoredText(text.substring(lastEnd, matcher.start()), Color.WHITE));
            }

            Color color = Color.WHITE; // Дефолтний колір

            // 2. Логіка підсвічування (Interpreter Heuristic)
            if (matcher.group(1) != null) { // String
                color = Color.CYAN;
                if(expectingCommand) {
                    expectingCommand = true;
                } else {
                    expectingCommand = false;
                }
            } else if (matcher.group(2) != null) { // Parameter
                color = Color.GRAY;
                expectingCommand = false;
            } else if (matcher.group(3) != null) { // Pipe (|)
                color = Color.WHITE; // Як ми домовились
                expectingCommand = true;
            } else if (matcher.group(4) != null) { // Generic Word
                if (expectingCommand) {
                    color = Color.YELLOW;
                    expectingCommand = false;
                } else {
                    color = Color.WHITE;
                }
            }

            nodes.add(createColoredText(matcher.group(), color));
            lastEnd = matcher.end();
        }

        // 3. Додаємо залишок тексту після останнього знайденого токена
        if (lastEnd < text.length()) {
            nodes.add(createColoredText(text.substring(lastEnd), Color.WHITE));
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