/*
    Purpose:
      To house functions used by the client's 'chooseAction' method.
      Likely these functions will grow more complex, and it'd be nice to have the code seperated
      from the client's main script so as not to clutter it.
 */
package antworld.client;

import antworld.common.*;

import java.util.ArrayList;

public class ActionFunctions
{
  // =========================
  //  Active action functions
  // =========================
  //<editor-fold desc="Active Action Functions">
  protected static boolean exitNest(AntData ant, AntAction action, PacketToClient ptc)
  {
    //=============================================================================
    // This method sets the given action to EXIT_NEST if and only if the given
    //   ant is underground.
    // Returns true if an action was set. Otherwise returns false
    //=============================================================================
    if (ant.state == AntAction.AntState.UNDERGROUND)
    {
      action.type = AntAction.AntActionType.EXIT_NEST;
      action.x =  ptc.nestData[ptc.myNest.ordinal()].centerX - (Constants.NEST_RADIUS-1) + Constants.random.nextInt(2 * (Constants.NEST_RADIUS-1));
      action.y = ptc.nestData[ptc.myNest.ordinal()].centerY - (Constants.NEST_RADIUS-1) + Constants.random.nextInt(2 * (Constants.NEST_RADIUS-1));
      return true;
    }
    return false;
  }
  protected static  boolean heal(AntData ant, AntAction action)
  {
    if(ant.health < ant.antType.getMaxHealth() - 3)
    {

      action.type = AntAction.AntActionType.HEAL;
      action.direction = null;
      action.quantity = 1;
      return true;
    }
    return false;
  }
  protected static  boolean pickUpWater(AntData ant, AntAction action)
  {
    if(ant.carryUnits < ant.antType.getCarryCapacity())
    {
      Direction dir = Direction.SOUTH;
      action.type = AntAction.AntActionType.PICKUP;
      action.quantity = ant.antType.getCarryCapacity();
      action.direction = dir;
      return (Constants.random.nextDouble() > 0.5); // 1/2 time we'll try to pick up water.
    }
    return false;
  }
  // Try to attack adjacent enemies, if present.
  protected static  boolean attackAdjacent(AntData ant, AntAction action, PacketToClient ptc)
  {
    Direction dir = adjacentToEnemy(ant, ptc.enemyAntList);
    if( dir != null )
    {
      action.direction = dir;
      action.type = AntAction.AntActionType.ATTACK;
      return true;
    }
    return false;
  }
  // Try to pick up food adjacent food, but only if it's there & we're able to carry it.
  protected static  boolean pickUpFoodAdjacent(AntData ant, AntAction action, PacketToClient ptc)
  {
    Direction dir = adjacentToFood(ant, ptc.foodList );
    if( dir != null && ant.carryUnits < ant.antType.getCarryCapacity())
    {
      action.direction = dir;
      action.type = AntAction.AntActionType.PICKUP;
      action.quantity = 1;
      return true;
    }
    return false;
  }
  protected static  boolean goExplore(AntData ant, AntAction action)
  {
    Direction dir = Direction.SOUTH; //Direction.getRandomDir();
    action.type = AntAction.AntActionType.MOVE;
    action.direction = dir;
    return true;
  }
  //</editor-fold>

  // =========================
  //  Unused action functions
  // =========================
  //<editor-fold desc="Unused Action Functions">
  protected static  boolean goToEnemyAnt(AntData ant, AntAction action)
  {
    return false;
  }
  protected static  boolean goToFood(AntData ant, AntAction action)
  {
    return false;
  }
  protected static  boolean goToGoodAnt(AntData ant, AntAction action)
  {
    return false;
  }
  protected static  boolean dropOffAtNest( AntData ant, AntAction action, PacketToClient ptc)
  {
    return false;
  }
  protected static  boolean goHomeIfCarryingOrHurt(AntData ant, AntAction action)
  {
    return false;
  }
  //</editor-fold>

