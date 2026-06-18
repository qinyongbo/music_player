package com.musicplayer;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Optional;

public class UIController {
    private static final Logger logger = LoggerFactory.getLogger(UIController.class);
    
    private final PlaybackEngine engine;
    private final PlaylistManager playlist;
    
    private ListView<Track> playlistView;
    private Label nowPlayingLabel;
    private Label currentTimeLabel;
    private Label totalTimeLabel;
    private Slider progressSlider;
    private Slider volumeSlider;
    private Button playPauseButton;
    private ToggleButton shuffleButton;
    private ComboBox<PlaylistManager.RepeatMode> repeatModeBox;
    private Label playStatusLabel;
    private ProgressBar bufferingBar;
    
    private boolean userIsDraggingSlider = false;
    private Stage primaryStage;
    
    public UIController(PlaybackEngine engine, PlaylistManager playlist) {
        this.engine = engine;
        this.playlist = playlist;
        wireCallbacks();
    }
    
    public Scene createScene() {
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1e1e1e; -fx-text-fill: #ffffff;");
        
        root.setTop(buildTopBar());
        root.setCenter(buildCenterArea());
        root.setBottom(buildBottomBar());
        
        return new Scene(root, 900, 700);
    }
    
    private Node buildTopBar() {
        VBox topBox = new VBox();
        topBox.setStyle("-fx-background-color: #2d2d2d; -fx-border-color: #444444; -fx-border-width: 0 0 1 0;");
        topBox.setPadding(new Insets(10));
        topBox.setSpacing(8);
        
        Label titleLabel = new Label("🎵 音乐播放器 Pro");
        titleLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #ffffff;");
        
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        
        Button addFileBtn = createStyledButton("📁 添加文件", e -> onAddLocalFiles());
        Button addFolderBtn = createStyledButton("📂 添加文件夹", e -> onAddLocalFolder());
        Button addUrlBtn = createStyledButton("🌐 添加网址", e -> onAddOnlineUrl());
        Button clearBtn = createStyledButton("🗑️ 清空列表", e -> onClearPlaylist());
        
        buttonBox.getChildren().addAll(addFileBtn, addFolderBtn, addUrlBtn, clearBtn);
        
        topBox.getChildren().addAll(titleLabel, buttonBox);
        return topBox;
    }
    
