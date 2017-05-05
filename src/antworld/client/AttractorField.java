package antworld.client;

import antworld.common.AntData;
import antworld.common.PacketToClient;

public class AttractorField
{
  // High value means it should be very attractive, low value is less attractive. Never repulsive, because this
  // could end up aliasing to strange effect.
  public static int[][] pfield;

  public static int resolution = 5;

  static
  {
    pfield = new int[2500/resolution][1500/resolution];
    for(int y = 0; y < 1500/resolution; y++ )
      for(int x = 0; x < 2500/resolution; x++)
        pfield[x][y] = 1;
  }

  // If any of our ants are sufficiently close, make this position less attractive as a destination.
  public static void updateField(PacketToClient ptc )
  {
    int distance;
    for( int y = 0; y < 1500/resolution; y++)
    {
      for (int x = 0; x < 2500/resolution; x++)
      {
        for (AntData ant : ptc.myAntList)
        {
          distance = Math.abs(ant.gridX-x*resolution) + Math.abs(ant.gridY-y*resolution);
          if( distance <= resolution )
          {
            pfield[x][y] = 0;
          }
        }
      }
    }
  }



}
