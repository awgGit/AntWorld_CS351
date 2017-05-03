/* Experimental, loosely defined 'group'.
 * As it stands I've commented out repulsion for the moment so it's more of a snake than a blob.
 * Will add repulsion back in when I get a chance to think about/tinker with the values.
 * Will also fix running into food (if attrition is on and we hit a dead ant, the whole thing fails)
 * */

package antworld.client;

import antworld.common.*;
import java.util.ArrayList;
import java.util.Map;

class BlobPatrol
{
  private Map<PathNode,PathNode> the_path;
  private PathNode current_target;

  private ArrayList<Integer> ant_ids;
  private int origin_x, origin_y;

  /* goExplore
    Just determines the direction for each ant in the blob/patrol based on the path of the patrol, the path to the lead
    node in the patrol's path, and the position of one's self relative to others within the blob/patrol.
    At the moment though, the position of other ants is not a part of the equation. When I figure out which values
    should/do work well, then I'll uncomment that portion of the code.
   */
  private boolean goExplore( AntData ant, AntAction action, PacketToClient ptc )
  {
    double sx, sy, mag, angle;

    // Get the first step in the local-to-static-A* path.
    PathNode n = A_Star.getPath(current_target,A_Star.board[ant.gridX][ant.gridY]).get(A_Star.board[ant.gridX][ant.gridY]);
    if(n == null) return false; // If we're already at the leading static-path node, then don't try moving anywhere.

    // Attract towards the first step in the local-to-static-A* path.
    sx = ant.gridX-n.x;
    sy = ant.gridY-n.y;

    // Mitigate this attraction by decaying over r.
    mag = Math.pow(Math.abs(sx) + Math.abs(sy), 1);
    sx /= mag;
    sy /= mag;

    // Experimental : Inactive
    //<editor-fold desc="Repel from Others">
    //    double density = BuildGraph.densityMap[ant.gridX/BuildGraph.resolution][ant.gridY/BuildGraph.resolution];
//    double power;
//    if( Math.abs(ant.gridX-current_target.x) + Math.abs(ant.gridY-current_target.y) < 50 )
//    {
//      for (AntData a : ptc.myAntList)
//      {
//        if (a == ant) continue;
//        if (!ant_ids.contains(a.id)) continue;
//
//        mag = Math.abs(ant.gridX - a.gridX) + Math.abs(ant.gridY - a.gridY);
//        // if density is *high*, we want them close together, so high power.
//                        // If density if *low*, we want them far apart, so low power.
//                        // Lowest power should be 2, highest should be 10, and if we're super high density, just don't
//                        // modify sx and sy at all.
//        power = 2+density*8;
//        if( density < 0.8 )
//        {
//          sx -= (ant.gridX - a.gridX) / Math.pow(mag, power);
//          sy -= (ant.gridY - a.gridY) / Math.pow(mag, power);
//        }
//      }
//    }
    //</editor-fold>

    // Experimental : Inactive
    //<editor-fold desc="Attract to Others">
    //    double density = BuildGraph.densityMap[ant.gridX/BuildGraph.resolution][ant.gridY/BuildGraph.resolution];
//    double power;
//    if( Math.abs(ant.gridX-current_target.x) + Math.abs(ant.gridY-current_target.y) < 50 )
//    {
//      for (AntData a : ptc.myAntList)
//      {
//        if (a == ant) continue;
//        if (!ant_ids.contains(a.id)) continue;
//
//        mag = Math.abs(ant.gridX - a.gridX) + Math.abs(ant.gridY - a.gridY);
//        // if density is *high*, we want them close together, so high power.
//                        // If density if *low*, we want them far apart, so low power.
//                        // Lowest power should be 2, highest should be 10, and if we're super high density, just don't
//                        // modify sx and sy at all.
//        power = 2+density*8;
//        if( density < 0.8 )
//        {
//          sx += (ant.gridX - a.gridX) / Math.pow(mag, power);
//          sy += (ant.gridY - a.gridY) / Math.pow(mag, power);
//        }
//      }
//    }
    //</editor-fold>

    // Normalize using manhattan distance.
    mag = Math.abs(sx) + Math.abs(sy);
    sx /= mag;
    sy /= mag;

    // Transform the vector into direction. Inefficient, optimize as necessary.
    angle = Math.toDegrees(Math.atan2( sy, sx ));
    angle = (angle+270)%360;
    Direction dir = Direction.values()[ (int) Math.floor(angle/45.0)];
    action.type = AntAction.AntActionType.MOVE;
    action.direction = dir;
    return true;
  }

  public BlobPatrol(int nest_x, int nest_y )
  {
    ant_ids = new ArrayList<>();
    the_path = A_Star.getPath(A_Star.board[200][300],A_Star.board[nest_x][nest_y]);
    current_target = A_Star.board[nest_x][nest_y];
    origin_x = nest_x;
    origin_y = nest_y;
  }

  /* updateCurrentNode
    Updates which node the group should builds its local paths to (where the node is always along the group's path.
    (that is, the variable called 'the_path', which for the moment doesn't change. It'll have to change eventually
    so that we can explore, but I haven't yet written that code yet.
   */
  public void updateCurrentNode( PacketToClient ptc )
  {

    int distance_threshold = 5; // Somewhat arbitrary. Tinker with it to find the best value.
    int ant_threshold = 0; // Somewhat arbitrary. Tinker with it to find the best value.
    int ants_present = 0; // Must be zero.

    System.out.println( current_target );
    for( AntData ant : ptc.myAntList )
    {
      if( !ant_ids.contains( ant.id ) ) continue;
      if( Math.abs(ant.gridX-current_target.x) + Math.abs(ant.gridY-current_target.y) < distance_threshold )
      {
        ants_present++;
      }
    }
    if( ants_present > ant_threshold )
    {
      current_target = the_path.get(current_target);
    }
  }

  public void addAnt( AntData ant )
  {
    if( ant_ids.contains( ant.id ) ) return;
    ant_ids.add( ant.id );
  }

  public void setAntActions ( PacketToClient ptc )
  {
    updateCurrentNode( ptc );
    AntAction action;
    for( AntData ant : ptc.myAntList )
    {
      action = new AntAction(AntAction.AntActionType.NOOP);
      if( !ant_ids.contains( ant.id )) continue;
      if(exitNest( ant, action, ptc )) ant.action = action;
      // The coordinates we send to goExplore should be those of the path.
      else if (goExplore(ant,action,ptc )) ant.action = action;
      ant.action = action;
    }
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
      action.x = origin_x + Constants.random.nextInt(15) - Constants.random.nextInt(15);
      action.y = origin_y + Constants.random.nextInt(15) - Constants.random.nextInt(15);

      action.type = AntAction.AntActionType.EXIT_NEST;
      return true;
    }
    return false;
  }

}
