package org.kpi;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.kpi.controller.TerminalController;

import java.io.IOException;

public class Main extends Application {

    private TerminalController controller;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/org/kpi/view/terminal.fxml"));
        Scene scene = new Scene(fxmlLoader.load());

        controller = fxmlLoader.getController();

        stage.setTitle("KPI PowerShell Terminal");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        if (controller != null) {
            controller.shutdown();
        }
        super.stop();
    }

    public static void main(String[] args) {
        launch();
    }
}