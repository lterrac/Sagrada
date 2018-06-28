package ingsw.controller.network.rmi;

import ingsw.controller.RemoteController;
import ingsw.controller.network.commands.*;
import ingsw.exceptions.InvalidUsernameException;
import ingsw.model.RemoteSagradaGame;
import ingsw.model.User;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public class RMIHandler implements RequestHandler {
    private String ipAddress;
    private ResponseHandler rmiController;
    private RMIUserObserver rmiUserObserver;
    private RemoteSagradaGame sagradaGame;
    private RemoteController remoteController;
    private User user;

    private final String RMI_SLASH ="rmi://";
    private final String RMI_PORT = ":1099/";

    /**
     * RMIHandler constructor which retrieves SagradaGame and sets
     *
     * @param rmiController
     * @param rmiUserObserver
     */
    RMIHandler(RMIController rmiController, RMIUserObserver rmiUserObserver, String ipAddress) {
        this.ipAddress = ipAddress;
        try {
            this.sagradaGame = (RemoteSagradaGame) Naming.lookup(rebindSagradaUrl(ipAddress));
        } catch (RemoteException | MalformedURLException e) {
            System.err.println("Could not retrieve SagradaGame");
            e.printStackTrace();
        } catch (NotBoundException e) {
            System.err.println("NotBoundException: SagradaGame");
            e.printStackTrace();
        }
        this.rmiController = rmiController;
        this.rmiUserObserver = rmiUserObserver;
    }

    private String rebindSagradaUrl(String ipAddress) {
        return RMI_SLASH + ipAddress + RMI_PORT + "sagrada";
    }

    private String rebindControllerUrl(String ipAddress, JoinMatchRequest joinMatchRequest) {
        return RMI_SLASH + ipAddress + RMI_PORT + joinMatchRequest.matchName;
    }

    private String rebindControllerUrl(String ipAddress, ReJoinMatchRequest reJoinMatchRequest) {
        return RMI_SLASH + ipAddress + RMI_PORT + reJoinMatchRequest.matchName;
    }

    @Override
    public Response handle(LoginUserRequest loginUserRequest) {
        try {
            user = sagradaGame.loginUser(loginUserRequest.username, rmiUserObserver);
        } catch (InvalidUsernameException | RemoteException e) {
            new LoginUserResponse(null).handle(rmiController);
        }

        return null;
    }

    @Override
    public Response handle(LogoutRequest logoutRequest) {
        try {
            sagradaGame.logoutUser(user.getUsername());
            return new LogoutResponse();
        } catch (RemoteException e) {
            return null;
        }
    }

    @Override
    public Response handle(CreateMatchRequest createMatchRequest) {
        try {
            sagradaGame.createMatch(createMatchRequest.matchName);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public Response handle(BundleDataRequest bundleDataRequest) {
        try {
            sagradaGame.sendBundleData(user.getUsername());
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public Response handle(MoveToolCardRequest moveToolCardRequest) {
        switch (moveToolCardRequest.toolCardType) {
            case GROZING_PLIERS:
                try {
                    remoteController.toolCardMove(((GrozingPliersRequest) moveToolCardRequest));
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
            case FLUX_REMOVER:
                try {
                    remoteController.toolCardMove((FluxRemoverRequest) moveToolCardRequest);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
            case FLUX_BRUSH:
                try {
                    remoteController.toolCardMove((FluxBrushRequest) moveToolCardRequest);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
            case GRINDING_STONE:
                try {
                    remoteController.toolCardMove((GrindingStoneRequest) moveToolCardRequest);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
            case COPPER_FOIL_BURNISHER:
                try {
                    remoteController.toolCardMove((CopperFoilBurnisherRequest) moveToolCardRequest);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
            case CORK_BACKED_STRAIGHT_EDGE:
                try {
                    remoteController.toolCardMove((CorkBackedStraightedgeRequest) moveToolCardRequest);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
            case LENS_CUTTER:
                try {
                    remoteController.toolCardMove((LensCutterRequest) moveToolCardRequest);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
            case EGLOMISE_BRUSH:
                try {
                    remoteController.toolCardMove((EglomiseBrushRequest) moveToolCardRequest);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
            case LATHEKIN:
                try {
                    remoteController.toolCardMove((LathekinRequest) moveToolCardRequest);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
            case RUNNING_PLIERS:
                try {
                    remoteController.toolCardMove((RunningPliersRequest) moveToolCardRequest);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
            case TAP_WHEEL:
                try {
                    remoteController.toolCardMove((TapWheelRequest) moveToolCardRequest);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;
        }
        return null;
    }

    @Override
    public Response handle(JoinMatchRequest joinMatchRequest) {
        try {
            sagradaGame.loginUserToController(joinMatchRequest.matchName, user.getUsername());
            try {
                remoteController = (RemoteController) Naming.lookup(rebindControllerUrl(ipAddress, joinMatchRequest));
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            return new JoinedMatchResponse(true);
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
            return new JoinedMatchResponse(false);
        }
    }

    @Override
    public Response handle(ReJoinMatchRequest reJoinMatchRequest) {
        try {
            sagradaGame.loginPrexistentPlayer(reJoinMatchRequest.matchName, user);
            remoteController = (RemoteController) Naming.lookup(rebindControllerUrl(ipAddress, reJoinMatchRequest));
        } catch (NotBoundException | RemoteException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            System.err.println("Error in URL String");
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Response handle(ChosenPatternCardRequest chosenPatternCardRequest) {
        try {
            remoteController.assignPatternCard(user.getUsername(), chosenPatternCardRequest.patternCard);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public Response handle(DraftDiceRequest draftDiceRequest) {
        try {
            remoteController.draftDice(user.getUsername());
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public Response handle(Ack ack) {
        try {
            remoteController.sendAck();
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public Response handle(PlaceDiceRequest placeDiceRequest) {
        try {
            remoteController.placeDice(placeDiceRequest.dice, placeDiceRequest.rowIndex, placeDiceRequest.columnIndex);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public Response handle(UseToolCardRequest useToolCardRequest) {
        try {
            remoteController.useToolCard(useToolCardRequest.toolCardName);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public Response handle(FinishedMatchesRequest finishedMatchesRequest) {

        try {
            sagradaGame.sendFinishedMatchesList(user.getUsername());
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public Response handle(ReadHistoryRequest readHistoryRequest) {

        try {
            sagradaGame.sendSelectedMatchHistory(user.getUsername(), readHistoryRequest.matchName);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public Response handle(EndTurnRequest endTurnRequest) {
        try {
            remoteController.endTurn(endTurnRequest.player);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return null;
    }
}