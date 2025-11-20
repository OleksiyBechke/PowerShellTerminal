package org.kpi.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
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
import org.kpi.view.component.TerminalTab;

import java.util.List;
import java.util.Optional;

public class TerminalController {

    // 1. Токенізатор: розбиває команду користувача на кольорові шматочки (для вводу/ехо)
    private final SyntaxTokenizer tokenizer = new SyntaxTokenizer();

    @FXML
    private TabPane tabPane;
    @FXML
    private MenuBar mainMenuBar;

    // ДОДАЄМО: DAO для роботи з базою
    private CommandLogDAO commandLogDAO;

    private SnippetDAO snippetDAO;

    // ДОДАЄМО: Trie для автодоповнення
    private Trie commandTrie;

    private UIFactory factory;

    @FXML
    public void initialize() {
        // 1. Ініціалізація інфраструктури
        commandLogDAO = new CommandLogDAO();
        snippetDAO = new SnippetDAO();
        DBConnection.getInstance();

        // 2. Ініціалізація Trie (один раз для всіх вкладок)
        commandTrie = new Trie();
        loadInitialCommands();

        // 3. Ініціалізація Меню
        factory = new SnippetUIFactory();
        buildMenuBar();

        // 4. Створення першої вкладки
        createNewTab();

        // 5. Гарячі клавіші (Global Shortcuts)
        // Ctrl+T = Нова вкладка
        // Ctrl+W = Закрити вкладку
        // Ctrl+Shift+T = Змінити тему
        tabPane.setOnKeyPressed(this::handleGlobalKeys);

        // Підписка на тему (щоб міняти стиль самого TabPane)
        ThemeManager.getInstance().subscribe(this::updateWindowStyle);
        updateWindowStyle();
    }

    // --- MENU & UI ---

    private void buildMenuBar() {
        mainMenuBar.getMenus().clear();
        Menu snippetMenu = factory.createSnippetMenu();

        MenuItem manageItem = factory.createSnippetMenuItem("Керування...", this::showManageSnippetsDialog);
        MenuItem addItem = factory.createSnippetMenuItem("Додати сніпет...", this::showAddSnippetDialog);

        snippetMenu.getItems().addAll(manageItem, addItem, new SeparatorMenuItem());

        List<Snippet> snippets = snippetDAO.findAll();
        if (snippets.isEmpty()) {
            snippetMenu.getItems().add(new MenuItem("Сніпетів немає"));
        } else {
            for (Snippet snippet : snippets) {
                Runnable action = () -> insertSnippetToCurrentTab(snippet.getCommandBody());
                snippetMenu.getItems().add(factory.createSnippetMenuItem(snippet.getTitle(), action));
            }
        }

        // Додаємо меню "Файл" для керування вкладками
        Menu fileMenu = new Menu("Файл");
        MenuItem newTabItem = new MenuItem("Нова вкладка (Ctrl+T)");
        newTabItem.setOnAction(e -> createNewTab());
        fileMenu.getItems().add(newTabItem);

        mainMenuBar.getMenus().addAll(fileMenu, snippetMenu);
    }

    private void insertSnippetToCurrentTab(String text) {
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        if (selectedTab instanceof TerminalTab) {
            ((TerminalTab) selectedTab).insertText(text);
        }
    }

    private void handleGlobalKeys(KeyEvent event) {
        if (event.isControlDown()) {
            if (event.getCode() == KeyCode.T) {
                if (event.isShiftDown()) {
                    ThemeManager.getInstance().toggleTheme(); // Ctrl+Shift+T -> Тема
                } else {
                    createNewTab(); // Ctrl+T -> Нова вкладка
                }
            } else if (event.getCode() == KeyCode.W) {
                closeCurrentTab(); // Ctrl+W -> Закрити вкладку
            }
        }
    }

    // --- TAB MANAGEMENT ---
    private void createNewTab() {
        int tabCount = tabPane.getTabs().size() + 1;
        TerminalTab newTab = new TerminalTab("Термінал " + tabCount, commandLogDAO, commandTrie);

        tabPane.getTabs().add(newTab);
        tabPane.getSelectionModel().select(newTab); // Фокус на нову вкладку

        // Фокус у поле вводу нової вкладки
        newTab.requestFocus();
    }

    private void closeCurrentTab() {
        Tab selectedItem = tabPane.getSelectionModel().getSelectedItem();
        if (selectedItem != null) {
            // Логіка закриття сесії є всередині setOnClosed у TerminalTab
            tabPane.getTabs().remove(selectedItem);
        }
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

    // Метод для зміни кольору фону вікна
    private void updateWindowStyle() {
        ColorTheme theme = ThemeManager.getInstance().getTheme();
        String hexColor = toHexString(theme.getBackgroundColor());

        // Фарбуємо фон TabPane
        tabPane.setStyle("-fx-background: " + hexColor + "; -fx-background-color: " + hexColor + ";");
    }

    // Допоміжний метод для конвертації кольору в HEX
    private String toHexString(Color color) {
        return String.format("#%02X%02X%02X",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
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

    public void shutdown() {
        // Проходимо по всіх відкритих вкладках
        for (javafx.scene.control.Tab tab : tabPane.getTabs()) {
            if (tab instanceof org.kpi.view.component.TerminalTab) {
                // Примусово закриваємо сесію кожної вкладки
                // (Ми використовуємо механізм події onClosed, який ми прописали в конструкторі TerminalTab)
                javafx.event.Event.fireEvent(tab, new javafx.event.Event(javafx.scene.control.Tab.CLOSED_EVENT));
            }
        }
    }
}