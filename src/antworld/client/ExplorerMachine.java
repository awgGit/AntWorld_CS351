/*
Explorer (state) machine:
  Sends explorers out along the GraphNode graph.
  When food site is found, explorers retreat into the nest.
 */

package antworld.client;

import antworld.common.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

class ExplorerMachine
{

  static boolean food_was_found = false;
  static boolean all_underground = false;
  static PathNode food_location = null;

  private HashMap<Integer,GraphNode> target_GraphNodes;
  private HashMap<Integer,PathNode> target_PathNodes;
  private HashMap<Integer,ArrayList<GraphNode>> path_taken;
  private HashMap<Integer, Map<PathNode, PathNode>> path_to_target;
  private ArrayList<Integer> ant_ids;
  private int nest_x;
  private int nest_y;

  // Initializes all the nest & state variables for ExplorerMachine.
  public ExplorerMachine( int nx, int ny )
  {
    nest_x = nx;
    nest_y = ny;
    ant_ids = new ArrayList<>();
    target_GraphNodes = new HashMap<>();
    target_PathNodes = new HashMap<>();
    path_to_target = new HashMap<>();
    path_taken = new HashMap<>();
  }

  // Configures a single ant to be controlled by ExplorerMachine.
  public void addAnt( AntData ant )
  {
    if( ant_ids.contains(ant.id) ) return;
    ant_ids.add( ant.id );

    // Initialize the path record so we may add to it.
    ArrayList<GraphNode> path = new ArrayList<>();
    path.add( BuildGraph.graphNodes[ nest_x ][ nest_y ]);
    path_taken.put( ant.id, path );

    // Add the closest node to our position as our current target GraphNode.
    target_GraphNodes.put( ant.id,
            BuildGraph.graphNodes
                    [ (ant.gridX/ BuildGraph.resolution)* BuildGraph.resolution ]
                    [ (ant.gridY/ BuildGraph.resolution)* BuildGraph.resolution ]);

    // Build the path from our position to our targeted *graph* node's position (as a *path* node)
    path_to_target.put( ant.id, A_Star.getPath(
            A_Star.board
                    [ (ant.gridX/ BuildGraph.resolution)* BuildGraph.resolution ]
                    [ (ant.gridY/ BuildGraph.resolution)* BuildGraph.resolution ],
            A_Star.board[ ant.gridX ][ ant.gridY ]));

    // Set our current position, wherever it is, as the first target *path* node.
    // This is also guarenteed to the first node on the path to our target *graph* node.
    target_PathNodes.put( ant.id, A_Star.board[ ant.gridX ][ ant.gridY ] );

  }

  // Here's where we decide the states & state transitions for explorers.
  public void setAntActions( PacketToClient ptc )
  {
    // We don't use these ants if we've already found food and they've retracted.
    if( food_was_found && all_underground ) { return; }

    // Check if we've found any food sites, avoiding marking dead bodies as valid food sites.
    if( ptc.foodList != null )
    {
      for( FoodData food : ptc.foodList )
      {
        if( food.objType == GameObject.GameObjectType.FOOD && food.quantity > 16)
        {
          food_was_found = true; // Indicate we've seen food, and remember the location.
          food_location = A_Star.board[ptc.foodList.get(0).gridX][ptc.foodList.get(0).gridY];
        }
      }
    }

    // Check if all the ants are underground yet.
    all_underground = checkIfAllUnderground( ptc );

    AntAction action;
    for( AntData ant : ptc.myAntList )
    {
      if (!ant_ids.contains(ant.id)) continue;
      action = new AntAction(AntAction.AntActionType.NOOP);
      if( food_was_found )
      {
          if( MiscFunctions.enterNest( ant, action, nest_x, nest_y ));
          else if( MiscFunctions.healSelf( ant, action ));
          else if ( attemptBeeline(ant, action));
          else if ( exploreGraph( ant, action ));
          if( MiscFunctions.jitter( ant, action, ptc ));
      }
      else
      {
        if (MiscFunctions.exitNest(ant, action, ptc, nest_x, nest_y));
        else
        {
          if( MiscFunctions.healSelf( ant, action ));
          else if ( exploreGraph( ant, action ) );
          if( MiscFunctions.jitter( ant, action, ptc ));
        }
      }
      ant.action = action; // This line is necessary.
    }
  }

  private boolean attemptBeeline( AntData ant, AntAction action )
  {
    int distance_to_nest = Math.abs( ant.gridX - nest_x ) + Math.abs( ant.gridY - nest_y );
    int raycast_distance = Raycasting.getDistanceToWaterUsingVector( ant.gridX, ant.gridY, nest_x, nest_y );
    if( distance_to_nest <= raycast_distance && distance_to_nest <= 200 )
    {
      return moveAlongPath(ant, action, A_Star.getPath( A_Star.board[ nest_x ][ nest_y ], A_Star.board[ ant.gridX ][ ant.gridY ]));
    }
    return false;
  }

