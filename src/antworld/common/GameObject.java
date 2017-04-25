package antworld.common;


import java.io.Serializable;

public abstract class GameObject implements Serializable
{
  public enum GameObjectType {ANT, FOOD, WATER}
  
  public GameObjectType objType;

  /** World Map pixel coordinates of this ant with (0,0) being upper-left.
   * In the ant world map, each game object (an ant or a food pile) occupies
   * exactly one pixel. No two game objects may occupy the same pixel at the
   * same time. NOTE: food being carried by an ant is part of the ant game object.
   * */
  public int gridX;
  public int gridY;


  public int getRGB()
  { if (objType == GameObjectType.ANT) return 0xD7240D;
    if (objType == GameObjectType.FOOD) return 0x7851A9;
    return 0x32ffff; //water
  }

  public static boolean isFood(GameObject obj)
  {
    if (obj == null) return false;
    if (obj.objType ==  GameObjectType.FOOD) return true;
    if (obj.objType ==  GameObjectType.WATER) return true;
    return false;
  }
}
