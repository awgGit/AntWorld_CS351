package antworld.server;

import java.io.Serializable;
import java.util.Random;

import antworld.common.Constants;
import antworld.common.FoodData;
import antworld.common.GameObject.GameObjectType;
import antworld.common.LandType;

public class FoodSpawnSite implements Serializable
{
  private static final long serialVersionUID = Constants.VERSION;
  private final int locationX, locationY;

  private static final int SPAWN_RADIUS = 30;
  private static final int MAX_SIMULTANEOUS_PILES_FROM_SITE = 5;
  private Cell[] pileList = new Cell[MAX_SIMULTANEOUS_PILES_FROM_SITE];
  private static final int MIN_SPAWN_UNITS = 20;
  private static final int MAX_SPAWN_UNITS = 200;

  private static Random random = Constants.random;
  
  public FoodSpawnSite(AntWorld world, int x, int y)
  {
    this.locationX = x;
    this.locationY = y;

  }
  
  public int getLocationX() {return locationX;}
  public int getLocationY() {return locationY;}


  /**
   * This method will sometimes spawn food.
   * A random number from 0 through MAX_SIMULTANEOUS_PILES_FROM_SITE is chosen.<br>
   * If pileList[] at the chosen index is either null or does no contain food,
   * then maxAttempts will me made to spawn food in a random location within range
   * of this site.<br>
   * Thus, the chance of spawning food decreases as the number of sites with food increases.
   * @param world
   */
  public void spawn(AntWorld world)
  {
    int siteIdx = random.nextInt(MAX_SIMULTANEOUS_PILES_FROM_SITE);
    if (pileList[siteIdx] != null)
    {
      if (pileList[siteIdx].getFoodUnits() > 0) return;
    }

    int count = MIN_SPAWN_UNITS + random.nextInt(MAX_SPAWN_UNITS - MIN_SPAWN_UNITS);

    int maxAttempts = 100;
    for (int n=0; n<maxAttempts; n++)
    {
      int x = locationX + random.nextInt(SPAWN_RADIUS) - random.nextInt(SPAWN_RADIUS);
      int y = locationY + random.nextInt(SPAWN_RADIUS) - random.nextInt(SPAWN_RADIUS);

      Cell myCell = world.getCell(x, y);

      if (myCell.getLandType() == LandType.GRASS)
      { if (myCell.isEmpty())
        {
          FoodData foodPile = new FoodData(GameObjectType.FOOD, x, y, count);
          world.addFood(foodPile);
          pileList[siteIdx] = myCell;
          return;
        }
      }
    }
  }

  
  public String toString()
  {
    return "FoodSpawnSite: ("+locationX+", "+locationY + ")";
  }
  
}
