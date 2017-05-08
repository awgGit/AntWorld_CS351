/*
PathCalculator:
  Once the explorers have found a food site and retracted into the nest,
  this class is called upon to calculate the A* paths from food to nest and
  vice-versa (nest to food).
 */

package antworld.client;
import antworld.common.Constants;

import java.util.Map;

class PathCalculator
{

  // All should be broadly accessible (hence static)
  static Map<PathNode,PathNode> food_to_nest = null;
  static Map<PathNode,PathNode> nest_to_food = null;
  static boolean paths_ready = false;
  static PathNode food_site_location;
  static PathNode nest_location;

  private int nest_x;
  private int nest_y;

  // Initializes all the nest & state variables for PathCalculator.
  PathCalculator ( int nx, int ny )
  {
    nest_x = nx;
    nest_y = ny;
  }

  // Calculates the path to the food source precisely once, and only if ExplorerMachine has indicated that it has
  // discovered food, retracted its ants, and given us a valid (non null) position for food.
  void calculatePathsWhenReady()
  {
    if( food_to_nest != null || nest_to_food != null ) return;
    if(     ExplorerMachine.all_underground &&
            ExplorerMachine.food_was_found &&
            ExplorerMachine.food_location != null )
    {
      // Calculate the location for the ants to travel to repeatedly.
      // It can't be on the food (or they'll never stop trying to reach the food)
      // And it can't be on a null location (or they'll never be able to reach that location)
      int fx = ExplorerMachine.food_location.x;
      int fy = ExplorerMachine.food_location.y;

      while( A_Star.board[ fx ][ fy ] == null || (fx == ExplorerMachine.food_location.x && fy == ExplorerMachine.food_location.y))
      {
        fx = ExplorerMachine.food_location.x + Constants.random.nextInt(2) - 1;
        fy = ExplorerMachine.food_location.y + Constants.random.nextInt(2) - 1;
      }

      food_site_location = A_Star.board[ fx ][ fy ];
      nest_location =  A_Star.board[ nest_x ][ nest_y ];

      System.out.println("Food site location: " + food_site_location);

      System.out.println(" Hi! I'm going to try to print the paths... ");
      food_to_nest = A_Star.getPath( nest_location, food_site_location );
      nest_to_food = A_Star.getPath( food_site_location, nest_location );
      System.out.println(" I've completed calculating the paths. ");
      paths_ready = true;
    }
  }
}
