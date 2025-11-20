package org.kpi.pattern.command;

import org.kpi.service.PowerShellSession;
import org.kpi.dao.CommandLogDAO;
import org.kpi.model.CommandLog;

public class PowerShellExecuteCommand implements Command {

    private final PowerShellSession session;
    private final CommandLogDAO logDAO;
    private final String commandText;

    public PowerShellExecuteCommand(PowerShellSession session, CommandLogDAO logDAO, String commandText) {
        this.session = session;
        this.logDAO = logDAO;
        this.commandText = commandText;
    }

    @Override
    public void execute() {
        // 1. Збереження в базу (логіка Етапу 4)
        CommandLog log = new CommandLog(commandText);
        logDAO.save(log);

        // 2. Виконання
        session.execute(commandText);
    }
}