package org.kpi.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.kpi.service.syntax.SyntaxTokenizer;
import org.kpi.service.PowerShellSession;
import org.kpi.view.component.ReplInputArea;
import org.kpi.pattern.interpreter.SyntaxHighlighter; // <-- КЛАС З ПАТЕРНОМ INTERPRETER (ПОВИНЕН БУТИ ТУТ)

import java.util.List;

public class TerminalController {

    // 1. Токенізатор: розбиває команду користувача на кольорові шматочки (для вводу/ехо)
    private final SyntaxTokenizer tokenizer = new SyntaxTokenizer();

    @FXML
    private VBox mainContainer;
    @FXML
    private TextFlow outputFlow;
    @FXML
    private ScrollPane scrollPane;

    private PowerShellSession session;
    // 2. HighLighter: інтерпретує вивід від PS (червоний/жовтий)
    private SyntaxHighlighter highlighter;
    private ReplInputArea inputArea;

    @FXML
    public void initialize() {
        session = new PowerShellSession();
        // Створюємо клас Interpreter Pattern
        highlighter = new SyntaxHighlighter();

        // 1. Створюємо наше поле вводу
        inputArea = new ReplInputArea(this::handleCommandExecution);

        // 2. Додаємо його в інтерфейс
        mainContainer.getChildren().add(inputArea);

        // 3. Клік по вікну фокусує ввід
        mainContainer.setOnMouseClicked(e -> inputArea.requestFocus());

        // 4. Налаштування сесії (вивід від PowerShell)
        session.setOutputHandler(text -> {
            Platform.runLater(() -> {
                // ВИКОРИСТОВУЄМО КЛАС INTERPRETER:
                Color color = highlighter.determineColor(text);
                String displayText = text.replace("[ERROR] ", "") + "\n";
                appendColoredText(displayText, color);
            });
        });
    }

    private void handleCommandExecution() {
        String command = inputArea.getCommandAndClear();
        appendCommandEcho(command);
        session.execute(command);
    }

    private void appendCommandEcho(String command) {
        if (command.trim().isEmpty()) {
            appendColoredText("PS User> \n", Color.LIGHTGREEN);
            return;
        }

        // 1. Додаємо промпт
        appendColoredText("PS User> ", Color.LIGHTGREEN);

        // 2. Використовуємо ТОКЕНІЗАТОР для фарбування ехо
        List<Text> tokens = tokenizer.tokenize(command, true);
        outputFlow.getChildren().addAll(tokens);

        // 3. Додаємо перехід на новий рядок
        appendColoredText("\n", Color.LIGHTGRAY);
    }

    private void appendColoredText(String content, Color color) {
        Text textNode = new Text(content);
        textNode.setFill(color);
        textNode.setFont(Font.font("Consolas", 14));
        outputFlow.getChildren().add(textNode);

        // Скролимо вниз
        scrollPane.layout();
        scrollPane.setVvalue(1.0);
    }

    public void shutdown() {
        if (session != null) session.close();
    }
}