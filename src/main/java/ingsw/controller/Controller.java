package ingsw.controller;

import ingsw.model.GameManager;
import ingsw.model.Player;
import ingsw.model.User;
import ingsw.view.RemoteView;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Controller extends UnicastRemoteObject implements RemoteController {
    Map<String, NetworkTransmitter> networkTransmitterMap;
    private GameManager gameManager;
    private List<Player> playerList;
    private int joinedUsers;

    public Controller() throws RemoteException {
        super();
        playerList = new ArrayList<>();
    }

    public int getJoinedUsers() {
        return joinedUsers;
    }

    public void loginUser(User user, RemoteView remoteView) {
        playerList.add(new Player(user, remoteView));
        joinedUsers++;
        if (playerList.size() == 4) createMatch();
    }

    private void createMatch() {
        gameManager = new GameManager(playerList);
    }
}
