package ir.ac.kntu.library;

import java.util.List;

public class AudioBook extends DigitalItem {
    // for people who say 'I don't have time to read'
    private static final int BORROW_PERIOD_DAYS = 21;

    private String narrator;
    private int durationMinutes;

    public AudioBook(String id, String ttl, String cat, int yr) {
        super(id, ttl, cat, yr);
    }

    @Override
    public String getItemType() {
        return "AUDIOBOOK";
    }

    @Override
    public int borrowPeriod() {
        return BORROW_PERIOD_DAYS;
    }

    @Override
    public String displayInfo() {
        return "Narrator: " + narrator + " (" + durationMinutes + " min)";
    }

    @Override
    public List<String> availableActions() {
        List<String> actions = baseActions();
        actions.add("Stream");
        return actions;
    }

    public String getNarrator() {
        return narrator;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setNarrator(String narrator) {
        this.narrator = narrator;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }
}