/*
  Any states/state transitions which only require a few things, independant of some
  seperate lists or hashmaps are stored here.
 */

package antworld.client;
import antworld.common.*;
import java.util.HashMap;

public class MiscFunctions
{
  public static HashMap<Integer, Boolean> healToFull = new HashMap<>();

  public static boolean randomWalk(AntData ant, AntAction action )
  {
    action.direction = Direction.getRandomDir();
    action.type = AntAction.AntActionType.MOVE;
    return true;
  }

  public static boolean jitter(AntData ant, AntAction action, PacketToClient ptc )
  {
    for (AntData other_ant : ptc.myAntList)
    {
      if( other_ant.id == ant.id) continue; // Don't jitter with self...obviously
      if (Math.abs(other_ant.gridX - ant.gridX) <= 1 && Math.abs(other_ant.gridY - ant.gridY) <= 1)
      {
        action.direction = Direction.getRandomDir();
        return true;
      }
      if(ptc.foodList != null)
      {
        if(ant.carryUnits < ant.antType.getCarryCapacity()) return false;
        for(FoodData food : ptc.foodList)
        {

          if (Math.abs(food.gridX - ant.gridX) <= 1 && Math.abs(food.gridY - ant.gridY) <= 1)
          {
            action.direction = Direction.getRandomDir();
            return true;
          }
        }
      }
    }
    return false;
  }

  public static boolean healSelf(AntData ant, AntAction action )
  {
    System.out.println(ant.antType.getMaxHealth());
    // Don't heal if we don't need to. (High watermark)
    if( ant.health >= ant.antType.getMaxHealth() && healToFull.get(ant.id) )
    {
      healToFull.put(ant.id, false);
      return false;
    }

    // Heal if we have enough units and we're at sufficiently low health.
    if(ant.carryUnits > 0 && ant.carryType == GameObject.GameObjectType.WATER)
    {
      if(!healToFull.get(ant.id)) healToFull.put(ant.id, true);

      action.type = AntAction.AntActionType.HEAL;
      action.direction = null;
      action.quantity = ant.carryUnits;
      return true;
    }

    // Don't go off path to heal unless we need to. (Low watermark)
    // Todo: I should use Mike's strategy here. Needs external state though, so I'll replace that later.
    if( ant.health > ant.antType.getMaxHealth()/3 && !healToFull.get(ant.id)) return false;

    // Point ourselves towards water.
    action.direction = Direction.values()[ (int) (Math.floor( Raycasting.getBearingToWater(ant.gridX, ant.gridY) )/45.0) ];

    System.out.println("Distance I see: " + Raycasting.getDistanceToWaterUsingVector(ant.gridX, ant.gridY, ant.gridX+action.direction.deltaX(), ant.gridY+action.direction.deltaY()));

    // If we're next to water, just pick it up.
    if( Raycasting.getDistanceToWaterUsingVector(ant.gridX, ant.gridY, ant.gridX+action.direction.deltaX(), ant.gridY+action.direction.deltaY()) <= 1
            && Constants.random.nextBoolean() ) // So maybe move closer.
    {
      //action.direction = //Direction.getRandomDir(); // For now... I mean, it should work, but whatever.
      int pickupAmt = ant.antType.getMaxHealth() - ant.health;

      action.type = AntAction.AntActionType.PICKUP;
      action.quantity = pickupAmt;

      // If we've moved off the path, then we'll need to recalculate it.
      // COM // recalculatePath( ant ); // We'll only do this once per heal. This should in theory work...

      return true;
    }
    else if(Raycasting.getDistanceToWaterUsingVector(ant.gridX, ant.gridY, ant.gridX+action.direction.deltaX(), ant.gridY+action.direction.deltaY()) <= 1 && ant.carryType == GameObject.GameObjectType.FOOD)
    {
      for(Direction dir : Direction.values())
      {
        if(A_Star.board[ant.gridX + dir.deltaX()][ant.gridY + dir.deltaY()] !=null)
        {
          action.direction = dir;
          action.type = AntAction.AntActionType.DROP;
          action.quantity = ant.carryUnits;
          return true;
        }
        }
    }
    else // If we aren't adjacent, then keep moving to the water.
    {
      action.type = AntAction.AntActionType.MOVE;
      return true;
    }
    return false;
  }

  public static boolean enterNest(AntData ant, AntAction action, int nx, int ny )
  {
    if( (Math.abs(ant.gridX-nx)+Math.abs(ant.gridY-ny) < 15))
    {
      action.direction = null;
      action.type = AntAction.AntActionType.ENTER_NEST;
      action.quantity = ant.carryUnits;
      return true;
    }
    return false;
  }

  public static boolean pickUpFoodAdjacent(AntData ant, AntAction action, PacketToClient data)
  {
    if(ant.carryUnits >= ant.antType.getCarryCapacity()) return false;

    int xDiff;
    int yDiff;
    if(data.foodList != null)
    {
      for(GameObject food : data.foodList)
      {
        if(food.objType == GameObject.GameObjectType.WATER) continue;
        xDiff = Math.abs(food.gridX - ant.gridX);
        yDiff = Math.abs(food.gridY - ant.gridY);
        if((xDiff + yDiff) <= 1 || (xDiff == 1 && yDiff == 1))
        {
          for(Direction dir : Direction.values())
          {
            if(ant.gridX + dir.deltaX() == food.gridX && ant.gridY + dir.deltaY() == food.gridY)
            {
              action.direction = dir;
              action.type = AntAction.AntActionType.PICKUP;
              action.quantity = ant.antType.getCarryCapacity();
              System.out.println("Going to try to pick up food");
              return true;
            }
          }
        }
      }
    }
    return false;
  }

  public static boolean exitNest(AntData ant, AntAction action, PacketToClient ptc, int nx, int ny)
  {
    if (ant.state == AntAction.AntState.UNDERGROUND)
    {
      // Drop units before exiting the nest, if any in possession.
      if(ant.carryUnits != 0)
      {
        action.type = AntAction.AntActionType.DROP;
        action.direction = null;
        action.quantity = ant.carryUnits;
        return true;
      }
      action.x = nx;
      action.y = ny;

      action.type = AntAction.AntActionType.EXIT_NEST;
      return true;
    }
    return false;
  }
  public static boolean attack(AntData ant, AntAction action, PacketToClient ptc)
  {
    if( ptc.enemyAntList == null ) return false;
    for(int j = 0; j < ptc.enemyAntList.size(); j++)
    {
      AntData enemy = ptc.enemyAntList.get(j);
      int delX, delY;
      Direction dir = null;
      if(Math.abs(enemy.gridX - ant.gridX) <= 1 && Math.abs(enemy.gridY - ant.gridY) <= 1)
      {
        delX = ant.gridX - enemy.gridX;
        delY = ant.gridY - enemy.gridY;
        if(delX == 0 && delY == 1) dir = Direction.NORTH;
        else if(delX == 1 && delY == 1) dir = Direction.NORTHWEST;
        else if(delX == 1 && delY == 0) dir = Direction.WEST;
        else if(delX == 1 && delY == -1) dir =  Direction.SOUTHWEST;
        else if (delX == 0 && delY == -1) dir = Direction.SOUTH;
        else if (delX == -1 && delY == -1) dir = Direction.SOUTHEAST;
        else if (delX == -1 && delY == 0) dir = Direction.EAST;
        else if (delX == -1 && delY == 1) dir = Direction.NORTHEAST;
        action.type = AntAction.AntActionType.ATTACK;
        action.direction = dir;
        if(action.direction == null) System.out.println( "Direction not set!" );
        return true;
      }
    }
    return false;
  }
}
