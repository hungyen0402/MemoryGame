package com.memorygame.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.memorygame.common.Player; 

public class Server {
    /**Server Singleton */
    private static final Server instance = new Server();

    private Map<String, ClientHandler> onlinePlayers;
    private Map<ClientHandler, Player> handlerToPlayer;
    private Map<Player, GameSession> playerToSession;

    private Server() {
        this.onlinePlayers = new ConcurrentHashMap<>();
        this.handlerToPlayer = new ConcurrentHashMap<>();
        this.playerToSession = new ConcurrentHashMap<>();
    }

    public static Server getInstance() {
        return instance;
    }

    public void startServer() {

    }

    public boolean handleLogin(String userName, String password, ClientHandler client) {
        return true;

    }

    public void handleInvite(Player inviter, String inviteUsername) {

    }

    public void createGameSession(Player player1, Player player2) {
        
    }

    public void broadcastOnlineList() {
        
    }
    
}