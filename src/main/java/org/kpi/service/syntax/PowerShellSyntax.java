package org.kpi.service.syntax;

import java.util.regex.Pattern;

public class PowerShellSyntax {

    // 1. Рядки в лапках (пріоритет №1)
    private static final String STRING_PATTERN = "\"[^\"]*\"?|'[^']*'?";

    // 2. Параметри (починаються з -)
    private static final String PARAM_PATTERN = "-[a-zA-Z0-9_?]+";

    // 3. Пайп (розділювач команд)
    private static final String PIPE_PATTERN = "\\|";

    // 4. Будь-яке інше слово (набір символів, що не є пробілами)
    // Це може бути і команда, і аргумент - залежить від позиції
    private static final String GENERIC_WORD = "[^\\s|]+";

    // Групи:
    // 1: Рядок
    // 2: Параметр
    // 3: Пайп
    // 4: Просто слово (Generic)
    public static final Pattern TOKEN_PATTERN = Pattern.compile(
            "(" + STRING_PATTERN + ")" +
                    "|(" + PARAM_PATTERN + ")" +
                    "|(" + PIPE_PATTERN + ")" +
                    "|(" + GENERIC_WORD + ")"
    );
}