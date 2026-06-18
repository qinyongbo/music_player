package com.musicplayer;

import javafx.beans.property.*;
import javafx.application.Platform;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class PlaybackEngine {
    private static final Logger logger = LoggerFactory.getLogger(PlaybackEngine.class);
    
    private static final String VLC_PATH;
    
    static {
        String vlcPath = System.getProperty("vlc.path");
        if (vlcPath == null || vlcPath.isEmpty()) {
            vlcPath = "D:\\Program Files\\VideoLAN\\VLC";
        }
        VLC_PATH = vlcPath;
        System.setProperty("jna.library.path", VLC_PATH);
        logger.info("VLC 路径: {}", VLC_PATH);
    }
    
    private MediaPlayerFactory factory;
    private MediaPlayer player;
    
    private final DoubleProperty progress = new SimpleDoubleProperty(0);
    private final StringProperty currentTimeText = new SimpleStringProperty("00:00");
    private final StringProperty totalTimeText = new SimpleStringProperty("00:00");
    private final BooleanProperty playing = new SimpleBooleanProperty(false);
    private final DoubleProperty buffering = new SimpleDoubleProperty(0);
    
    private Runnable onEndOfMedia;
    private Consumer<String> onError;
    private Consumer<Track> onPlayingTrack;
    
    private boolean userSeeking = false;
    private long totalDuration = 0;
    
    public PlaybackEngine() {
        initVLC();
    }
    
    private void initVLC() {
        try {
            factory = new MediaPlayerFactory();
            player = factory.mediaPlayers().newMediaPlayer();
            player.events().addMediaPlayerEventListener(new PlayerEventListener());
            logger.info("VLC 初始化成功");
        } catch (Exception e) {
            logger.error("VLC 初始化失败", e);
            if (onError != null) {
                onError.accept("VLC 初始化失败: " + e.getMessage() + "\n请检查 VLC 是否安装在: " + VLC_PATH);
            }
        }
    }
    
    private class PlayerEventListener extends MediaPlayerEventAdapter {
        @Override
        public void playing(MediaPlayer mediaPlayer) {
            Platform.runLater(() -> playing.set(true));
        }
        
        @Override
        public void paused(MediaPlayer mediaPlayer) {
            Platform.runLater(() -> playing.set(false));
        }
        
        @Override
        public void stopped(MediaPlayer mediaPlayer) {
            Platform.runLater(() -> {
                playing.set(false);
                progress.set(0);
                currentTimeText.set("00:00");
            });
        }
        
        @Override
        public void finished(MediaPlayer mediaPlayer) {
            Platform.runLater(() -> {
                playing.set(false);
                progress.set(0);
                if (onEndOfMedia != null) {
                    onEndOfMedia.run();
                }
            });
        }
        
        @Override
        public void error(MediaPlayer mediaPlayer) {
            Platform.runLater(() -> {
                playing.set(false);
                if (onError != null) {
                    onError.accept("播放错误: 无法加载媒体文件");
                }
            });
        }
        
        @Override
        public void buffering(MediaPlayer mediaPlayer, float newCache) {
            Platform.runLater(() -> buffering.set(newCache / 100.0));
        }
        
        @Override
        public void timeChanged(MediaPlayer mediaPlayer, long newTime) {
            if (!userSeeking && totalDuration > 0) {
                Platform.runLater(() -> {
                    currentTimeText.set(formatDuration(newTime));
                    progress.set((double) newTime / totalDuration);
                });
            }
        }
        
        @Override
        public void lengthChanged(MediaPlayer mediaPlayer, long newLength) {
            totalDuration = newLength;
            Platform.runLater(() -> totalTimeText.set(formatDuration(newLength)));
        }
    }
    
    public void play(Track track) {
        if (track == null || player == null) return;
        
        try {
            stop();
            
            String mrl = track.getUri();
            if (track.getSource() == Track.Source.LOCAL_FILE) {
                java.io.File file = new java.io.File(new java.net.URI(mrl));
                mrl = file.getAbsolutePath();
            }
            
            player.media().play(mrl);
            player.audio().setVolume((int) (100));
            
            if (onPlayingTrack != null) {
                Platform.runLater(() -> onPlayingTrack.accept(track));
            }
            
            logger.info("播放: {}", track.getTitle());
        } catch (Exception e) {
            logger.error("播放失败", e);
            playing.set(false);
            if (onError != null) {
                onError.accept("无法播放: " + e.getMessage());
            }
        }
    }
    
    public void togglePlayPause() {
        if (player != null) {
            if (playing.get()) {
                player.controls().pause();
            } else {
                player.controls().play();
            }
        }
    }
    
    public void stop() {
        if (player != null) {
            player.controls().stop();
            playing.set(false);
            progress.set(0);
            currentTimeText.set("00:00");
            totalDuration = 0;
        }
    }
    
    public void seekTo(double fraction) {
        if (player != null && totalDuration > 0) {
            long targetTime = (long) (fraction * totalDuration);
            player.controls().setTime(targetTime);
        }
    }
    
    public void setVolume(double v) {
        if (player != null) {
            player.audio().setVolume((int) (Math.max(0, Math.min(1, v)) * 100));
        }
    }
    
    public void shutdown() {
        try {
            if (player != null) {
                player.controls().stop();
                player.release();
            }
            if (factory != null) {
                factory.release();
            }
        } catch (Exception e) {
            logger.error("关闭引擎失败", e);
        }
    }
    
    private String formatDuration(long millis) {
        if (millis <= 0) return "00:00";
        int totalSeconds = (int) (millis / 1000);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
    
    public void setOnEndOfMedia(Runnable r) { this.onEndOfMedia = r; }
    public void setOnError(Consumer<String> c) { this.onError = c; }
    public void setOnPlayingTrack(Consumer<Track> c) { this.onPlayingTrack = c; }
    
    public DoubleProperty progressProperty() { return progress; }
    public StringProperty currentTimeTextProperty() { return currentTimeText; }
    public StringProperty totalTimeTextProperty() { return totalTimeText; }
    public BooleanProperty playingProperty() { return playing; }
    public DoubleProperty bufferingProperty() { return buffering; }
}
