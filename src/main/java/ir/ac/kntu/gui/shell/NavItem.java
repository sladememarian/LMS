package ir.ac.kntu.gui.shell;

import java.util.function.Supplier;

import javafx.scene.Node;

/**
 * One entry in the application sidebar: a label plus a lazy factory that builds
 * the content node when the item is selected. Content is built on demand so
 * screens are only constructed when actually visited.
 */
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
