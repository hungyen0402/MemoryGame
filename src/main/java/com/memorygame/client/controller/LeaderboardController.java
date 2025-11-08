package com.memorygame.client.controller;

import java.util.List;

import com.memorygame.common.Player;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;

public class LeaderboardController {
    @FXML
    private Button btnReload;

    @FXML
    private TableView<Player> tblRank;

    @FXML
    private TableColumn<Player, Integer> colRank;

    @FXML
    private TableColumn<Player, String> colPlayer;

    @FXML
    private TableColumn<Player, Integer> colWins;

    private ObservableList<Player> playerList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colPlayer.setCellValueFactory(new PropertyValueFactory<>("username"));
        colWins.setCellValueFactory(new PropertyValueFactory<>("totalWins"));
        
        setupRankColumn();

        tblRank.setItems(playerList);

        loadLeaderboardData();
    }

    /**NetworkClient sẽ gọi nó để cập nhật BXH khi nhận được dữ liệu BXH */
    public void onLeaderboardDataReceived(List<Player> players) {
        Platform.runLater(() -> {
            playerList.clear();
            if (playerList != null) {
                playerList.addAll(players); // Không cần sắp xếp do câu lệnh sql đã sắp xếp sẵn rồi
            }
        });
    }

    @FXML
    private void reloadLeaderboard() {
        loadLeaderboardData();
    }

    @FXML
    private void backToMenu() {

    }

    /**Gửi yêu cầu đến server để lấy BXH */
    private void loadLeaderboardData() {

    }

    private void setupRankColumn() {
        colRank.setCellFactory(new Callback<TableColumn<Player, Integer>, TableCell<Player, Integer>>() {
            @Override
            public TableCell<Player, Integer> call(TableColumn<Player, Integer> param) {
                return new TableCell<Player, Integer>() {
                    @Override
                    protected void updateItem(Integer item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setText(null);
                        } else {
                            // STT của hàng + 1, do câu lệnh sql đã sắp xếp sẵn rồi
                            int rank = getIndex() + 1;
                            setText(String.valueOf(rank));
                        }
                    }
                };
            }
        });
    }
}
