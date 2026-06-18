package com.musicplayer;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.Random;

public class PlaylistManager {
    private final ObservableList<Track> tracks = FXCollections.observableArrayList();
    private int currentIndex = -1;
    private boolean shuffleMode = false;
    private final Random random = new Random();
    
    public enum RepeatMode {
        OFF("不循环"),
        ONE("单曲循环"),
        ALL("列表循环");
        
        private final String label;
        RepeatMode(String label) { this.label = label; }
        
        @Override
        public String toString() { return label; }
    }
    
    public ObservableList<Track> getTracks() {
        return tracks;
    }
    
    public void add(Track track) {
        tracks.add(track);
    }
    
    public void removeAt(int index) {
        if (index < 0 || index >= tracks.size()) return;
        tracks.remove(index);
        if (currentIndex > index) {
            currentIndex--;
        } else if (currentIndex == index) {
            currentIndex = -1;
        }
    }
    
    public void clear() {
        tracks.clear();
        currentIndex = -1;
    }
    
    public boolean isEmpty() { return tracks.isEmpty(); }
    public int size() { return tracks.size(); }
    
    public int getCurrentIndex() { return currentIndex; }
    public void setCurrentIndex(int index) {
        if (index >= 0 && index < tracks.size()) {
            currentIndex = index;
        }
    }
    
    public Track getCurrent() {
        if (currentIndex < 0 || currentIndex >= tracks.size()) return null;
        return tracks.get(currentIndex);
    }
    
    public Track next() {
        if (tracks.isEmpty()) return null;
        if (shuffleMode && tracks.size() > 1) {
            int newIndex;
            do {
                newIndex = random.nextInt(tracks.size());
            } while (newIndex == currentIndex);
            currentIndex = newIndex;
        } else {
            currentIndex = (currentIndex + 1) % tracks.size();
        }
        return tracks.get(currentIndex);
    }
    
    public Track previous() {
        if (tracks.isEmpty()) return null;
        if (shuffleMode && tracks.size() > 1) {
            int newIndex;
            do {
                newIndex = random.nextInt(tracks.size());
            } while (newIndex == currentIndex);
            currentIndex = newIndex;
        } else {
            currentIndex = (currentIndex - 1 + tracks.size()) % tracks.size();
        }
        return tracks.get(currentIndex);
    }
    
    public void setShuffleMode(boolean shuffle) { this.shuffleMode = shuffle; }
    public boolean isShuffleMode() { return shuffleMode; }
}
