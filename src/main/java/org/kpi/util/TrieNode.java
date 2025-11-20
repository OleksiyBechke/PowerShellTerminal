package org.kpi.util;

import lombok.Getter; // Потрібні імпорти
import lombok.Setter; // Потрібні імпорти
import java.util.HashMap;
import java.util.Map;

/**
 * Вузол Префіксного дерева (Trie).
 */
@Getter
@Setter // Додаємо, щоб Lombok згенерував setEndOfWord та setWord
public class TrieNode {

    // Поле children залишаємо final, його не можна змінити ззовні
    private final Map<Character, TrieNode> children = new HashMap<>();

    private boolean isEndOfWord = false;
    private String word; // Зберігаємо повне слово
}