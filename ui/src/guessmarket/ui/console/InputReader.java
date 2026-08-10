package guessmarket.ui.console;

import java.util.Scanner;

/**
 * Reads input from the console and refuses to return until it gets something
 * usable. The exercise requires that typing letters where a number is expected
 * never crashes the program, and this is where that is enforced -- once, rather
 * than at every call site.
 */
class InputReader {

    private final Scanner scanner;

    InputReader(Scanner scanner) {
        this.scanner = scanner;
    }

    String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    /** Reads a whole number within the given range, re-prompting until valid. */
    int readIntInRange(String prompt, int min, int max) {
        while (true) {
            String raw = readLine(prompt);
            if (raw.isEmpty()) {
                System.out.println("Nothing entered. Please type a number between " + min + " and " + max + ".");
                continue;
            }
            try {
                int value = Integer.parseInt(raw);
                if (value < min || value > max) {
                    System.out.println("Please enter a number between " + min + " and " + max + ". You entered " + value + ".");
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                System.out.println("\"" + raw + "\" is not a whole number. Please enter a number between " + min + " and " + max + ".");
            }
        }
    }

    /** Reads a positive whole number, re-prompting until valid. */
    long readPositiveLong(String prompt) {
        while (true) {
            String raw = readLine(prompt);
            if (raw.isEmpty()) {
                System.out.println("Nothing entered. Please type a positive whole number.");
                continue;
            }
            try {
                long value = Long.parseLong(raw);
                if (value <= 0) {
                    System.out.println("The amount must be greater than zero. You entered " + value + ".");
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                System.out.println("\"" + raw + "\" is not a whole number. Please enter a positive whole number.");
            }
        }
    }
}
