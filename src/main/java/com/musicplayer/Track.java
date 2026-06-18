package com.musicplayer;

import java.io.File;

public class Track {
    public enum Source {
        LOCAL_FILE,
        ONLINE_URL
    }
    
    private final String title;
    private final String uri;
    private final Source source;
    private final String format;
    
    public Track(String title, String uri, Source source, String format) {
        this.title = title;
        this.uri = uri;
        this.source = source;
        this.format = format;
    }
    
    public static Track fromFile(File file) {
        String name = file.getName();
        int dotIndex = name.lastIndexOf('.');
        String displayName = dotIndex > 0 ? name.substring(0, dotIndex) : name;
        String format = dotIndex > 0 ? name.substring(dotIndex + 1).toUpperCase() : "UNKNOWN";
        return new Track(displayName, file.toURI().toString(), Source.LOCAL_FILE, format);
    }
    
    public static Track fromUrl(String url, String customTitle) {
        String title = (customTitle == null || customTitle.isBlank())
            ? extractTitleFromUrl(url)
            : customTitle;
        return new Track(title, url, Source.ONLINE_URL, "STREAM");
    }
    
    private static String extractTitleFromUrl(String url) {
        try {
            String path = url;
            int queryIndex = path.indexOf('?');
            if (queryIndex > 0) {
                path = path.substring(0, queryIndex);
            }
            int slashIndex = path.lastIndexOf('/');
            String name = slashIndex >= 0 ? path.substring(slashIndex + 1) : path;
            return !name.isBlank() ? name : url;
        } catch (Exception e) {
            return url;
        }
    }
    
    public String getTitle() { return title; }
    public String getUri() { return uri; }
    public Source getSource() { return source; }
    public String getFormat() { return format; }
    
    @Override
    public String toString() {
        String prefix = source == Source.ONLINE_URL ? "🌐 " : "🎵 ";
        return prefix + title;
    }
}
