package org.kpi.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.kpi.dao.DBConnection;
import org.kpi.pattern.command.Command;
import org.kpi.pattern.command.PowerShellExecuteCommand;
import org.kpi.service.syntax.CommandLoader;
import org.kpi.service.syntax.SyntaxTokenizer;
import org.kpi.service.PowerShellSession;
import org.kpi.util.Trie;
import org.kpi.view.component.ReplInputArea;
import org.kpi.pattern.interpreter.SyntaxHighlighter;
import org.kpi.dao.CommandLogDAO;
import org.kpi.model.CommandLog;

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

    // ДОДАЄМО: DAO для роботи з базою
    private CommandLogDAO commandLogDAO;

    // ДОДАЄМО: Trie для автодоповнення
    private Trie commandTrie;

    @FXML
    public void initialize() {
        session = new PowerShellSession();
        // Створюємо клас Interpreter Pattern
        highlighter = new SyntaxHighlighter();
        commandLogDAO = new CommandLogDAO();
        DBConnection.getInstance();

        // НОВЕ: Ініціалізація Trie та завантаження команд
        commandTrie = new Trie();
        loadInitialCommands();

        // 1. Створюємо ReplInputArea: передаємо handleCommandExecution та getSuggestions
        // ЗВЕРНИ УВАГУ: Тепер ReplInputArea приймає ДВА параметри
        inputArea = new ReplInputArea(this::handleCommandExecution, this::getSuggestions);

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

        if (command.trim().isEmpty()) {
            appendCommandEcho(command);
            return;
        }

        // 1. Відображення команди
        appendCommandEcho(command);

        // 2. СТВОРЕННЯ ТА ВИКОНАННЯ COMMAND ПАТЕРНУ
        Command executeCommand = new PowerShellExecuteCommand(session, commandLogDAO, command);

        try {
            executeCommand.execute();
        } catch (RuntimeException e) {
            // Обробка помилок (наприклад, якщо БД відвалилася, але термінал має працювати)
            appendColoredText("[APP ERROR] Command Execution Failed: " + e.getMessage() + "\n", Color.RED);
        }
    }

    // НОВИЙ МЕТОД: Завантаження початкових команд
    private void loadInitialCommands() {
        CommandLoader loader = new CommandLoader();
        List<String> dynamicCommands = loader.loadAllCommands(); // <-- ДИНАМІЧНИЙ ВИКЛИК

        if (dynamicCommands.isEmpty() || dynamicCommands.size() < 10) {
            // Fallback: Залишаємо мінімальний набір, якщо завантаження з PS не вдалося
            dynamicCommands.add("dir");
            dynamicCommands.add("cd");
            dynamicCommands.add("exit");
        }

        for (String cmd : dynamicCommands) {
            commandTrie.insert(cmd);
        }
        System.out.println("--- IntelliSense Trie Loaded (" + dynamicCommands.size() + " commands) ---");
    }

    // НОВИЙ МЕТОД: Викликається ReplInputArea для отримання підказок (Trie Search)
    public List<String> getSuggestions(String prefix) {
        if (prefix.isBlank()) return List.of();
        // Беремо тільки перші 10 підказок
        List<String> results = commandTrie.searchByPrefix(prefix);
        return results.size() > 10 ? results.subList(0, 10) : results;
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
        scrollPane.setVvalue(1.0);
    }

    public void shutdown() {
        if (session != null) session.close();
    }
}