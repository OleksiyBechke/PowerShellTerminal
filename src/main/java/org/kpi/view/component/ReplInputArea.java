package org.kpi.view.component;

import javafx.application.Platform;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.kpi.service.syntax.PowerShellSyntax;
import org.kpi.service.syntax.SyntaxTokenizer;

import java.util.List;
import java.util.regex.Matcher;

public class ReplInputArea extends TextFlow {

    private final SyntaxTokenizer tokenizer = new SyntaxTokenizer();

    private final StringBuilder inputBuffer = new StringBuilder();
    private final Runnable onEnterAction;
    private final Line caret;

    public ReplInputArea(Runnable onEnterAction) {
        this.onEnterAction = onEnterAction;

        this.setStyle("-fx-background-color: transparent; -fx-padding: 5 0 5 0;");

        this.caret = new Line(0, 0, 0, 14);
        this.caret.setStroke(Color.WHITE);
        this.caret.setStrokeWidth(2);
        startCaretBlinking();

        this.setFocusTraversable(true);
        this.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                onEnterAction.run();
            } else if (event.getCode() == KeyCode.BACK_SPACE) {
                if (inputBuffer.length() > 0) {
                    inputBuffer.deleteCharAt(inputBuffer.length() - 1);
                    refreshContent();
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
    }

    private void refreshContent() {
        this.getChildren().clear();

        // 1. Малюємо Промт (PS User>)
        Text prompt = new Text("PS User> ");
        prompt.setFill(Color.LIGHTGREEN);
        prompt.setFont(Font.font("Consolas", 14));
        this.getChildren().add(prompt);

        // 2. Використовуємо ТОКЕНІЗАТОР для фарбування тексту
        // Передаємо false, бо prompt вже був надрукований
        List<Text> tokens = tokenizer.tokenize(inputBuffer.toString(), true);
        this.getChildren().addAll(tokens);

        // 3. Додаємо курсор
        this.getChildren().add(caret);
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
        return cmd;
    }

    private void startCaretBlinking() {
        Thread thread = new Thread(() -> {
            try {
                while (true) {
                    // Виконуємо зміну UI в FX потоці, щоб не було помилок
                    Platform.runLater(() -> caret.setVisible(!caret.isVisible()));
                    Thread.sleep(500);
                }
            } catch (InterruptedException ignored) {}
        });
        thread.setDaemon(true);
        thread.start();
    }
}