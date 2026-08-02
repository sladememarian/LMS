package ir.ac.kntu.gui.view;

import ir.ac.kntu.gui.Navigator;
import ir.ac.kntu.gui.View;
import ir.ac.kntu.gui.util.BrandAssets;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Boot splash shown before the login screen. It presents the Collaberry brand
 * on a black background and animates a purple progress bar to <em>simulate</em>
 * loading the data store from PostgreSQL, then hands off to {@link LoginView}.
 *
 * <p>Purely presentational — no backend work happens here; the bar is a timed
 * animation, not a real DB probe. It is wired only from {@code App.start()} so
 * the TestFX suites (which build views directly) are unaffected.</p>
 */
public class SplashView implements View {

    /** How long the simulated "loading from postgres" animation runs. */
    private static final Duration LOAD_TIME = Duration.seconds(2.4);

    private final VBox root = new VBox(18);
    private final ProgressBar progress = new ProgressBar(0);

    private Navigator navigator;
    private View next;

    public SplashView() {
        build();
    }

    /** Injected by {@code App}: where to go once the bar fills. */
    public void attachNavigator(Navigator navigator, View next) {
        this.navigator = navigator;
        this.next = next;
    }

    private void build() {
        root.setAlignment(Pos.CENTER);
        // Black background per the brief; kept inline so it needs no theme CSS.
        root.setStyle("-fx-background-color: black;");

        root.getChildren().add(buildLogo());

        progress.setPrefWidth(320);
        // Purple bar via -fx-accent (the property ProgressBar colours itself by).
        progress.setStyle("-fx-accent: #8B5CF6;");

        Label loading = new Label("loading from postgres");
        loading.setStyle("-fx-text-fill: #C4B5FD; -fx-font-size: 13px;");

        Label brand = new Label("Collaberry co.");
        brand.setStyle("-fx-text-fill: #3B82F6; -fx-font-size: 34px; -fx-font-weight: bold;");

        Label author = new Label("author:apmemarian");
        author.setStyle("-fx-text-fill: #3B82F6; -fx-font-size: 14px;");

        root.getChildren().addAll(progress, loading, brand, author);
    }

    /**
     * Loads the brand icon from {@code <project>/icon/Collaberry_groups.png}. If
     * the file is missing we simply omit the image rather than fail the splash.
     */
    private ImageView buildLogo() {
        ImageView view = new ImageView();
        Image icon = BrandAssets.loadIcon();
        if (icon != null) {
            view.setImage(icon);
        }
        view.setPreserveRatio(true);
        view.setFitHeight(180);
        return view;
    }

    /**
     * Starts the loading animation; when the bar reaches 100% the login screen
     * takes over. Call after the stage is shown.
     */
    public void start() {
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(progress.progressProperty(), 0)),
                new KeyFrame(LOAD_TIME, new KeyValue(progress.progressProperty(), 1)));
        timeline.setOnFinished(event -> {
            if (navigator != null && next != null) {
                navigator.switchTo(next);
            }
        });
        timeline.play();
    }

    @Override
    public Parent getRoot() {
        return root;
    }

    @Override
    public String title() {
        return "Loading";
    }
}