  // ==========================
  //   Action helper functions
  //  ========================
  //<editor-fold desc="Action Helper Functions">
  // Returns direction to adjacent food. If no food adjacent, return null.
  private static Direction adjacentToFood(AntData ant, ArrayList<FoodData> foodList )
  {
    if( foodList == null ) return null;
    if( foodList.isEmpty() ) return null;
    for( FoodData fd : foodList )
    {
      if( Math.abs(fd.gridX-ant.gridX)<=1 && Math.abs(fd.gridY-ant.gridY)<=1 )
      {
        if( fd.gridX > ant.gridX )
        {
          if( fd.gridY > ant.gridY ) { return Direction.NORTHEAST; }
          else if (fd.gridY < ant.gridY ){ return Direction.SOUTHEAST; }
          else { return Direction.EAST; }
        }
        else if ( fd.gridX < ant.gridX )
        {
          if( fd.gridY > ant.gridY ) { return Direction.NORTHWEST; }
          else if (fd.gridY < ant.gridY ){ return Direction.SOUTHWEST; }
          else { return Direction.WEST; }
        }
        else
        {
          if( fd.gridY > ant.gridY ) { return Direction.NORTH; }
          else if (fd.gridY < ant.gridY ){ return Direction.SOUTH; }
          else { /*Shouldn't end up here - can't have Ant & Food in same spot.*/ }
        }
      }
    }
    return null;
  }
  // Returns direction to adjacent enemy. If no enemy adjacent, return null.
  private static Direction adjacentToEnemy(AntData ant, ArrayList<AntData> enemyAntList )
  {
    if( enemyAntList == null) return null;
    if( enemyAntList.isEmpty() ) return null;
    for( AntData ea : enemyAntList )
    {
      if( Math.abs(ea.gridX-ant.gridX)<=1 && Math.abs(ea.gridY-ant.gridY)<=1 )
      {
        if( ea.gridX > ant.gridX )
        {
          if( ea.gridY > ant.gridY ) { return Direction.NORTHEAST; }
          else if (ea.gridY < ant.gridY ){ return Direction.SOUTHEAST; }
          else { return Direction.EAST; }
        }
        else if ( ea.gridX < ant.gridX )
        {
          if( ea.gridY > ant.gridY ) { return Direction.NORTHWEST; }
          else if (ea.gridY < ant.gridY ){ return Direction.SOUTHWEST; }
          else { return Direction.WEST; }
        }
        else
        {
          if( ea.gridY > ant.gridY ) { return Direction.NORTH; }
          else if (ea.gridY < ant.gridY ){ return Direction.SOUTH; }
          else { /*Shouldn't end up here - can't have Ant & Food in same spot.*/ }
        }
      }
    }
    return null;
  }
  // Returns the closest ant on the list (enemies or friends), excluding of yourself.
  private static AntData getClosestAnt(AntData ant, ArrayList<AntData> list_of_ants )
  {
    int min_dist = Integer.MIN_VALUE;
    int temp_dist;

    AntData closest_ant = null;
    for( AntData fa : list_of_ants )
    {
      if( fa == ant ) continue;
      temp_dist = Math.abs(fa.gridX-ant.gridX) + Math.abs(fa.gridY-ant.gridY);
      if( temp_dist < min_dist && temp_dist > 0 )
      {
        min_dist = temp_dist;
        closest_ant = fa;
      }
    }
    return closest_ant;
  }
  // Returns the lowest health ant from what's visible of the enemy team. Null if no visible enemies.
  private static AntData getLowestHealthVisibleEnemy(ArrayList<AntData> enemyAntList)
  {
    if(enemyAntList.isEmpty()) return null;
    AntData lowest_health_ant = enemyAntList.get(0);
    for( AntData ea : enemyAntList )
    {
      if(ea.health<lowest_health_ant.health){ lowest_health_ant=ea; }
    }
    return lowest_health_ant;
  }
  //</editor-fold>

}
