package org.kpi.pattern.abstractFactory;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;

/**
 * Конкретна фабрика: реалізує створення елементів меню для сніпетів.
 */
public class SnippetUIFactory implements UIFactory {

    @Override
    public MenuBar createMenuBar() {
        return new MenuBar();
    }

    @Override
    public Menu createSnippetMenu() {
        Menu snippetMenu = new Menu("Сніпети");
        snippetMenu.setStyle("-fx-font-weight: bold;");
        return snippetMenu;
    }

    @Override
    public MenuItem createSnippetMenuItem(String title, Runnable action) {
        MenuItem menuItem = new MenuItem(title);
        menuItem.setOnAction(e -> action.run());
        return menuItem;
    }
}