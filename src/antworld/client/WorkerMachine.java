/*
Worker (state) machine:
  Sends workers out to gather food from a single food site,
  contingent upon explorers discovering a food site.
 */

package antworld.client;

import antworld.common.AntAction;
import antworld.common.AntData;
import antworld.common.Direction;
import antworld.common.PacketToClient;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

class WorkerMachine
{
  private ArrayList<Integer> ant_ids;
  private HashMap<Integer,Integer> ant_phases;
  private HashMap<Integer,PathNode> target_PathNodes;

  private int nest_x;
  private int nest_y;

  // Initializes all the nest & state variables for WorkerMachine.
  WorkerMachine( int nx, int ny )
  {
    nest_x = nx;
    nest_y = ny;
    ant_ids = new ArrayList<>();
    ant_phases = new HashMap<>();
    target_PathNodes = new HashMap<>();
  }

  // Configures a single ant to be controlled by WorkerMachine.
  void addAnt ( AntData ant )
  {
    if( ant_ids.contains(ant.id) ) return;
    ant_ids.add( ant.id );

    // Every ant starts at phase 0 - that is, going from the nest to the food site.
    target_PathNodes.put( ant.id, PathCalculator.nest_location );
    ant_phases.put( ant.id, 0 );
  }

  // Here's where we decide the states & state transitions for workers.
  void setAntActions(PacketToClient ptc )
  {
    // Don't use the workers until we've got paths ready for them.
    if( PathCalculator.paths_ready == false) return;

    AntAction action;
    for( AntData ant : ptc.myAntList )
    {
      if (!ant_ids.contains(ant.id)) continue;
      action = new AntAction(AntAction.AntActionType.NOOP);

      // State goes here...
      // 1. Travel along the path to food.
      // 2. Wiggle around, searching for food, picking up if some is found.
      // 3. Travel along the path to the nest.
      switch ( ant_phases.get( ant.id ) )
      {
        case 0: // Travel along nest_to_food.
          if( MiscFunctions.exitNest( ant, action, ptc, nest_x, nest_y ));
          else
          {
            if( MiscFunctions.attack(ant,action,ptc));
            else if( MiscFunctions.healSelf( ant, action ));
            else if (moveFoodToNest( ant, action ));
            if( MiscFunctions.jitter( ant, action, ptc ));
          }
          break;
        case 1: // Search for food.
          if( MiscFunctions.attack( ant, action, ptc));
          else if( MiscFunctions.healSelf( ant, action ));
          else if( MiscFunctions.pickUpFoodAdjacent( ant, action, ptc ));
            // Todo: random walk is kinda lame, we might be able to do better
          else if (MiscFunctions.randomWalk( ant, action ));
          if( ant.carryUnits > 0 )
          {
            System.out.println("Goin' home!");
            ant_phases.put( ant.id, 2);
          }
          break;
        case 2: // Travel along food_to_nest.
          if( MiscFunctions.enterNest( ant, action, nest_x, nest_y ))
          {
            ant_phases.put( ant.id, 0 );
            target_PathNodes.put( ant.id, PathCalculator.nest_location );
          }
          else
          {
            if( MiscFunctions.attack( ant, action, ptc));
            else if( MiscFunctions.healSelf( ant, action ));
            else if (moveNestToFood( ant, action ));
            if( MiscFunctions.jitter( ant, action, ptc ));
          }
          break;
        default:
          System.out.printf("Error: WorkerMachine's [%d] Is in an uncontrolled phase.\n", ant.id);
      }
      ant.action = action; // This line is necessary.
    }
  }

