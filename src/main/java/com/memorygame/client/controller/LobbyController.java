package com.memorygame.client.controller;

import java.util.List;

import com.memorygame.client.NetworkClient;
import com.memorygame.client.SceneManager;
import com.memorygame.common.Message;
import com.memorygame.common.Player;
import com.memorygame.common.PlayerStatus;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
public class LobbyController {
    @FXML
    private TextField txtSearch;

    @FXML
    private Button btnOnline;

    @FXML
    private Button btnBusy;

    @FXML
    private TableView<Player> tblPlayers;

    @FXML
    private TableColumn <Player, String> colName;

    // @FXML
    // private TableColumn <Player, PlayerStatus> colStatus;

    @FXML
    private TableColumn <Player, Integer> colWins;
    
    @FXML
    private TableColumn <Player, Void> colAction;

    private SceneManager sceneManager;
    private NetworkClient networkClient;

    /** tblPlayers sẽ theo dõi danh sách này, khi thêm/xóa Player khỏi đây thì TableView sẽ tự động cập nhật */
    private ObservableList<Player> playerList = FXCollections.observableArrayList();

    private FilteredList<Player> filteredPlayerList;

    private PlayerStatus currentStatusFilter = PlayerStatus.ONLINE;

    @FXML
    public void initialize() {
        colName.setCellValueFactory(new PropertyValueFactory<>("username")); // Tất cả phải khớp với tên biến
        // colStatus.setCellValueFactory(new PropertyValueFactory<>("status")); // trong class Player
        colWins.setCellValueFactory(new PropertyValueFactory<>("totalWins"));
        setupActionColumn();

        // Lọc
        filteredPlayerList = new FilteredList<>(playerList, p -> true);

        // Gán danh sách cho TableView
        tblPlayers.setItems(filteredPlayerList);

        // Áp dụng bộ lọc ban đầu, mặc định là ONLINE
        applyFilter();
    }

    public void setupController(SceneManager sceneManager, NetworkClient networkClient) {
        this.sceneManager = sceneManager;
        this.networkClient = networkClient;
        networkClient.sendMessage(new Message("C_SQL_PLAYER", null));
    }


    @FXML
    private void showOnlinePlayers() {

    }

    @FXML
    private void showBusyPlayers() {

    }

    @FXML
    private void backToMenu() {
        sceneManager.showMainMenuScene();
    }

    @FXML
    private void handleSearchButton() {
        applyFilter();
    }

    @FXML
    private void reloadListPlayer() {
        networkClient.sendMessage(new Message("C_SQL_PLAYER", null));
    }

    // Gọi khi client nhận được S_ONLINE_LIST từ server
    public void updateOnlineList(List<Player> players) {
        Platform.runLater(() -> {
            playerList.clear();
            if (players != null) {
                playerList.addAll(players);
            }
        });
    }

    private void applyFilter() {
        String keyword = txtSearch.getText();
        final String lowerCaseKeyword = (keyword == null) ? "" : keyword.toLowerCase();
        
        filteredPlayerList.setPredicate(player -> {
            // boolean statusMatch = (player.getStatus() == this.currentStatusFilter);

            boolean keywordMatch;
            if (lowerCaseKeyword.isEmpty()) {
                keywordMatch = true;
            } else {
                keywordMatch = player.getUsername().toLowerCase().contains(lowerCaseKeyword);
            }

            return keywordMatch;
        });
    }

    /** Setup cột Hành động để hiển thị nút thách đấu */
    private void setupActionColumn() {
        Callback<TableColumn<Player, Void>, TableCell<Player, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<Player, Void> call(final TableColumn<Player, Void> param) {
                final TableCell<Player, Void> cell = new TableCell<>() {
                    private final Button btn = new Button("Thách đấu");
                    {
                        btn.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold; -fx-background-radius: 8;");

                        btn.setOnAction(event -> {
                            Player player = getTableView().getItems().get(getIndex());
                            handleChallengePlayer(player);
                        });
                    }

                    /** Table Column tự dùng, không cần gọi nó ở đâu khác */
                    @Override
                    public void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null); // Không hiển thị gì nếu hàng trống
                        } else {
                            Player player = getTableView().getItems().get(getIndex());
                            if (player.getStatus() != PlayerStatus.ONLINE) {
                                btn.setDisable(true);
                            } else {
                                btn.setDisable(false);
                            }

                            setGraphic(btn);
                        }
                    }
                };
                return cell;
            }
        };
        colAction.setCellFactory(cellFactory);
    }

    private void handleChallengePlayer(Player opponent) {
        sceneManager.showChallengeConfigScene(opponent);
    }
    // Hàm mới được thêm vào: 12h 10/11
    /*Hiển thị alert (được gọi từ SceneManager) */
    public void showInviteStatusAlert(String title, String content, AlertType type) {
        Platform.runLater(() -> {
            Alert a = new Alert(type, content, ButtonType.OK); 
            a.setTitle(title); 
            a.setHeaderText(null); 
            a.showAndWait();            
        });
    }
}
