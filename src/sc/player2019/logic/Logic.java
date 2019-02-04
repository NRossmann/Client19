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




  final int midgamemyswarm = 10;
  final int midgameopsswarm = 1;
  final int midgameVerbindung = 5;
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
  Controller controller;
  /**
   * {@inheritDoc}
   */
  @Override
  public void onRequestAction() {
    long startTime = System.currentTimeMillis();
    PlayerColor mycolor = currentPlayer.getColor();
    log.info("Es wurde ein Zug angefordert.");
    ArrayList<Move> possibleMoves = GameRuleLogic.getPossibleMoves(gameState);
    controller = new Controller(currentPlayer,gameState,possibleMoves,board);

    //Spielfeld ausgeben
    int[][] field = getBoard(board);
    controller.printarray(field);


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
    if (turn < 5) {
      log.error("Early Game");
      return early_game_move(possibleMoves);
    } else  {
      log.error("Mid Game");
      //Mid Game Logic
      int[] moveints = new int[possibleMoves.size()];
      for (int i = 0; i < possibleMoves.size(); i++) {
        moveints[i] = new SimulatedMove(possibleMoves.get(i),gameState,0).evaluate();
      }
      int bestmove = controller.search(moveints, moveints.length);
      controller.printMoveswithValues(moveints);
      return possibleMoves.get(bestmove);
    }

  }




    public boolean check_use(Move move)
    {
        int distance = GameRuleLogic.calculateMoveDistance(board, move.x, move.y, move.direction);
        Field field = GameRuleLogic.getFieldInDirection(board, move.x, move.y, move.direction, distance);

        if (Math.abs(field.getX()- 5) < Math.abs(move.x-5) &&  Math.abs(field.getY()-5) <= Math.abs(move.y-5) || Math.abs(field.getY()-5) < Math.abs(move.y-5) && Math.abs(field.getX()- 5) <= Math.abs(move.x-5))
        {

            return true;
        }
        else
        {
            return false;
        }
    }
    //gibt aus einem Array gegebener Züge die Züge zurück die Auf ein gegenes Feld führen
    public ArrayList<Move> get_Move_to_Field(Field field, ArrayList<Move> moves)
    {

        ArrayList<Move> return_moves = new ArrayList();
        for(Move m: moves) {
            int distance = GameRuleLogic.calculateMoveDistance(board, m.x, m.y, m.direction);
            Field destination = GameRuleLogic.getFieldInDirection(board, m.x, m.y, m.direction, distance);

            if(destination.getX() == field.getX() && destination.getY() == field.getY())
            {
                return_moves.add(m);
            }
        }
        return return_moves;
    }

    // gibt den besten Zug fürs earlygame zurück
    public Move early_game_move(ArrayList<Move> possiblemoves)
    {
        ArrayList moves = sort_paring_moves(possiblemoves);

        if (moves.size() == 0) {moves = possiblemoves; System.out.println("warning");}
        System.out.println(moves.toString());

        int position = 4;
        int runde = 0;

        boolean moveFound = false;
        ArrayList<Move> final_moves = new ArrayList();
        while(runde < 5)
        {
            for (int i = position - runde; i <= position + 1 + runde; i++)
            {
                for (int j = position - runde; j <= position + 1 + runde; j++)
                {


                    Field f = board.getField(i, j);
                    final_moves.addAll(get_Move_to_Field(f, moves));
                }
            }
            if (final_moves.size() > 0)
            {

                moveFound = !moveFound;
                Random rand = new Random();
                return final_moves.get(rand.nextInt(final_moves.size()));
            }
            runde++;
        }
        return null;
    }
    //gibt aus allen möglichen zügen eine ArrayList mit zügen bei denen ein Nachbarfisch vorhanden ist
    public ArrayList<Move> sort_paring_moves(ArrayList<Move> possiblemoves){

        ArrayList<Move> paring_moves = new ArrayList();
        ArrayList<Field> used_piranias = new ArrayList();
        for (Move m: possiblemoves)
        {
            // berechnung des Zielfeldes
            int distance = GameRuleLogic.calculateMoveDistance(board, m.x, m.y, m.direction);
            Field destination = GameRuleLogic.getFieldInDirection(board, m.x, m.y, m.direction, distance);

            // berechnet die Nachbarfelder des Zielfeldes
            for(int k = destination.getX() - 1; k < destination.getX() + 1; k++)
            {
                for(int i = destination.getY() - 1; i < destination.getY() + 1; i++)
                {
                    //überprüft ob auf dem Nachbarfeld ein Fischder gleichen Farbe ist
                    try
                    {
                        Field field = board.getField(k, i);
                        FieldState f = field.getState();
                        if(gameState.getCurrentPlayerColor() == PlayerColor.BLUE)
                        {
                            if (f == FieldState.BLUE && controller.check_sites(destination,field, m.x, m.y)  && check_use(m) ){
                                paring_moves.add(m);
                                used_piranias.add(destination);
                                break;
                            }
                        }
                        else
                        {
                            if(f == FieldState.RED && controller.check_sites(destination,field, m.x, m.y) && check_use(m) )
                            {
                                paring_moves.add(m);
                                used_piranias.add(destination);
                                break;
                            }
                        }
                    }
                    catch (Exception e){}
                }
            }
        }

        return paring_moves;
    }



}


