package sc.player2019.logic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sc.framework.plugins.Player;
import sc.plugin2019.*;
import sc.plugin2019.util.Constants;
import sc.plugin2019.util.GameRuleLogic;
import sc.shared.InvalidGameStateException;
import sc.shared.InvalidMoveException;
import sc.shared.PlayerColor;
import sc.shared.WinCondition;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static sc.plugin2019.util.GameRuleLogic.*;

public class SimulatedMove {
    Move move;
    GameState gameState;
    Board board;
    Player currentPlayer;
    Player opponent;
    GameState afterMove;
    Logger log;

    final int midgamemyswarm = 5;
    final int midgameopsswarm = 1;
    final int midgameVerbindung = 3;


    public SimulatedMove(Move move, GameState gameState) {
        log = LoggerFactory.getLogger(SimulatedMove.class);
        this.move = move;
        this.gameState = gameState;
        this.board = gameState.getBoard();
        this.currentPlayer = gameState.getCurrentPlayer();
        this.opponent = gameState.getOpponent(currentPlayer);
    }

    public MovewithValue evaluate() {
        //Neues Game State Objekt
        GameState gameState1;
        //Rückgabe Variable
        int moveint = 0;

        //Schwarm größen vor dem Zug
        int swarmbefore = größterSchwarm(gameState.getBoard(), this.currentPlayer.getColor()).size();
        int opswarmbefore = größterSchwarm(gameState.getBoard(), gameState.getOtherPlayerColor()).size();

        //Zug hypothetisch durchführen
        gameState1 = domove(move);
        afterMove = gameState1;
        //Prüfen ob jemand gewonnen hat
        WinCondition winCondition = checkWinCondition(gameState1);
        if (winCondition != null) {
            PlayerColor winnercolor = winCondition.getWinner();
            if (winnercolor == currentPlayer.getColor()) {
                return new MovewithValue(this.move,Integer.MAX_VALUE,gameState1);
            } else {
                return new MovewithValue(this.move,Integer.MIN_VALUE,gameState1);
            }
        }

        //Schwarm größen nach dem Zug
        int swarmafter = größterSchwarm(gameState1.getBoard(), currentPlayer.getColor()).size();
        int opswarmafter = größterSchwarm(gameState1.getBoard(), gameState.getOtherPlayerColor()).size();

        //Prüfen ob sich mein Schwarm vergößert hat
        int moveint1 = ((swarmafter - swarmbefore) * (swarmafter - swarmbefore)) * midgamemyswarm;
        if (swarmafter > swarmbefore) {
            moveint += moveint1;
            moveint += guteVerbindung(gameState1.getBoard(), move, currentPlayer.getColor()) * midgameVerbindung;
        } else {
            moveint -= moveint1;
        }

        //Prüfen ob sich der Schwarm des Gegners vergrößert hat
        int moveint2 = (opswarmafter - opswarmbefore) * (opswarmafter - opswarmbefore) * midgameopsswarm;
        if (opswarmafter > opswarmbefore) {
            moveint -= moveint2;
        } else {
            moveint += moveint2;
        }
        return new MovewithValue(this.move,moveint,gameState1);

    }


    public Set<Field> größterSchwarm(Board board, PlayerColor player) {
        Set<Field> occupiedFields = GameRuleLogic.getOwnFields(board, player);
        Set<Field> returnFields = new HashSet<>();
        Set<Field> falseFields = new HashSet<>();


        for (Field f : occupiedFields) {
            if (f.getX() != 0 && f.getY() != 0 && f.getX() != 9 && f.getY() != 9) {
                returnFields.add(f);
            } else {
                falseFields.add(f);
            }
        }
        Set<Field> innerswarmSet = GameRuleLogic.greatestSwarm(board, returnFields);
        Field[] innerswarm = new Field[innerswarmSet.size()];
        int i = 0;
        for (Field f : innerswarmSet) {
            innerswarm[i]=f;
            i++;
        }
        Set<Field> swarm = GameRuleLogic.greatestSwarm(board, returnFields);
        for (i = 0; i < innerswarm.length; i++) {
            Set<Field> neighbours = getDirectNeighbour(board, innerswarm[i]);
            swarm.addAll(neighbours);
        }
        return GameRuleLogic.greatestSwarm(board, swarm);
    }

