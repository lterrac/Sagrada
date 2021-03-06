/*
 *
 * Un GameManager per ogni partita
 * Board passata da SagradaGame al momento della scelta o creazione della nuova partita o preesistente
 *
 * Contiene tutti gli oggetti del gioco perché si occupa del setup della Board e della creazione della stessa
 *
 */

package ingsw.model;

import com.google.gson.Gson;
import ingsw.controller.Controller;
import ingsw.controller.network.commands.*;
import ingsw.model.cards.patterncard.*;
import ingsw.model.cards.privateoc.*;
import ingsw.model.cards.publicoc.*;
import ingsw.model.cards.toolcards.*;
import ingsw.utilities.ControllerTimer;
import ingsw.utilities.MoveStatus;
import ingsw.utilities.PlayerBroadcaster;
import ingsw.utilities.Tuple;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Thread.sleep;

/**
 * Class that handles the entire game process and modifies the model itself
 */
public class GameManager {
    private Board board;
    private int maxTurnSeconds;
    private Round currentRound;
    private Controller controller;
    private List<Player> playerList;
    private List<MoveStatus> movesHistory;
    private List<PrivateObjectiveCard> privateObjectiveCards;
    private List<PublicObjectiveCard> publicObjectiveCards;
    private List<ToolCard> toolCards;
    private List<PatternCard> patternCards;
    private List<List<Dice>> roundTrack;
    private final AtomicInteger noOfAck;
    private final AtomicBoolean endRound;
    private final AtomicBoolean stop;
    private final AtomicBoolean doubleMove;
    private final AtomicBoolean cancelTimer;
    private final AtomicInteger turnInRound;
    private final AtomicBoolean toolCardLock;
    private Set<Player> disconnectedPlayers;
    private PlayerBroadcaster playerBroadcaster;
    private final AtomicBoolean endOfMatch;
    private final ControllerTimer controllerTimer;
    private Thread toolCardThread;
    private final AtomicBoolean patternCardsChosen;
    private final AtomicBoolean draftedDiceSet;
    private AtomicBoolean endGameDueToDisconnection;


    /**
     * Creates an instance of GameManager with every object needed by the game itself and initializes its players
     * assigning to each of them a PrivateObjectiveCard and asking them to choose a PatternCard.
     *
     * @param players         players that joined the match
     * @param maxTurnSeconds  max seconds that a user should use to complete a turn
     * @param controllerTimer Timer used to schedule the time for choosing pattern cards, drafting the dice and doing an entire turn
     */
    public GameManager(List<Player> players, int maxTurnSeconds, Controller controller, ControllerTimer controllerTimer) {
        playerList = players;
        this.controller = controller;
        roundTrack = new ArrayList<>();
        movesHistory = new LinkedList<>();
        stop = new AtomicBoolean(true);
        noOfAck = new AtomicInteger(0);
        endRound = new AtomicBoolean(false);
        doubleMove = new AtomicBoolean(false);
        cancelTimer = new AtomicBoolean(false);
        toolCardLock = new AtomicBoolean(false);
        turnInRound = new AtomicInteger(0);
        disconnectedPlayers = new HashSet<>();
        playerBroadcaster = new PlayerBroadcaster(players);
        endOfMatch = new AtomicBoolean(false);
        this.controllerTimer = controllerTimer;
        patternCardsChosen = new AtomicBoolean(false);
        this.maxTurnSeconds = maxTurnSeconds;
        draftedDiceSet = new AtomicBoolean(false);
        endGameDueToDisconnection = new AtomicBoolean(false);
        setUpGameManager();
    }


    /**
     * Method that will setup the GameManager.
     * This will populate the GameManager with every component needed for the game
     */
    private synchronized void setUpGameManager() {
        setUpPrivateObjectiveCards();

        for (Player player : playerList) {
            player.setPrivateObjectiveCard(privateObjectiveCards.get(0));
            privateObjectiveCards.remove(0);
        }

        setUpPublicObjectiveCards();
        setUpToolCards();
        setUpPatternCards();
    }

    /**
     * Method that populates the PatternCards in the List
     */
    private void setUpPatternCards() {
        this.patternCards = new LinkedList<>();
        this.patternCards.add(new AuroraeMagnificus());
        this.patternCards.add(new AuroraSagradis());
        this.patternCards.add(new Batllo());
        this.patternCards.add(new Bellesguard());
        this.patternCards.add(new ChromaticSplendor());
        this.patternCards.add(new Comitas());
        this.patternCards.add(new Firelight());
        this.patternCards.add(new Firmitas());
        this.patternCards.add(new FractalDrops());
        this.patternCards.add(new FulgorDelCielo());
        this.patternCards.add(new Gravitas());
        this.patternCards.add(new Industria());
        this.patternCards.add(new KaleidoscopicDream());
        this.patternCards.add(new LuxAstram());
        this.patternCards.add(new LuxMundi());
        this.patternCards.add(new LuzCelestial());
        this.patternCards.add(new RipplesOfLight());
        this.patternCards.add(new ShadowThief());
        this.patternCards.add(new SunCatcher());
        this.patternCards.add(new SunsGlory());
        this.patternCards.add(new SymphonyOfLight());
        this.patternCards.add(new ViaLux());
        this.patternCards.add(new Virtus());
        this.patternCards.add(new WaterOfLife());
        Collections.shuffle(patternCards);
    }

    /**
     * Method that populates the ToolCards in the List
     */
    private void setUpToolCards() {
        toolCards = new LinkedList<>();
        this.toolCards.add(new CopperFoilBurnisher());
        this.toolCards.add(new CorkBackedStraightEdge());
        this.toolCards.add(new EglomiseBrush());
        this.toolCards.add(new FluxBrush());
        this.toolCards.add(new FluxRemover());
        this.toolCards.add(new GlazingHammer());
        this.toolCards.add(new GrindingStone());
        this.toolCards.add(new GrozingPliers());
        this.toolCards.add(new Lathekin());
        this.toolCards.add(new LensCutter());
        this.toolCards.add(new RunningPliers());
        this.toolCards.add(new TapWheel());

    }

    /**
     * Method that populates the PublicObjectiveCards in the List
     */
    private void setUpPublicObjectiveCards() {
        publicObjectiveCards = new LinkedList<>();
        this.publicObjectiveCards.add(new ColorDiagonals());
        this.publicObjectiveCards.add(new ColorVariety());
        this.publicObjectiveCards.add(new ColumnShadeVariety());
        this.publicObjectiveCards.add(new DeepShades());
        this.publicObjectiveCards.add(new ColumnColorVariety());
        this.publicObjectiveCards.add(new LightShades());
        this.publicObjectiveCards.add(new MediumShades());
        this.publicObjectiveCards.add(new RowColorVariety());
        this.publicObjectiveCards.add(new RowShadeVariety());
        this.publicObjectiveCards.add(new ShadeVariety());
    }

    /**
     * Method that populates the PrivateObjectiveCards in the List
     */
    private void setUpPrivateObjectiveCards() {
        privateObjectiveCards = new LinkedList<>();
        this.privateObjectiveCards.add(new PrivateObjectiveCard(Color.BLUE));
        this.privateObjectiveCards.add(new PrivateObjectiveCard(Color.GREEN));
        this.privateObjectiveCards.add(new PrivateObjectiveCard(Color.RED));
        this.privateObjectiveCards.add(new PrivateObjectiveCard(Color.PURPLE));
        this.privateObjectiveCards.add(new PrivateObjectiveCard(Color.YELLOW));
        Collections.shuffle(privateObjectiveCards);
    }

    /**
     * Method that will randomly pick three ToolCards that will be used throughout the game
     *
     * @return three randomly picked ToolCards
     */
    private List<ToolCard> chooseToolCards() {
        Collections.shuffle(toolCards);
        return new ArrayList<>(toolCards.subList(0, 3));
    }

    /**
     * Method that will randomly pick three PublicObjectiveCards that will be used throughout the game
     *
     * @return three randomly picked PublicObjectiveCards
     */
    private List<PublicObjectiveCard> choosePublicObjectiveCards() {
        Collections.shuffle(publicObjectiveCards);
        return new ArrayList<>(publicObjectiveCards.subList(0, 3));
    }

