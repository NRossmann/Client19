package sc.player2019.logic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sc.framework.plugins.Player;
import sc.player2019.Starter;
import sc.plugin2019.*;
import sc.plugin2019.util.GameRuleLogic;
import sc.shared.GameResult;
import sc.shared.PlayerColor;
import sc.shared.WinCondition;

import java.util.ArrayList;
import java.util.Set;

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
    log.info("Error Message: "+errorMessage);
    log.info("Winner: "+data.getWinners());
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
    int[][]field = getBoard(board);
    printarray(field);


    sendAction(getsendmove(possibleMoves));



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
  public void sendAction(Move move) {client.sendMove(move);log.info(move.toString());}


  //Gibt Spielfeld als int Array zurück
   int[][] getBoard(Board useboard){
    try {
      int[][] field = new int[10][10];
      for (int i = 0; i <= 9; i++) {
        for (int j = 0; j <= 9; j++) {
          FieldState f = useboard.getField(i,j).getState();
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
    }catch (NullPointerException e) {
      e.printStackTrace();
      return new int[10][10];

    }

  }

  //Zu getMovetoSwarm
  private ArrayList<Move> getMovetoField(Field field){
    ArrayList<Move> returnmoves = new ArrayList<>();
    Field[] ownFields = GameRuleLogic.getOwnFields(board,currentPlayer.getColor()).toArray(new Field[15]);
    for (Field ownField : ownFields) {
      if (ownField != null) {
        if (Fieldcheck(ownField.getX(), ownField.getY(), Direction.DOWN, field)) {
          returnmoves.add( new Move(ownField.getX(), ownField.getY(), Direction.DOWN));
        }

        if (Fieldcheck(ownField.getX(), ownField.getY(), Direction.DOWN_RIGHT, field)) {
          returnmoves.add( new Move(ownField.getX(), ownField.getY(), Direction.DOWN_RIGHT));
        }

        if (Fieldcheck(ownField.getX(), ownField.getY(), Direction.DOWN_LEFT, field)) {
          returnmoves.add( new Move(ownField.getX(), ownField.getY(), Direction.DOWN_LEFT));
        }

        if (Fieldcheck(ownField.getX(), ownField.getY(), Direction.LEFT, field)) {
          returnmoves.add( new Move(ownField.getX(), ownField.getY(), Direction.LEFT));
        }

        if (Fieldcheck(ownField.getX(), ownField.getY(), Direction.RIGHT, field)) {
          returnmoves.add( new Move(ownField.getX(), ownField.getY(), Direction.RIGHT));
        }

        if (Fieldcheck(ownField.getX(), ownField.getY(), Direction.UP, field)) {
          returnmoves.add( new Move(ownField.getX(), ownField.getY(), Direction.UP));
        }

        if (Fieldcheck(ownField.getX(), ownField.getY(), Direction.UP_RIGHT, field)) {
          returnmoves.add( new Move(ownField.getX(), ownField.getY(), Direction.UP_RIGHT));
        }

        if (Fieldcheck(ownField.getX(), ownField.getY(), Direction.UP_LEFT, field)) {
          returnmoves.add( new Move(ownField.getX(), ownField.getY(), Direction.UP_LEFT));
        }

      }
    }

    return returnmoves;
    }


  //Zu getMovetoSwarm
  private Boolean Fieldcheck(int x, int y, Direction direction, Field field){
    try{
      int distance = GameRuleLogic.calculateMoveDistance(board, x, y, direction);
      GameRuleLogic.isValidToMove(gameState, x, y, direction, distance);
      Field calField = GameRuleLogic.getFieldInDirection(board, x, y, direction, distance);
      if (calField.getX() == field.getX() && calField.getY() == field.getY()){
        return true;
      }
    }catch (Exception ignored){
    }
    return false;
  }

  //Gibt Züge zurück die zu dem eigenen größten Schwarm führen
  private ArrayList<Move> getMovetoSwarm(Player player){
    Set<Field> swarmset = GameRuleLogic.greatestSwarm(board,player.getColor());
    Field[] swarm = swarmset.toArray(new Field[0]);
    ArrayList<Field> amswarm = new ArrayList<>();
    ArrayList<Move> movestoswarm = new ArrayList<>();
    for (Field checkfield : swarm) {
      for(int i = checkfield.getX()-1;i<=checkfield.getX()+1;i++){
        for(int j = checkfield.getY()-1;j<=checkfield.getY()+1;j++) {
          if (!swarmset.contains(new Field(i, j))) {
            amswarm.add(new Field(i, j));
          }
        }
      }
    }

    for (Field anAmswarm : amswarm) {
      try {
        ArrayList<Move> tofield = getMovetoField(anAmswarm);
        for (Move aTofield : tofield) {
          Field startfield = new Field(aTofield.x, aTofield.y);
          if (!swarmset.contains(startfield)) {
            movestoswarm.add(aTofield);
          }
        }
      } catch (NullPointerException e) {
        log.debug(e.getMessage());
      }

    }
    return movestoswarm;
  }


  //Gibt den Zug zurück der gemacht werden soll
  private Move getsendmove(ArrayList<Move> possibleMoves){
    int[] moveints = new int[possibleMoves.size()];
    for (int i = 0; i<possibleMoves.size();i++){
      moveints[i]=evaluateMove(possibleMoves.get(i));
    }
    int bestmove = search(moveints,moveints.length);

    return possibleMoves.get(bestmove);

  }





    // Weist jedem Zug einen Wert je nach seiner Nützlichkeit zu
  private int evaluateMove(Move move){
    //Neues Game State Objekt
    GameState gameState1 = gameState.clone();
    //Rückgabe Variable
    int moveint = 0;

    //Schwarm größen vor dem Zug
    int swarmbefore = GameRuleLogic.greatestSwarmSize(gameState1.getBoard(),currentPlayer.getColor());
    int opswarmbefore = GameRuleLogic.greatestSwarmSize(gameState1.getBoard(),gameState.getOtherPlayerColor());

    //Zug hypothetisch durchführen
    gameState1 = domove(move);

    //Prüfen ob jemand gewonnen hat
    WinCondition winCondition = checkWinCondition(gameState1);
    if (winCondition!=null){
      PlayerColor winnercolor = winCondition.getWinner();
      if (winnercolor == currentPlayer.getColor()){
        return 1000;
      }else {
        return -1000;
      }
    }

    //Schwarm größen nach dem Zug
    int swarmafter = GameRuleLogic.greatestSwarmSize(gameState1.getBoard(),currentPlayer.getColor());
    int opswarmafter = GameRuleLogic.greatestSwarmSize(gameState1.getBoard(),currentPlayer.getColor());

    //Prüfen ob sich mein Schwarm vergößert hat
    if (swarmafter>swarmbefore){
      moveint += ((swarmafter-swarmbefore)*(swarmafter-swarmbefore))*2;
    }else{
      moveint -= ((swarmafter-swarmbefore)*(swarmafter-swarmbefore))*2;
    }

    //Prüfen ob sich der Schwarm des Gegners vergrößert hat
    if (opswarmafter>opswarmbefore){
      moveint -= ((opswarmafter-opswarmbefore)*(opswarmafter-opswarmbefore));
    }else {
      moveint += ((opswarmafter-opswarmbefore)*(opswarmafter-opswarmbefore));
    }

    return moveint;
  }

  //Zug simuliert durchführen
  private GameState domove(Move move){
    GameState gameState1 = gameState.clone();
    try {
      move.perform(gameState1);
    }catch (Exception e){
      e.printStackTrace();
    }
    return gameState1;
  }



  private void printarray(int[][] field){
    for (int i = 0;i<=9 ; i++) {
      int s = 9-i;
      System.out.print(ANSI_CYAN+s+ANSI_RESET);
      for (int j = 0; j<=9;j++){
        if (field[j][s]==0) {
          System.out.print(field[j][s]);
        } else if (field[j][s]==1) {
          System.out.print(ANSI_RED+field[j][s]+ANSI_RESET);
        } else if (field[j][s]==2) {
          System.out.print(ANSI_BLUE+field[j][s]+ANSI_RESET);
        } else if (field[j][s]==3) {
          System.out.print(ANSI_GREEN+field[j][s]+ANSI_RESET);
        }
      }
      System.out.print("\n");
    }
    System.out.print(" ");
    for (int k = 0; k<10;k++){
      System.out.print(ANSI_CYAN+k+ANSI_RESET);
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
  private int search(int arr[], int n)
  {
    int s = 0;
    for (int i = 0; i < n; i++) {

      if (arr[i] > s)
        s = arr[i];
    }

    return s;
  }
}


