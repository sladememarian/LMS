package ir.ac.kntu.gui.shell;

import java.util.function.Supplier;

import javafx.scene.Node;

// One sidebar entry: a label plus a lazy factory that builds the screen only
// when the item is selected, so screens aren't constructed until visited.
public final class NavItem {

    private final String label;
    private final Supplier<Node> contentFactory;

    public NavItem(String label, Supplier<Node> contentFactory) {
        this.label = label;
        this.contentFactory = contentFactory;
    }

    public String label() {
        return label;
    }

    public Node buildContent() {
        return contentFactory.get();
    }
}
