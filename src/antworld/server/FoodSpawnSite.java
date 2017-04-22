package antworld.server;

import java.io.Serializable;
import java.util.Random;

import antworld.common.Constants;
import antworld.common.FoodData;
import antworld.common.GameObject;
import antworld.common.LandType;
import antworld.common.NestNameEnum;

public class FoodSpawnSite implements Serializable
{
  private static final long serialVersionUID = Constants.VERSION;
  private int locationX, locationY;
  private static final int SPAWN_RADIUS = 30;
  private static final int NUMBER_OF_SPAWNS_PER_RESET = 100;
  private static final int MAX_SIMULTANEOUS_PILES_FROM_SITE = 5;
  private static Random random = Constants.random;
  private int spawnCountSinceReset = 0;
  private boolean[] didNestGatherFromThisSiteRecently; 
  private int activeFoodPileCount = 0;
  private boolean needSpawn = true;
  
  public FoodSpawnSite(int x, int y, int totalNestCount)
  {
    this.locationX = x;
    this.locationY = y;
    didNestGatherFromThisSiteRecently = new boolean[totalNestCount];
  }
  
  public int getLocationX() {return locationX;}
  public int getLocationY() {return locationY;}
  
  public void nestGatheredFood(NestNameEnum nestName, int foodUnitCount)
  {
    if (foodUnitCount <=0)activeFoodPileCount--;
    if (foodUnitCount < 5) needSpawn = true;
    if (nestName != null)
    {
      didNestGatherFromThisSiteRecently[nestName.ordinal()] = true;
    }
  }

  public void spawn(AntWorld world)
  {
    System.out.println("AWG: Spawned food at site.");
    if (!needSpawn) return;
    if (activeFoodPileCount >= MAX_SIMULTANEOUS_PILES_FROM_SITE) return;
    
    int siteGatherCount = 0;
    for (int i=0; i<didNestGatherFromThisSiteRecently.length; i++)
    { if (didNestGatherFromThisSiteRecently[i]) siteGatherCount++;
    }
    
    if (spawnCountSinceReset > NUMBER_OF_SPAWNS_PER_RESET)
    {
      spawnCountSinceReset = 0;
      for (int i=0; i<didNestGatherFromThisSiteRecently.length; i++) didNestGatherFromThisSiteRecently[i] = false;
    }
    
    int spawnGoal = 100 + siteGatherCount/2;
    int quantityMultiplier = 1;
    if (siteGatherCount > 2) quantityMultiplier = 3;
    else if (siteGatherCount > 1) quantityMultiplier = 2;
    
    int spawnCount = 0;
    int x=0, y=0;

    while(spawnCount < spawnGoal)
    {

      System.out.println("AWG: Spawncount: " + spawnCount + " spawnGoal: " + spawnGoal);
      int count = (20 + AntWorld.random.nextInt(400)); 
      
      x = locationX + random.nextInt(SPAWN_RADIUS) - random.nextInt(SPAWN_RADIUS);
      y = locationY + random.nextInt(SPAWN_RADIUS) - random.nextInt(SPAWN_RADIUS);
      count *= quantityMultiplier;

      
      Cell myCell = world.getCell(x, y);

      if (myCell.getLandType() != LandType.GRASS) continue;
      if (!myCell.isEmpty())  continue;

      FoodData foodPile = new FoodData(GameObject.GameObjectType.FOOD, x, y, count);
      world.addFood(this, foodPile);
      spawnCount++;
      activeFoodPileCount++;
      spawnCountSinceReset++;
      needSpawn = false;
    }
  }

  
  public String toString()
  {
    return "FoodSpawnSite: ("+locationX+", "+locationY + ") activeFoodPileCount=" + activeFoodPileCount +
        ", needSpawn=" + needSpawn;
  }
  
}
