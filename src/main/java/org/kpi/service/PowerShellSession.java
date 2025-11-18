package org.kpi.service;

import java.io.*;
import java.nio.charset.Charset;

public class PowerShellSession {

    private Process process;
    private BufferedWriter commandWriter;
    private BufferedReader outputReader;
    private BufferedReader errorReader;
    private boolean isAlive;

    public PowerShellSession() {
        try {
            // Запускаємо процес PowerShell
            ProcessBuilder builder = new ProcessBuilder("powershell.exe", "-NoExit", "-Command", "-");

            // false означає, що помилки будуть йти окремим потоком (для майбутнього червоного кольору)
            builder.redirectErrorStream(false);

            this.process = builder.start();
            this.isAlive = true;

            // CP866 - кодування для коректного відображення кирилиці у Windows консолі
            Charset consoleCharset = Charset.forName("CP866");

            this.commandWriter = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), consoleCharset));
            this.outputReader = new BufferedReader(new InputStreamReader(process.getInputStream(), consoleCharset));
            this.errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream(), consoleCharset));

            // Запускаємо слухачів виводу
            startOutputListener();
            startErrorListener();

            System.out.println("--- PowerShell Session Started ---");

        } catch (IOException e) {
            throw new RuntimeException("Не вдалося запустити PowerShell", e);
        }
    }

    public void execute(String command) {
        if (!isAlive) return;
        try {
            commandWriter.write(command);
            commandWriter.newLine(); // Емуляція Enter
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
                    System.out.println("[PS]: " + line);
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
                    System.err.println("[ERROR]: " + line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void close() {
        isAlive = false;
        if (process != null) {
            process.destroy();
        }
    }
}