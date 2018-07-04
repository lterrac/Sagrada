package ingsw.controller.network.socket;

import ingsw.controller.network.commands.*;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

public interface UserObserver extends Remote {

    void onJoin(int numberOfConnectedUsers) throws RemoteException;

    void receiveNotification(Notification notification) throws RemoteException;

    void activateTurnNotification(Map<String,Boolean[][]> booleanMapGrid) throws RemoteException;

    void activatePinger();

    void sendResponse(Response response) throws RemoteException;

    void checkIfActive() throws RemoteException;

    void notifyVictory(int score) throws RemoteException;

    void notifyLost(int score) throws RemoteException;
}
