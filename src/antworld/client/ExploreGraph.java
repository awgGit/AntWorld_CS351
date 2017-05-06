package antworld.client;

import antworld.common.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/*
  Explores a graph, wherein each node of the graph has a list of neighbors, and a flag to show if it's been explored.

    1. Each ant targets a node randomly from the list of neighboring nodes, and acquires the A* path to this node.
    2. The ant moves along this path until it has reached the node it's targeting.
    3. If the newly acquired node has unexplored neighbors, go to 1.
        Otherwise, set the previous node as the target, and go to 2.
 */
public class ExploreGraph
{
  Map<Integer,GraphNode> targets;
  Map<Integer,ArrayList<GraphNode>> path_taken; // The *big* nodes traversed.
  Map<Integer,Map<PathNode,PathNode>> path_to_target; // The *grid* (small) nodes traversed.

  ArrayList<Integer> ant_ids;

  private int origin_x;
  private int origin_y;

  // Initialize the maps & lists.
  public ExploreGraph( int nest_x, int nest_y )
  {
    path_taken = new HashMap<>();
    ant_ids = new ArrayList<>();
    targets = new HashMap<>();
    path_to_target = new HashMap<>();

    origin_x = nest_x;
    origin_y = nest_y;
  }

  // Add an ant to the exploration process : requires that the ant be *on* a node when added.
  public void addAnt( AntData ant )
  {
    if( ant_ids.contains( ant.id ) ) return;
    if( BuildGraph.graphNodes[ant.gridX][ant.gridY] == null )
    {
      System.out.println("Error: Ant is not on a node.");
    }
    ant_ids.add( ant.id );
    path_taken.put( ant.id, new ArrayList<>() );

    ArrayList<GraphNode> path = new ArrayList<>();
    path.add(BuildGraph.graphNodes[ant.gridX][ant.gridY]);
    path_taken.put( ant.id, path );
    targets.put( ant.id, BuildGraph.graphNodes[ant.gridX][ant.gridY] );
    path_to_target.put( ant.id, null );
  }

  // The logic for how to explore the graph. Probably requires some more attention.
  public boolean goExplore( AntData ant, AntAction action )
  {
    GraphNode current_target; // Target of the ant we're using in this function.

    // If we're on our target node, mark as explored and change path to the next unexplored node.
    // Further, if the node we're traveling to has already been explored, backtrack.
    if( Math.abs(ant.gridX - targets.get(ant.id).x)<2 && Math.abs(ant.gridY-targets.get(ant.id).y)<2 || targets.get(ant.id).explored )
    {
      ArrayList<GraphNode> path = path_taken.get(ant.id);
      targets.get(ant.id).explored = true;

      // If we've hit a dead end ...
      if( targets.get( ant.id ).getUnexploredNeighbors().isEmpty() )
      {
        // If we've collapsed the tree and there's nowhere else to turn, try to find a random unexplored point,
        // starting from where we are all the way to anywhere, with a preference for nearby nodes.
        if( path.size() <= 1 ) // || some random 0-1 > 0.9
        {
          // DEBUGGING: I wanna see if this makes them fast all the time.
//          GraphNode rp = null;
//          int r = 1;
//          //while( rp == null )
//          while( rp == null || rp.explored )
//          {
//            rp = BuildGraph.graphNodes[ ((Constants.random.nextInt(r)+(ant.gridX/10))*10) % 2500 ][ ((Constants.random.nextInt(r)+(ant.gridY/10))*10) % 1500];
//            r += Constants.random.nextFloat() > 0.98? 1 : 0; // Constants.random.nextInt(2500)>r? 1 : 0; // Asymptotically decay addition as distance expands.
//          }
//          targets.put(ant.id, rp );
        }
        // On the other hand, if we can still collapse, do so.
        else
        {
          path.remove(path.size() - 1);
          targets.put(ant.id, path.get(path.size() - 1));
        }
      }
      else // If there are still neighbords to be explored, pick a random neighbor.
      {
        path.add(targets.get(ant.id)); // Add the last explored node to our list of traversed nodes.
        targets.put( ant.id, targets.get(ant.id).getUnexploredNeighbors().get(
                Constants.random.nextInt(targets.get(ant.id).getUnexploredNeighbors().size()) ) );
      }
      current_target = targets.get(ant.id);
      PathNode antSpot = A_Star.board[ant.gridX][ant.gridY];
      PathNode finalSpot = A_Star.board[current_target.x][current_target.y];
      path_to_target.put( ant.id, A_Star.getPath( finalSpot, antSpot)); // Only recalculate if we're at a node.
    }

    return moveAlongPath( ant, action, path_to_target.get(ant.id));
  }