  // Moves a worker along the food-to-nest path.
  /* Both this method and its counterpart (moveNestToFood) are robust to diversion -
  that is to say, jittering and healing won't cause them to lose their way. */
  private boolean moveFoodToNest(AntData ant, AntAction action )
  {
    // If we're moving from food to nest, move our target node along appropriately.
    if(ant.gridX == target_PathNodes.get(ant.id).x && ant.gridY == target_PathNodes.get(ant.id).y ){
      target_PathNodes.put( ant.id, PathCalculator.nest_to_food.get(target_PathNodes.get(ant.id))); }

    // If the target is null, we've hit the end of the path.
    if( target_PathNodes.get( ant.id ) == null )
    {
      ant_phases.put( ant.id, 1 );
      target_PathNodes.put( ant.id, PathCalculator.food_site_location );
      return false;
    }
    return moveAlongPath( ant, action, A_Star.getPath( target_PathNodes.get(ant.id), A_Star.board[ant.gridX][ant.gridY]));
  }

  // Moves a worker along the nest-to-food path.
    /* Both this method and its counterpart (moveFoodToNest) are robust to diversion -
  that is to say, jittering and healing won't cause them to lose their way. */
  private boolean moveNestToFood(AntData ant, AntAction action )
  {
    // If we're moving from food to nest, move our target node along appropriately.
    if(ant.gridX == target_PathNodes.get(ant.id).x && ant.gridY == target_PathNodes.get(ant.id).y ){
      target_PathNodes.put( ant.id, PathCalculator.food_to_nest.get(target_PathNodes.get(ant.id))); }

    // If the target is null, we've hit the end of the path.
    if( target_PathNodes.get( ant.id ) == null )
    {
      ant_phases.put( ant.id, 0 );
      target_PathNodes.put( ant.id, PathCalculator.nest_location );
      return false;
    }
    return moveAlongPath( ant, action, A_Star.getPath( target_PathNodes.get(ant.id), A_Star.board[ant.gridX][ant.gridY]));
  }

  // Provides the nest step from one's current position using an A* hashmap.
  private boolean moveAlongPath(AntData ant, AntAction action, Map<PathNode,PathNode> path)
  {
    Direction dir;
    PathNode antSpot = A_Star.board[ant.gridX][ant.gridY];
    PathNode nextStep = path.get(antSpot);

    try
    {
      if (antSpot == null)
      {
        System.out.println("Error, the spot the ant is in is somehow invalid.");
        return false;
      }
      if (nextStep == null)
      {
        System.out.println("I would recalculate, but in this instance I can't.");
      }
      if ((ant.gridY - 1 == nextStep.y) && (ant.gridX == nextStep.x))
      {
        dir = Direction.NORTH;
      }
      else if ((ant.gridY - 1 == nextStep.y) && (ant.gridX + 1 == nextStep.x))
      {
        dir = Direction.NORTHEAST;
      }
      else if ((ant.gridY == nextStep.y) && (ant.gridX + 1 == nextStep.x))
      {
        dir = Direction.EAST;
      }
      else if ((ant.gridY + 1 == nextStep.y) && (ant.gridX + 1 == nextStep.x))
      {
        dir = Direction.SOUTHEAST;
      }
      else if ((ant.gridY + 1 == nextStep.y) && (ant.gridX == nextStep.x))
      {
        dir = Direction.SOUTH;
      }
      else if ((ant.gridY + 1 == nextStep.y) && (ant.gridX - 1 == nextStep.x))
      {
        dir = Direction.SOUTHWEST;
      }
      else if ((ant.gridY - 1 == nextStep.y) && (ant.gridX - 1 == nextStep.x))
      {
        dir = Direction.NORTHWEST;
      }
      else if ((ant.gridY == nextStep.y) && (ant.gridX - 1 == nextStep.x))
      {
        dir = Direction.WEST;
      }
      else
      {
        System.out.println("Something got messed up, A* gave me a node that's too far: " + (ant.gridX - nextStep.x) + " " + (ant.gridY - nextStep.y));
        dir = null;
      }
      action.type = AntAction.AntActionType.MOVE;
      action.direction = dir;
      return true;
    }
    catch(NullPointerException e)
    {
      return false;
    }
  }


}
