package antworld.client;

import antworld.common.*;
import java.util.ArrayList;

// AWG: Still needs to be debugged and made robus to ant death.
public class WaterChain
{
  ArrayList<Integer> ant_ids = new ArrayList<>();
  private int distance_to_sea; // How many squares to the sea?
  private Direction direction_to_sea; // Direction to closest water.
  private Direction direction_to_nest; // Opposite of the closest water.
  private int origin_x;
  private int origin_y;

  public WaterChain ( int nest_x, int nest_y )
  {
    origin_x = nest_x;
    origin_y = nest_y;
    double angle_to_sea = Raycasting.getBearingToWater(nest_x,nest_y);
    this.direction_to_sea = Direction.values()[ (int) Math.floor( angle_to_sea/45.0)];
    this.distance_to_sea = Raycasting.castRays(nest_x,nest_y).get((int) (angle_to_sea/(360/Raycasting.num_rays)));
    this.direction_to_nest = Direction.values()[ (4+(int) Math.floor( angle_to_sea/45.0)) % 8];
  }

  public void addAnt( AntData ant )
  {
    if(!ant_ids.contains(ant.id) && ant_ids.size() <= distance_to_sea/2) ant_ids.add(ant.id);
  }
  public void setAntActions( PacketToClient data )
  {
    AntAction action;
    boolean wic = waterInChain( data );
    for( AntData ant : data.myAntList )
    {
      if(ant_ids.contains(ant.id))
      {
        action = new AntAction(AntAction.AntActionType.NOOP);
        if( wic || data.nestData[data.myNest.ordinal()].waterInNest < 200 )
        {
          if (exitNest(ant, action, data)) {}
          else if (pickUpWater(ant, action, data)) ant.action =  action;
          else if (healSelf(ant, action)) ant.action =  action;
          else if (passWater(ant, action)) ant.action =  action;
          else if (goToSea(ant, action, data)) ant.action =  action;
        }
        else
        {
          if( enterNest(ant,action)) ant.action =  action;
          else if( returnToNest(ant,action)) ant.action =  action;
        }
        ant.action = action;
        System.out.println(ant.id + " (waterchain) :" + ant.action);
      }
    }
  }

  // Actions, primarily copied from client random walk.
  private boolean enterNest( AntData ant, AntAction action )
  {
    if( Math.abs(origin_x-ant.gridX) + Math.abs(origin_y-ant.gridY) > 15  || ant.state == AntAction.AntState.UNDERGROUND) return false;
    action.direction = null;
    action.type = AntAction.AntActionType.ENTER_NEST;
    return true;
  }
  private boolean returnToNest( AntData ant, AntAction action )
  {
    action.direction = direction_to_nest;
    action.type = AntAction.AntActionType.MOVE;
    return true;
  }
  private boolean exitNest(AntData ant, AntAction action, PacketToClient ptc)
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
      action.x = origin_x;
      action.y = origin_y;

      action.type = AntAction.AntActionType.EXIT_NEST;
      return true;
    }
    return false;
  }
  private boolean pickUpWater( AntData ant, AntAction action, PacketToClient ptc )
  {

    if( ant.carryUnits >= ant.antType.getCarryCapacity() ) return false;

    ArrayList<Integer> ray_distances = Raycasting.castRays(ant.gridX, ant.gridY);
    boolean adjacent_to_water = false;
    for( Integer i : ray_distances )
    {
      if ( i == 0 )
      {
        adjacent_to_water = true;
        if( ptc.nestData[ ptc.myNest.ordinal() ].waterInNest > 101 ) return false;
        break;
      }
    }

    // Only pick up if there's food not just adjacent, but in the expected direction...
    if( ptc.foodList != null)
    {
      // gotta get the direction to the water...
      for (GameObject food : ptc.foodList)
      {
        if( food.objType == GameObject.GameObjectType.FOOD) continue;
        if( food.gridX == (ant.gridX+direction_to_sea.deltaX()) && food.gridY == (ant.gridY+direction_to_sea.deltaY()) )
        {
          adjacent_to_water = true;
        }

      }
    }

    if( !adjacent_to_water ) return false;

    action.direction = direction_to_sea;
    action.type = AntAction.AntActionType.PICKUP;
    action.quantity = 3; //ant.antType.getCarryCapacity();
    return true;
  }
  private boolean healSelf( AntData ant, AntAction action )
  {
    if( ant.health < 18 && ant.carryUnits > 0)
    {
      action.type = AntAction.AntActionType.HEAL;
      action.direction = null;
      action.quantity = 1; //ant.carryUnits;
      return true;
    }
    return false;
  }
  private boolean passWater(AntData ant, AntAction action)
  {
    if( ant.carryUnits == 0) return false; // Don't try to pass unless we have stuff.
    // If we're near the nest, drop ourselves into the nest to add the resource.
    if( Math.abs(ant.gridX-origin_x)+Math.abs(ant.gridY-origin_y) < 15)
    {
      action.direction = null;
      action.type = AntAction.AntActionType.ENTER_NEST;
      action.quantity = ant.carryUnits;
      return true;
    }

    // Pass back in the direction opposite to the water.
    action.direction = direction_to_nest;
    action.type = AntAction.AntActionType.DROP;
    action.quantity = ant.carryUnits;
    return true;
  }
  private boolean goToSea(AntData ant, AntAction action, PacketToClient ptc)
  {
    // Point towards the nearest ocean.
    /* Travel in a straight instead of A* because queue is shortest this way,
    and with relaying, terrain is irrelevant anyway. */
    Direction dir = direction_to_sea;
    int x_diff;
    int y_diff;

    for( AntData a : ptc.myAntList )
    {
      if(a == ant) continue;
      if(a.state == AntAction.AntState.UNDERGROUND) continue;
      x_diff = Math.abs(a.gridX-(ant.gridX+dir.deltaX()));
      y_diff = Math.abs(a.gridY-(ant.gridY+dir.deltaY()));
      if( (x_diff+y_diff)<=1 || (x_diff==1&&y_diff==1) )
      {
        dir = null;
        break;
      }
    }

    action.type = AntAction.AntActionType.MOVE;
    action.direction = dir;
    return true;
  }
  private boolean waterInChain(PacketToClient ptc)
  {
    if( ptc.foodList == null) return false;
    for( FoodData food : ptc.foodList )
      for( AntData ant : ptc.myAntList )
        if( ant_ids.contains(ant.id) )
          if((food.gridX == ant.gridX+direction_to_sea.deltaX() && food.gridY == ant.gridY+direction_to_sea.deltaY()) ||
                  (food.gridX == ant.gridX+direction_to_nest.deltaX() && food.gridY == ant.gridY+direction_to_nest.deltaY()))
                    return true;
    return false;
  }

}
