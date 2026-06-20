package ir.ac.kntu.library;

public class AudioBook extends DigitalItem {
    // for people who say 'I don't have time to read'
    private String narrator;
    private int durationMinutes;

    public AudioBook(String id, String ttl, String cat, int yr) {
        super(id, ttl, cat, yr);
    }

    @Override
    public String getItemType() {
        return "AUDIOBOOK";
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