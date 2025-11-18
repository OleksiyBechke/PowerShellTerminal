package org.kpi.service;

import java.io.*;
import java.nio.charset.Charset;
import java.util.function.Consumer;

public class PowerShellSession {

    private Process process;
    private BufferedWriter commandWriter;
    private BufferedReader outputReader;
    private BufferedReader errorReader;
    private boolean isAlive;

    // Змінна для передачі тексту у вікно (Consumer приймає рядок і нічого не повертає)
    private Consumer<String> outputHandler;

    public PowerShellSession() {
        try {
            ProcessBuilder builder = new ProcessBuilder("powershell.exe", "-NoExit", "-Command", "-");
            builder.redirectErrorStream(false);

            this.process = builder.start();
            this.isAlive = true;

            Charset consoleCharset = Charset.forName("CP866");

            this.commandWriter = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), consoleCharset));
            this.outputReader = new BufferedReader(new InputStreamReader(process.getInputStream(), consoleCharset));
            this.errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream(), consoleCharset));

            startOutputListener();
            startErrorListener();

        } catch (IOException e) {
            throw new RuntimeException("Не вдалося запустити PowerShell", e);
        }
    }

    // Метод, щоб контролер міг підписатися на вивід
    public void setOutputHandler(Consumer<String> handler) {
        this.outputHandler = handler;
    }

    public void execute(String command) {
        if (!isAlive) return;
        try {
            commandWriter.write(command);
            commandWriter.newLine();
            commandWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startOutputListener() {
        new Thread(() -> {
            try {
                String line;
                while ((line = outputReader.readLine()) != null) {
                    // Якщо є підписник (вікно), віддаємо текст йому. Якщо ні - в консоль.
                    if (outputHandler != null) {
                        outputHandler.accept(line);
                    } else {
                        System.out.println("[PS]: " + line);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void startErrorListener() {
        new Thread(() -> {
            try {
                String line;
                while ((line = errorReader.readLine()) != null) {
                    if (outputHandler != null) {
                        outputHandler.accept("[ERROR] " + line);
                    } else {
                        System.err.println("[ERROR]: " + line);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void close() {
        isAlive = false;
        if (process != null) process.destroy();
    }
}