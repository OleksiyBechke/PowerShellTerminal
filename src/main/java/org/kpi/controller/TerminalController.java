package org.kpi.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.kpi.dao.DBConnection;
import org.kpi.dao.SnippetDAO;
import org.kpi.model.Snippet;
import org.kpi.pattern.abstractFactory.SnippetUIFactory;
import org.kpi.pattern.abstractFactory.UIFactory;
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

import java.util.List;
import java.util.Optional;

public class TerminalController {

    // 1. Токенізатор: розбиває команду користувача на кольорові шматочки (для вводу/ехо)
    private final SyntaxTokenizer tokenizer = new SyntaxTokenizer();

    @FXML
    private VBox mainContainer;
    @FXML
    private TextFlow outputFlow;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private MenuBar mainMenuBar;

    private PowerShellSession session;
    // 2. HighLighter: інтерпретує вивід від PS (червоний/жовтий)
    private SyntaxHighlighter highlighter;
    private ReplInputArea inputArea;

    // ДОДАЄМО: DAO для роботи з базою
    private CommandLogDAO commandLogDAO;

    private SnippetDAO snippetDAO;

    // ДОДАЄМО: Trie для автодоповнення
    private Trie commandTrie;

    private UIFactory factory;

    @FXML
    public void initialize() {
        session = new PowerShellSession();
        // Створюємо клас Interpreter Pattern
        highlighter = new SyntaxHighlighter();
        commandLogDAO = new CommandLogDAO();
        snippetDAO = new SnippetDAO();
        DBConnection.getInstance();

        factory = new SnippetUIFactory();

        // НОВЕ: Ініціалізація Trie та завантаження команд
        commandTrie = new Trie();
        loadInitialCommands();

        // ПІДПИСКА НА ЗМІНУ ТЕМИ
        ThemeManager.getInstance().subscribe(this::repaintHistory); // <-- НОВИЙ РЯДОК

        // 1. Створюємо ReplInputArea: передаємо handleCommandExecution та getSuggestions
        // ЗВЕРНИ УВАГУ: Тепер ReplInputArea приймає ДВА параметри
        inputArea = new ReplInputArea(this::handleCommandExecution, this::getSuggestions);

        // 2. Додаємо його в інтерфейс
        mainContainer.getChildren().add(inputArea);

        // 3. Клік по вікну фокусує ввід
        mainContainer.setOnMouseClicked(e -> inputArea.requestFocus());

        // НОВЕ: Ініціалізуємо меню
        buildMenuBar();

        // Ініціалізуємо стиль при запуску
        updateWindowStyle();

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

    // ОНОВЛЕНО: Додано пункт для створення сніпета та виклик оновлення меню
    private void buildMenuBar() {
        // Очищаємо, щоб уникнути дублікатів при оновленні
        mainMenuBar.getMenus().clear();

        // 1. Патерн Abstract Factory: Отримуємо фабрику

        // Створюємо головне меню сніпетів
        Menu snippetMenu = factory.createSnippetMenu();

        // ДОДАНО: Керування сніпетами
        MenuItem manageSnippetsItem = factory.createSnippetMenuItem("Керування сніпетами...", this::showManageSnippetsDialog);
        snippetMenu.getItems().add(manageSnippetsItem);

        // НОВИЙ ПУНКТ: Додати сніпет
        MenuItem addSnippetItem = factory.createSnippetMenuItem("Додати сніпет...", this::showAddSnippetDialog);
        snippetMenu.getItems().add(addSnippetItem);
        snippetMenu.getItems().add(new SeparatorMenuItem()); // Роздільник

        // 2. Завантажуємо сніпети з бази даних
        List<Snippet> snippets = snippetDAO.findAll();

        if (snippets.isEmpty()) {
            snippetMenu.getItems().add(factory.createSnippetMenuItem("Сніпетів немає", () -> { /* Do nothing */ }));
        } else {
            for (Snippet snippet : snippets) {
                // Створюємо елемент меню з дією, що вставляє текст
                Runnable action = () -> insertSnippetText(snippet.getCommandBody());
                snippetMenu.getItems().add(factory.createSnippetMenuItem(snippet.getTitle(), action));
            }
        }

        // Додаємо меню до MenuBar
        mainMenuBar.getMenus().add(snippetMenu);
    }

    /**
     * НОВИЙ МЕТОД: Показує діалог для створення нового сніпета.
     */
    private void showAddSnippetDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Додати новий сніпет");
        dialog.setHeaderText("Введіть назву, команду та опис для сніпета.");

        // Налаштування кнопок
        ButtonType saveButtonType = new ButtonType("Зберегти", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Налаштування полів вводу
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField titleField = new TextField();
        titleField.setPromptText("Назва (напр., Get-Info)");

        TextArea commandField = new TextArea();
        commandField.setPromptText("Команда (напр., Get-ComputerInfo)");

        TextArea descField = new TextArea();
        descField.setPromptText("Опис (опціонально)");

        // Розміщення елементів
        grid.addRow(0, new Label("Назва:"), titleField);
        grid.addRow(1, new Label("Команда:"), commandField);
        grid.addRow(2, new Label("Опис:"), descField);

        dialog.getDialogPane().setContent(grid);

        // Обробка результату діалогу
        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isPresent() && result.get() == saveButtonType) {
            String title = titleField.getText().trim();
            String command = commandField.getText().trim();
            String desc = descField.getText().trim();

            if (!title.isEmpty() && !command.isEmpty()) {
                Snippet newSnippet = new Snippet();
                newSnippet.setTitle(title);
                newSnippet.setCommandBody(command);
                newSnippet.setDescription(desc);

                snippetDAO.save(newSnippet);

                // Після збереження оновлюємо меню
                buildMenuBar();
            } else {
                // Можна додати попередження, але поки обмежимося цим
            }
        }
    }

    /**
     * НОВИЙ МЕТОД: Показує діалог для керування (видалення) сніпетами.
     */
    private void showManageSnippetsDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Керування сніпетами");
        dialog.setHeaderText("Виберіть сніпет для видалення.");

        // Налаштування кнопок
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);

        // Завантажуємо актуальний список сніпетів
        List<Snippet> snippets = snippetDAO.findAll();

        // Створення VBox для розміщення елементів
        VBox content = new VBox(5);
        content.setPrefWidth(500);

        if (snippets.isEmpty()) {
            content.getChildren().add(new Label("Немає збережених сніпетів."));
        } else {
            // Додавання кожного сніпета з кнопкою "Видалити"
            for (Snippet snippet : snippets) {
                HBox itemRow = new HBox(10);
                itemRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                Label titleLabel = new Label(snippet.getTitle());
                titleLabel.setPrefWidth(200);

                Button deleteButton = new Button("Видалити");
                deleteButton.setStyle("-fx-base: #CC0000;"); // Червона кнопка

                deleteButton.setOnAction(event -> {
                    // Видалення сніпета з БД
                    snippetDAO.delete(snippet.getId());

                    // Закриваємо поточний діалог та оновлюємо меню
                    dialog.close();
                    buildMenuBar();
                    // Відкриваємо діалог керування знову для оновлення
                    Platform.runLater(this::showManageSnippetsDialog);
                });

                itemRow.getChildren().addAll(titleLabel, new Separator(), deleteButton);
                content.getChildren().add(itemRow);
            }
        }

        dialog.getDialogPane().setContent(content);
        dialog.showAndWait();
    }

    // НОВИЙ МЕТОД: Вставляє текст сніпета у поле вводу (викликається з меню)
    private void insertSnippetText(String command) {
        // Використовуємо новий публічний метод у ReplInputArea
        inputArea.insertText(command);
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