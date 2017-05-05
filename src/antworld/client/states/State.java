package antworld.client.states;
import antworld.client.A_Star;
import antworld.common.*;

/**
 * Created by ryanvary on 4/30/17.
 */
public abstract class State
{
  public boolean enemyAntsInArea() {return false;}
  public boolean determineIfMovementPossible(AntData ant, Direction direction)
  {
    int x = direction.deltaX();
    int y = direction.deltaY();
    if(A_Star.board[x+ant.gridX][y+ant.gridY] != null) return true;
    else return false;
  }

  public abstract boolean rejuvenateHealth();
  public abstract boolean foodInSight();
  public abstract void move();
}
