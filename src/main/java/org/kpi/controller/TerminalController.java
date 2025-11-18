package org.kpi.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.kpi.service.PowerShellSession;

public class TerminalController {

    @FXML
    private TextArea outputArea;

    @FXML
    private TextField inputField;

    private PowerShellSession session;

    @FXML
    public void initialize() {
        // Ініціалізуємо сесію
        session = new PowerShellSession();

        // Підписуємось на отримання тексту
        session.setOutputHandler(text -> {
            // Оновлюємо інтерфейс у головному потоці JavaFX
            Platform.runLater(() -> {
                outputArea.appendText(text + "\n");
            });
        });
    }

    @FXML
    public void onEnterPressed() {
        String command = inputField.getText();

        // Відображаємо введену команду
        outputArea.appendText("> " + command + "\n");

        // Виконуємо команду
        session.execute(command);

        // Очищаємо поле вводу
        inputField.clear();
    }

    public void shutdown() {
        if (session != null) {
            session.close();
        }
    }
}