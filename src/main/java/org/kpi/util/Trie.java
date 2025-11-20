package org.kpi.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Префіксне дерево (Trie) для швидкого пошуку слів за префіксом.
 */
public class Trie {

    private final TrieNode root = new TrieNode();

    /**
     * Додає слово в Trie.
     */
    public void insert(String word) {
        TrieNode current = root;
        String lowerWord = word.toLowerCase(); // Зберігаємо у нижньому регістрі для пошуку
        for (char ch : lowerWord.toCharArray()) {
            current = current.getChildren().computeIfAbsent(ch, c -> new TrieNode());
        }
        current.setEndOfWord(true);
        current.setWord(word); // Зберігаємо оригінальний регістр
    }

    /**
     * Шукає всі слова, що починаються з даного префікса.
     */
    public List<String> searchByPrefix(String prefix) {
        List<String> results = new ArrayList<>();
        TrieNode current = root;
        String lowerPrefix = prefix.toLowerCase();

        // 1. Йдемо до кінця префікса
        for (char ch : lowerPrefix.toCharArray()) {
            TrieNode node = current.getChildren().get(ch);
            if (node == null) {
                return results; // Префікс не знайдено
            }
            current = node;
        }

        // 2. Збираємо всі слова з цього вузла і нижче
        collectAllWords(current, results);
        return results;
    }

    private void collectAllWords(TrieNode node, List<String> results) {
        if (node.isEndOfWord()) {
            results.add(node.getWord());
        }

        for (TrieNode child : node.getChildren().values()) {
            collectAllWords(child, results);
        }
    }
}