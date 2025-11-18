package org.kpi;

import org.kpi.service.PowerShellSession;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        PowerShellSession session = new PowerShellSession();
        Scanner scanner = new Scanner(System.in);

        System.out.println("Термінал готовий. Введіть команду (наприклад 'dir'):");

        while (true) {
            String input = scanner.nextLine();

            if ("exit".equalsIgnoreCase(input)) {
                session.close();
                break;
            }

            session.execute(input);
        }
    }
}