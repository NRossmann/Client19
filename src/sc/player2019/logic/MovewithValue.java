package sc.player2019.logic;

import sc.plugin2019.GameState;
import sc.plugin2019.Move;

public class MovewithValue {
    Move move;
    int value;
    GameState gameState;

    public MovewithValue(Move move, int value, GameState gameState) {
        this.move = move;
        this.value = value;
        this.gameState = gameState;
    }

    @Override
    public String toString() {
        return "MovewithValue{" +
                "move=" + move.toString() +
                ", value=" + value +
                '}';
    }
}
