package org.kpi.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.kpi.service.PowerShellSession;
import org.kpi.pattern.interpreter.SyntaxHighlighter; // Імпортуємо наш новий клас

public class TerminalController {

    @FXML
    private TextFlow outputFlow;

    @FXML
    private ScrollPane scrollPane;

    @FXML
    private TextField inputField;

    private PowerShellSession session;
    private SyntaxHighlighter highlighter; // Додаємо поле парсера

    @FXML
    public void initialize() {
        session = new PowerShellSession();
        highlighter = new SyntaxHighlighter(); // Ініціалізуємо парсер

        session.setOutputHandler(text -> {
            Platform.runLater(() -> {
                // 1. Визначаємо колір через патерн Interpreter
                Color color = highlighter.determineColor(text);

                // 2. Якщо це помилка, прибираємо технічний префікс [ERROR] для краси
                String displayText = text.replace("[ERROR] ", "") + "\n";

                // 3. Виводимо
                appendColoredText(displayText, color);
            });
        });
    }

    @FXML
    public void onEnterPressed() {
        String command = inputField.getText();
        appendColoredText("> " + command + "\n", Color.LIGHTGREEN); // Команди користувача завжди зелені
        session.execute(command);
        inputField.clear();
    }

    private void appendColoredText(String content, Color color) {
        Text textNode = new Text(content);
        textNode.setFill(color);
        textNode.setFont(Font.font("Consolas", 14));
        outputFlow.getChildren().add(textNode);
        scrollPane.setVvalue(1.0);
    }

    public void shutdown() {
        if (session != null) session.close();
    }
}