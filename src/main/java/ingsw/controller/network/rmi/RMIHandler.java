package ingsw.controller.network.rmi;

import ingsw.controller.RemoteController;
import ingsw.controller.network.commands.*;
import ingsw.exceptions.InvalidUsernameException;
import ingsw.model.RemoteSagradaGame;
import ingsw.model.User;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;

public class RMIHandler implements RequestHandler {
    private ResponseHandler rmiController;
    private RMIUserObserver rmiUserObserver;
    private RemoteSagradaGame sagradaGame;
    private RemoteController remoteController;
    private User user;

    /**
     * RMIHandler constructor which retrieves SagradaGame and sets
     *
     * @param rmiController
     * @param rmiUserObserver
     */
    RMIHandler(RMIController rmiController, RMIUserObserver rmiUserObserver) {
        try {
            this.sagradaGame = (RemoteSagradaGame) LocateRegistry.getRegistry().lookup("sagrada");
        } catch (RemoteException e) {
            System.err.println("Could not retrieve SagradaGame");
            e.printStackTrace();
        } catch (NotBoundException e) {
            System.err.println("NotBoundException: SagradaGame");
            e.printStackTrace();
        }
        this.rmiController = rmiController;
        this.rmiUserObserver = rmiUserObserver;
    }

    @Override
    public Response handle(LoginUserRequest loginUserRequest) {
        try {
            user = sagradaGame.loginUser(loginUserRequest.username, rmiUserObserver);
            return new LoginUserResponse(user, sagradaGame.getConnectedUsers());
        } catch (InvalidUsernameException | RemoteException e) {
            return new LoginUserResponse(null, -1);
        }
    }

    @Override
    public Response handle(ChosenPatternCardRequest chosenPatternCardRequest) {
        return null;
    }

    @Override
    public void handle(CreateMatchRequest createMatchRequest) {
        try {
            sagradaGame.createMatch(createMatchRequest.matchName);
        } catch (RemoteException e) {
            new CreateMatchResponse(null).handle(rmiController);
        }
    }
}