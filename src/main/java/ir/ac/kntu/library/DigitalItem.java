package ir.ac.kntu.library;

public class DigitalItem extends LibraryItem {

    @Override
    public String getItemType() {
        return "DIGITAL";
    }

    private String downloadUrl;
    private long fileSize;

    public DigitalItem(String id, String title, String cat, int yr) {
        super(id, title, cat, yr);
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }
}