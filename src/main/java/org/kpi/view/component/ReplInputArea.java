package org.kpi.view.component;

import javafx.application.Platform;
import javafx.scene.control.ListView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Popup;
import org.kpi.pattern.strategy.ColorTheme;
import org.kpi.pattern.strategy.ThemeManager;
import org.kpi.service.syntax.SyntaxTokenizer;

import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

public class ReplInputArea extends TextFlow {

    private final SyntaxTokenizer tokenizer = new SyntaxTokenizer();

    private final StringBuilder inputBuffer = new StringBuilder();
    private final Runnable onEnterAction;
    private final Function<String, List<String>> suggestionSupplier;
    private final Line caret;

    private final Popup suggestionPopup;
    private final ListView<String> suggestionList;
    private final VBox popupContent;

    public ReplInputArea(Runnable onEnterAction, Function<String, List<String>> suggestionSupplier) {
        this.onEnterAction = onEnterAction;
        this.suggestionSupplier = suggestionSupplier;

        this.setStyle("-fx-background-color: transparent; -fx-padding: 5 0 5 0;");

        // 1. Отримуємо тему для ініціалізації
        ColorTheme theme = ThemeManager.getInstance().getTheme();

        this.caret = new Line(0, 0, 0, 14);
        // ВИКОРИСТОВУЄМО КОЛІР КУРСОРУ З ТЕМИ
        this.caret.setStroke(theme.getCursorColor());
        this.caret.setStrokeWidth(2);
        startCaretBlinking();

        // 1. Ініціалізація Popup
        suggestionList = new ListView<>();
        suggestionList.setPrefHeight(150);
        suggestionList.setMaxWidth(300);
        suggestionList.getStyleClass().add("suggestion-list");

        popupContent = new VBox(suggestionList);
        popupContent.getStyleClass().add("popup-container");

        suggestionPopup = new Popup();
        suggestionPopup.getContent().add(popupContent);
        suggestionPopup.setAutoHide(true);

        // --- НОВИЙ БЛОК: ВІДНОВЛЕННЯ ПІДКАЗОК ПРИ ОТРИМАННІ ФОКУСУ ---
        this.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) { // Якщо отримали фокус
                // Якщо є текст і це не повна команда - показуємо підказки знову
                if (inputBuffer.length() > 0 && getCurrentPrefix(inputBuffer.toString()).length() > 0) {
                    showSuggestions();
                }
            } else {
                // Якщо втратили фокус - ховаємо
                hideSuggestions();
            }
        });
        // ------------------------------------------------------------

        // Слухач сцени (для глобальної втрати фокусу вікном) залишаємо як страховку
        this.sceneProperty().addListener((obsScene, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.windowProperty().addListener((obsWindow, oldWindow, newWindow) -> {
                    if (newWindow != null) {
                        newWindow.focusedProperty().addListener((obsFocus, wasFocused, isFocused) -> {
                            if (!isFocused) {
                                hideSuggestions();
                            }
                        });
                    }
                });
            }
        });

        // ВИПРАВЛЕНО: Застосовуємо підказку ТІЛЬКИ при подвійному кліку
        suggestionList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                applySuggestion();
            }
        });

        // 2. Обробка клавіш у списку
        suggestionList.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.TAB) {
                applySuggestion();
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                hideSuggestions();
                Platform.runLater(this::requestFocus);
                event.consume();
            }
        });

        // Клік мишкою повертає фокус у поле вводу (щоб працював слухач focusedProperty)
        this.setOnMouseClicked(e -> this.requestFocus());

        // 3. Головна логіка KeyPress
        this.setFocusTraversable(true);
        this.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (suggestionPopup.isShowing()) {
                    applySuggestion();
                } else {
                    onEnterAction.run();
                }
                event.consume();
            } else if (event.getCode() == KeyCode.BACK_SPACE) {
                if (inputBuffer.length() > 0) {
                    inputBuffer.deleteCharAt(inputBuffer.length() - 1);
                    refreshContent();
                } else {
                    hideSuggestions();
                }
            } else if (event.getCode() == KeyCode.UP || event.getCode() == KeyCode.DOWN) {
                if (suggestionPopup.isShowing()) {
                    suggestionList.requestFocus();
                    event.consume();
                }
            } else if (event.getCode() == KeyCode.TAB) {
                if (suggestionPopup.isShowing()) {
                    applySuggestion();
                    event.consume();
                }
            }
        });

        this.setOnKeyTyped(event -> {
            String character = event.getCharacter();
            if (character.length() > 0 && !character.equals("\r") && !character.equals("\b") && !character.equals("\t")) {
                inputBuffer.append(character);
                refreshContent();
            }
        });

        refreshContent();

        // Підписуємось на зміну теми: коли тема міняється -> перемалювати текст
        org.kpi.pattern.strategy.ThemeManager.getInstance().subscribe(this::refreshContent);
    }

    private void applySuggestion() {
        String selected = suggestionList.getSelectionModel().getSelectedItem();
        if (selected == null && !suggestionList.getItems().isEmpty()) {
            selected = suggestionList.getItems().get(0);
        }

        if (selected == null) return;

        String currentInput = inputBuffer.toString();
        String currentPrefix = getCurrentPrefix(currentInput);

        int lastWordStart = currentInput.lastIndexOf(currentPrefix);
        if (lastWordStart == -1) {
            lastWordStart = currentInput.length() - currentPrefix.length();
        }

        String newCommand = currentInput.substring(0, lastWordStart) + selected + " ";

        inputBuffer.setLength(0);
        inputBuffer.append(newCommand);

        hideSuggestions();
        refreshContent();
        Platform.runLater(this::requestFocus);
    }

    private String getCurrentPrefix(String input) {
        if (input.trim().isEmpty()) return "";
        java.util.regex.Matcher matcher = Pattern.compile("(\\S+)$").matcher(input);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private void showSuggestions() {
        String input = inputBuffer.toString();
        String prefix = getCurrentPrefix(input);

        if (input.isEmpty() || input.endsWith(" ") || prefix.isEmpty()) {
            hideSuggestions();
            return;
        }

        List<String> suggestions = suggestionSupplier.apply(prefix);

        if (suggestions.isEmpty() || (suggestions.size() == 1 && suggestions.get(0).equalsIgnoreCase(prefix))) {
            hideSuggestions();
            return;
        }

        suggestionList.getItems().setAll(suggestions);
        suggestionList.getSelectionModel().select(0);

        double caretX = this.localToScreen(caret.getBoundsInParent()).getMinX();
        double caretY = this.localToScreen(caret.getBoundsInParent()).getMaxY();

        if (!suggestionPopup.isShowing()) {
            suggestionPopup.show(this.getScene().getWindow(), caretX, caretY + 5);
        } else {
            suggestionPopup.setX(caretX);
            suggestionPopup.setY(caretY + 5);
        }
    }

    private void hideSuggestions() {
        if (suggestionPopup.isShowing()) {
            suggestionPopup.hide();
        }
    }

    private void refreshContent() {
        this.getChildren().clear();

        // Отримуємо кольори з поточної теми
        ColorTheme theme = ThemeManager.getInstance().getTheme();
        Color promptColor = theme.getPromptColor();

        // НОВИЙ РЯДОК: ОНОВЛЮЄМО КОЛІР КУРСОРУ З ТЕМИ
        this.caret.setStroke(theme.getCursorColor());

        Text prompt = new Text("PS User> ");
        prompt.setFill(promptColor);
        prompt.setFont(Font.font("Consolas", 14));
        this.getChildren().add(prompt);

        List<Text> tokens = tokenizer.tokenize(inputBuffer.toString(), true);
        this.getChildren().addAll(tokens);

        this.getChildren().add(caret);

        Platform.runLater(() -> {
            // Викликаємо showSuggestions тільки якщо вікно має фокус
            if (this.isFocused() && inputBuffer.length() > 0) {
                showSuggestions();
            } else {
                hideSuggestions();
            }
        });
    }

    private void addTextNode(String content, Color color) {
        Text t = new Text(content);
        t.setFill(color);
        t.setFont(Font.font("Consolas", 14));
        this.getChildren().add(t);
    }

    public String getCommandAndClear() {
        String cmd = inputBuffer.toString();
        inputBuffer.setLength(0);
        refreshContent();
        hideSuggestions();
        return cmd;
    }

    private void startCaretBlinking() {
        Thread thread = new Thread(() -> {
            try {
                while (true) {
                    Platform.runLater(() -> caret.setVisible(!caret.isVisible()));
                    Thread.sleep(500);
                }
            } catch (InterruptedException ignored) {}
        });
        thread.setDaemon(true);
        thread.start();
    }
}