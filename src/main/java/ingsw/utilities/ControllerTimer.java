package ingsw.utilities;

import ingsw.controller.Controller;
import ingsw.controller.network.commands.TimeOutResponse;
import ingsw.model.GameManager;
import ingsw.model.cards.patterncard.PatternCard;

import java.rmi.RemoteException;
import java.util.*;

public class ControllerTimer {
    private Timer timer;
    private static ControllerTimer controllerTimer;

    public ControllerTimer() {
        this.timer = new Timer("TimerThread");
    }

    public void startLoginTimer(int loginSeconds, Controller controller, boolean hasStarted) {
        timer.schedule(new LaunchMatch(controller, hasStarted), (long) loginSeconds * 1000);
    }

    public void startTurnTimer(int turnSeconds, GameManager gameManager) {
        timer.schedule(new EndTurn(gameManager), (long) turnSeconds * 1000);
    }

    public void startPatternCardTimer(int patternCardSeconds, GameManager gameManager, Map<String, List<PatternCard>> patternCards) {
        timer.schedule(new ChoosePatternCard(gameManager, patternCards), (long) patternCardSeconds * 1000 );
    }

    public void startDraftedDiceTimer(GameManager gameManager){
        timer.schedule(new DraftDiceTask(gameManager),(long) 20 * 1000);
    }

    /**
     * Method to stop the timer that is running
     */
    public void cancelTimer() {
        timer.cancel();
    }

    /**
     * Task class for Launch Match Timer
     */
    public class LaunchMatch extends TimerTask {
        Controller controller;
        boolean hasStarted;

        LaunchMatch(Controller controller, boolean hasStarted) {
            this.controller = controller;
            this.hasStarted = hasStarted;
        }

        @Override
        public void run() {
            hasStarted = true;
            controller.createMatch();
        }
    }

    /**
     * Task class for choosing Pattern Card Timer
     */
    class ChoosePatternCard extends TimerTask {
        GameManager gameManager;
        Map<String, List<PatternCard>> patternCards;

        public ChoosePatternCard(GameManager gameManager, Map<String, List<PatternCard>> patternCards) {
            this.gameManager = gameManager;
            this.patternCards = patternCards;
        }

        /**
         * When the time to choose the pattern card expires this task runs
         * It calls the gameManager method to choose randomly the pattern card for the players
         */
        @Override
        public void run() {
            gameManager.randomizePatternCards(patternCards);
        }
    }

    /**
     * Task class for draft the Dice automatically if player waits too much time
     */
    class DraftDiceTask extends TimerTask {

        GameManager gameManager;

        public DraftDiceTask(GameManager gameManager){
            this.gameManager = gameManager;
        }

        @Override
        public void run() {
            gameManager.draftDiceFromBoard();
        }
    }

    /**
     * Task class for End Turn Timer
     */
    class EndTurn extends TimerTask {
        GameManager gameManager;

        EndTurn(GameManager gameManager) {
            this.gameManager = gameManager;
        }

        @Override
        public void run() {

            try {
                gameManager.getCurrentRound().getCurrentPlayer().getUserObserver().sendResponse(new TimeOutResponse());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            gameManager.endTurn(gameManager.getCurrentRound().getCurrentPlayer().getPlayerUsername());
        }
    }
}
