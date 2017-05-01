package antworld.server;

import antworld.common.AntData;
import antworld.common.FoodData;
import antworld.common.GameObject;
import antworld.common.LandType;
import antworld.common.NestNameEnum;
import antworld.common.GameObject.GameObjectType;

public class Cell 
{
  private final int height;
  private final int x, y;
  public int lookedAtTick = 0;
  public NestNameEnum lookedAtNest = null;


  
  private LandType landType;
  private Nest nest = null;
  
  private GameObject gameObject = null;
  
  
  public Cell(LandType landType, int height, int x, int y)
  {
    this.landType = landType;
    this.height = height;
    this.x = x;
    this.y = y;
    nest = null;
  }
  
  public LandType getLandType() {return landType;}
  public int getHeight() {return height;}
  public int getLocationX() {return x;}
  public int getLocationY() {return y;}

  public boolean isEmpty() {if (gameObject == null) return true; else return false;}
  
  public GameObject getGameObject() { return gameObject;}

  public FoodData getFoodOrWater()
  {
    if (gameObject == null) return null;
    if (gameObject.objType == GameObjectType.ANT) return null;
    return (FoodData) gameObject;
  }

  public int getFoodUnits()
  {
    if (gameObject.objType != GameObjectType.FOOD) return 0;
    FoodData food = (FoodData) gameObject;
    return food.quantity;
  }

  public AntData getAnt()
  {
    if (gameObject == null) return null;
    if (gameObject.objType != GameObjectType.ANT) return null;
    return (AntData) gameObject;
  }
  
  
  public Nest getNest() {  return nest;}
  public NestNameEnum getNestName()
  { if (nest == null) return null;
    return nest.nestName;
  }
  
  public void setGameObject(GameObject obj)
  { 
    gameObject = obj;
  }
  
  
  public void setNest(Nest nest) 
  { this.nest = nest;
    this.landType = LandType.NEST;
  }

  
  public int getRGB()
  { 
    if (gameObject != null)
    {
      return gameObject.getRGB();
    }
    
    if (landType == LandType.WATER) return landType.getMapColor();
    if (landType == LandType.NEST)
    { 
      if ((x == nest.centerX) && (y == nest.centerY)) return 0x0;
      return landType.getMapColor();
    }
    if (landType == LandType.GRASS)
    { 
      int r1 = 40, r2=186;
      int g = Math.min(255, 55 + height);
      int b1 = 36, b2=166;
      
      
      int diffR = r2-r1;
      int diffB = b2-b1;
      
      int r = r1 + (diffR*height)/200;
      int b = b1 + (diffB*height)/200;
      
      //System.out.println("r="+r+", g="+g+", b="+b);

      
      return (r<<16) | (g<<8) | b;
    }
    else return 0;
  }
  
  public String toString()
  {
    return "Cell: " + landType + "("+x+", " + y + "), height=" +height;
  }
}

