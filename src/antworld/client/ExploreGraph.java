package antworld.client;

import antworld.common.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
  public ExploreGraph(int nest_x, int nest_y )
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
    if( ant.gridX == targets.get(ant.id).x && ant.gridY == targets.get(ant.id).y )
    {
      ArrayList<GraphNode> path = path_taken.get(ant.id);
      targets.get(ant.id).explored = true;

      // If we've hit a dead end ...
      if( targets.get( ant.id ).getUnexploredNeighbors().isEmpty() )
      {
        //System.out.println("No more nodes on this branch to explore - collapsing.");
        if( path.size() <= 1 )
        {
          GraphNode rp = null;

          // If we wanted to assure a consistent direction (e.g. for patrols), this is where we'd do it - rather than
          // picking a random next node, we could search ndoes for those most closely aligned to our current direction.
          // There is of course no guarentee that a node in a similar direction exists, nor that the path one would take
          // to it would at all times be in the same direction.
          while( rp == null ){ rp = BuildGraph.graphNodes[Constants.random.nextInt(2500)][Constants.random.nextInt(1500)]; }
          targets.put(ant.id, rp );
        }
        else
        {
          path.remove(path.size() - 1);
          //System.out.println("Now I'm going to go to : " + path.get(path.size() - 1).x + " " + path.get(path.size() - 1).y);
          targets.put(ant.id, path.get(path.size() - 1));
        }
      }
      else
      {
        path.add(targets.get(ant.id)); // Add the last explored node to our list of traversed nodes.
        //targets.put( ant.id, targets.get(ant.id).getUnexploredNeighbors().get(0) );
        targets.put( ant.id, targets.get(ant.id).getUnexploredNeighbors().get(
                Constants.random.nextInt(targets.get(ant.id).getUnexploredNeighbors().size()) ) );
      }
      current_target = targets.get(ant.id);
      PathNode antSpot = A_Star.board[ant.gridX][ant.gridY];
      PathNode finalSpot = A_Star.board[current_target.x][current_target.y];
      path_to_target.put( ant.id, A_Star.getPath( finalSpot, antSpot));
    }

    // This is the new code, to use A* (locally applied).
//    PathNode antSpot = A_Star.board[ant.gridX][ant.gridY];
//    PathNode finalSpot = A_Star.board[current_target.x][current_target.y];
//    A_Star.getPath( finalSpot, antSpot)
    return moveAlongPath( ant, action, path_to_target.get(ant.id));
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
        else if (goExplore(ant, action)) {}
        ant.action = action;
      }
    }
  }
  static boolean moveAlongPath( AntData ant, AntAction action, Map<PathNode,PathNode> path)
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
      System.out.println("Error, the spot that you're trying to go to is somehow invalid.");
      return false;
    }

//    System.out.println("Here's the path A* found: ");
//    System.out.printf("(Starting at (%d,%d))\n", ant.gridX, ant.gridY);
//    while( nextStep != null)
//    {
//      System.out.println("  " + nextStep);
//      nextStep = path.get(nextStep);
//    }

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
      action.x = origin_x;
      action.y = origin_y;

      action.type = AntAction.AntActionType.EXIT_NEST;
      return true;
    }
    return false;
  }
  //</editor-fold>


}