  // Recalculate the path from ourselves to our target.
  // Handy to have as its own function so we can call it after we get off path.
  public void recalculatePath( AntData ant )
  {
    GraphNode current_target = targets.get(ant.id);
    PathNode antSpot = A_Star.board[ant.gridX][ant.gridY];
    PathNode finalSpot = A_Star.board[current_target.x][current_target.y];
    path_to_target.put( ant.id, A_Star.getPath( finalSpot, antSpot));
  }

  // Implementation details are just instructions to say how to get out & follow a path.
  //<editor-fold desc="Implementation details">
  public void setAntActions( PacketToClient data )
  {
    AntAction action;
    for (AntData ant : data.myAntList)
    {
      action = new AntAction(AntAction.AntActionType.NOOP);
      if (ant_ids.contains(ant.id))
      {
        if (exitNest(ant, action, data)) {}
        else
        {
          if (healSelf(ant,action)) {}         // Raycast, go to water, pick it up, go back to path.
          //else if (getFood(ant,action)) {}        // If food is spotted and is nearby, go fetch it. If we hold food, go back to nest.
          //else if (killOthers(ant,action)) {}     // If other ants are nearby, kill them.
          //else if (askForHelp(ant,action)) {}     // If low health, enemy ants nearby, call for help.
          //else if (respondToHelp(ant,action)) {}  // If nearby and in fighting shape, run to assist the ant needing help.
          else if (goExplore(ant, action)) {}
          if (jitter(ant, action, data)) {}
        }
        ant.action = action;
      }
    }
  }

  private boolean jitter( AntData ant, AntAction action, PacketToClient ptc )
  {
    for (AntData other_ant : ptc.myAntList)
    {
      if( other_ant.id == ant.id) continue; // Don't jitter with self...obviously
      // For now, don't jitter if we're too close the nest.
      if( Math.abs(ant.gridX-origin_x) < 20 && Math.abs(ant.gridY-origin_y) < 20 )
      {
        continue;
      }
      if (Math.abs(other_ant.gridX - ant.gridX) <= 1 && Math.abs(other_ant.gridY - ant.gridY) <= 1)
      {
        // Just twist aside, rather than randomly jitter.
        action.direction = Direction.getRandomDir(); //  Direction.values()[ (action.direction.ordinal() + (Constants.random.nextBoolean()? 1 : 7)) % 8 ];
        // COM // recalculatePath( ant );
        return true;
      }
    }
    return false;
  }

