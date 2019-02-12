package sc.player2019.logic;

import sc.framework.plugins.Player;
import sc.plugin2019.*;
import sc.plugin2019.util.Constants;
import sc.plugin2019.util.GameRuleLogic;
import sc.shared.PlayerColor;
import sc.shared.WinCondition;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class Controller {
    //Color for print field
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BLUE = "\u001B[34m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_CYAN = "\u001B[36m";


    Player currentplayer;
    GameState gameState;
    ArrayList<Move> possibleMoves;
    Board board;

    public Controller(Player currentplayer, GameState gameState, ArrayList<Move> possibleMoves, Board board) {
        this.currentplayer = currentplayer;
        this.gameState = gameState;
        this.possibleMoves = possibleMoves;
        this.board = board;
    }

    //Zug simuliert durchführen
    public GameState domove(Move move) {
        GameState gameState1 = gameState.clone();
        try {
            move.perform(gameState1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return gameState1;
    }

    public void printarray(int[][] field) {
        for (int i = 0; i <= 9; i++) {
            int s = 9 - i;
            System.out.print(ANSI_CYAN + s + ANSI_RESET);
            for (int j = 0; j <= 9; j++) {
                if (field[j][s] == 0) {
                    System.out.print(field[j][s]);
                } else if (field[j][s] == 1) {
                    System.out.print(ANSI_RED + field[j][s] + ANSI_RESET);
                } else if (field[j][s] == 2) {
                    System.out.print(ANSI_BLUE + field[j][s] + ANSI_RESET);
                } else if (field[j][s] == 3) {
                    System.out.print(ANSI_GREEN + field[j][s] + ANSI_RESET);
                }
            }
            System.out.print("\n");
        }
        System.out.print(" ");
        for (int k = 0; k < 10; k++) {
            System.out.print(ANSI_CYAN + k + ANSI_RESET);
        }
        System.out.print("\n");
        System.out.print("\n");
    }

    //Zu getMovetoSwarm
    public Boolean Fieldcheck(int x, int y, Direction direction, Field field) {
        try {
            int distance = GameRuleLogic.calculateMoveDistance(board, x, y, direction);
            GameRuleLogic.isValidToMove(gameState, x, y, direction, distance);
            Field calField = GameRuleLogic.getFieldInDirection(board, x, y, direction, distance);
            if (calField.getX() == field.getX() && calField.getY() == field.getY()) {
                return true;
            }
        } catch (Exception ignored) {
        }
        return false;
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


    //Suche nach dem höchsten Wert
    public ArrayList<MovewithValue> sort(ArrayList<MovewithValue> tosort) {
        int n = tosort.size();
        int i, j, k, h;
        MovewithValue t;
        int[] cols = {1391376, 463792, 198768, 86961, 33936,
                13776, 4592, 1968, 861, 336, 112, 48, 21, 7, 3, 1};

        for (k=0; k<16; k++)
        {
            h=cols[k];
            for (i=h; i<n; i++)
            {
                j=i;
                t=tosort.get(j);
                while (j>=h &&tosort.get(j-h).value > t.value)
                {
                    tosort.set(j,tosort.get(j-h));
                    j=j-h;
                }
                tosort.set(j,t);
            }
        }
        return tosort;
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

    // überprüft ob der zug auf ein Feld in die Ecken geht und verhinder das in sort_paring_moves() das Ursprungsfeld als Partnerfeld betrachtet wird
    public boolean check_sites(Field f,Field field, int x, int y)
    {
        if (f.getX() == 0 && f.getY() == 0 || f.getX() == 0 && f.getY() == 9 || f.getX() == 9 && f.getY() == 0 || f.getX() == 9 && f.getY() == 9 || field.getX() == x && field.getY() == y)
        {
            return false;
        }
        else {return true;}
    }

    public void printMoveswithValues(ArrayList<MovewithValue> moveswithvalues){
        for (MovewithValue m : moveswithvalues){
            println(m.toString());
        }
    }

    private void println(String n){System.out.println(n);}
    private void print(String n){System.out.print(n);}
}
