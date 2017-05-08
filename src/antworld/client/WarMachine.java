/*
  Hunts down enemy nests & surrounds them.
 */
package antworld.client;

import antworld.common.*;

import java.util.ArrayList;
import java.util.Map;

public class WarMachine
{
  private Map<PathNode,PathNode> the_path;
  private PathNode current_target;
  public ArrayList<Integer> ant_ids;
  private int origin_x, origin_y;
  private boolean spread_on_target = false;
  private boolean lockdown = false;

  public WarMachine(int nest_x, int nest_y, Map<PathNode,PathNode> path )
  {
    ant_ids = new ArrayList<>();
    the_path = path;
    current_target = A_Star.board[nest_x][nest_y];
    origin_x = nest_x;
    origin_y = nest_y;
  }
  public void setAntActions ( PacketToClient ptc )
  {
    updateCurrentNode( ptc );
    shouldSearch( ptc );
    AntAction action;
    for( AntData ant : ptc.myAntList )
    {
      action = new AntAction(AntAction.AntActionType.NOOP);
      if( !ant_ids.contains( ant.id )) continue;
      if(exitNest( ant, action, ptc )) ant.action = action;
      else
      {
        if (lockdown && MiscFunctions.attack(ant, action, ptc)) ant.action = action;
        else if (lockdown && MiscFunctions.healSelf(ant, action))
        {
          ant.action = action;
          if(MiscFunctions.jitter(ant,action,ptc)) ant.action = action;
        }
        else if (goExplore(ant, action, ptc)) ant.action = action; // Don't jitter on the explore for these guys.
      }
      ant.action = action;
    }
  }

  // Really we just need the ant's id, not an actual ant. So you can pre-assign groups, essentially.
  public void addAnt( AntData ant )
  {
    if( ant_ids.contains( ant.id ) ) return;
    ant_ids.add( ant.id );
  }

  //<editor-fold desc="Per group states / state transitions">
  public void shouldSearch( PacketToClient ptc )
  {
    int c = 0;
    if( !spread_on_target ) return;
    for( AntData ant : ptc.myAntList )
    {
      if( !ant_ids.contains( ant.id ) ) continue;
      if( Math.abs(ant.gridX-current_target.x) + Math.abs(ant.gridY-current_target.y) < ant_ids.size()*2 )
        c++;
      if( c >= ant_ids.size()/2 ) //ant_ids.size() )
      {
        lockdown = true;
        return;
      }
    }
  }
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
      if( the_path.get(current_target) == null )
      {
        System.out.println("Path is completed, but maybe not everyone's here yet.");
        spread_on_target = true;
        // We don't yet update the target.
        return;
      }
      current_target = the_path.get(current_target);
    }
  }
  //</editor-fold>
  //<editor-fold desc="Per-ant states / state transitions">
  public boolean goExplore(AntData ant, AntAction action, PacketToClient ptc )
  {
    // If we're still travelling to the location, move along path.
    if( !lockdown)
    {
      PathNode n = A_Star.getPath(current_target, A_Star.board[ant.gridX][ant.gridY]).get(A_Star.board[ant.gridX][ant.gridY]);
      if( n == null ) return false;
      double angle = Math.toDegrees(Math.atan2( ant.gridY-n.y, ant.gridX-n.x));
      angle = (angle + 270) % 360;
      action.direction = Direction.values()[(int) Math.floor(angle / 45.0)];
    }
    else // Lockdown: Surround their nest, killing anything going in or out.
    // This is particularly effective for enemy ants which try to run from us -
    // Presumably they'll just run away from their nest. Attrition will kill them.
    {
      action.direction = Direction.getRandomDir();

      // Try to maintain a distance from the nest and also from one's peers.
      double sx = 0;
      double sy = 0;
      double distance_to_nest = Math.abs( current_target.x - ant.gridX ) + Math.abs( current_target.y - ant.gridY );
      if( distance_to_nest > 30 ) // Move straight towards the nest.
      {
        sx += (ant.gridX - current_target.x) / Math.pow(distance_to_nest, 2);
        sy += (ant.gridY - current_target.y) / Math.pow(distance_to_nest, 2);
      }
      else if (distance_to_nest < 28 ) // Spiral away from the nest.
      {
        sx += (ant.gridY - current_target.y) / Math.pow(distance_to_nest, 2);
        sy -= (ant.gridX - current_target.x) / Math.pow(distance_to_nest, 2);
      }

      // Keep distance from one's peers.
      double distance_to_peer;
      for( AntData a : ptc.myAntList )
      {
        if(a == ant) continue;
        if( !ant_ids.contains(a.id)) continue;

        distance_to_peer = Math.abs(ant.gridX-a.gridX)+Math.abs(ant.gridY-a.gridY);
        sx -= (ant.gridX-a.gridX)/ Math.pow(distance_to_peer,4);
        sy -= (ant.gridY-a.gridY)/ Math.pow(distance_to_peer,4);
      }

      // Normalize the vector. Not sure if we even need to do this.
//      double mag = Math.abs(sx) + Math.abs(sy);
//      sx /= mag;
//      sy /= mag;

      // Transform the vector into direction. Inefficient, optimize as necessary.
      double angle = Math.toDegrees(Math.atan2( sy, sx ));
      angle = (angle+270)%360;
      action.direction = Direction.values()[ (int) Math.floor(angle/45.0)];
    }
    action.type = AntAction.AntActionType.MOVE;
    return true;
  }

  public boolean exitNest(AntData ant, AntAction action, PacketToClient ptc)
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
  //</editor-fold>

}
