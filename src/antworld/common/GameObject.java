package antworld.common;

import antworld.common.AntData;
import antworld.common.FoodData;
import antworld.common.NestNameEnum;
import antworld.server.AntWorld;
import antworld.server.FoodSpawnSite;

import java.io.Serializable;

public abstract class GameObject implements Serializable
{
  public enum GameObjectType {ANT, FOOD, WATER}
  
  public GameObjectType type;

  /** World Map pixel coordinates of this ant with (0,0) being upper-left.
   * In the ant world map, each game object (an ant or a food pile) occupies
   * exactly one pixel. No two game objects may occupy the same pixel at the
   * same time. NOTE: food being carried by an ant is part of the ant game object.
   * */
  public int gridX;
  public int gridY;


  public int getRGB()
  { if (type == GameObjectType.ANT) return 0xD7240D;
    if (type == GameObjectType.FOOD) return 0x7851A9;
    return 0x32ffff; //water
  }

  public static boolean isFood(GameObject obj)
  {
    if (obj == null) return false;
    if (obj.type ==  GameObjectType.FOOD) return true;
    if (obj.type ==  GameObjectType.WATER) return true;
    return false;
  }

  //public AntData ant = null;
  //public FoodData food = null;
  //public FoodSpawnSite foodSpawnSite = null;
  
  //public GameObject(AntData ant)
  //{
  //  type =  GameObjectType.ANT;
  //  this.ant = ant;
  //}

  /*
  public GameObject(FoodSpawnSite foodSpawnSite, FoodData food)
  {
    type =  GameObjectType.FOOD;
    this.foodSpawnSite = foodSpawnSite;
    this.food = food;
  }
  */

  /*
  public void setFoodCount(AntWorld world, NestNameEnum nestName, int count)
  {
    if (food == null) return;
    if (count <= 0)
    { 
      count = 0;
      world.removeFood(food);
    }
    food.count = count;
    
    
    
    if (foodSpawnSite != null)
    { foodSpawnSite.nestGatheredFood(nestName, count);
    }
    
  }
  
  public int getFoodCount()
  {
    if (type !=  GameObjectType.FOOD) return 0;
    if (food == null) return 0;
    return food.getCount();
  }

  */
}
