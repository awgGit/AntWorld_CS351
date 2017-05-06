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
  Map<Integer, Boolean> homePath_set;
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
    homePath_set = new HashMap<>();


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
    homePath_set.put(ant.id, false);
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
          // picking a random next node, we could search nodes for those most closely aligned to our current direction.
          // There is of course no guarantee that a node in a similar direction exists, nor that the path one would take
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

  public boolean pickUpFoodAdjacent(AntData ant, AntAction action, PacketToClient data)
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
              return true;
            }
          }
        }
      }
    }
    return false;
  }

  private boolean goToFood(AntData ant, AntAction action, PacketToClient ptc)
  {
    if( ant.carryUnits >= ant.antType.getCarryCapacity() ) return false;
    int x_diff;
    int y_diff;
    int distance;
    if( ptc.foodList != null)
    {
      for (GameObject food : ptc.foodList)
      {
        if (food.objType == GameObject.GameObjectType.WATER) continue;
        x_diff = Math.abs(food.gridX - ant.gridX);
        y_diff = Math.abs(food.gridY - ant.gridY);
        distance = (int) Math.sqrt((x_diff * x_diff) + (y_diff * y_diff));
        if(distance < ant.antType.getVisionRadius())
        {
          int xDiff = ant.gridX - food.gridX;
          int yDiff = ant.gridY - food.gridY;
          if (xDiff < 0 && yDiff > 0) action.direction = Direction.NORTHEAST;
          else if (xDiff == 0 && yDiff > 0) action.direction = Direction.NORTH;
          else if (xDiff > 0 && yDiff > 0) action.direction = Direction.NORTHWEST;
          else if (xDiff > 0 && yDiff == 0) action.direction = Direction.WEST;
          else if (xDiff > 0 && yDiff < 0) action.direction = Direction.SOUTHWEST;
          else if (xDiff == 0 && yDiff > 0) action.direction = Direction.SOUTH;
          else if (xDiff < 0 && yDiff < 0) action.direction = Direction.SOUTHEAST;
          else if (xDiff < 0 && yDiff == 0) action.direction = Direction.EAST;
          action.type = AntAction.AntActionType.MOVE;
          action.quantity = ant.antType.getCarryCapacity();
          return true;
        }
      }
    }
    return false;
  }

  private boolean goHomeIfCarrying(AntData ant, AntAction action, PacketToClient ptc)
  {
    return  enterNest(ant,action,ptc) // Enter the nest if we're close enough to it.
            || goHomeIfCarryingFood(ant,action,ptc,ant.antType.getCarryCapacity()/2); // Return if full with food.
  }

  // Split the logic into multiple functions
  private boolean goHomeIfCarryingFood( AntData ant, AntAction action, PacketToClient ptc, int carry_threshold)
  {
    return ant.carryType == GameObject.GameObjectType.FOOD && ant.carryUnits >= carry_threshold && goHome(ant, action, ptc);
  }

  private boolean goHome( AntData ant, AntAction action, PacketToClient ptc )
  {
    //int nest_y = ptc.nestData[ptc.myNest.ordinal()].centerY;
    //int nest_x = ptc.nestData[ptc.myNest.ordinal()].centerX;
    //PathNode nestSpot = A_Star.board[nest_x][nest_y];
    //Map<PathNode,PathNode> path = A_Star.getPath( nestSpot, antSpot, ptc, ant.id);
    Map<PathNode,PathNode> path;
    PathNode antSpot = A_Star.board[ant.gridX][ant.gridY];
    if(!homePath_set.get(ant.id))
    {
      PathNode target = A_Star.board[targets.get(ant.id).x][targets.get(ant.id).y];
      path = A_Star.getPath(target, antSpot);
      path_to_target.put(ant.id, path);
      homePath_set.put(ant.id, true);
      System.out.println("Ant[" + ant.id + "]: Home Path Set!");
    }
    else if(antSpot.x == targets.get(ant.id).x && antSpot.y == targets.get(ant.id).y)
    {
      ArrayList<GraphNode> nodes = path_taken.get(ant.id);
      nodes.remove(nodes.size()-1);
      GraphNode targetNode = nodes.get(nodes.size()-1);
      PathNode target = A_Star.board[targetNode.x][targetNode.y];
      path_to_target.put(ant.id, A_Star.getPath(target, antSpot));
      targets.put(ant.id, targetNode);
      System.out.println("Ant[" + ant.id + "]: Going Home!");
    }
    return moveAlongPath( ant, action, path_to_target.get(ant.id) );
  }

  // Jitter to avoid getting stuck...
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

  private boolean enterNest( AntData ant, AntAction action, PacketToClient ptc )
  {
    int nestY = ptc.nestData[ptc.myNest.ordinal()].centerY;
    int nestX = ptc.nestData[ptc.myNest.ordinal()].centerX;

    if( (Math.abs(ant.gridX-nestX)+Math.abs(ant.gridY-nestY) < 15) && (ant.carryUnits !=0 || ant.health < ant.antType.getMaxHealth() / 2))
    {
      action.direction = null;
      action.type = AntAction.AntActionType.ENTER_NEST;
      action.quantity = ant.carryUnits;
      homePath_set.put(ant.id, false);
      return true;
    }
    return false;
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
        if (exitNest(ant, action, data))
        {
          System.out.println("Antid: [" + ant.id + "] is taking action EXITNEST");
        }
        else
        {
          if (goHomeIfCarrying(ant, action, data))
          {
            System.out.println("Antid: [" + ant.id + "] is taking action GOHOMEIFCARRYINGORHURT");
          }
          else if(healSelf(ant,action))
          {
            System.out.printf("ant_it : [%d] is taking action HEALSELF\n", ant.id);
          }
          else if (pickUpFoodAdjacent(ant, action, data))
          {
            System.out.println("Antid: [" + ant.id + "] is taking action PICKUPFOODADJACENT");
          }
          else if (goToFood(ant, action, data))
          {
            System.out.println("Antid: [" + ant.id + "] is taking action GOTOFOOD");
          }
          else if (goExplore(ant, action))
          {
            System.out.println("Antid: [" + ant.id + "] is taking action GOEXPLORE");
          }
          if( jitter(ant,action,data))
          {
            System.out.println("initiating jitterbug");
          }
        }
        ant.action = action;
      }
    }
  }

  private boolean healSelf( AntData ant, AntAction action )
  {
    // Don't heal if we don't need to. (High watermark)
    if( ant.health >= ant.antType.getMaxHealth() ) return false;

    // Heal if we have enough units and we're at sufficiently low health.
    if(ant.carryUnits > 0 && ant.carryType == GameObject.GameObjectType.WATER)
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


  private boolean moveAlongPath( AntData ant, AntAction action, Map<PathNode,PathNode> path)
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
      GraphNode target = targets.get(ant.id);
      PathNode antSpot2 = A_Star.board[ant.gridX][ant.gridY];
      PathNode finalSpot = A_Star.board[target.x][target.y];
      path = A_Star.getPath(finalSpot, antSpot);
      path_to_target.put(ant.id, path);
      nextStep = path.get(antSpot2);
    }

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

  private boolean attack(AntData ant, AntAction action, PacketToClient ptc)
  {
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
        if(action.direction == null) System.out.println("Direction not set!!");
        return true;
      }
    }
    return false;
  }

  //</editor-fold>


}
