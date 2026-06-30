package ir.ac.kntu.generic;

import java.util.List;
import java.util.Scanner;

import ir.ac.kntu.interfaces.Displayable;
import ir.ac.kntu.util.ConsoleColor;
import ir.ac.kntu.util.ConsoleMenu;

public class Menu<T extends Displayable> {

    private final String title;
    private final List<T> options;

    public Menu(String title, List<T> options) {
        this.title = title;
        this.options = options;
    }

    public void render() {
        ConsoleMenu.banner(title);
        for (int idx = 0; idx < options.size(); idx++) {
            System.out.println(ConsoleColor.CYAN + (idx + 1) + ". "
                + ConsoleColor.RESET + options.get(idx).toDisplayString());
        }
        System.out.println(ConsoleColor.RED + "0. " + ConsoleColor.RESET + "Back");
    }

    public T select(Scanner scanner) {
        render();
        int choice = ConsoleMenu.readInt(scanner,
            ConsoleColor.BOLD + ConsoleColor.YELLOW + "Choice: " + ConsoleColor.RESET);
        if (choice < 1 || choice > options.size()) {
            return null;
        }
        return options.get(choice - 1);
    }

    public List<T> getOptions() {
        return options;
    }
}
