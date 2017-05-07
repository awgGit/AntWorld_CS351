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
  //ArrayList<Integer> ant_ids;
  ArrayList<FoodData> food_list;

  Map<Integer, Boolean> homePath_set;
  Map<Integer, Boolean> foodPathTaken;
  Map<Integer,Map<PathNode, PathNode>> nest_to_food;
  Map<Integer,Map<PathNode, PathNode>> food_to_nest;
  Map<Integer, Map<PathNode, PathNode>> path_to_pheromone_trail;
  A_Star path_generator;
  Map<Integer, Boolean> healingToFull;
  ArrayList<Integer> ant_ids;

  protected boolean[] pheromone_path_generated;
  boolean [] convoy_sent;
  private boolean pheromone_paths_found = false;
  int nest_x, nest_y;
  int food_sites_to_broadcast = 1;

  // Initialize the maps & lists.
  public ExploreGraph(A_Star path_tool, int nest_x, int nest_y)
  {
    path_taken = new HashMap<>();
    ant_ids = new ArrayList<>();
    food_list = new ArrayList<>();
    targets = new HashMap<>();
    path_to_target = new HashMap<>();
    foodPathTaken = new HashMap<>();
    homePath_set = new HashMap<>();
    nest_to_food = new HashMap<>();
    food_to_nest = new HashMap<>();
    path_generator = path_tool;
    pheromone_path_generated = new boolean[food_sites_to_broadcast];
    healingToFull = new HashMap<>();

    path_to_pheromone_trail = new HashMap<>();
    convoy_sent = new boolean[food_sites_to_broadcast];
    for(int j = 0; j < food_sites_to_broadcast; j++)
    {
      pheromone_path_generated[j] = false;
      convoy_sent[j] = false;
    }

    this.nest_x = nest_x;
    this.nest_y = nest_y;
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
    foodPathTaken.put(ant.id, false);
    foodPathTaken.putIfAbsent(ant.id, false);
    healingToFull.putIfAbsent(ant.id, false);
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
              foodPathTaken.put(ant.id, false);
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
    if( pheromone_paths_found) return false;
    int x_diff;
    int y_diff;
    int distance;
    if( ptc.foodList != null)
    {
      for (FoodData food : ptc.foodList)
      {
        if(food.quantity <= 2) continue;
        if (food.objType == GameObject.GameObjectType.WATER) continue;
        x_diff = Math.abs(food.gridX - ant.gridX);
        y_diff = Math.abs(food.gridY - ant.gridY);
        distance = (int) Math.sqrt((x_diff * x_diff) + (y_diff * y_diff));
        if(distance < ant.antType.getVisionRadius())
        {
          if(!foodPathTaken.get(ant.id))
          {
            foodPathTaken.put(ant.id, true);
            PathNode foodSpot = A_Star.board[food.gridX][food.gridY];
            PathNode antSpot = A_Star.board[ant.gridX][ant.gridY];
            path_to_target.put(ant.id, A_Star.getPath(foodSpot, antSpot));
            moveAlongPath(ant, action, path_to_target.get(ant.id));
          }
          else
          {
            moveAlongPath(ant, action, path_to_target.get(ant.id));
          }

          return true;
        }
      }
    }
    return false;
  }

  private boolean goHomeIfCarrying(AntData ant, AntAction action, PacketToClient ptc)
  {
    return  enterNest(ant,action) // Enter the nest if we're close enough to it.
            || goHomeIfCarryingFood(ant,action,ptc,ant.antType.getCarryCapacity()/2);
  }

  // Split the logic into multiple functions
  private boolean goHomeIfCarryingFood( AntData ant, AntAction action, PacketToClient ptc, int carry_threshold)
  {
    if(ant.carryType == GameObject.GameObjectType.FOOD && ant.carryUnits >= carry_threshold)
    {
      return goHome(ant, action);
    }
    else return false;
  }

  private boolean foodSitesFound(AntData ant, AntAction action)
  {
    /*
    for(int j = 0; j < food_list.size(); j++)
    {
      if(food_to_nest.get(j) == null && !pheromone_path_generated[j])
      {
        pheromone_path_generated[j] = true;
        System.out.println("Calling A*");
        path_generator.setPath(food_to_nest, j, new PathNode(food_list.get(j).gridX, food_list.get(j).gridY, 0),
                new PathNode(nest_x, nest_y, 0));
        new Thread(path_generator).start();
        path_generator.setPath(nest_to_food, j, new PathNode(nest_x, nest_y, 0),
                new PathNode(food_list.get(j).gridX, food_list.get(j).gridY, 0));
        new Thread(path_generator).start();
      }
    }
    */
    if (food_list.size() == food_sites_to_broadcast)
    {
      pheromone_paths_found = true;
      return goHome(ant, action);
    }

    return false;
  }

  private boolean goHome( AntData ant, AntAction action)
  {
    //int nest_y = ptc.nestData[ptc.myNest.ordinal()].centerY;
    //int nest_x = ptc.nestData[ptc.myNest.ordinal()].centerX;
    //PathNode nestSpot = A_Star.board[nest_x][nest_y];
    //Map<PathNode,PathNode> path = A_Star.getPath( nestSpot, antSpot, ptc, ant.id);
    ArrayList<GraphNode> path = path_taken.get(ant.id);
    PathNode antSpot = A_Star.board[ant.gridX][ant.gridY];
    if(!homePath_set.get(ant.id))
    {
      GraphNode node = path.get(path.size()-1);
      path.remove(path.size()-1);
      PathNode target = A_Star.board[node.x][node.y];
      targets.put(ant.id,node);
      path_to_target.put(ant.id, A_Star.getPath(target, antSpot));
      homePath_set.put(ant.id, true);
      System.out.println("Ant[" + ant.id + "]: Home Path Set!");
    }
    else if(antSpot.x == targets.get(ant.id).x && antSpot.y == targets.get(ant.id).y)
    {
      path.remove(path.size()-1);
      GraphNode node = path.get(path.size()-1);
      PathNode target = A_Star.board[node.x][node.y];
      path_to_target.put(ant.id, A_Star.getPath(target, antSpot));
      targets.put(ant.id, node);
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
      if( Math.abs(ant.gridX-nest_x) < 20 && Math.abs(ant.gridY-nest_y) < 20 )
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

  public boolean enterNest( AntData ant, AntAction action)
  {
    if( (Math.abs(ant.gridX-nest_x)+Math.abs(ant.gridY-nest_y) < 15) && (ant.carryUnits > 0))
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
        else if (healSelf(ant, action))
        {
          System.out.printf("ant_it : [%d] is taking action HEALSELF\n", ant.id);
        }
        else
        {
          switch (ant.antType.ordinal())
          {
            case 0:
            {
              if (takingPathToFood(ant, action))
              {
                System.out.println("Antid: [" + ant.id + "] is a worker ant taking action GOING TO FOOD");
              }
            }
            break;
            case 1:
              if (goToFood(ant, action, data))
              {
                foodSitePresent(data);
                System.out.println("Antid: [" + ant.id + "] is taking action GOTOFOOD");
              }
              else if (foodSitesFound(ant, action)); //return explorer ant home if food sites discovered
              else if (goExplore(ant, action))
              {
                System.out.println("Antid: [" + ant.id + "] is taking action GOEXPLORE");
              }
              break;
            case 2:
              break;
          }
          if (jitter(ant, action, data))
          {
            System.out.println("initiating jitterbug");
          }
        }
      }
      ant.action = action;
    }
  }

  private boolean takingPathToFood(AntData ant, AntAction action)
  {
    return takePathToFood(ant, action) && ant.carryUnits < ant.antType.getCarryCapacity()/2;
  }

  public boolean takePathToFood(AntData ant, AntAction action)
  {
    PathNode antSpot = new PathNode(ant.gridX, ant.gridY, 0);
    Map<PathNode,PathNode> path;
    if(ant.gridX != nest_x && ant.gridY != nest_y)
    {
      path = A_Star.getPath(new PathNode(nest_x, nest_y, 0), antSpot);
      path_to_target.put(ant.id, path);
      return moveAlongPath(ant, action, path_to_target.get(ant.id));
    }
    return moveAlongPath(ant,action, nest_to_food.get(ant.id));
  }

  private void foodSitePresent(PacketToClient ptc)
  {
    if(food_list.size() >= food_sites_to_broadcast) return;
    ArrayList<FoodData> foodData = ptc.foodList;
    Random rand = new Random();
    for (FoodData food : foodData)
    {
      if (food.objType == GameObject.GameObjectType.FOOD && food_list.size() == 0)
      {
        food_list.add(new FoodData(
                GameObject.GameObjectType.FOOD,
                food.gridX - rand.nextInt(30),
                food.gridY - rand.nextInt(30),
                0
        ));
      }
      else if (food_list.size() > 0)
      {
        for (int k = 0; k < food_list.size(); k++)
        {
          if (Math.abs(food.gridX - food_list.get(k).gridX) > 60 &&
                  Math.abs(food.gridY - food_list.get(k).gridY) > 60)
          {
            food_list.add(new FoodData(
                    GameObject.GameObjectType.FOOD,
                    food.gridX - rand.nextInt(30),
                    food.gridY - rand.nextInt(30),
                    0
            ));
          }
        }
      }
    }
  }

  private boolean healSelf( AntData ant, AntAction action)
  {
    // Don't heal if we don't need to. (High watermark)
    if( ant.health >= ant.antType.getMaxHealth() && healingToFull.get(ant.id) )
    {
      healingToFull.put(ant.id, false);
      return false;
    }

    // Heal if we have enough units and we're at sufficiently low health.
    if(ant.carryUnits > 0 && ant.carryType == GameObject.GameObjectType.WATER)
    {
      if(!healingToFull.get(ant.id)) healingToFull.put(ant.id, true);
      action.type = AntAction.AntActionType.HEAL;
      action.direction = null;
      action.quantity = ant.carryUnits;
      return true;
    }

    // Don't go off path to heal unless we need to. (Low watermark)
    if( ant.health > ant.antType.getMaxHealth()/3 && !healingToFull.get(ant.id) ) return false;

    // Point ourselves towards water.
    action.direction = Direction.values()[ (int) (Math.floor( Raycasting.getBearingToWater(ant.gridX, ant.gridY) )/45.0) ];

    System.out.println("Distance I see: " + Raycasting.getDistanceToWaterUsingVector(ant.gridX, ant.gridY, ant.gridX+action.direction.deltaX(), ant.gridY+action.direction.deltaY()));

    // If we're next to water, just pick it up.
    if( Raycasting.getDistanceToWaterUsingVector(ant.gridX, ant.gridY, ant.gridX+action.direction.deltaX(), ant.gridY+action.direction.deltaY()) <= 1
            && Constants.random.nextBoolean() ) // So maybe move closer.
    {
      //action.direction = //Direction.getRandomDir(); // For now... I mean, it should work, but whatever.
      int pickupAmt = ant.antType.getMaxHealth() - ant.health;
      if(ant.carryType == GameObject.GameObjectType.FOOD)
      {
        for (Direction dir : Direction.values())
        {
          if (A_Star.board[ant.gridX + dir.deltaX()][ant.gridY + dir.deltaY()] != null)
          {
            action.direction = dir;
            action.type = AntAction.AntActionType.DROP;
            action.quantity = ant.antType.getCarryCapacity();
            return true;
          }
        }
      }
      action.type = AntAction.AntActionType.PICKUP;
      action.quantity = pickupAmt;
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
      action.x = nest_x;
      action.y = nest_y;

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
