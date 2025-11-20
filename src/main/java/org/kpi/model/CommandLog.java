package org.kpi.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CommandLog {
    private int id;
    private String commandText;
    private LocalDateTime executedAt;

    public CommandLog(String commandText) {
        this.commandText = commandText;
        this.executedAt = LocalDateTime.now();
    }
}