    private Node buildCenterArea() {
        HBox centerBox = new HBox();
        centerBox.setPadding(new Insets(10));
        centerBox.setSpacing(10);
        centerBox.setStyle("-fx-background-color: #1e1e1e;");
        
        playlistView = new ListView<>(playlist.getTracks());
        playlistView.setStyle("-fx-background-color: #2d2d2d; -fx-text-fill: #ffffff; -fx-control-inner-background: #2d2d2d;");
        playlistView.setPrefWidth(400);
        playlistView.setCellFactory(param -> new PlaylistCell());
        playlistView.setPlaceholder(new Label("播放列表为空\n请添加音乐文件或网址"));
        
        playlistView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                int idx = playlistView.getSelectionModel().getSelectedIndex();
                if (idx >= 0) {
                    playlist.setCurrentIndex(idx);
                    engine.play(playlist.getCurrent());
                }
            }
        });
        
        ContextMenu menu = new ContextMenu();
        MenuItem removeItem = new MenuItem("删除");
        removeItem.setOnAction(e -> {
            int idx = playlistView.getSelectionModel().getSelectedIndex();
            if (idx >= 0) {
                playlist.removeAt(idx);
            }
        });
        MenuItem clearAllItem = new MenuItem("清空全部");
        clearAllItem.setOnAction(e -> onClearPlaylist());
        menu.getItems().addAll(removeItem, new SeparatorMenuItem(), clearAllItem);
        playlistView.setContextMenu(menu);
        
        VBox infoPanel = buildInfoPanel();
        
        HBox.setHgrow(playlistView, Priority.ALWAYS);
        HBox.setHgrow(infoPanel, Priority.ALWAYS);
        
        centerBox.getChildren().addAll(playlistView, infoPanel);
        return centerBox;
    }
    
    private VBox buildInfoPanel() {
        VBox infoBox = new VBox();
        infoBox.setStyle("-fx-background-color: #2d2d2d; -fx-border-radius: 8; -fx-padding: 10;");
        infoBox.setSpacing(10);
        
        Label infoTitle = new Label("当前信息");
        infoTitle.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #ffffff;");
        
        playStatusLabel = new Label("未播放");
        playStatusLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #b0b0b0; -fx-wrap-text: true;");
        playStatusLabel.setMaxWidth(Double.MAX_VALUE);
        
        Label bufferingLabel = new Label("缓冲进度");
        bufferingLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #888888;");
        bufferingBar = new ProgressBar(0);
        bufferingBar.setPrefHeight(8);
        bufferingBar.setStyle("-fx-accent: #4CAF50;");
        
        Label statsLabel = new Label(String.format("列表歌曲数: %d", playlist.size()));
        statsLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #888888;");
        playlist.getTracks().addListener((obs, oldVal, newVal) -> {
            statsLabel.setText(String.format("列表歌曲数: %d", playlist.size()));
        });
        
        infoBox.getChildren().addAll(
            infoTitle,
            playStatusLabel,
            new Separator(),
            bufferingLabel,
            bufferingBar,
            new Separator(),
            statsLabel
        );
        
        return infoBox;
    }
    
    private Node buildBottomBar() {
        VBox bottomBox = new VBox();
        bottomBox.setStyle("-fx-background-color: #2d2d2d; -fx-border-color: #444444; -fx-border-width: 1 0 0 0;");
        bottomBox.setPadding(new Insets(10));
        bottomBox.setSpacing(8);
        
        nowPlayingLabel = new Label("未播放");
        nowPlayingLabel.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #4CAF50;");
        nowPlayingLabel.setMaxWidth(Double.MAX_VALUE);
        
        currentTimeLabel = new Label("00:00");
        currentTimeLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #888888; -fx-min-width: 40;");
        
        totalTimeLabel = new Label("00:00");
        totalTimeLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #888888; -fx-min-width: 40;");
        
        progressSlider = new Slider(0, 1, 0);
        progressSlider.setStyle("-fx-control-inner-background: #444444;");
        progressSlider.setOnMousePressed(e -> userIsDraggingSlider = true);
        progressSlider.setOnMouseReleased(e -> {
            engine.seekTo(progressSlider.getValue());
            userIsDraggingSlider = false;
        });
        HBox.setHgrow(progressSlider, Priority.ALWAYS);
        
        HBox progressBox = new HBox(8);
        progressBox.setAlignment(Pos.CENTER);
        progressBox.getChildren().addAll(currentTimeLabel, progressSlider, totalTimeLabel);
        
        HBox controlBox = new HBox(15);
        controlBox.setAlignment(Pos.CENTER);
        
        Button prevBtn = createPlayerButton("⏮", "上一曲", e -> onPrevious());
        playPauseButton = createPlayerButton("▶", "播放", e -> onPlayPause());
        Button stopBtn = createPlayerButton("⏹", "停止", e -> onStop());
        Button nextBtn = createPlayerButton("⏭", "下一曲", e -> onNext());
        
        shuffleButton = new ToggleButton("🔀");
        shuffleButton.setStyle("-fx-font-size: 14; -fx-padding: 8 12 8 12; -fx-background-color: #444444; -fx-text-fill: #ffffff; -fx-border-radius: 4;");
        shuffleButton.setOnAction(e -> playlist.setShuffleMode(shuffleButton.isSelected()));
        
        repeatModeBox = new ComboBox<>();
        repeatModeBox.getItems().addAll(
            PlaylistManager.RepeatMode.OFF,
            PlaylistManager.RepeatMode.ONE,
            PlaylistManager.RepeatMode.ALL
        );
        repeatModeBox.setValue(PlaylistManager.RepeatMode.ALL);
        repeatModeBox.setStyle("-fx-background-color: #444444; -fx-text-fill: #ffffff; -fx-border-radius: 4;");
        repeatModeBox.setPrefWidth(80);
        
        Label volumeLabel = new Label("🔊");
        volumeLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #ffffff;");
        
        volumeSlider = new Slider(0, 1, 0.7);
        volumeSlider.setPrefWidth(100);
        volumeSlider.setStyle("-fx-control-inner-background: #444444;");
        volumeSlider.valueProperty().addListener((obs, oldV, newV) ->
            engine.setVolume(newV.doubleValue())
        );
        
        controlBox.getChildren().addAll(
            prevBtn, playPauseButton, stopBtn, nextBtn,
            new Separator(),
            shuffleButton, repeatModeBox,
            new Separator(),
            volumeLabel, volumeSlider
        );
        
        bottomBox.getChildren().addAll(nowPlayingLabel, progressBox, controlBox);
        return bottomBox;
    }
    
    private Button createStyledButton(String text, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button btn = new Button(text);
        btn.setStyle("-fx-font-size: 12; -fx-padding: 8 12 8 12; -fx-background-color: #4CAF50; -fx-text-fill: #ffffff; -fx-border-radius: 4; -fx-cursor: hand;");
        btn.setOnAction(handler);
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-font-size: 12; -fx-padding: 8 12 8 12; -fx-background-color: #45a049; -fx-text-fill: #ffffff; -fx-border-radius: 4; -fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-font-size: 12; -fx-padding: 8 12 8 12; -fx-background-color: #4CAF50; -fx-text-fill: #ffffff; -fx-border-radius: 4; -fx-cursor: hand;"));
        return btn;
    }
    
    private Button createPlayerButton(String symbol, String tooltip, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        Button btn = new Button(symbol);
        btn.setStyle("-fx-font-size: 16; -fx-padding: 8 12 8 12; -fx-background-color: #444444; -fx-text-fill: #ffffff; -fx-border-radius: 4; -fx-cursor: hand;");
        btn.setTooltip(new Tooltip(tooltip));
        btn.setOnAction(handler);
        btn.setOnMouseEntered(e -> btn.setStyle("-fx-font-size: 16; -fx-padding: 8 12 8 12; -fx-background-color: #555555; -fx-text-fill: #ffffff; -fx-border-radius: 4; -fx-cursor: hand;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-font-size: 16; -fx-padding: 8 12 8 12; -fx-background-color: #444444; -fx-text-fill: #ffffff; -fx-border-radius: 4; -fx-cursor: hand;"));
        return btn;
    }
    
    private void wireCallbacks() {
        engine.progressProperty().addListener((obs, oldV, newV) -> {
            if (!progressSlider.isValueChanging() && !userIsDraggingSlider) {
                progressSlider.setValue(newV.doubleValue());
            }
        });
        
        engine.currentTimeTextProperty().addListener((obs, oldV, newV) ->
            currentTimeLabel.setText(newV)
        );
        
        engine.totalTimeTextProperty().addListener((obs, oldV, newV) ->
            totalTimeLabel.setText(newV)
        );
        
        engine.playingProperty().addListener((obs, oldV, newV) -> {
            playPauseButton.setText(newV ? "⏸" : "▶");
        });
        
        engine.bufferingProperty().addListener((obs, oldV, newV) ->
            bufferingBar.setProgress(newV.doubleValue())
        );
        
        engine.setOnPlayingTrack(track -> {
            nowPlayingLabel.setText("▶ " + track.getTitle());
            playStatusLabel.setText("正在播放: " + track.getTitle() + "\n格式: " + track.getFormat());
        });
        
        engine.setOnEndOfMedia(this::onTrackFinished);
        
        engine.setOnError(msg -> Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("播放错误");
            alert.setHeaderText("播放失败");
            alert.setContentText(msg);
            alert.showAndWait();
        }));
    }
    
    private void onAddLocalFiles() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("选择音乐文件");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
            "音频文件", "*.mp3", "*.wav", "*.m4a", "*.aac", "*.flac",
            "*.ape", "*.wma", "*.ogg", "*.opus", "*.alac"
        ));
        
        Stage stage = (Stage) playlistView.getScene().getWindow();
        List<File> files = chooser.showOpenMultipleDialog(stage);
        if (files != null) {
            for (File f : files) {
                playlist.add(Track.fromFile(f));
            }
            logger.info("添加了 {} 个本地文件", files.size());
        }
    }
    
    private void onAddLocalFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("选择音乐文件夹");
        
        Stage stage = (Stage) playlistView.getScene().getWindow();
        File dir = chooser.showDialog(stage);
        if (dir != null && dir.isDirectory()) {
            int count = 0;
            File[] files = dir.listFiles((d, name) -> {
                String lower = name.toLowerCase();
                return lower.matches(".*(mp3|wav|m4a|aac|flac|ape|wma|ogg|opus|alac)$");
            });
            if (files != null) {
                for (File f : files) {
                    playlist.add(Track.fromFile(f));
                    count++;
                }
            }
            logger.info("从文件夹添加了 {} 个音乐文件", count);
        }
    }
    
    private void onAddOnlineUrl() {
        Dialog<Track> dialog = new Dialog<>();
        dialog.setTitle("添加在线音乐URL");
        dialog.setHeaderText("输入音频直链地址（支持 http/https）");
        
        ButtonType addButtonType = new ButtonType("添加", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);
        dialog.getDialogPane().setStyle("-fx-background-color: #2d2d2d; -fx-text-fill: #ffffff;");
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 10, 10, 10));
        
        TextField urlField = new TextField();
        urlField.setPromptText("https://example.com/song.mp3");
        urlField.setPrefWidth(350);
        urlField.setStyle("-fx-background-color: #444444; -fx-text-fill: #ffffff; -fx-padding: 8;");
        
        TextField titleField = new TextField();
        titleField.setPromptText("可选，留空则自动从URL提取");
        titleField.setStyle("-fx-background-color: #444444; -fx-text-fill: #ffffff; -fx-padding: 8;");
        
        grid.add(new Label("音频URL:"), 0, 0);
        grid.add(urlField, 1, 0);
        grid.add(new Label("标题(可选):"), 0, 1);
        grid.add(titleField, 1, 1);
        
        dialog.getDialogPane().setContent(grid);
        
        Button addButton = (Button) dialog.getDialogPane().lookupButton(addButtonType);
        addButton.setDisable(true);
        urlField.textProperty().addListener((obs, oldV, newV) ->
            addButton.setDisable(newV.trim().isEmpty())
        );
        
        dialog.setResultConverter(buttonType -> {
            if (buttonType == addButtonType) {
                String url = urlField.getText().trim();
                String title = titleField.getText().trim();
                if (url.matches("https?://.*")) {
                    return Track.fromUrl(url, title);
                }
            }
            return null;
        });
        
        Optional<Track> result = dialog.showAndWait();
        result.ifPresent(track -> {
            playlist.add(track);
            logger.info("添加在线音乐: {}", track.getTitle());
        });
    }
    
    private void onClearPlaylist() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("确认");
        confirm.setHeaderText("清空播放列表");
        confirm.setContentText("确认要清空所有歌曲吗？此操作不可撤销。");
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            engine.stop();
            playlist.clear();
            nowPlayingLabel.setText("未播放");
            playStatusLabel.setText("未播放");
        }
    }
    
    private void onPlayPause() {
        if (playlist.getCurrent() == null) {
            if (!playlist.isEmpty()) {
                playlist.setCurrentIndex(0);
                engine.play(playlist.getCurrent());
            } else {
                showWarning("播放列表为空，请先添加音乐");
            }
        } else {
            engine.togglePlayPause();
        }
    }
    
    private void onStop() {
        engine.stop();
        playPauseButton.setText("▶");
    }
    
    private void onNext() {
        if (!playlist.isEmpty()) {
            Track t = playlist.next();
            if (t != null) {
                engine.play(t);
                playlistView.getSelectionModel().select(playlist.getCurrentIndex());
                playlistView.scrollTo(playlist.getCurrentIndex());
            }
        }
    }
    
    private void onPrevious() {
        if (!playlist.isEmpty()) {
            Track t = playlist.previous();
            if (t != null) {
                engine.play(t);
                playlistView.getSelectionModel().select(playlist.getCurrentIndex());
                playlistView.scrollTo(playlist.getCurrentIndex());
            }
        }
    }
    
    private void onTrackFinished() {
        PlaylistManager.RepeatMode mode = repeatModeBox.getValue();
        if (mode == PlaylistManager.RepeatMode.ONE) {
            engine.play(playlist.getCurrent());
        } else if (mode == PlaylistManager.RepeatMode.ALL) {
            onNext();
        }
    }
    
    private void showWarning(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("提示");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
    
    private static class PlaylistCell extends ListCell<Track> {
        @Override
        protected void updateItem(Track item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setStyle("");
            } else {
                setText(item.toString());
                setStyle("-fx-text-fill: #ffffff; -fx-font-size: 12; -fx-padding: 5;");
                if (isSelected()) {
                    setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 12; -fx-padding: 5; -fx-background-color: #444444;");
                }
            }
        }
    }
}