    private int guteVerbindung(Board board, Move move, PlayerColor currentplayercolor) {
        int outval = 0;
        Field field = movetoField(board,move);
        Set<Field> neighbours = getDirectNeighbour(board,field);
        for (Field f : neighbours){
            if (currentplayercolor == PlayerColor.BLUE){
                if (f.getState() == FieldState.BLUE){
                    outval++;
                }
            }else{
                if (f.getState() == FieldState.RED){
                    outval++;
                }
            }
        }
        return outval;
    }

    private Field movetoField(Board board, Move move){
        int distance = GameRuleLogic.calculateMoveDistance(board,move.x,move.y,move.direction);
        return GameRuleLogic.getFieldInDirection(board,move.x,move.y,move.direction,distance);
    }

    public Set<Field> getDirectNeighbour(Board board, Field f) {
        Set<Field> returnSet = new HashSet<>();
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                int x = f.getX() + i;
                int y = f.getY() + j;
                if (x < 0 || x >= Constants.BOARD_SIZE || y < 0 || y >= Constants.BOARD_SIZE || (i == 0 && j == 0))
                    continue;

                Field field = board.getField(x, y);
                returnSet.add(field);

            }
        }
        return returnSet;
    }

    //Zug simuliert durchführen
    public GameState domove(Move move) {
        GameState gameState1 = gameState.clone();
//        log.error(gameState1.toString());
        try {
            gameState1 = perform(gameState1, move);
        } catch (Exception e) {
            e.printStackTrace();
        }

//        log.error(gameState1.toString());
        return gameState1;
    }



    //Testen ob jemand Gewonnen hat in hypothetischem GameState
    public WinCondition checkWinCondition(GameState gameState1) {
        int[][] stats = gameState1.getGameStats();
        if (gameState1.getTurn() % 2 == 1) {
            return null;
        } else {
            Player winningPlayer;
            if (gameState1.getTurn() < 60) {
                winningPlayer = this.getWinner(gameState1);
                return winningPlayer != null ? new WinCondition(winningPlayer.getColor(), "Das Spiel ist beendet.\nEin Spieler hat seinen Schwarm vereint.") : null;
            } else {
                winningPlayer = this.getWinner(gameState1);
                if (winningPlayer != null) {
                    return new WinCondition(winningPlayer.getColor(), "Das Spiel ist beendet.\nEin Spieler hat seinen Schwarm vereint.");
                } else {
                    PlayerColor winner;
                    if (stats[0][0] > stats[1][0]) {
                        winner = PlayerColor.RED;
                    } else if (stats[0][0] < stats[1][0]) {
                        winner = PlayerColor.BLUE;
                    } else {
                        winner = null;
                    }

                    return new WinCondition(winner, "Das Rundenlimit wurde erreicht.");
                }
            }
        }
    }


    public Player getWinner(GameState gameState1) {
        if (GameRuleLogic.isSwarmConnected(gameState1.getBoard(), PlayerColor.RED)) {

            if (GameRuleLogic.isSwarmConnected(gameState1.getBoard(), PlayerColor.BLUE)) {

                if (gameState1.getPointsForPlayer(PlayerColor.RED) > gameState1.getPointsForPlayer(PlayerColor.BLUE)) {
                    return gameState1.getPlayer(PlayerColor.RED);
                } else if (gameState1.getPointsForPlayer(PlayerColor.RED) < gameState1.getPointsForPlayer(PlayerColor.BLUE)) {
                    return gameState1.getPlayer(PlayerColor.BLUE);
                } else {

                    return null;
                }
            } else {
                return gameState1.getPlayer(PlayerColor.RED);
            }
        } else {

            if (GameRuleLogic.isSwarmConnected(gameState1.getBoard(), PlayerColor.BLUE)) {

                return gameState1.getPlayer(PlayerColor.BLUE);
            } else {

                return null;
            }
        }
    }

    public GameState perform(GameState state, Move move) throws InvalidMoveException, InvalidGameStateException {
        int distance = calculateMoveDistance(state.getBoard(), move.x, move.y, move.direction);
        if (GameRuleLogic.isValidToMove(state, move.x, move.y, move.direction, distance)) {
            Field start = state.getField(move.x, move.y);
            Field destination = getFieldInDirection(state.getBoard(), move.x, move.y, move.direction, distance);
            start.setState(FieldState.EMPTY);
            destination.setPiranha(state.getCurrentPlayerColor());
        }

        // Bereite nächsten Zug vor
        state.setLastMove(move);
        state.setTurn(state.getTurn() + 1);
        state.switchCurrentPlayer();
        return state;
    }

}
