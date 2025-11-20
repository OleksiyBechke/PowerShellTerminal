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
import org.kpi.pattern.strategy.ColorTheme;
import org.kpi.pattern.strategy.ThemeManager;
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

        // ПІДПИСКА НА ЗМІНУ ТЕМИ
        ThemeManager.getInstance().subscribe(this::repaintHistory); // <-- НОВИЙ РЯДОК

        // Ініціалізуємо стиль при запуску
        updateWindowStyle();

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

        // --- ГАРЯЧА КЛАВІША ДЛЯ ЗМІНИ ТЕМИ (Ctrl + T) ---
        mainContainer.setOnKeyPressed(event -> {
            if (event.isControlDown() && event.getCode() == javafx.scene.input.KeyCode.T) {
                // 1. Перемикаємо тему
                ThemeManager.getInstance().toggleTheme();

                // 2. Оновлюємо фон вікна
                updateWindowStyle();
            }
        });

        // Ініціалізуємо стиль при запуску
        updateWindowStyle();
    }

    // Метод для зміни кольору фону вікна
    private void updateWindowStyle() {
        var theme = org.kpi.pattern.strategy.ThemeManager.getInstance().getTheme();
        String hexColor = toHexString(theme.getBackgroundColor());

        // Міняємо фон VBox і ScrollPane
        mainContainer.setStyle("-fx-background-color: " + hexColor + "; -fx-padding: 10;");
        scrollPane.setStyle("-fx-background: " + hexColor + "; -fx-background-color: " + hexColor + ";");
        outputFlow.setStyle("-fx-background-color: " + hexColor + ";");
    }

    // Допоміжний метод для конвертації кольору в HEX
    private String toHexString(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
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
        // 1. Отримуємо поточну тему
        ColorTheme theme = ThemeManager.getInstance().getTheme();

        if (command.trim().isEmpty()) {
            // Використовуємо колір промпту з теми
            appendColoredText("PS User> \n", theme.getPromptColor());
            return;
        }

        // 1. Додаємо промпт
        // Використовуємо колір промпту з теми
        appendColoredText("PS User> ", theme.getPromptColor());

        // 2. Використовуємо ТОКЕНІЗАТОР (який вже використовує ThemeManager для фарбування тіла команди)
        List<Text> tokens = tokenizer.tokenize(command, true);
        outputFlow.getChildren().addAll(tokens);

        // 3. Додаємо перехід на новий рядок (використовуємо колір аргументів)
        appendColoredText("\n", theme.getArgumentColor());
    }

    private void appendColoredText(String content, Color color) {
        Text textNode = new Text(content);
        textNode.setFill(color);
        textNode.setFont(Font.font("Consolas", 14));
        outputFlow.getChildren().add(textNode);

        // Скролимо вниз
        scrollPane.setVvalue(1.0);
    }

    /**
     * Перемальовує всю історію у TextFlow відповідно до поточної теми.
     */
    private void repaintHistory() {
        // Отримуємо поточну тему та SyntaxHighlighter
        ColorTheme theme = ThemeManager.getInstance().getTheme();

        // Оновлюємо фон вікна
        updateWindowStyle();

        // Перемальовування історії в outputFlow
        for (javafx.scene.Node node : outputFlow.getChildren()) {
            if (node instanceof Text) {
                Text textNode = (Text) node;

                // Ми повинні визначити, чи це був вивід команди, чи її ехо, що є складно.
                // Спростимо: будь-який вузол Text є частиною команди або виводу.

                // Якщо текст починається з промпта (PS User >), фарбуємо його промптом
                if (textNode.getText().startsWith("PS User>")) {
                    textNode.setFill(theme.getPromptColor());
                } else if (textNode.getText().startsWith(" ") || textNode.getText().endsWith(" ") || textNode.getText().trim().isEmpty()) {
                    // Фарбуємо пробіли і порожні рядки кольором аргументів
                    textNode.setFill(theme.getArgumentColor());
                } else {
                    // Для всіх інших елементів (токени команди, аргументи, вивід)
                    // Використовуємо Highlighter для виводу або General Text для аргументів

                    // Оскільки ми не зберігаємо тип токена, ми можемо лише перевірити,
                    // чи це вивід помилки/успіху, а решту фарбувати загальним кольором тексту.
                    Color newColor = highlighter.determineColor(textNode.getText().trim());

                    // Якщо Highlighter не дав специфічний колір (червоний/жовтий), використовуємо колір тексту
                    if (newColor.equals(theme.getTextColor())) {
                        textNode.setFill(theme.getTextColor());
                    } else if (newColor.equals(Color.RED)) {
                        textNode.setFill(theme.getErrorColor());
                    } else {
                        // Якщо це не помилка, але і не загальний, використовуємо аргумент color
                        textNode.setFill(theme.getArgumentColor());
                    }
                }
            }
        }
    }

    public void shutdown() {
        if (session != null) session.close();
    }
}