    /**
     * Method that will distribute four PatternCards to each Player
     */
    public Map<String, List<PatternCard>> pickPatternCards() {
        HashMap<String, List<PatternCard>> patternCardToChoose = new HashMap<>();
        for (Player player : playerList) {

            try {
                //Need to create another List: subList is not Serializable
                List<PatternCard> patternCardArrayList = new ArrayList<>(patternCards.subList(0, 4));
                player.getUserObserver().sendResponse(new PatternCardNotification(patternCardArrayList));
                patternCardToChoose.put(player.getPlayerUsername(), patternCardArrayList);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            for (int i = 0; i < 4; i++) {
                patternCards.remove(0);
            }
        }
        return patternCardToChoose;
    }

    /**
     * Returns the number of the current Round
     *
     * @return Current round
     */
    public int getNoOfCurrentRound() {
        return roundTrack.size() + 1;
    }

    /**
     * Returns the round track
     *
     * @return Current round track
     */
    public List<List<Dice>> getRoundTrack() {
        return roundTrack;
    }

    /**
     * Deletes the match. Users can't join it anymore
     */
    private void deleteMatch() {
        controller.removeMatch();
        SagradaGame.get().writeUsersStatsToFile();
    }

    /**
     * Method that checks if every user is connected to the game.
     *
     * @param disconnectedPlayers A set in which there are all the disconnected players
     */
    private void checkUserConnection(Set<Player> disconnectedPlayers) {
        for (Player player : playerList) {
            try {
                // If there are at least two active players then...
                if (disconnectedPlayers.size() < playerList.size() - 1) {

                    // If a user was in the disconnectedPlayers' Set and it's now active
                    // He gets removed from the set and the necessary data will be notified to him
                    if (disconnectedPlayers.contains(player) && player.getUser().isActive() && player.getUser().isReady()) {
                        System.out.println("User: " + player.getPlayerUsername() + " is back online! ---> Sending data");
                        disconnectedPlayers.remove(player);
                        player.getUserObserver().sendResponse(new BoardDataResponse(playerList, board.getPublicObjectiveCards(), board.getToolCards(), roundTrack));
                        player.getUserObserver().sendResponse(new MoveStatusNotification(movesHistory));

                        sleep(500);
                        player.getUserObserver().sendResponse(new DraftedDiceResponse(board.getDraftedDice()));
                    } else if (!disconnectedPlayers.contains(player) && !player.getUser().isActive()) {
                        System.out.println("User " + player.getPlayerUsername() + " has disconnected, adding it to disconnected Users iterating Player " + player.getPlayerUsername() + " " + disconnectedPlayers.size() + " " + (playerList.size() - 1));
                        addMoveToHistoryAndNotify(new MoveStatus(player.getPlayerUsername(), "Has disconnected"));
                        disconnectedPlayers.add(player);

                    } else if (!disconnectedPlayers.contains(player) && player.getUser().isActive()) {
                        // Check if the User is disconnected or not
                        // If it's disconnected the catch block will handle the disconnection
                        player.getUserObserver();
                    }

                    // If there's only a user connected then...
                } else {

                    endGameDueToDisconnection.set(true);

                    stop.set(true);

                    playerBroadcaster.disableBroadcaster();

                    closeThreads();

                    synchronized (endOfMatch) {
                        endOfMatch.notifyAll();
                    }

                    break;
                }

            } catch (RemoteException e) {
                // If a RMI user disconnects, this code will execute
                System.out.println("RMI User " + player.getPlayerUsername() + " disconnected");
                player.getUser().setActive(false);
                player.getUser().setReady(false);
                disconnectedPlayers.add(player);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        }
    }

    /**
     * Method used to close all the active game threads (match thread, round thread and turn thread) and also the active turn timer.
     *
     * @throws InterruptedException
     */
    private void closeThreads() throws InterruptedException {
        while (!endOfMatch.get()) {

            controllerTimer.cancelTimer();

            stopTurn();

            synchronized (currentRound.hasPlayerEndedTurn()) {
                if (currentRound != null) {
                    currentRound.setPlayerEndedTurn(true);
                    currentRound.hasPlayerEndedTurn().wait(1000);
                }
            }

            synchronized (endRound) {
                endRound.set(true);
                endRound.notifyAll();
            }
        }
    }

    /**
     * Method that will check every two seconds which player is active and those who has disconnected from the game.
     * This method is especially handy for checking RMI users disconnection since when a RMI user disconnects you
     * don't get an immediate Exception.
     */
    private void listenForPlayerDisconnection() {
        stop.set(false);
        new Thread(() -> {
            try {
                do {
                    // PING every 2 seconds
                    sleep(5000);

                    checkUserConnection(disconnectedPlayers);

                } while (!stop.get());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Method that return the current players as a List
     *
     * @return current Players List
     */
    List<Player> getPlayerList() {
        return playerList;
    }

    /**
     * Method that will assign the chosen pattern card to the player
     *
     * @param username    player who chose the pattern card
     * @param patternCard pattern card chosen by the player
     */
    public void setPatternCardForPlayer(String username, PatternCard patternCard) {
        synchronized (patternCardsChosen) {
            if (noOfAck.get() >= 0 && !patternCardsChosen.get()) {
                for (Player player : playerList) {
                    if (player.getPlayerUsername().equals(username)) {
                        player.setPatternCard(patternCard);
                        receiveAck();
                        synchronized (noOfAck) {
                            noOfAck.notifyAll();
                        }
                    }
                }
            }
        }
    }

    /**
     * Method that will return the dice drafted at the beginning of the current round
     *
     * @return dice drafted in the Board at the beginning of the current round
     */
    public List<Dice> getDraftedDice() {
        return board.getDraftedDice();
    }

    /**
     * Method that returns the current round's number
     *
     * @return round number
     */
    public int getTurnInRound() {
        return turnInRound.get();
    }

    /**
     * Method that waits for every users to choose a patternCard
     */
    public void waitForEveryPatternCard(Map<String, List<PatternCard>> patternCardToChoose) {
        new Thread(() -> {
            controllerTimer.startPatternCardTimer(30, this, patternCardToChoose);
            waitAck();

            synchronized (patternCardsChosen) {
                if (noOfAck.get() == playerList.size() && !patternCardsChosen.get()) {
                    controllerTimer.cancelTimer();
                    resetAck();
                    patternCardsChosen.set(true);
                    setBoardAndStartMatch();
                }
            }
        }).start();
    }

    /**
     * Method that choose the pattern card of the player who didn't choose in time randomly
     * Then it starts the match only if the player size is greater than 2
     *
     * @param patternCardToChoose map of username and a list of their four pattern cards from wich
     *                            he has to choose one
     */
    public void randomizePatternCards(Map<String, List<PatternCard>> patternCardToChoose) {
        synchronized (patternCardsChosen) {

            if (!patternCardsChosen.get()) {
                patternCardsChosen.set(true);

                for (Player player : playerList) {
                    if (player.getPatternCard() == null) {
                        Collections.shuffle(patternCardToChoose.get(player.getPlayerUsername()));
                        player.setPatternCard(patternCardToChoose.get(player.getPlayerUsername()).get(0));
                    }
                }

                //Before starting the match
                if (playerList.size() > 1)
                    setBoardAndStartMatch();
                else {
                    try {
                        closeThreads();
                        deleteMatch();
                        return;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }


                resetAck();

                synchronized (noOfAck) {
                    noOfAck.notifyAll();
                }
            }
        }
    }

    /**
     * Method that sends all the static data (players' pattern card, Public Objective cards and Tool cards) to the players,
     * starts the thread which listens for the players disconnection and starts the match.
     */
    private void setBoardAndStartMatch() {
        BoardDataResponse boardDataResponse = new BoardDataResponse(playerList,
                                                                    choosePublicObjectiveCards(),
                                                                    chooseToolCards(),
                                                                    roundTrack);
        playerBroadcaster.broadcastResponseToAll(boardDataResponse);
        this.board = new Board(boardDataResponse.publicObjectiveCards, boardDataResponse.toolCards);
        listenForPlayerDisconnection();
        startMatch();
    }

    /**
     * Method that is used to keep track of how many users chose their pattern card
     */
    private void waitAck() {
        synchronized (noOfAck) {
            while (noOfAck.get() < playerList.size()) {
                try {
                    noOfAck.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Method that drafts the dice from the board and sends them to every user view
     */
    public void draftDiceFromBoard() {
        synchronized (draftedDiceSet) {

            if (!draftedDiceSet.get()) {
                controllerTimer.cancelTimer();
                playerBroadcaster.broadcastResponseToAll(board.draftDice(playerList.size()));
                addMoveToHistoryAndNotify(new MoveStatus(playerList.get(0).getPlayerUsername(), "Drafted dice"));
                waitForDiceAck();
                draftedDiceSet.set(true);
            }
        }
    }

    /**
     * Method that stalls the program until every user has received every dice
     */
    private void waitForDiceAck() {
        new Thread(() -> {
            System.out.println("Waiting Dice Ack");
            waitAck();
            resetAck();
            startRound();
        }).start();
    }

    /**
     * Method that places a dice in the player's pattern card.
     *
     * @param dice        Dice to place
     * @param rowIndex    Line index
     * @param columnIndex Column index
     */
    public void placeDiceForPlayer(Dice dice, int rowIndex, int columnIndex) {
        for (Dice diceInDraftedDice : board.getDraftedDice()) {
            if (diceInDraftedDice.getDiceColor().equals(dice.getDiceColor())
                    && (diceInDraftedDice.getFaceUpValue() == dice.getFaceUpValue())) {
                currentRound.makeMove(diceInDraftedDice, rowIndex, columnIndex);
                break;
            }
        }
    }

    /**
     * Method called when end turn button is clicked and if the button
     * is pressed by the host which is the current player in that moment.
     * Deletes the timer, sends an <code>EndTurnResponse</code>
     * to the current player and makes the turn thread terminate gracefully.
     *
     * @param currentPlayer Player username to check the identity of the host who wants
     *                      to end the turn.
     */
    public void endTurn(String currentPlayer) {
        if (currentPlayer.equals(currentRound.getCurrentPlayer().getPlayerUsername())) {
            controllerTimer.cancelTimer();
            currentRound.avoidEndTurnNotification(true);
            stopTurn();
        }
    }

    /**
     * Method triggered when the turn ends
     * Stops the turn of the current player in the server
     * and if it is active, stops the toolcard move execution
     */
    public void stopTurn() {
        addMoveToHistoryAndNotify(new MoveStatus(currentRound.getCurrentPlayer().getPlayerUsername(), "Ended turn"));
        currentRound.setPlayerEndedTurn(true);

        if (toolCardThread != null && toolCardThread.isAlive()) {
            toolCardLock.set(false);
            wakeUpToolCardThread();
        }
    }

    /**
     * Method that resets the received acks to zero
     */
    private void resetAck() {
        noOfAck.set(0);
    }

    /**
     * Method that increments the acks received
     */
    public synchronized void receiveAck() {
        noOfAck.getAndIncrement();
        synchronized (noOfAck) {
            noOfAck.notifyAll();
        }
    }

    /**
     * Method that alerts the user to draft, this activates a button "Draft" in the view
     *
     * @param player player to be notified
     */
    private void notifyDraftToPlayer(Player player) {
        player.notifyDraft();
        controllerTimer.startDraftedDiceTimer(this);
    }

    /**
     * Method that opens a thread dedicated to the match
     */
    private void startMatch() {
        new Thread(() -> {

            try {
                sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }

            int i = 0;
            while (i < 10) {
                if (disconnectedPlayers.size() != (playerList.size() - 1)) {
                    if (playerList.get(0).getUser().isActive()) {

                        addMoveToHistoryAndNotify(new MoveStatus(playerList.get(0).getPlayerUsername(), "starts round " + i));

                        notifyDraftToPlayer(playerList.get(0));
                        endRound.set(false);

                        System.out.println("Round " + i);
                        //wait until the end of the round
                        synchronized (endRound) {
                            while (!endRound.get()) {
                                try {
                                    endRound.wait();
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                    e.printStackTrace();
                                }
                            }
                        }
                    } else {
                        shiftPlayerList();
                    }
                }
                addMoveToHistoryAndNotify(new MoveStatus(playerList.get(0).getPlayerUsername(), "ended round " + i));
                i++;
            }

            endOfMatch.set(true);

            if (endGameDueToDisconnection.get()) {
                synchronized (endOfMatch) {
                    try {
                        endOfMatch.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        e.printStackTrace();
                    }
                }
            }


            addMoveToHistoryAndNotify(new MoveStatus(currentRound.getCurrentPlayer().getPlayerUsername(), "ended the match"));


            assignPointsToPlayers();

            notifyWinner();

            writeHistoryToFile(movesHistory);

            stop.set(true);

            deleteMatch();

        }).start();
    }

    /**
     * Method that writes the match history in a File inside /histories
     *
     * @param movesHistory List of all match moves
     */
    private void writeHistoryToFile(List<MoveStatus> movesHistory) {
        Gson gson = new Gson();
        String moveHistoryJSON = gson.toJson(movesHistory);

        File jarPath = new File(GameManager.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        String jarParentFolderPath = jarPath.getParentFile().getAbsolutePath();
        File jarParentFolder = new File(jarParentFolderPath + "/histories");
        if (!jarParentFolder.exists()) {
            jarParentFolder.mkdir();
        }

        try (FileWriter file = new FileWriter(jarParentFolder + "/" + playerList.toString() + ".txt")) {
            file.write(moveHistoryJSON);
        } catch (IOException e) {
            System.err.println("There was an error writing the file! Could not complete.");
        }
    }

    /**
     * Method that notifies the winner and the losers of the match.
     */
    private void notifyWinner() {
        Player winner = evaluateWinner();

        for (Player player : playerList) {
            if (winner != null && winner.equals(player) && winner.getUser().isActive()) {
                try {
                    player.getUser().incrementNoOfWins();
                    addMoveToHistoryAndNotify(new MoveStatus(currentRound.getCurrentPlayer().getPlayerUsername(), "wins the match"));
                    if (player.getUser().isActive()) {
                        player.getUserObserver().notifyVictory(player.getScore());
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    player.getUser().incrementNoOfLose();
                    addMoveToHistoryAndNotify(new MoveStatus(currentRound.getCurrentPlayer().getPlayerUsername(), "lose the match"));
                    if (player.getUser().isActive()) {
                        player.getUserObserver().notifyLost(player.getScore());
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Method that assign the points the players using the Public objective cards and subtracting the number of empty boxes
     */
    void assignPointsToPlayers() {
        for (Player player : playerList) {
            int score = 0;
            for (PublicObjectiveCard publicObjectiveCard : board.getPublicObjectiveCards()) {
                score += publicObjectiveCard.getScore(player.getPatternCard().getGrid());
            }
            score -= player.getPatternCard().getNoOfEmptyBoxes();
            player.setScore(score);
        }
    }

    /**
     * Method that checks player's point and decide a Winner and, in case. the tie.
     *
     * @return Set of possible winnners
     */
    private Set<Player> evaluateBasicPoints() {
        Set<Player> tiePlayers = new HashSet<>();
        int maxScore = -30;
        Player winner = null;

        for (Player player : playerList) {
            if (maxScore < player.getScore()) {
                tiePlayers.clear();
                winner = player;
                maxScore = player.getScore();
            } else if (maxScore == player.getScore()) {
                if (!tiePlayers.contains(winner)) {
                    tiePlayers.add(winner);
                }
                tiePlayers.add(player);
                winner = player;
            }
        }

        if (!tiePlayers.contains(winner)) {
            tiePlayers.add(winner);
        }

        return tiePlayers;
    }

    /**
     * Method that updates the player's score using the private objective cards.
     *
     * @param tiePlayers Set of Players with the same score
     * @return The Set updated
     */
    private Set<Player> evaluatePrivateObjectiveCardPoints(Set<Player> tiePlayers) {
        int privateOc = 0;
        Set<Player> tieAgainPlayers = new HashSet<>();
        Player winner = null;

        for (Player player : tiePlayers) {
            if (privateOc < player.getPrivateObjectiveCard().check(player.getPatternCard().getGrid())) {
                tieAgainPlayers.clear();
                winner = player;
                privateOc = player.getPrivateObjectiveCard().check(player.getPatternCard().getGrid());
            } else if (privateOc == player.getPrivateObjectiveCard().check(player.getPatternCard().getGrid())) {
                if (!tieAgainPlayers.contains(winner)) {
                    tieAgainPlayers.add(winner);
                }
                tieAgainPlayers.add(player);
                winner = player;
            }
        }

        if (!tieAgainPlayers.contains(winner)) {
            tieAgainPlayers.add(winner);
        }

        return tieAgainPlayers;
    }

    /**
     * Method that updates the player's score using the favor tokens
     *
     * @param tieAgainPlayers Set of Players with the same score
     * @return The Set updated
     */
    private Set<Player> evaluateFavourTokenPoints(Set<Player> tieAgainPlayers) {
        int favorTokens = 0;
        Player winner = null;
        Set<Player> tiePlayers = new HashSet<>();

        for (Player player : tieAgainPlayers) {
            if (favorTokens < player.getFavourTokens()) {
                winner = player;
                tiePlayers.clear();
                favorTokens = player.getFavourTokens();
            } else if (favorTokens == player.getFavourTokens()) {
                if (!tiePlayers.contains(winner)) {
                    tiePlayers.add(winner);
                }
                tiePlayers.add(player);
                winner = player;
            }
        }

        if (!tiePlayers.contains(winner)) {
            tiePlayers.add(winner);
        }

        return tiePlayers;
    }

    /**
     * Method that computes the winner and notifies each player with their results
     */
    Player evaluateWinner() {
        Player winner = null;
        boolean found = false;
        Set<Player> possibleWinners = evaluateBasicPoints();
        int activeUsers = 0;

        for (Player player : playerList) {
            if (player.getUser().isActive()) {
                activeUsers++;
                winner = player;
            }
        }

        if (activeUsers > 1) {
            winner = null;
            if (possibleWinners.size() > 1) {
                possibleWinners = evaluatePrivateObjectiveCardPoints(possibleWinners);
            } else found = true;

            if (possibleWinners.size() > 1) {
                possibleWinners = evaluateFavourTokenPoints(possibleWinners);
            } else found = true;

            if (possibleWinners.size() > 1) {
                for (Player player : playerList) {
                    if (possibleWinners.contains(player)) {
                        winner = player;
                        found = true;
                        break;
                    }
                }
            }

            for (Player player : possibleWinners) {
                winner = player;
            }
        } else {
            if (winner != null) {
                winner.setScore(0);
            }
        }

        return winner;
    }

    /**
     * Method that starts a single round
     */
    private void startRound() {
        currentRound = new Round(this);
        //Rounds going forward
        turnInRound.set(1);

        for (int i = 0; i < playerList.size(); i++) {
            executeTurn(i, "Turn forward ");
        }
        turnInRound.set(2);

        for (int i = playerList.size() - 1; i >= 0; i--) {
            executeTurn(i, "Turn backward ");
        }

        System.out.println("End of turn in GameManager");

        if (!board.getDraftedDice().isEmpty()) {
            roundTrack.add(board.getDraftedDice());
            notifyUpdatedRoundTrack();
        }

        draftedDiceSet.set(false);
        shiftPlayerList();

        //wake up the match thread
        synchronized (endRound) {
            endRound.notifyAll();
        }
    }

    /**
     * Method that starts the turn and wait for the end of it only if the user is active
     *
     * @param playerIndex The index of the new current player
     * @param turnState   Specify the state turn(forward or backward)
     */
    private void executeTurn(int playerIndex, String turnState) {

        addMoveToHistoryAndNotify(new MoveStatus(playerList.get(playerIndex).getPlayerUsername(), "starts turn"));

        currentRound.setPlayerEndedTurn(false);

        if (playerList.get(playerIndex).getUser().isActive() && (disconnectedPlayers.size() != (playerList.size() - 1))) {
            currentRound.startForPlayer(playerList.get(playerIndex));
            System.out.println(turnState + currentRound.getCurrentPlayer().getPlayerUsername());
            cancelTimer.set(false);
            controllerTimer.startTurnTimer(maxTurnSeconds, this);
            System.out.println("Starting the timer.");

            //wait until turn has ended
            waitEndTurn();
        }
    }

    /**
     * Returns the toolCardLock userd to set the toolCardThread in WAIT and to avoid the method execution if the timer
     * expires
     *
     * @return The AtomicBoolean used as lock
     */
    public AtomicBoolean getToolCardLock() {
        return toolCardLock;
    }

    /**
     * Returns the timer used for the single turn duration
     *
     * @return ControllerTimer used for turn
     */
    ControllerTimer getControllerTimer() {
        return controllerTimer;
    }

    /**
     * Reorder the playerList after the end of the round. The first player is inserted in tail
     */
    private void shiftPlayerList() {
        Player tmp = playerList.get(0);
        playerList.remove(0);
        playerList.add(tmp);
        endRound.set(true);
    }

    /**
     * Send the updated round track at the end of the round
     */
    private void notifyUpdatedRoundTrack() {
        int round = 0;
        if (!roundTrack.isEmpty()) round = roundTrack.size() - 1;
        playerBroadcaster.broadcastResponseToAll(new RoundTrackNotification(roundTrack.get(round)));
    }

    /**
     * Method that set the round Thread in GameManager in WAIT. Wakes up when notify is called on
     * <code>currentRound.hasPlayerEndedTurn()</code>
     */
    private void waitEndTurn() {
        synchronized (currentRound.hasPlayerEndedTurn()) {
            while (!currentRound.hasPlayerEndedTurn().get()) {
                try {
                    currentRound.hasPlayerEndedTurn().wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Method that notifies the player with a patternCard's mask which indicates the available positions in which
     * a dice can be placed
     *
     * @param player player who's playing at the current time
     */
    Map<String, Boolean[][]> sendAvailablePositions(Player player) {
        return player.getPatternCard().computeAvailablePositionsDraftedDice(board.getDraftedDice());
    }

    /*
     *
     *
     * GAME MOVES
     *
     *
     */

    /**
     * Method for the place-dice move. Inserts <code>dice</code> into the grid
     *
     * @param player      Current player
     * @param dice        Dice to place
     * @param rowIndex    Row index
     * @param columnIndex Line index
     * @return
     */
    boolean makeMove(Player player, Dice dice, int rowIndex, int columnIndex) {
        if (player.getPatternCard().getGrid().get(rowIndex).get(columnIndex).getDice() == null) {


            player.getPatternCard().getGrid().get(rowIndex).get(columnIndex).insertDice(dice);
            board.getDraftedDice().remove(dice);

            // Send updated draftedDice
            playerBroadcaster.broadcastResponseToAll(board.getDraftedDice());

            // Update MovesHistory
            addMoveToHistoryAndNotify(new MoveStatus(player.getPlayerUsername(),
                                                     "Placed dice" + dice + " in [" + rowIndex + ", " + columnIndex + "]"));

            // UpdateView response
            playerBroadcaster.broadcastResponseToAll(new UpdateViewResponse(player, sendAvailablePositions(getCurrentRound().getCurrentPlayer())));
            return true;
        } else
            return false;
    }

    /**
     * Method called when an user selected a patternCard from the view
     *
     * @param toolCardName name of the ToolCard to use
     */
    public void useToolCard(String toolCardName) {
        toolCardThread = new Thread(
                () -> {
                    toolCardLock.set(true);
                    for (ToolCard toolCard : board.getToolCards()) {
                        if (toolCard.getName().equals(toolCardName)) {
                            addMoveToHistoryAndNotify(new MoveStatus(currentRound.getCurrentPlayer().getPlayerUsername(), "Used toolcard " + toolCardName));
                            currentRound.makeMove(toolCard);

                        }
                    }
                });
        toolCardThread.start();
    }

    /**
     * Method that will update the current Moves made by each Player in every User's View
     *
     * @param moveStatus move to be added in the List of Moves made
     */
    public void addMoveToHistoryAndNotify(MoveStatus moveStatus) {
        movesHistory.add(moveStatus);
        playerBroadcaster.updateMovesHistory(movesHistory);
    }

    /**
     * @return the current available Round
     */
    public Round getCurrentRound() {
        return currentRound;
    }


    /*

    -------------------------  TOOLCARDS METHODS  --------------------------------------

     */

    /**
     * Place Dice for Tool Cards
     * <p>
     * Method that place a die in the pattern card for the tool cards that implement this move
     * Find the current player and insert the die in the patterncard, if it is possible, then remove the die from draftedDice
     *
     * @param dice        die to place in Pattern Card
     * @param rowIndex    row index of the position in Pattern Card
     * @param columnIndex column index of the position in Pattern Card
     */
    private void placeDiceToolCard(Dice dice, int rowIndex, int columnIndex) {
        Player player = getCurrentRound().getCurrentPlayer();
        if (player.getPatternCard().getGrid().get(rowIndex).get(columnIndex).getDice() == null) {
            player.getPatternCard().getGrid().get(rowIndex).get(columnIndex).insertDice(dice);

            for (Dice diceToRemove : getDraftedDice()) {
                if (diceToRemove.toString().equals(dice.toString())) {
                    dice = diceToRemove;
                }
            }

            getDraftedDice().remove(dice);
        }
        synchronized (toolCardLock) {
            toolCardLock.notifyAll();
        }
    }

    /**
     * Avoid ToolCard Use
     * <p>
     * Notify the player with an AvoidToolCardResponse that he cannot use the selected tool card
     */
    public void avoidToolCardUse() {
        try {
            currentRound.getCurrentPlayer().getUserObserver().sendResponse(new AvoidToolCardResponse());
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        toolCardLock.set(false);
    }

    /**
     * Wake Up ToolCard Response
     * <p>
     * Unlock the tool card thread in wait to finish the tool card move
     */
    private void wakeUpToolCardThread() {
        synchronized (toolCardLock) {
            toolCardLock.notifyAll();
        }
    }

    /**
     * GLAZING HAMMER
     * Method that roll again the drafted dice and send the new drafted pool to all the players
     */
    public synchronized void glazingHammerResponse() {
        for (Dice dice : board.getDraftedDice()) {
            dice.roll();
        }
        addMoveToHistoryAndNotify(new MoveStatus(currentRound.getCurrentPlayer().getPlayerUsername(), "rolled the drafted dice"));
        playerBroadcaster.broadcastResponseToAll(new DraftedDiceToolCardResponse(board.getDraftedDice(), true));
        endTurn(getCurrentRound().getCurrentPlayer().getPlayerUsername());
    }

    /**
     * GROZING PLIERS: Move Method
     * <p>
     * Tool card that increase or decrease by one the value of a selected dice from the drafted dice pool:
     * Find the selected dice from the draftedDice and call the method for increasing or decreasing the die's value, depending on increase boolean parameter
     *
     * @param dice     the selected die from the player
     * @param increase if true increase the die value, if false decrease the die value
     */
    public synchronized void grozingPliersMove(Dice dice, Boolean increase) {
        if (toolCardLock.get()) {
            for (Dice diceInPool : board.getDraftedDice()) {
                if (dice.toString().equals(diceInPool.toString())) {
                    if (increase) {
                        diceInPool.increasesByOneValue();
                        addMoveToHistoryAndNotify(new MoveStatus(currentRound.getCurrentPlayer().getPlayerUsername(), "increased " + diceInPool.toString() + " by one value"));
                    } else {
                        diceInPool.decreasesByOneValue();
                        addMoveToHistoryAndNotify(new MoveStatus(currentRound.getCurrentPlayer().getPlayerUsername(), "decreased " + diceInPool.toString() + " by one value"));
                    }
                }
            }
            wakeUpToolCardThread();
        }
    }

    /**
     * After the toolCard move ends with success this method sends the
     * updated data to the players
     */
    public void grozingPliersResponse() {
        if (toolCardLock.get()) {
            playerBroadcaster.broadcastResponseToAll(new DraftedDiceToolCardResponse(board.getDraftedDice(), false));
            try {
                currentRound.getCurrentPlayer().getUserObserver().sendResponse(new AvailablePositionsResponse(sendAvailablePositions(currentRound.getCurrentPlayer())));
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * FLUX BRUSH 1ST PHASE
     * Received the selected die from the player thi method re-roll
     *
     * @param selectedDice Dice to be rolled again
     */
    public synchronized void fluxBrushMove(Dice selectedDice) {
        if (toolCardLock.get()) {

            FluxBrush fluxBrush = (FluxBrush) getSelectedToolCard("FluxBrush");

            assert fluxBrush != null;

            List<Dice> list = new ArrayList<>();
            for (Dice dice : getDraftedDice()) {
                list.add(new Dice(dice.getFaceUpValue(), dice.getDiceColor()));
            }

            fluxBrush.setTemporaryDraftedDice(list);

            for (Dice diceInPool : fluxBrush.getTemporaryDraftedDice()) {
                if (selectedDice.toString().equals(diceInPool.toString())) {
                    diceInPool.roll();
                    Map<String, Boolean[][]> availablePositions = getCurrentRound().getCurrentPlayer().getPatternCard().computeAvailablePositionsDraftedDice(fluxBrush.getTemporaryDraftedDice());
                    try {
                        getCurrentRound().getCurrentPlayer().getUserObserver().sendResponse(new FluxBrushResponse(fluxBrush.getTemporaryDraftedDice(), diceInPool, availablePositions));
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }

                    addMoveToHistoryAndNotify(new MoveStatus(currentRound.getCurrentPlayer().getPlayerUsername(), "rolled again the drafted dice"));

                    break;
                }
            }
        }
    }

    /**
     * FLUX BRUSH 2ND PHASE
     *
     * @param dice
     * @param rowIndex
     * @param columnIndex
     */
    public synchronized void fluxBrushMove(Dice dice, int rowIndex, int columnIndex) {
        if (toolCardLock.get()) {
            FluxBrush fluxBrush = (FluxBrush) getSelectedToolCard("FluxBrush");
            assert fluxBrush != null;
            board.setDraftedDice(fluxBrush.getTemporaryDraftedDice());
            placeDiceToolCard(dice, rowIndex, columnIndex);
            addMoveToHistoryAndNotify(new MoveStatus(currentRound.getCurrentPlayer().getPlayerUsername(), "placed " + dice.toString() + " in " + rowIndex + " - " + columnIndex));
        }
    }

    /**
     * FLUX BRUSH 3RD PHASE
     * <p>
     * The move is done so we can save the toolcard move changes in the board
     */
    public void fluxBrushMove() {
        if (toolCardLock.get()) {
            FluxBrush fluxBrush = (FluxBrush) getSelectedToolCard("FluxBrush");
            assert fluxBrush != null;
            board.setDraftedDice(fluxBrush.getTemporaryDraftedDice());
            wakeUpToolCardThread();
            addMoveToHistoryAndNotify(new MoveStatus(currentRound.getCurrentPlayer().getPlayerUsername(), "choose a dice that cannot be placed"));
        }
    }

    /**
     * After the toolCard move ends with success this method sends the
     * updated data to the players
     */
    public void fluxBrushResponse() {
        if (toolCardLock.get()) {
            playerBroadcaster.broadcastResponseToAll(new DraftedDiceToolCardResponse(board.getDraftedDice(), false));
            playerBroadcaster.broadcastResponseToAll(new PatternCardToolCardResponse(currentRound.getCurrentPlayer(), sendAvailablePositions(getCurrentRound().getCurrentPlayer())));
        }
    }

    /**
     * FLUX REMOVER 1ST PHASE
     *
     * @param selectedDice
     */
    public synchronized void fluxRemoverMove(Dice selectedDice) {
        if (toolCardLock.get()) {
            FluxRemover fluxRemover = (FluxRemover) getSelectedToolCard("FluxRemover");

            assert fluxRemover != null;
            fluxRemover.setDiceFromBag(board.draftOneDice());
            List<Dice> list = new ArrayList<>(getDraftedDice());
            list.add(fluxRemover.getDiceFromBag());
            fluxRemover.setDraftedDice(list);

            for (Dice dice : fluxRemover.getDraftedDice()) {
                if (dice.toString().equals(selectedDice.toString())) {
                    selectedDice = dice;
                    break;
                }
            }

            addMoveToHistoryAndNotify(new MoveStatus(currentRound.getCurrentPlayer().getPlayerUsername(), "pulled out " + selectedDice.toString() + "from drafted dice"));
            fluxRemover.getDraftedDice().remove(selectedDice);


            try {
                getCurrentRound().getCurrentPlayer().getUserObserver().sendResponse(new FluxRemoverResponse(fluxRemover.getDiceFromBag()));
            } catch (RemoteException e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * FLUX REMOVER 2ND PHASE
     * <p>
     * Received the new face up value for the drafted die,
     * this method sets the chosen value and send to the client the drafted dice,
     * the selected die and the available positions to makes the player choose
     * where to place the die
     *
     * @param selectedDice the die previously drafted from the bag and whose
     *                     the player choose the value
     * @param chosenValue  the value chosen from the player to set as face up value
     */
    public synchronized void fluxRemoverMove(Dice selectedDice, int chosenValue) {
        if (toolCardLock.get()) {
            FluxRemover fluxRemover = (FluxRemover) getSelectedToolCard("FluxRemover");

            assert fluxRemover != null;
            for (Dice dice : fluxRemover.getDraftedDice()) {
                if (selectedDice.toString().equals(dice.toString())) {
                    dice.setFaceUpValue(chosenValue);
                    selectedDice.setFaceUpValue(chosenValue);
                    break;
                }
            }
            Map<String, Boolean[][]> availablePositions = getCurrentRound().getCurrentPlayer().getPatternCard().computeAvailablePositionsDraftedDice(fluxRemover.getDraftedDice());
            try {
                getCurrentRound().getCurrentPlayer().getUserObserver().sendResponse(new FluxRemoverResponse(fluxRemover.getDraftedDice(), selectedDice, availablePositions));
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            addMoveToHistoryAndNotify(new MoveStatus(currentRound.getCurrentPlayer().getPlayerUsername(), "set the dice value to " + chosenValue));

        }
    }

    /**
     * FLUX REMOVER 3RD PHASE a
     * <p>
     * This method handles the case
     *
     * @param selectedDice
     * @param rowIndex
     * @param columnIndex
     */
    public synchronized void fluxRemoverMove(Dice selectedDice, int rowIndex, int columnIndex) {
        if (toolCardLock.get()) {
            FluxRemover fluxRemover = (FluxRemover) getSelectedToolCard("FluxRemover");

            assert fluxRemover != null;
            board.addDiceToBag(fluxRemover.getDiceFromBag());
            board.setDraftedDice(fluxRemover.getDraftedDice());
            placeDiceToolCard(selectedDice, rowIndex, columnIndex);
            addMoveToHistoryAndNotify(new MoveStatus(currentRound.getCurrentPlayer().getPlayerUsername(), "placed " + selectedDice.toString() + " in " + rowIndex + " - " + columnIndex));
        }
    }

    /**
     * Returns the selected tool card
     *
     * @param toolCardName tool card to return
     * @return Selected Toolcard
     */
    private ToolCard getSelectedToolCard(String toolCardName) {
        for (ToolCard toolCard : board.getToolCards()) {
            if (toolCard.getName().equals(toolCardName)) return toolCard;
        }
        return null;
    }

    /**
     * FLUX REMOVER 3RD PHASE b
     * <p>
     * This method handles the case the dice can't be placed in the pattern card
     * it will be leave in the drafted dice
     * The move is done so we can save the toolcard move changes in the board
     */
    public synchronized void fluxRemoverMove() {
        if (toolCardLock.get()) {
            FluxRemover fluxRemover;
            fluxRemover = (FluxRemover) getSelectedToolCard("FluxRemover");
            assert fluxRemover != null;
            board.addDiceToBag(fluxRemover.getDiceFromBag());
            board.setDraftedDice(fluxRemover.getDraftedDice());
            wakeUpToolCardThread();
        }
    }

    /**
     * After the toolCard move ends with success this method sends the
     * updated data to the players
     */
    public void fluxRemoverResponse() {
        if (toolCardLock.get()) {
            playerBroadcaster.broadcastResponseToAll(new DraftedDiceToolCardResponse(board.getDraftedDice(), false));
            playerBroadcaster.broadcastResponseToAll(new PatternCardToolCardResponse(currentRound.getCurrentPlayer(), sendAvailablePositions((getCurrentRound().getCurrentPlayer()))));
        }
    }

    /**
     * GRINDING STONE
     * <p>
     * Received the selected dice, this method sets its value to the opposite side
     * (if 1 becomes 6, if 2 becomes 5, if 3 becomes 4 and the other way around)
     *
     * @param selectedDice the selected dice whose the value has to be flipped
     */
    public synchronized void grindingStoneMove(Dice selectedDice) {
        if (toolCardLock.get()) {
            for (Dice diceInPool : board.getDraftedDice()) {
                if (selectedDice.toString().equals(diceInPool.toString())) {
                    diceInPool.setOppositeFace();
                    addMoveToHistoryAndNotify(new MoveStatus(currentRound.getCurrentPlayer().getPlayerUsername(), "set " + selectedDice.toString() + " to the opposite face"));
                }
            }
            wakeUpToolCardThread();
        }
    }

    /**
     * After the toolCard move ends with success this method sends the
     * updated data to the players
     */
    public void grindingStoneResponse() {
        if (toolCardLock.get()) {
            playerBroadcaster.broadcastResponseToAll(new DraftedDiceToolCardResponse(board.getDraftedDice(), false));
            try {
                currentRound.getCurrentPlayer().getUserObserver().sendResponse(new AvailablePositionsResponse(sendAvailablePositions(currentRound.getCurrentPlayer())));
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Place a Dice in the selected position during the use of CopperFoilBurnisher
     *
     * @param dicePosition coordinates of dice to be moved
     * @param position     coordinates of where to place the dice
     */
    public synchronized void copperFoilBurnisherMove(Tuple dicePosition, Tuple position) {
        if (toolCardLock.get()) {
            List<List<Box>> patternCard = currentRound.getCurrentPlayer().getPatternCard().getGrid();
            patternCard.get(position.getFirst()).get(position.getSecond()).insertDice(patternCard.get(dicePosition.getFirst()).get(dicePosition.getSecond()).getDice());
            patternCard.get(dicePosition.getFirst()).get(dicePosition.getSecond()).removeDice();
            addMoveToHistoryAndNotify(new MoveStatus(currentRound.getCurrentPlayer().getPlayerUsername(), "placed the dice from " + dicePosition.getFirst() + " - " + dicePosition.getSecond() + " to " + position.getFirst() + " - " + position.getSecond()));
            wakeUpToolCardThread();
        }
    }

    /**
     * COPPER FOIL BURNISHER
     * <p>
     * =======
     * <p>
     * <p>
     * After the toolCard move ends with success this method sends the
     * updated data to the players
     */
    public void copperFoilBurnisherResponse() {
        if (toolCardLock.get()) {
            playerBroadcaster.broadcastResponseToAll(new PatternCardToolCardResponse(currentRound.getCurrentPlayer(), sendAvailablePositions(getCurrentRound().getCurrentPlayer())));
        }
    }

    /**
     * CORK BACKED STRAIGHTEDGE
     * This method
     *
     * @param selectedDice
     * @param row
     * @param column
     */
    public synchronized void corkBackedStraightedgeMove(Dice selectedDice, int row, int column) {
        if (toolCardLock.get()) {
            Map<String, Boolean[][]> availablePositions = currentRound.getCurrentPlayer().getPatternCard().computeAvailablePositionsNoDiceAround(board.getDraftedDice());
            if (availablePositions.get(selectedDice.toString())[row][column]) {
                placeDiceForPlayer(selectedDice, row, column);
            }
            addMoveToHistoryAndNotify(new MoveStatus(currentRound.getCurrentPlayer().getPlayerUsername(), "placed " + selectedDice.toString() + " in " + row + " - " + column));
            wakeUpToolCardThread();
        }
    }

    /**
     * After the toolCard move ends with success this method sends the
     * updated data to the players
     */
    public void corkBackedStraightedgeResponse() {
        if (toolCardLock.get()) {
            playerBroadcaster.broadcastResponseToAll(new PatternCardToolCardResponse(currentRound.getCurrentPlayer(), currentRound.getCurrentPlayer().getPatternCard().computeAvailablePositions()));
        }
    }

    /**
     * LENS CUTTER
     * <p>
     * Received the two selected dice from the player,
     * one from the roundtrack and one frome the drafted pool,
     * this method swipes them. it gets the die from the round track and puts it
     * in the drafted pool, and then takes the other one from the
     * drafted pool and puts it in the right round of the roundtrack
     *
     * @param roundIndex     the round where the selected die in the roundtrack belongs
     * @param roundTrackDice the selected die from the round track
     * @param poolDice       the selected die from the draft pool
     */
    public synchronized void lensCutterMove(int roundIndex, String roundTrackDice, String poolDice) {
        if (toolCardLock.get()) {
            Dice fromTrackDice;
            Dice fromPoolDice;
            for (int i = 0; i < roundTrack.get(roundIndex).size(); i++) {
                if (roundTrack.get(roundIndex).get(i).toString().equals(roundTrackDice)) {
                    fromTrackDice = roundTrack.get(roundIndex).get(i);
                    roundTrack.get(roundIndex).remove(i);
                    for (int j = 0; j < getDraftedDice().size(); j++) {
                        if (getDraftedDice().get(j).toString().equals(poolDice)) {
                            fromPoolDice = getDraftedDice().get(j);
                            getDraftedDice().remove(j);
                            getDraftedDice().add(fromTrackDice);
                            roundTrack.get(roundIndex).add(fromPoolDice);
                            break;
                        }
                    }
                }
            }
            addMoveToHistoryAndNotify(new MoveStatus(currentRound.getCurrentPlayer().getPlayerUsername(), "the dice is swapped to the opposite side"));
            wakeUpToolCardThread();
        }
    }

    /**
     * After the toolCard move ends with success this method sends the
     * updated data to the players
     */
    public void lensCutterResponse() {
        if (toolCardLock.get()) {
            playerBroadcaster.broadcastResponseToAll(new DraftedDiceToolCardResponse(board.getDraftedDice(), false));
            playerBroadcaster.broadcastResponseToAll(new RoundTrackToolCardResponse(roundTrack));
            try {
                getCurrentRound().getCurrentPlayer().getUserObserver().sendResponse(new AvailablePositionsResponse(getCurrentRound().getCurrentPlayer().getPatternCard().computeAvailablePositionsDraftedDice(getDraftedDice())));
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * EGLOMISE BRUSH
     *
     * @param dicePosition
     * @param position
     */
    public synchronized void eglomiseBrushMove(Tuple dicePosition, Tuple position) {
        if (toolCardLock.get()) {
            List<List<Box>> patternCard = currentRound.getCurrentPlayer().getPatternCard().getGrid();
            if (patternCard.get(dicePosition.getFirst()).get(dicePosition.getSecond()).getDice() != null) {
                patternCard.get(position.getFirst()).get(position.getSecond()).insertDice(patternCard.get(dicePosition.getFirst()).get(dicePosition.getSecond()).getDice());
                patternCard.get(dicePosition.getFirst()).get(dicePosition.getSecond()).removeDice();

                addMoveToHistoryAndNotify(new MoveStatus(currentRound.getCurrentPlayer().getPlayerUsername(), "placed the dice from " + dicePosition.getFirst() + " - " + dicePosition.getSecond() + " to " + position.getFirst() + " - " + position.getSecond()));

            } else System.out.println("Eglomise Brusher: Error invalid selected dice");
            wakeUpToolCardThread();
        }
    }

    /**
     * After the toolCard move ends with success this method sends the
     * updated data to the players
     */
    public void eglomiseBrushResponse() {
        if (toolCardLock.get()) {
            playerBroadcaster.broadcastResponseToAll(new PatternCardToolCardResponse(currentRound.getCurrentPlayer(), sendAvailablePositions((getCurrentRound().getCurrentPlayer()))));
        }
    }

    /**
     * LATHEKIN
     * Receives two positions: the first is the die's positions to move,
     * the second is the one where the player wants to put the die.
     * The player has to move two dice to complete the move.
     * <p>
     * In the first call the method moves the dice and saves the changes and the previous state
     * in the ToolCard attributes, because in the case the move ends with no success the previous
     * state would be restored.
     * In the second call the player has made both the moves, it copie the modified in the original
     * pattern card and moves the second die
     * In case of doubleMove, the player makes the two moves in one swiping two dice positions.
     *
     * @param dicePosition position of the die to move
     * @param position     the position where put the die
     * @param doubleMove   if the player want to swipe to dice
     */
    public synchronized void lathekinMove(Tuple dicePosition, Tuple position, boolean doubleMove) {
        if (toolCardLock.get()) {
            Lathekin lathekin = (Lathekin) getSelectedToolCard("Lathekin");

            assert lathekin != null;

            //if newGrid is null means that is the first move, so it has to save the temporary changes in the tool card
            if (lathekin.getNewGrid() == null) {
                //create a backup of the pattern card in Lathekin
                lathekin.setOldGrid(copyPatternCard());
            } else {
                //if newGrid is not null means that is the second move and the move is completed, it can copy the changes
                getCurrentRound().getCurrentPlayer().getPatternCard().setGrid(lathekin.getNewGrid());
            }

            List<List<Box>> grid = currentRound.getCurrentPlayer().getPatternCard().getGrid();

            if (grid.get(dicePosition.getFirst()).get(dicePosition.getSecond()).getDice() != null) {

                if (!doubleMove) {
                    //single move case
                    grid.get(position.getFirst()).get(position.getSecond()).insertDice(grid.get(dicePosition.getFirst()).get(dicePosition.getSecond()).getDice());
                    grid.get(dicePosition.getFirst()).get(dicePosition.getSecond()).removeDice();
                } else {
                    //Double move case
                    Dice dice = grid.get(position.getFirst()).get(position.getSecond()).getDice();
                    grid.get(position.getFirst()).get(position.getSecond()).removeDice();
                    grid.get(position.getFirst()).get(position.getSecond()).insertDice(grid.get(dicePosition.getFirst()).get(dicePosition.getSecond()).getDice());
                    grid.get(dicePosition.getFirst()).get(dicePosition.getSecond()).removeDice();
                    grid.get(dicePosition.getFirst()).get(dicePosition.getSecond()).insertDice(dice);
                    this.doubleMove.set(true);
                }
            } else
                System.out.println("Lathekin: Error invalid selected dice");

            System.out.println("Waking up toolcard thread");

            if (lathekin.getNewGrid() == null) {
                try {
                    //Modified data is saved on newGrid in Lathekin and in the player is restored the old one
                    PatternCard patternCard = getCurrentRound().getCurrentPlayer().getPatternCard();
                    getCurrentRound().getCurrentPlayer().getUserObserver().sendResponse(new LathekinResponse(getCurrentRound().getCurrentPlayer().getPlayerUsername(), patternCard, patternCard.computeAvailablePositionsLathekin(), true));


                    lathekin.setNewGrid(getCurrentRound().getCurrentPlayer().getPatternCard().getGrid());
                    getCurrentRound().getCurrentPlayer().getPatternCard().setGrid(lathekin.getOldGrid());

                    System.out.println("sending data for the second lathekin move");
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            addMoveToHistoryAndNotify(new MoveStatus(currentRound.getCurrentPlayer().getPlayerUsername(), "placed the dice from " + dicePosition.getFirst() + " - " + dicePosition.getSecond() + " to " + position.getFirst() + " - " + position.getSecond()));


            wakeUpToolCardThread();
        }
    }

    /**
     * After the toolCard move ends with success this method sends the
     * updated data to the players
     */
    public void lathekinResponse() {
        if (toolCardLock.get()) {
            System.out.println("sending Lathekin response");
            playerBroadcaster.broadcastResponseToAll(new PatternCardToolCardResponse(currentRound.getCurrentPlayer(), sendAvailablePositions((getCurrentRound().getCurrentPlayer()))));
        }
    }

    /**
     * Method used to create a copy of a PatternCard grid
     * it is used to save the progress of the tool card move
     * until the end of the move, only if it ends with success,
     * then it will be copied to the original pattern card
     *
     * @return the copy of the original pattern card grid
     */
    private synchronized List<List<Box>> copyPatternCard() {
        List<List<Box>> gridPattern = new ArrayList<>();
        for (int i = 0; i < currentRound.getCurrentPlayer().getPatternCard().getGrid().size(); i++) {
            gridPattern.add(new ArrayList<>());
            for (int j = 0; j < currentRound.getCurrentPlayer().getPatternCard().getGrid().get(i).size(); j++) {
                Box box = currentRound.getCurrentPlayer().getPatternCard().getGrid().get(i).get(j);
                if (box.isValueSet()) {
                    gridPattern.get(i).add(new Box(box.getValue()));
                } else gridPattern.get(i).add(new Box(box.getColor()));

                if (box.getDice() != null)
                    gridPattern.get(i).get(j).insertDice(box.getDice());
            }
        }
        return gridPattern;
    }

    /**
     * RUNNING PLIERS
     * Method that makes the player draft and place another die before his end turn
     *
     * @param selectedDice dice to place
     * @param rowIndex     row index of the position in the patterncard
     *                     where the place has to be placed
     * @param columnIndex  column index of the position in the patterncard
     *                     where the place has to be placed
     */
    public synchronized void runningPliersMove(Dice selectedDice, int rowIndex, int columnIndex) {
        if (toolCardLock.get()) {
            placeDiceToolCard(selectedDice, rowIndex, columnIndex);
            addMoveToHistoryAndNotify(new MoveStatus(currentRound.getCurrentPlayer().getPlayerUsername(), "placed the dice in " + rowIndex + " - " + columnIndex));

        }
    }

    /**
     * After the toolCard move ends with success this method sends the
     * updated data to the players
     */
    public void runningPliersResponse() {
        playerBroadcaster.broadcastResponseToAll(new PatternCardToolCardResponse(currentRound.getCurrentPlayer(), sendAvailablePositions(getCurrentRound().getCurrentPlayer())));
        playerBroadcaster.broadcastResponseToAll(new DraftedDiceToolCardResponse(getDraftedDice(), true));
    }

    public boolean getdoubleMove() {
        return doubleMove.get();
    }

    /**
     * Select a dice from the round track and move at most two dices of the same color.
     * Phases:
     * -1 - Close the tool card
     * 0  - Send the available positions of all dice of the selected color
     * 1  - Place one (or two in case of double move) dice in the pattern card
     * 2  - Place the second dice(double move not allowed)
     *
     * @param roundTrackDice Dice from which extract the selected color
     * @param phase          phase of the toolcard
     * @param dicePosition   initial position of the dice
     * @param position       final position of the dice
     * @param doubleMove
     */
    public synchronized void tapWheelMove(Dice roundTrackDice, int phase, Tuple dicePosition, Tuple position, boolean doubleMove) {
        if (toolCardLock.get()) {
            if (phase == -1) {
                setDoubleMove(true);
                wakeUpToolCardThread();
                tapWheelResponse(null, 3);
            }
            if (phase == 0) {
                System.out.println("Calculating the mask");
                Map<String, Boolean[][]> availablePositions = currentRound.getCurrentPlayer().getPatternCard().computeAvailablePositionsTapWheel(roundTrackDice, false);

                addMoveToHistoryAndNotify(new MoveStatus(currentRound.getCurrentPlayer().getPlayerUsername(), "choose the dice color " + roundTrackDice.getDiceColor()));


                tapWheelResponse(availablePositions, 1);
            }
            if (phase == 1) {
                List<List<Box>> patternCard = currentRound.getCurrentPlayer().getPatternCard().getGrid();
                if (!doubleMove) {
                    patternCard.get(position.getFirst()).get(position.getSecond()).insertDice(patternCard.get(dicePosition.getFirst()).get(dicePosition.getSecond()).getDice());

                    Dice dice1 = patternCard.get(dicePosition.getFirst()).get(dicePosition.getSecond()).getDice();
                    patternCard.get(dicePosition.getFirst()).get(dicePosition.getSecond()).removeDice();

                    Map<String, Boolean[][]> hashMapGrid = currentRound.getCurrentPlayer().getPatternCard().computeAvailablePositionsTapWheel(dice1, true);
                    hashMapGrid.remove(dice1.toString() + position.getFirst() + position.getSecond());

                    System.out.println("The dice removed is\t" + dice1.toString() + position.getFirst() + position.getSecond());

                    addMoveToHistoryAndNotify(new MoveStatus(currentRound.getCurrentPlayer().getPlayerUsername(), "placed the first dice from " + dicePosition.getFirst() + " - " + dicePosition.getSecond() + " to " + position.getFirst() + " - " + position.getSecond()));


                    wakeUpToolCardThread();

                    tapWheelResponse(hashMapGrid, 2);
                } else {
                    System.out.println("doubleMove");
                    Dice dice = patternCard.get(position.getFirst()).get(position.getSecond()).getDice();
                    patternCard.get(position.getFirst()).get(position.getSecond()).removeDice();
                    patternCard.get(position.getFirst()).get(position.getSecond()).insertDice(patternCard.get(dicePosition.getFirst()).get(dicePosition.getSecond()).getDice());
                    patternCard.get(dicePosition.getFirst()).get(dicePosition.getSecond()).removeDice();
                    patternCard.get(dicePosition.getFirst()).get(dicePosition.getSecond()).insertDice(dice);
                    setDoubleMove(true);

                    addMoveToHistoryAndNotify(new MoveStatus(currentRound.getCurrentPlayer().getPlayerUsername(), "placed the dice in " + dicePosition.getFirst() + " - " + dicePosition.getSecond() + " and " + position.getFirst() + " - " + position.getSecond()));


                    wakeUpToolCardThread();
                    tapWheelResponse(null, 3);
                }
            }
            if (phase == 2) {

                List<List<Box>> patternCard = currentRound.getCurrentPlayer().getPatternCard().getGrid();
                patternCard.get(position.getFirst()).get(position.getSecond()).insertDice(patternCard.get(dicePosition.getFirst()).get(dicePosition.getSecond()).getDice());
                patternCard.get(dicePosition.getFirst()).get(dicePosition.getSecond()).removeDice();

                addMoveToHistoryAndNotify(new MoveStatus(currentRound.getCurrentPlayer().getPlayerUsername(), "placed the dice from " + dicePosition.getFirst() + " - " + dicePosition.getSecond() + " to " + position.getFirst() + " - " + position.getSecond()));

                wakeUpToolCardThread();
                tapWheelResponse(null, 3);
            }
        }
    }

    /**
     * Set if there was a double move during the tool card use
     *
     * @param doubleMove
     */
    public void setDoubleMove(boolean doubleMove) {
        this.doubleMove.set(doubleMove);
    }

    /**
     * Method that notify the player in TapWheel.
     * Phase:
     * 1 - sends available positions(counting the double move);
     * 2 - sends available positions(without the double move);
     * 3 - sends the updated pattern card
     *
     * @param availablePositions
     * @param phase
     */
    private void tapWheelResponse(Map<String, Boolean[][]> availablePositions, int phase) {
        if (toolCardLock.get()) {
            if (phase == 1) {
                try {
                    getCurrentRound().getCurrentPlayer().getUserObserver().sendResponse(new TapWheelResponse(availablePositions, getCurrentRound().getCurrentPlayer(), 1));
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            if (phase == 2) {
                try {
                    getCurrentRound().getCurrentPlayer().getUserObserver().sendResponse(new TapWheelResponse(availablePositions, getCurrentRound().getCurrentPlayer(), 2));
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            if (phase == 3) {
                playerBroadcaster.broadcastResponseToAll(new PatternCardToolCardResponse(currentRound.getCurrentPlayer(), sendAvailablePositions((getCurrentRound().getCurrentPlayer()))));
                currentRound.toolCardMoveDone();
            }
        }
    }
}
