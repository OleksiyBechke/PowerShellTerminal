package org.kpi.pattern.command;

public interface Command {
    void execute();
    // public void undo(); // Можна додати для функціоналу скасування
}