  // Go from where we are to our target node.
  private boolean exploreGraph (AntData ant, AntAction action )
  {
    // If we've reached our target *path* node, then advance the targeted *path* along the path
    // to the targeted *graph* node.
    if(ant.gridX == target_PathNodes.get(ant.id).x && ant.gridY == target_PathNodes.get(ant.id).y ){
      target_PathNodes.put( ant.id, path_to_target.get(ant.id).get(target_PathNodes.get(ant.id))); }

    if( target_PathNodes.get(ant.id) == null)
    {
      System.out.println("Gonna crash.");
      System.out.println( target_GraphNodes.get(ant.id).x + " " + target_GraphNodes.get(ant.id).y + " <- " + ant.gridX + " " + ant.gridY );
    }

    if( ant.gridX == target_GraphNodes.get(ant.id).x && ant.gridY == target_GraphNodes.get(ant.id).y)
    {
      // Mark target as explored.
      target_GraphNodes.get(ant.id).explored = true;

      // If our target (that we've reached) has no neighbors, collapse.
      // If collapsing is impossible, search for a new target node.
      ArrayList<GraphNode> path = path_taken.get(ant.id);
      if( target_GraphNodes.get( ant.id ).getUnexploredNeighbors().isEmpty() || food_was_found)
      {
        // If we cannot collapse further, find an entirely new GraphNode.
        if( path.size() <= 1 )
        {
          // If we're in the retraction mode, and we've ended up without a way
          // back to our nest somehow, set the nest as the target and A* back.
          if( food_was_found )
          {
            target_GraphNodes.put(ant.id, BuildGraph.graphNodes[nest_x][nest_y]);
          }
          // If we're still exploring, pick a random node to explore.
          // ToDo: Right now it's totally random, which is bad.
          else
          {
            GraphNode new_target = null;
            while( new_target == null ){ new_target = BuildGraph.graphNodes[Constants.random.nextInt(2500)][Constants.random.nextInt(1500)]; }
            target_GraphNodes.put(ant.id, new_target );
          }
        }
        // If we can collapse further, do so. Set previous as target.
        // Todo: Right now this is only collapsing once - it'd be nice if it collapsed faster.
        // Todo: Better yet, have it A* if a raycast check says it's directly accessible.
        else
        {
          path.remove(path.size() - 1);
          target_GraphNodes.put(ant.id, path.get(path.size() - 1));
        }
      }
      // If we've got neighbors to explore, add this node to our path,
      // and pick a random GraphNode from the list of neighbors.
      else
      {
        path.add(target_GraphNodes.get(ant.id));
        target_GraphNodes.put( ant.id, target_GraphNodes.get(ant.id).getUnexploredNeighbors().get(
                Constants.random.nextInt(target_GraphNodes.get(ant.id).getUnexploredNeighbors().size()) ) );
      }

      // Now that we have a graphnode as a target, we calculate the A* path from
      // our current position (wherever that may be) to the GraphNode.
      GraphNode current_target = target_GraphNodes.get(ant.id);
      PathNode antSpot = A_Star.board[ant.gridX][ant.gridY];
      PathNode finalSpot = A_Star.board[current_target.x][current_target.y];
      path_to_target.put( ant.id, A_Star.getPath( finalSpot, antSpot));
      target_PathNodes.put( ant.id, antSpot);
    }

    // If the path is of length zero (we're already at our destination) wait for the next tick.
    // We must do this to avoid recalculating A* when we're assigned our own position as the first node.
    // Mildly inefficient (wastes a turn).
    if( target_PathNodes.get(ant.id).x == ant.gridX && target_PathNodes.get(ant.id).y == ant.gridY ) return false;

    // Move along the path from wherever we are to our targeted *path* node.
    // The reason for this indirection is so that we can jitter but always get back on path.
    return moveAlongPath( ant, action, A_Star.getPath( target_PathNodes.get(ant.id), A_Star.board[ant.gridX][ant.gridY]));
  }

  // Ascertain whether or not all *explorer* ants are underground.
  private boolean checkIfAllUnderground( PacketToClient ptc )
  {
    int count = 0;
    for( AntData ant : ptc.myAntList )
    {
      if( !ant_ids.contains(ant.id) ) continue;
      if( ant.state != AntAction.AntState.UNDERGROUND ) count++;
    }
    return count < 3;
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
        System.out.println("Recalculating A* b/c nextStep was null...");
        GraphNode target = target_GraphNodes.get(ant.id);
        PathNode antSpot2 = A_Star.board[ant.gridX][ant.gridY];
        PathNode finalSpot = A_Star.board[target.x][target.y];
        path = A_Star.getPath(finalSpot, antSpot);
        path_to_target.put(ant.id, path);
        nextStep = path.get(antSpot2);
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