  private boolean healSelf( AntData ant, AntAction action )
  {
    // Don't heal if we don't need to. (High watermark)
    if( ant.health >= ant.antType.getMaxHealth() ) return false;

    // Heal if we have enough units and we're at sufficiently low health.
    if( ant.carryUnits > 0 )
    {
      action.type = AntAction.AntActionType.HEAL;
      action.direction = null;
      action.quantity = ant.carryUnits;
      return true;
    }

    // Don't go off path to heal unless we need to. (Low watermark)
    if( ant.health > ant.antType.getMaxHealth()/3 ) return false;

    // Point ourselves towards water.
    action.direction = Direction.values()[ (int) (Math.floor( Raycasting.getBearingToWater(ant.gridX, ant.gridY) )/45.0) ];

    System.out.println("Distance I see: " + Raycasting.getDistanceToWaterUsingVector(ant.gridX, ant.gridY, ant.gridX+action.direction.deltaX(), ant.gridY+action.direction.deltaY()));

    // If we're next to water, just pick it up.
    if( Raycasting.getDistanceToWaterUsingVector(ant.gridX, ant.gridY, ant.gridX+action.direction.deltaX(), ant.gridY+action.direction.deltaY()) <= 1
            && Constants.random.nextBoolean() ) // So maybe move closer.
    {
      //action.direction = //Direction.getRandomDir(); // For now... I mean, it should work, but whatever.
      action.type = AntAction.AntActionType.PICKUP;
      action.quantity = ant.antType.getCarryCapacity();

      // If we've moved off the path, then we'll need to recalculate it.
      // COM // recalculatePath( ant ); // We'll only do this once per heal. This should in theory work...

      return true;
    }
    else // If we aren't adjacent, then keep moving to the water.
    {
      action.type = AntAction.AntActionType.MOVE;
      return true;
    }
  }

  boolean moveAlongPath( AntData ant, AntAction action, Map<PathNode,PathNode> path)
  {
    Direction dir;
    PathNode antSpot = A_Star.board[ant.gridX][ant.gridY];

    PathNode nextStep = path.get(antSpot);

    if( antSpot == null)
    {
      System.out.println("Error, the spot the ant is in is somehow invalid.");
      return false;
    }
    if( nextStep == null)
    {
      recalculatePath( ant );
      path = path_to_target.get(ant.id);
      if( path.get(antSpot) == null)
      {
        System.out.println("ERROR: Can't seem to calculate a path.");
        if( antSpot.x == targets.get(ant.id).x && antSpot.y == targets.get(ant.id).y)
        {
          // So if we get here, then we need to update the target.
          System.out.println("   Well, because we're there: " + BuildGraph.graphNodes[ant.gridX][ant.gridY].explored );
        }
        else
        {
          System.out.println("   " + antSpot.x + " " + antSpot.y + " : " + targets.get(ant.id).x + " " + targets.get(ant.id).y);
        }
        return false;
      }
      // Otherwise, continue on our merry way ...
    }

    nextStep = path.get(antSpot);

    if((ant.gridY-1 == nextStep.y)&& (ant.gridX == nextStep.x)) { dir = Direction.NORTH; }
    else if((ant.gridY-1 == nextStep.y)&& (ant.gridX+1 == nextStep.x)) { dir = Direction.NORTHEAST; }
    else if((ant.gridY == nextStep.y)&& (ant.gridX+1 == nextStep.x)) { dir = Direction.EAST; }
    else if ((ant.gridY+1 == nextStep.y)&& (ant.gridX+1 == nextStep.x)) { dir = Direction.SOUTHEAST; }
    else if ((ant.gridY+1 == nextStep.y)&& (ant.gridX == nextStep.x)) { dir = Direction.SOUTH; }
    else if ((ant.gridY+1 == nextStep.y)&& (ant.gridX-1 == nextStep.x)) { dir = Direction.SOUTHWEST; }
    else if ((ant.gridY-1 == nextStep.y)&& (ant.gridX-1 == nextStep.x)) { dir = Direction.NORTHWEST; }
    else if ((ant.gridY == nextStep.y) && (ant.gridX-1 == nextStep.x)) { dir = Direction.WEST; }
    else
    {
      System.out.println("Something got messed up, A* gave me a node that's too far: " + (ant.gridX-nextStep.x) + " " + (ant.gridY-nextStep.y) );
      dir = null;
    }

    action.type = AntAction.AntActionType.MOVE;
    action.direction = dir;
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

      // Something, something, something ... dark side.
      action.x = origin_x;
      action.y = origin_y;

      action.type = AntAction.AntActionType.EXIT_NEST;
      return true;
    }
    return false;
  }
  //</editor-fold>


}
