package ingsw.controller.network.socket;

import ingsw.controller.Controller;
import ingsw.controller.network.commands.*;
import ingsw.exceptions.InvalidUsernameException;
import ingsw.model.SagradaGame;
import ingsw.model.User;

import java.io.Serializable;
import java.rmi.RemoteException;

/**
 * Class that handles the request received from the ClientController and read by the ClientHandler
 */
public class ServerController implements RequestHandler, Serializable {
    private transient ClientHandler clientHandler;
    private final transient SagradaGame sagradaGame;
    private transient Controller controller;
    private User user;

    /**
     * Creates a new ServerController and set the ClientHandler
     * @param clientHandler ClientHandler
     */
    ServerController(ClientHandler clientHandler) {
        this.clientHandler = clientHandler;
        sagradaGame = SagradaGame.get();
    }

    /**
     * Method that handles a LoginUserRequest
     *
     * @param loginUserRequest request
     * @return a Response, if the user has been logged in successfully, that is going to be handled
     * by the ClientController, otherwise it returns a negative Response
     */
    @Override
    public Response handle(LoginUserRequest loginUserRequest) {
        try {
            user = sagradaGame.loginUser(loginUserRequest.username, clientHandler);
        } catch (InvalidUsernameException | RemoteException e) {
            return new LoginUserResponse(null);
        }

        return null;
    }

    /**
     * Method that handles LogoutRequest
     *
     * @param logoutRequest request
     * @return Returns a response and a boolean inside of it is set to true if the user logged out
     *          succesfully, otherwise it's false
     */
    @Override
    public Response handle(LogoutRequest logoutRequest) {
        try {
            sagradaGame.logoutUser(user.getUsername());
        } catch (RemoteException e) {
            return new LogoutResponse(false);
        }

        return new LogoutResponse(true);
    }

    /**
     * Method that handles CreateMatchRequest
     *
     * @param createMatchRequest request
     * @return Nothing, the response will be send from another method()
     */
    @Override
    public Response handle(CreateMatchRequest createMatchRequest) {
        try {
            sagradaGame.createMatch(createMatchRequest.matchName);
        } catch (RemoteException e) {
            System.err.println("Partita non disponibile");
            return new CreateMatchResponse(null);
        }

        return null;
    }

    /**
     * Method that handles JoinMatchRequest
     *
     * @param joinMatchRequest Request
     * @return Returns a response and a boolean inside of it is set to true if the user logged out
     *          succesfully, otherwise it's false
     */
    @Override
    public Response handle(JoinMatchRequest joinMatchRequest) {
        try {
            sagradaGame.loginUserToController(joinMatchRequest.matchName, user.getUsername());
            controller = sagradaGame.getMatchController(joinMatchRequest.matchName);
        } catch (RemoteException e) {
            return new JoinedMatchResponse(false);
        }

        return new JoinedMatchResponse(true);
    }

    /**
     * Method that handle ReJoinMatchRequest
     * It also re-attach the controller field to the actual one.
     *
     * @param reJoinMatchRequest Request
     * @return Response is sent when RemoteExecption is catched
     */
    @Override
    public Response handle(ReJoinMatchRequest reJoinMatchRequest) {
        try {
            sagradaGame.loginPrexistentPlayer(reJoinMatchRequest.matchName, user.getUsername());
            controller = sagradaGame.getMatchController(reJoinMatchRequest.matchName);
        } catch (RemoteException e) {
            return new LoginUserResponse(user);
        }

        return null;
    }

