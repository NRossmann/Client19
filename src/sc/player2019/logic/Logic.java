package sc.player2019.logic;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sc.framework.plugins.Player;
import sc.player2019.Starter;
import sc.plugin2019.*;
import sc.plugin2019.util.Constants;
import sc.plugin2019.util.GameRuleLogic;
import sc.shared.GameResult;
import sc.shared.PlayerColor;
import sc.shared.WinCondition;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.util.*;


/**
 * Das Herz des Clients:
 * Eine sehr simple Logik, die ihre Zuege zufaellig waehlt,
 * aber gueltige Zuege macht. Ausserdem werden zum Spielverlauf
 * Konsolenausgaben gemacht.
 */
public class Logic implements IGameHandler {

  private Starter client;
  private GameState gameState;
  private Player currentPlayer;
  private Board board;

  //Color for print field
  private static final String ANSI_RED = "\u001B[31m";
  private static final String ANSI_RESET = "\u001B[0m";
  private static final String ANSI_BLUE = "\u001B[34m";
  private static final String ANSI_GREEN = "\u001B[32m";
  private static final String ANSI_CYAN = "\u001B[36m";


  final int midgamemyswarm = 4;
  final int midgameopsswarm = 1;
  final int endgamemyswarm = 4;
  final int endgameopsswarm = 1;

  private static final Logger log = LoggerFactory.getLogger(Logic.class);

  /**
   * Erzeugt ein neues Strategieobjekt, das zufaellige Zuege taetigt.
   *
   * @param client Der zugrundeliegende Client, der mit dem Spielserver
   *               kommuniziert.
   */
  public Logic(Starter client) {
    this.client = client;
  }

