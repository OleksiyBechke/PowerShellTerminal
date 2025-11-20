package org.kpi.view.component;

import javafx.application.Platform;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.kpi.dao.CommandLogDAO;
import org.kpi.pattern.command.Command;
import org.kpi.pattern.command.PowerShellExecuteCommand;
import org.kpi.pattern.interpreter.SyntaxHighlighter;
import org.kpi.pattern.strategy.ColorTheme;
import org.kpi.pattern.strategy.ThemeManager;
import org.kpi.service.PowerShellSession;
import org.kpi.service.syntax.SyntaxTokenizer;
import org.kpi.util.Trie;

import java.util.ArrayList;
import java.util.List;

public class TerminalTab extends Tab {

    private final PowerShellSession session;
    private final SyntaxHighlighter highlighter;
    private final SyntaxTokenizer tokenizer = new SyntaxTokenizer();
    private final CommandLogDAO commandLogDAO;
    private final Trie commandTrie;

    private final VBox container;
    private final TextFlow outputFlow;
    private final ScrollPane scrollPane;
    private final ReplInputArea inputArea;

    // Історія для цієї конкретної вкладки
    private final List<String> fullRawHistory = new ArrayList<>();

    public TerminalTab(String title, CommandLogDAO dao, Trie trie) {
        super(title);
        this.commandLogDAO = dao;
        this.commandTrie = trie;

        this.session = new PowerShellSession();
        this.highlighter = new SyntaxHighlighter();

        this.outputFlow = new TextFlow();
        this.scrollPane = new ScrollPane(outputFlow);
        this.scrollPane.setFitToWidth(true);

        this.container = new VBox();
        this.container.setStyle("-fx-padding: 10;");

        this.inputArea = new ReplInputArea(this::handleCommandExecution, this::getSuggestions);

        this.container.getChildren().addAll(scrollPane, inputArea);
        this.setContent(container);

        setupSessionOutput();

        // Підписка на зміну теми: викликаємо repaintHistory для цієї вкладки
        ThemeManager.getInstance().subscribe(this::repaintHistory);

        // Застосовуємо стиль одразу
        repaintHistory();

        this.setOnClosed(e -> session.close());
    }

    private void setupSessionOutput() {
        session.setOutputHandler(text -> {
            // Зберігаємо вивід
            fullRawHistory.add("[PS_OUT] " + text);

            Platform.runLater(() -> {
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

        // Зберігаємо ввід
        fullRawHistory.add("[ECHO] " + command);

        appendCommandEcho(command);

        Command executeCommand = new PowerShellExecuteCommand(session, commandLogDAO, command);
        try {
            executeCommand.execute();
        } catch (RuntimeException e) {
            appendColoredText("[Error] " + e.getMessage() + "\n", Color.RED);
        }
    }

    /**
     * Перемальовує всю історію цієї вкладки при зміні теми.
     */
    private void repaintHistory() {
        ColorTheme theme = ThemeManager.getInstance().getTheme();

        // 1. Оновлюємо фон вкладки
        String hexColor = String.format("#%02X%02X%02X",
                (int) (theme.getBackgroundColor().getRed() * 255),
                (int) (theme.getBackgroundColor().getGreen() * 255),
                (int) (theme.getBackgroundColor().getBlue() * 255));

        container.setStyle("-fx-background-color: " + hexColor + "; -fx-padding: 10;");
        scrollPane.setStyle("-fx-background: " + hexColor + "; -fx-background-color: " + hexColor + ";");
        outputFlow.setStyle("-fx-background-color: " + hexColor + ";");

        // 2. Очищаємо та перемальовуємо текст
        outputFlow.getChildren().clear();

        for (String line : fullRawHistory) {
            if (line.startsWith("[ECHO] ")) {
                String command = line.replace("[ECHO] ", "");
                // Перемальовуємо команду з новою темою
                appendCommandEcho(command);

            } else if (line.startsWith("[PS_OUT] ")) {
                String psLine = line.replace("[PS_OUT] ", "");
                // Перемальовуємо вивід (Highlighter візьме нові кольори з ThemeManager)
                Color color = highlighter.determineColor(psLine);
                String displayText = psLine.replace("[ERROR] ", "") + "\n";
                appendColoredText(displayText, color);
            }
        }

        // 3. Оновлюємо поле вводу (якщо метод зроблено публічним, як ми домовлялися)
        // Якщо метод refreshContent() не публічний, ThemeManager оновить його окремо,
        // бо ReplInputArea сам підписаний на ThemeManager.
    }

    private java.util.List<String> getSuggestions(String prefix) {
        if (prefix.isBlank()) return java.util.List.of();
        java.util.List<String> results = commandTrie.searchByPrefix(prefix);
        return results.size() > 10 ? results.subList(0, 10) : results;
    }

    private void appendCommandEcho(String command) {
        ColorTheme theme = ThemeManager.getInstance().getTheme();

        if (command.trim().isEmpty()) {
            appendColoredText("PS User> \n", theme.getPromptColor());
            return;
        }

        appendColoredText("PS User> ", theme.getPromptColor());
        // Токенізатор бере кольори з поточної теми
        outputFlow.getChildren().addAll(tokenizer.tokenize(command, true));
        appendColoredText("\n", theme.getArgumentColor());
    }

    private void appendColoredText(String content, Color color) {
        Text textNode = new Text(content);
        textNode.setFill(color);
        textNode.setFont(Font.font("Consolas", 14));
        outputFlow.getChildren().add(textNode);
        scrollPane.setVvalue(1.0);
    }

    public void insertText(String text) {
        inputArea.insertText(text);
    }

    public void requestFocus() {
        inputArea.requestFocus();
    }
}