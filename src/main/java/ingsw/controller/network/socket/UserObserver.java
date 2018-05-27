package ingsw.controller.network.socket;

import ingsw.controller.network.Message;
import ingsw.controller.network.commands.*;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface UserObserver extends Remote {

    void onJoin(int numberOfConnectedUsers) throws RemoteException;

    void sendMessage(Message message) throws RemoteException;

    void receiveNotification(Notification notification) throws RemoteException;

    void activateTurnNotification(List<Boolean[][]> booleanListGrid) throws RemoteException;

    void sendResponse(Response response) throws RemoteException;
}
