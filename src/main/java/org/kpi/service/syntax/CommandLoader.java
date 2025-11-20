package org.kpi.service.syntax;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Сервіс для синхронного отримання всіх команд та аліасів із PowerShell.
 * Використовує окремий, блокуючий процес Runtime.
 */
public class CommandLoader {

    public List<String> loadAllCommands() {
        List<String> commands = new ArrayList<>();

        // Команда: Отримати Cmdlets, Функції, Аліаси, вивести лише їхні назви
        // | ForEach-Object { $_.Name } забезпечує вивід одного слова на рядок
        String command = "Get-Command -CommandType Cmdlet, Function, Alias | Select-Object Name | ForEach-Object { $_.Name }";

        try {
            // 1. Запуск нового, тимчасового, блокуючого процесу
            // Використовуємо /c, щоб процес закрився після виконання команди
            Process process = Runtime.getRuntime().exec("powershell.exe /c " + command);

            // Використовуємо CP866 для Windows-сумісності з кирилицею
            Charset consoleCharset = Charset.forName("CP866");

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), consoleCharset))) {
                String line;

                // 2. Парсинг виводу (тут PowerShell виводить просто назви, по одній на рядок)
                while ((line = reader.readLine()) != null) {
                    String trimmedLine = line.trim();

                    // Ігноруємо заголовки та порожні рядки
                    if (!trimmedLine.isEmpty() && !trimmedLine.contains("Name") && !trimmedLine.startsWith("----")) {
                        commands.add(trimmedLine);
                    }
                }
            }

            // 3. Чекаємо завершення процесу (блокування)
            process.waitFor();

        } catch (Exception e) {
            System.err.println("Помилка динамічного завантаження команд: " + e.getMessage());
            // 4. Fallback: Якщо завантаження не вдалося, використовуємо мінімальний набір
            commands.add("dir");
            commands.add("cd");
            commands.add("exit");
            commands.add("Get-Process");
        }

        return commands;
    }
}