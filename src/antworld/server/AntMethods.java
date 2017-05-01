package antworld.server;

import java.util.Random;

import antworld.common.AntAction;
import antworld.common.AntAction.AntActionType;
import antworld.common.AntAction.AntState;
import antworld.common.AntData;
import antworld.common.AntType;
import antworld.common.Constants;
import antworld.common.FoodData;
import antworld.common.GameObject.GameObjectType;
import antworld.common.LandType;
import antworld.common.NestNameEnum;
import antworld.common.TeamNameEnum;

public class AntMethods
{
  private static volatile int globalAntID = -1;
  private static Random random = Constants.random;
  private static final boolean DEBUG = false;

  
  public static AntData createAnt(AntType type, NestNameEnum nestName, TeamNameEnum teamName)
  {
    globalAntID++;
    int id = globalAntID;
    return new AntData(id, type, nestName, teamName);
  }


  

  
  public static AntActionType update(AntWorld world, AntData ant, AntAction action)
  {
    if (DEBUG) System.out.println("Ant.update(): "+ant+ "\n   =======>" + action);

    if (ant.state == AntState.DEAD) return AntActionType.DIED;

    //Note: this should never be true since this method should not be called in this case.
    if (ant.action.type == AntActionType.BIRTH) return AntActionType.BIRTH;

    //Note: MOVE to BUSY and BUSY quantity is updated before this method is called.
    if (ant.action.type == AntActionType.BUSY) return AntActionType.BUSY;

    
    if (action == null || action.type == AntActionType.NOOP || action.type == AntActionType.BUSY)
    {
      return AntActionType.NOOP;
    }


    if (action.type == AntActionType.EXIT_NEST)
    {
      if (ant.state != AntState.UNDERGROUND) return AntActionType.NOOP;
      Cell exitCell = world.getCell(action.x,action.y);

      if (exitCell == null) return AntActionType.NOOP;
      if (!exitCell.isEmpty()) return AntActionType.NOOP;
      if (exitCell.getNestName() != ant.nestName) return AntActionType.NOOP;
      
      ant.gridX = action.x;
      ant.gridY = action.y;
      ant.state = AntState.OUT_AND_ABOUT;
      world.addAnt(ant);
      return AntActionType.EXIT_NEST;
    }
    
    if (action.type == AntActionType.ENTER_NEST)
    {
      if (ant.state != AntState.OUT_AND_ABOUT) return AntActionType.NOOP;
      if (world.getCell(ant.gridX, ant.gridY).getNestName() != ant.nestName) return AntActionType.NOOP;

      ant.state = AntState.UNDERGROUND;
      world.removeGameObject(ant);
      return AntActionType.ENTER_NEST;
    }
   
    Nest myNest = world.getNest(ant.nestName);
    
    if (action.type == AntActionType.HEAL)
    { 
      if (ant.state == AntState.UNDERGROUND)
      { if (ant.health >= ant.antType.getMaxHealth()) return AntActionType.NOOP;
        if (myNest.getWaterCount() < 1) return AntActionType.NOOP;
        int waterUnits = ant.antType.getHealWaterUnitsPerTick(AntState.UNDERGROUND);
        if (waterUnits > myNest.getWaterCount()) waterUnits = myNest.getWaterCount();
        if (waterUnits+ant.health > ant.antType.getMaxHealth())
        {
          waterUnits = ant.antType.getMaxHealth()-ant.health;
        }
        myNest.addResource(GameObjectType.WATER, -waterUnits);
        ant.health +=waterUnits;
        return AntActionType.HEAL;
      }
      
      //OUT_AND_ABOUT heal
      if (ant.carryType != GameObjectType.WATER) return AntActionType.NOOP;
      if (ant.carryUnits <= 0) return AntActionType.NOOP;

      //Note: it is ok for target ant to be self
      AntData targetAnt = getTargetAnt(world, ant, action);
      if (targetAnt == null) return AntActionType.NOOP;
      
      if (targetAnt.health >= targetAnt.antType.getMaxHealth()) return AntActionType.NOOP;
      int waterUnits = ant.antType.getHealWaterUnitsPerTick(AntState.OUT_AND_ABOUT);
      if (waterUnits > ant.carryUnits) waterUnits = ant.carryUnits;
      if (waterUnits + targetAnt.health > targetAnt.antType.getMaxHealth())
      {
        waterUnits = targetAnt.antType.getMaxHealth() - targetAnt.health;
      }
      ant.carryUnits -= waterUnits;
      targetAnt.health += waterUnits;
      return AntActionType.HEAL;
    }
      
    if (action.type == AntActionType.ATTACK)
    {
      AntData targetAnt = getTargetAnt(world, ant, action);
      //Note: it is possible for an ant to attack itself by setting attack direction to null.
      if (targetAnt == null) return AntActionType.NOOP;
      targetAnt.health -= getAttackDamage(ant);
      if (targetAnt.action.type == AntActionType.BUSY) targetAnt.action.type = AntActionType.BUSY_ATTACKED;
      return AntActionType.ATTACK;
    }
    
    
    if (action.type == AntActionType.MOVE)
    {
      if (ant.state != AntState.OUT_AND_ABOUT) return AntActionType.NOOP;
      Cell cellTo = getTargetCell(world, ant, action);
      if (cellTo == null) return AntActionType.NOOP;
      if (cellTo.getLandType() == LandType.WATER) return AntActionType.NOOP;
      if (!cellTo.isEmpty()) return AntActionType.NOOP;
      Cell cellFrom = world.getCell(ant.gridX, ant.gridY);

      ant.action.quantity = ant.antType.getBaseMovementTicksPerCell();
      if (cellTo.getHeight() > cellFrom.getHeight()) ant.action.quantity *= ant.antType.getUpHillMultiplier();
      if (ant.carryUnits > ant.antType.getCarryCapacity()/2)
      { ant.action.quantity *= ant.antType.getEncumbranceMultiplier();
      }
      ant.action.quantity--; //one tick of movement has just been spent.
      world.moveAnt(ant, cellFrom, cellTo);
      return AntActionType.MOVE;
    }
    
    if (action.type == AntActionType.DROP)
    {
      if (ant.carryType == null) return AntActionType.NOOP;
      if (ant.carryUnits <= 0) return AntActionType.NOOP;
      if (action.quantity <= 0) return AntActionType.NOOP;
      if (action.quantity > ant.carryUnits) action.quantity = ant.carryUnits;
      if (ant.state == AntState.UNDERGROUND)
      { 
    	  myNest.addResource(ant.carryType, action.quantity);
      }
      else
      {
        Cell targetCell = getTargetCell(world, ant, action);
        if (targetCell == null) return AntActionType.NOOP;
        if (!targetCell.isEmpty()) return AntActionType.NOOP;
      
        int x = targetCell.getLocationX();
        int y = targetCell.getLocationY();
        FoodData droppedFood = new FoodData(ant.carryType, x, y, ant.carryUnits);
        world.addFood(droppedFood);
      }

      ant.carryUnits -= action.quantity;
      if (ant.carryUnits == 0) ant.carryType = null;
      return AntActionType.DROP;
    }
    
    
    if (action.type == AntActionType.PICKUP)
    {
      if (ant.state != AntState.OUT_AND_ABOUT) return AntActionType.NOOP;
      if (action.quantity <=0) return AntActionType.NOOP;
      Cell targetCell = getTargetCell(world, ant, action);
      if (targetCell == null) return AntActionType.NOOP;
      FoodData groundFood = null;

      if (targetCell.getLandType() == LandType.WATER)
      { 
        if ((ant.carryUnits > 0) && (ant.carryType!=GameObjectType.WATER)) return AntActionType.NOOP;
        ant.carryType = GameObjectType.WATER;
      }
      else
      {  
        groundFood = targetCell.getFoodOrWater();
        if (groundFood == null) return AntActionType.NOOP;

        if ((ant.carryUnits > 0) && (ant.carryType!=groundFood.objType)) return AntActionType.NOOP;
        if (action.quantity > groundFood.quantity) action.quantity = groundFood.quantity;
        ant.carryType = groundFood.objType;
      }
      if (ant.carryUnits + action.quantity > ant.antType.getCarryCapacity()) action.quantity = ant.antType.getCarryCapacity() - ant.carryUnits;
      ant.carryUnits += action.quantity;
      
      if (targetCell.getLandType() != LandType.WATER)
      {
        groundFood.quantity -=action.quantity;
        if (groundFood.quantity <= 0) world.removeGameObject(groundFood);
      }
      return AntActionType.PICKUP;
    }
    return AntActionType.NOOP;
  }


  public static int getAttackDamage(AntData ant)
  {
    int damage = 0;
    int dice = ant.antType.getAttackDiceD4();
    for (int i=0; i<dice; i++)
    {
      //add a uniformly distributed integer 1, 2, 3 or 4.
      damage += random.nextInt(4) + 1;
    }
    return damage;
  }




  
  private static Cell getTargetCell(AntWorld world, AntData ant, AntAction action)
  {
    int targetX = ant.gridX;
    int targetY = ant.gridY;
    if (action.direction != null)
    { targetX += action.direction.deltaX();
      targetY += action.direction.deltaY();
    }
    return world.getCell(targetX, targetY);
  }

  
  
  private static AntData getTargetAnt(AntWorld world, AntData ant, AntAction action)
  {
    Cell targetCell = getTargetCell(world, ant, action);
    if (targetCell == null) return null;
    return targetCell.getAnt();
  }
}