    /**
     * Method that handle ChosenPatternCardRequest
     *
     * @param chosenPatternCard Request
     * @return Nothing
     */
    @Override
    public Response handle(ChosenPatternCardRequest chosenPatternCard) {
        try {
            controller.assignPatternCard(user.getUsername(), chosenPatternCard.patternCard);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Method that handle DraftDiceRequest
     *
     * @param draftDiceRequest Request
     * @return Nothing
     */
    @Override
    public Response handle(DraftDiceRequest draftDiceRequest) {
        try {
            controller.draftDice(user.getUsername());
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Method that handle bundleDataRequest
     *
     * @param bundleDataRequest Request
     * @return Nothing
     */
    @Override
    public Response handle(BundleDataRequest bundleDataRequest) {
        try {
            sagradaGame.sendBundleData(user.getUsername());
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Method that handle every moveToolCardRequest
     *
     * @param moveToolCardRequest Request
     * @return Nothing
     */
    @Override
    public Response handle(MoveToolCardRequest moveToolCardRequest) {
        try {
            switch (moveToolCardRequest.toolCardType) {
                case GROZING_PLIERS:
                    controller.toolCardMove(((GrozingPliersRequest) moveToolCardRequest));
                    break;
                case FLUX_BRUSH:
                    controller.toolCardMove((FluxBrushRequest) moveToolCardRequest);
                    break;
                case FLUX_REMOVER:
                    controller.toolCardMove((FluxRemoverRequest) moveToolCardRequest);
                    break;
                case GRINDING_STONE:
                    controller.toolCardMove((GrindingStoneRequest) moveToolCardRequest);
                    break;
                case COPPER_FOIL_BURNISHER:
                    controller.toolCardMove((CopperFoilBurnisherRequest) moveToolCardRequest);
                    break;
                case CORK_BACKED_STRAIGHT_EDGE:
                    controller.toolCardMove((CorkBackedStraightedgeRequest) moveToolCardRequest);
                    break;
                case LENS_CUTTER:
                    controller.toolCardMove((LensCutterRequest) moveToolCardRequest);
                    break;
                case EGLOMISE_BRUSH:
                    controller.toolCardMove((EglomiseBrushRequest) moveToolCardRequest);
                    break;
                case LATHEKIN:
                    controller.toolCardMove((LathekinRequest) moveToolCardRequest);
                    break;
                case RUNNING_PLIERS:
                    controller.toolCardMove((RunningPliersRequest) moveToolCardRequest);
                    break;
                case TAP_WHEEL:
                    controller.toolCardMove((TapWheelRequest) moveToolCardRequest);
                    break;
                default:
                    break;
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Method that handle Ack
     *
     * @param ack Request
     * @return Nothing
     */
    @Override
    public Response handle(Ack ack) {
        try {
            controller.sendAck();
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return null;
    }


    /**
     * Method that handle placeDiceRequest
     *
     * @param placeDiceRequest Request
     * @return Nothing
     */
    @Override
    public Response handle(PlaceDiceRequest placeDiceRequest) {
        try {
            controller.placeDice(placeDiceRequest.dice, placeDiceRequest.rowIndex, placeDiceRequest.columnIndex);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Method that handle useToolCardRequest
     *
     * @param useToolCardRequest Request
     * @return Nothing
     */
    @Override
    public Response handle(UseToolCardRequest useToolCardRequest) {
        try {
            controller.useToolCard(useToolCardRequest.toolCardName);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Method that handle endTurnRequest
     *
     * @param endTurnRequest Request
     * @return Nothing
     */
    @Override
    public Response handle(EndTurnRequest endTurnRequest) {
        try {
            controller.endTurn(endTurnRequest.player);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Method that handle finishedMatchesRequest
     *
     * @param finishedMatchesRequest Request
     * @return Nothing
     */
    @Override
    public Response handle(FinishedMatchesRequest finishedMatchesRequest) {

        try {
            sagradaGame.sendFinishedMatchesList(user.getUsername());
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return null;
    }
    /**
     * Method that handle readHistoryRequest
     *
     * @param readHistoryRequest Request
     * @return Nothing
     */
    @Override
    public Response handle(ReadHistoryRequest readHistoryRequest) {

        try {
            sagradaGame.sendSelectedMatchHistory(user.getUsername(), readHistoryRequest.matchName);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Method that removes a user from the Server in case it did not join a match, otherwise it just deactivates it from
     * Sagrada and the match's Controller
     */
    public void deactivateUser() {
        try {
            sagradaGame.deactivateUser(user.getUsername());
        } catch (RemoteException e) {
            System.err.println("User already disconnected");
        }
    }
}
