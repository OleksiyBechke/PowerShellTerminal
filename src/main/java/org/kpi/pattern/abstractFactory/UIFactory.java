package org.kpi.pattern.abstractFactory;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.MenuBar;

/**
 * Абстрактна фабрика для створення сімейства елементів керування GUI.
 */
public interface UIFactory {
    MenuBar createMenuBar();
    Menu createSnippetMenu();
    MenuItem createSnippetMenuItem(String title, Runnable action);
}