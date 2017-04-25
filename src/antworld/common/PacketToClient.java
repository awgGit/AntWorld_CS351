package antworld.common;

import java.io.Serializable;
import java.util.ArrayList;

/**
 */
public class PacketToClient implements Serializable
{
  private static final long serialVersionUID = Constants.VERSION;

  /**
   * AntWorld is a real-time game with a minimum time between turns equal to
   * antworld.common.Constants.TIME_STEP_MSEC.<br>
   * When the server starts, gameTick = 0. Each time the server sends an update to its
   * set of clients, gameTick is incremented by 1.
   */
  public volatile int tick;

  /** Time, in seconds, of the current gameTick.<br>
   * This is set using the Java Virtual Machine's high-resolution time source, in nanoseconds.
   * This method provides nanosecond precision, but not necessarily nanosecond resolution
   * (that is, how frequently the value changes) - no guarantees are made except that
   * the resolution is at least as good as that of currentTimeMillis().
   * This value will becomes unpredictable if the game runs over approximately 292 years.
   */
  public volatile double tickTime;
  
  /** If not null, then an error has occurred.  Display it! */
  public volatile String errorMsg = null;

  public final NestNameEnum myNest;
  
  /**
   * List of ants that belong to this nest.<br>
   * When an ant dies, the server will include it in this list (with state==DEAD)
   * only for the next update.<br><br>
   * If an ant's action is NOOP or BUSY for two or more consecutive ticks, it will
   * only be included in <tt>myAntList</tt> if something about its state changed since
   * the last update when it was included.
   */
  public volatile ArrayList<AntData> myAntList = new ArrayList<>();


  /**
   * Nest data for each nest in the game.<br>
   *   Index this array with: <br>
   * nestData[NestNameEnum.ordinal]<br><br>
   *
   * If a nest has not been claimed by a team, then its corresponding
   *   element is set to null.<br>
   * If there was no change in a particular nest since the last update, then the corresponding
   *   element is set to null.
   */
  public volatile NestData[] nestData;


  /** A list of all enemy ants that were visible to one or more of this team's ants
   * during the most recent tick.
   * Note: if none of your ants see any enemy ants, then this is null.*/
  public volatile ArrayList<AntData> enemyAntList;

  /** A list of all food piles (including water droplets) that were visible to one or
   * more of this team's ants during the most recent tick.
   * Note: if none of your ants see any food, then this is null.*/
  public volatile ArrayList<FoodData> foodList;


  public PacketToClient(NestNameEnum myNest)
  {
    this.myNest = myNest;
  }

  // Also not used...
  public PacketToClient(PacketToClient source)
  {

    myNest = source.myNest;
    tick = source.tick;
    tickTime = source.tickTime;
    errorMsg = source.errorMsg;
    myAntList = new ArrayList<>();
    for (AntData ant : source.myAntList)
    {
      myAntList.add(new AntData(ant));
    }

    nestData = new NestData[source.nestData.length];
    for (int i = 0; i<source.nestData.length; i++)
    {
      nestData[i] = new NestData(source.nestData[i]);
    }


    if (source.enemyAntList != null)
    {
      enemyAntList = new ArrayList<>();
      for (AntData ant : source.enemyAntList)
      {
        enemyAntList.add(new AntData(ant));
      }
    }


    if (source.foodList != null)
    {
      foodList = new ArrayList<>();
      for (FoodData food : source.foodList)
      {
        foodList.add(new FoodData(food));
      }
    }
  }

  public String toString()
  {
    String out = "PacketToClient["+serialVersionUID+":"+tick+":]: "+ myNest + "\n  ";
    if (errorMsg != null)
    { out = out + "**ERROR**: " + errorMsg + "\n  ";
    }
    
    out = out+ "myAntList:";
    for (AntData ant : myAntList)
    { out = out + "\n     " + ant;
    }
    if (enemyAntList != null)
    {  out = out + "\n  enemyAntList:";
      for (AntData ant : enemyAntList)
      { out = out + "\n     " + ant;
      }
    }
    if (foodList != null)
    { out = out + "\n  FoodList:";
      for (FoodData food : foodList)
      { out = out + "\n     " + food;
      }
    }
    return out;
  }
}