  /**
   * {@inheritDoc}
   */
  public void gameEnded(GameResult data, PlayerColor color, String errorMessage) {
    log.info("Das Spiel ist beendet.");
    log.info("Error Message: " + errorMessage);
    log.info("Winner: " + data.getWinners());
    log.info(currentPlayer.getColor().toString());

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onRequestAction() {
    long startTime = System.currentTimeMillis();
    PlayerColor mycolor = currentPlayer.getColor();
    log.info("Es wurde ein Zug angefordert.");
    ArrayList<Move> possibleMoves = GameRuleLogic.getPossibleMoves(gameState);


    //Spielfeld ausgeben
    int[][] field = getBoard(board);
    printarray(field);


    sendAction(getsendmove(possibleMoves, gameState.getTurn()));


    log.trace("Time needed for turn: {}", (System.nanoTime() - startTime) / 1000000);

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onUpdate(Player player, Player otherPlayer) {
    currentPlayer = player;
    log.info("Spielerwechsel: " + player.getColor());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onUpdate(GameState gameState) {
    this.gameState = gameState;

    currentPlayer = gameState.getCurrentPlayer();
    board = gameState.getBoard();
    log.info("Zug: {} Spieler: {}", gameState.getTurn(), currentPlayer.getColor());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void sendAction(Move move) {
    client.sendMove(move);
    log.info(move.toString());
  }


  //Gibt Spielfeld als int Array zurück
  int[][] getBoard(Board useboard) {
    try {
      int[][] field = new int[10][10];
      for (int i = 0; i <= 9; i++) {
        for (int j = 0; j <= 9; j++) {
          FieldState f = useboard.getField(i, j).getState();
          if (f == FieldState.EMPTY) {
            field[i][j] = 0;
          } else if (f == FieldState.RED) {
            field[i][j] = 1;
          } else if (f == FieldState.BLUE) {
            field[i][j] = 2;
          } else if (f == FieldState.OBSTRUCTED) {
            field[i][j] = 3;
          }
        }
      }
      return field;
    } catch (NullPointerException e) {
      e.printStackTrace();
      return new int[10][10];

    }

  }


  //Gibt den Zug zurück der gemacht werden soll
  private Move getsendmove(ArrayList<Move> possibleMoves, int turn) {

    ArrayList test = new ArrayList();


    if (turn < 10) {
      log.error("Early Game");
      return early_game_move();
    } else /*if (turn < 30)*/ {
      log.error("Mid Game");
      //Mid Game Logic
      int[] moveints = new int[possibleMoves.size()];
      for (int i = 0; i < possibleMoves.size(); i++) {
        moveints[i] = evaluateMove_midGame(possibleMoves.get(i));
      }
      int bestmove = search(moveints, moveints.length);

      return possibleMoves.get(bestmove);
    }/* else {
            log.error("late Game");
            //late Game Logic
            int[] moveints = new int[possibleMoves.size()];
            for (int i = 0; i < possibleMoves.size(); i++) {
                moveints[i] = evaluateMove(possibleMoves.get(i));
            }
            int bestmove = search(moveints, moveints.length);
            return possibleMoves.get(bestmove);
        }*/

  }

  //Ist der größte Schwarm in der Mitte?
  private boolean bigestswarmmiddle() {
    Set<Field> swarmmiddle = größterSchwarm(board, currentPlayer.getColor());
    Set<Field> biggestswarm = GameRuleLogic.greatestSwarm(board, currentPlayer.getColor());
    return swarmmiddle == biggestswarm;
  }


  // Weist jedem Zug einen Wert je nach seiner Nützlichkeit zu
  private int evaluateMove(Move move) {
    //Neues Game State Objekt
    GameState gameState1;
    //Rückgabe Variable
    int moveint = 0;

    //Schwarm größen vor dem Zug
    int swarmbefore = GameRuleLogic.greatestSwarmSize(gameState.getBoard(), currentPlayer.getColor());
    int opswarmbefore = GameRuleLogic.greatestSwarmSize(gameState.getBoard(), gameState.getOtherPlayerColor());

    //Zug hypothetisch durchführen
    gameState1 = domove(move);

    //Prüfen ob jemand gewonnen hat
    WinCondition winCondition = checkWinCondition(gameState1);
    if (winCondition != null) {
      PlayerColor winnercolor = winCondition.getWinner();
      if (winnercolor == currentPlayer.getColor()) {
        return 1000;
      } else {
        return -1000;
      }
    }

    //Schwarm größen nach dem Zug
    int swarmafter = GameRuleLogic.greatestSwarmSize(gameState1.getBoard(), currentPlayer.getColor());
    int opswarmafter = GameRuleLogic.greatestSwarmSize(gameState1.getBoard(), gameState1.getOtherPlayerColor());

    //Prüfen ob sich mein Schwarm vergößert hat
    if (swarmafter > swarmbefore) {
      moveint += ((swarmafter - swarmbefore) * (swarmafter - swarmbefore)) * endgamemyswarm;
    } else {
      moveint -= ((swarmafter - swarmbefore) * (swarmafter - swarmbefore)) * endgamemyswarm;
    }

    //Prüfen ob sich der Schwarm des Gegners vergrößert hat
    if (opswarmafter > opswarmbefore) {
      moveint -= ((opswarmafter - opswarmbefore) * (opswarmafter - opswarmbefore) * endgameopsswarm);
    } else {
      moveint += ((opswarmafter - opswarmbefore) * (opswarmafter - opswarmbefore) * endgameopsswarm);
    }

    return moveint;
  }

  //Mid Game Evaluate
  private int evaluateMove_midGame(Move move) {
    //Neues Game State Objekt
    GameState gameState1;
    //Rückgabe Variable
    int moveint = 0;

    //Schwarm größen vor dem Zug
    int swarmbefore = größterSchwarm(gameState.getBoard(), currentPlayer.getColor()).size();
    int opswarmbefore = größterSchwarm(gameState.getBoard(), gameState.getOtherPlayerColor()).size();

    //Zug hypothetisch durchführen
    gameState1 = domove(move);

    //Prüfen ob jemand gewonnen hat
    WinCondition winCondition = checkWinCondition(gameState1);
    if (winCondition != null) {
      PlayerColor winnercolor = winCondition.getWinner();
      if (winnercolor == currentPlayer.getColor()) {
        return 1000;
      } else {
        return -1000;
      }
    }

    //Schwarm größen nach dem Zug
    int swarmafter = größterSchwarm(gameState1.getBoard(), currentPlayer.getColor()).size();
    int opswarmafter = größterSchwarm(gameState1.getBoard(), gameState.getOtherPlayerColor()).size();

    //Prüfen ob sich mein Schwarm vergößert hat
    if (swarmafter > swarmbefore) {
      moveint += ((swarmafter - swarmbefore) * (swarmafter - swarmbefore)) * midgamemyswarm;
    } else {
      moveint -= ((swarmafter - swarmbefore) * (swarmafter - swarmbefore)) * midgamemyswarm;
    }

    //Prüfen ob sich der Schwarm des Gegners vergrößert hat
    if (opswarmafter > opswarmbefore) {
      moveint -= ((opswarmafter - opswarmbefore) * (opswarmafter - opswarmbefore) * midgameopsswarm);
    } else {
      moveint += ((opswarmafter - opswarmbefore) * (opswarmafter - opswarmbefore) * midgameopsswarm);
    }

    return moveint;
  }

  //Zug simuliert durchführen
  private GameState domove(Move move) {
    GameState gameState1 = gameState.clone();
    try {
      move.perform(gameState1);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return gameState1;
  }


  private void printarray(int[][] field) {
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


  private Player getWinner(GameState gameState1) {
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
  private int search(int[] arr, int n) {
    int s = 0;
    int j = 0;
    for (int i = 0; i < n; i++) {

      if (arr[i] > s){
        s = arr[i];
        j = i;
      }
    }

    return j;
  }

  public static Set<Field> größterSchwarm(Board board, PlayerColor player) {
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
      for (Field fi : neighbours) {
        swarm.add(fi);
      }
    }
    return GameRuleLogic.greatestSwarm(board, swarm);
  }

  private ArrayList<Move> getMovetoField(Field field) {
    ArrayList<Move> returnmoves = new ArrayList<>();
    Field[] ownFields = GameRuleLogic.getOwnFields(board, currentPlayer.getColor()).toArray(new Field[15]);
    for (Field ownField : ownFields) {
      if (ownField != null) {
        if (Fieldcheck(ownField.getX(), ownField.getY(), Direction.DOWN, field)) {
          returnmoves.add(new Move(ownField.getX(), ownField.getY(), Direction.DOWN));
        }

        if (Fieldcheck(ownField.getX(), ownField.getY(), Direction.DOWN_RIGHT, field)) {
          returnmoves.add(new Move(ownField.getX(), ownField.getY(), Direction.DOWN_RIGHT));
        }

        if (Fieldcheck(ownField.getX(), ownField.getY(), Direction.DOWN_LEFT, field)) {
          returnmoves.add(new Move(ownField.getX(), ownField.getY(), Direction.DOWN_LEFT));
        }

        if (Fieldcheck(ownField.getX(), ownField.getY(), Direction.LEFT, field)) {
          returnmoves.add(new Move(ownField.getX(), ownField.getY(), Direction.LEFT));
        }

        if (Fieldcheck(ownField.getX(), ownField.getY(), Direction.RIGHT, field)) {
          returnmoves.add(new Move(ownField.getX(), ownField.getY(), Direction.RIGHT));
        }

        if (Fieldcheck(ownField.getX(), ownField.getY(), Direction.UP, field)) {
          returnmoves.add(new Move(ownField.getX(), ownField.getY(), Direction.UP));
        }

        if (Fieldcheck(ownField.getX(), ownField.getY(), Direction.UP_RIGHT, field)) {
          returnmoves.add(new Move(ownField.getX(), ownField.getY(), Direction.UP_RIGHT));
        }

        if (Fieldcheck(ownField.getX(), ownField.getY(), Direction.UP_LEFT, field)) {
          returnmoves.add(new Move(ownField.getX(), ownField.getY(), Direction.UP_LEFT));
        }

      }
    }

    return returnmoves;
  }


  //Zu getMovetoSwarm
  private Boolean Fieldcheck(int x, int y, Direction direction, Field field) {
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

  public Move get_good_move(ArrayList<Move> moves) {
    for (Move m : moves) {
      int distance = GameRuleLogic.calculateMoveDistance(board, m.x, m.y, m.direction);
      Field destination = GameRuleLogic.getFieldInDirection(board, m.x, m.y, m.direction, distance);
      for (int k = destination.getX() - 1; k < destination.getX() + 1; k++) {
        for (int i = destination.getY() - 1; i < destination.getY() + 1; i++) {
          try {
            FieldState f = board.getField(k, i).getState();
            if (gameState.getCurrentPlayerColor() == PlayerColor.BLUE) {
              if (f == FieldState.BLUE) {
                log.error("logic");
                return m;
              }
            } else {
              if (f == FieldState.RED) {
                log.error("logic");
                return m;
              }
            }

          } catch (Exception e) {

          }

        }
      }

    }
    Random rand = new Random();
    log.error("not logic");
    return moves.get(rand.nextInt(moves.size()));


  }

  public Move early_game_move() {

    int position = 5;

    int runde = 0;

    boolean moveFound = false;
    while (!moveFound) {
      for (int i = position - runde; i <= position; i++) {
        for (int j = position - runde; j <= position + runde; j++) {

          Field f = new Field(i, j);
          ArrayList<Move> m = getMovetoField(f);
          if (m.size() > 0) {
            moveFound = !moveFound;

            return get_good_move(m);

          }
        }

      }

      runde++;
    }
    return null;
  }

  private static Set<Field> getDirectNeighbour(Board board, Field f) {
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

}


