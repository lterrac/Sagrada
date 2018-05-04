package ingsw.model;

import ingsw.controller.Controller;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;

public class SagradaGame extends UnicastRemoteObject implements RemoteSagradaGame {
    private static SagradaGame sagradaGameSingleton;
    Map<String, Controller> matchesByName; // List of all open matches
    Map<String, User> connectedUsers; // List of connected users

    private SagradaGame() throws RemoteException {
        super();
    }

    public static SagradaGame get() throws RemoteException {
        if (sagradaGameSingleton == null) {
            sagradaGameSingleton = new SagradaGame();
        }

        return sagradaGameSingleton;
    }

    public void createMatch(String matchName, int noOfPlayers) {

    }

    public void joinMatch(String matchName, int noOfPlayers) {

    }

    public void joinSagradaGame(String username) {
        connectedUsers.put(username, new User(username));
    }

}
