package antworld.client;


import antworld.common.*;

import java.util.ArrayList;

public class HiveMind
{
  ArrayList<Integer> ant_ids = new ArrayList<>();
  public void addAnt( AntData ant )
  {
    if(!ant_ids.contains(ant.id)) ant_ids.add(ant.id);
  }
  private int origin_x;
  private int origin_y;

  public HiveMind ( int nest_x, int nest_y )
  {
    origin_x = nest_x;
    origin_y = nest_y;
  }

  public void setAntActions( PacketToClient data )
  {
    AntAction action;
    for (AntData ant : data.myAntList)
    {
      action = new AntAction(AntAction.AntActionType.NOOP);
      if (ant_ids.contains(ant.id))
      {
        if (exitNest(ant, action, data)) {}
        //else if (goToWater(ant,action,data)){}
        else if (goExplore(ant, action, data)) {}
        ant.action = action;
        //System.out.println(ant.id + " (hivemind) :" + ant.action);
      }
    }
  }
  private boolean goToWater(AntData ant, AntAction action, PacketToClient ptc)
  {
    if( ant.health > 20 || ant.health < 15) return false;
    double angle_to_sea = Raycasting.getBearingToLand(ant.gridX,ant.gridY);
    Direction direction_to_sea = Direction.values()[ (int) Math.floor( angle_to_sea/45.0)];
    action.type = AntAction.AntActionType.MOVE;
    action.direction = direction_to_sea;
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
      action.x = origin_x + Constants.random.nextInt( 15 ) - Constants.random.nextInt(15);
      action.y = origin_y + Constants.random.nextInt( 15 ) - Constants.random.nextInt(15);

      action.type = AntAction.AntActionType.EXIT_NEST;
      return true;
    }
    return false;
  }
  private boolean goExplore(AntData ant, AntAction action, PacketToClient ptc)
  {
    double sx, sy, mag, angle, distance;
    sx = 0;
    sy = 0;

    // Try to attract the player towards the nest a bit.
    /*  sx = ant.gridX - origin_x + 100;
      sy = ant.gridY - origin_y + 10;

      mag = 1; //Math.pow(Math.abs(sx) + Math.abs(sy), 1);
      sx /= mag;
      sy /= mag;
      */

//    for( AntData a : ptc.myAntList )
//    {
//      if(a == ant) continue;
//      if( !ant_ids.contains(a.id)) continue;
//
//      mag = Math.abs(ant.gridX-a.gridX)+Math.abs(ant.gridY-a.gridY);
//      sx -= (ant.gridX-a.gridX)/ Math.pow(mag,4);
//      sy -= (ant.gridY-a.gridY)/ Math.pow(mag,4);
//    }

    // Experimentally, repel from the coastline.
    ArrayList<Integer> rays = Raycasting.castRays( ant.gridX, ant.gridY );
    for( int i = 0; i < rays.size(); i++)
    {
      angle = (360.0/Raycasting.num_rays)*i;
      distance = rays.get(i);

      mag = Math.abs(Math.cos( Math.toRadians(angle) ) * distance) +
              Math.abs(Math.sin( Math.toRadians(angle) ) * distance);
      sx += Math.sin( Math.toRadians(angle) ) * distance / Math.pow(mag,4);
      sy -= Math.cos( Math.toRadians(angle) ) * distance / Math.pow(mag,4);
    }

    for( int y = 0; y < 150; y++)
    {
      for(int x = 0; x < 250; x++)
      {
        mag = Math.abs(x*10 - ant.gridX) + Math.abs(y*10 - ant.gridY);
        if( AttractorField.pfield[x][y] != 0 && mag < 50)
        {
          sx += (ant.gridX-x*10) / Math.pow(mag,4);
          sy += (ant.gridY-y*10) / Math.pow(mag,4);
        }
      }
    }

    mag = Math.abs(sx) + Math.abs(sy);
    sx /= mag;
    sy /= mag;

    angle = Math.toDegrees(Math.atan2( sy, sx ));
    angle = (angle+270)%360;
    Direction dir = Direction.values()[ (int) Math.floor(angle/45.0)];
    action.type = AntAction.AntActionType.MOVE;
    action.direction = dir;
    return true;
  }


}
