package sc.player2019.logic;

import sc.plugin2019.GameState;
import sc.plugin2019.Move;

import java.util.ArrayList;

public class MovewithValue {
    Move testmove;
    int value;
    GameState gameState;
    ArrayList<Move> moves = new ArrayList<>();

    public MovewithValue(Move move, int value, GameState gameState) {
        this.testmove = move;
        this.value = value;
        this.gameState = gameState;
        this.moves.add(move);
    }

    public void newMove(Move m){
        moves.add(m);
    }

    public Move newestMove(){
        return moves.get(moves.size()-1);
    }

    public void newValueandGamestate(int value, GameState state){
        this.value += value;
        this.gameState = state;
    }

    public boolean compare(MovewithValue tocompare){
        if (!this.gameState.equals(tocompare.gameState) &&
            !this.testmove.equals(tocompare.testmove) &&
            this.value != tocompare.value &&
            !this.moves.equals(tocompare.moves)){
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "MovewithValue{" +
                "move=" + testmove +
                ", value=" + value +
                ", gameState=" + gameState +
                '}';
    }
}
