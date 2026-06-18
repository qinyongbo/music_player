package com.musicplayer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Optional;

public class MusicPlayerApp extends Application {
    private static final Logger logger = LoggerFactory.getLogger(MusicPlayerApp.class);
    
    private PlaybackEngine engine;
    private PlaylistManager playlist;
    private UIController uiController;
    
    @Override
    public void start(Stage primaryStage) {
        try {
            engine = new PlaybackEngine();
            playlist = new PlaylistManager();
            uiController = new UIController(engine, playlist);
            
            primaryStage.setTitle("音乐播放器 Pro");
            primaryStage.setWidth(900);
            primaryStage.setHeight(700);
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);
            
            Scene scene = uiController.createScene();
            primaryStage.setScene(scene);
            
            primaryStage.setOnCloseRequest(e -> {
                engine.shutdown();
                Platform.exit();
            });
            
            primaryStage.show();
            logger.info("应用启动成功");
        } catch (Exception e) {
            logger.error("应用启动失败", e);
            Platform.exit();
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}