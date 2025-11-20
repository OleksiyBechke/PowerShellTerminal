package org.kpi.model;

import lombok.Data;

/**
 * Модель для зберігання шаблону команди (сніпета).
 */
@Data
public class Snippet {
    private int id;
    private String title;
    private String commandBody;
    private String description;
}