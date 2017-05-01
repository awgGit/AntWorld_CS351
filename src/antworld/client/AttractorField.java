package antworld.client;

import antworld.common.AntData;
import antworld.common.PacketToClient;

public class AttractorField
{
  // High value means it should be very attractive, low value is less attractive. Never repulsive, because this
  // could end up aliasing to strange effect.
  public static int[][] pfield;

  static
  {
    pfield = new int[250][150];
    for(int y = 0; y < 150; y++ )
      for(int x = 0; x < 250; x++)
        pfield[x][y] = 1;
  }

  // If any of our ants are sufficiently close, make this position less attractive as a destination.
  public static void updateField(PacketToClient ptc )
  {
    int distance;
    for( int y = 0; y < 150; y++)
    {
      for (int x = 0; x < 250; x++)
      {
        for (AntData ant : ptc.myAntList)
        {
          distance = Math.abs(ant.gridX-x*10) + Math.abs(ant.gridY-y*10);
          if( distance <= 10 )
          {
            pfield[x][y] = 0;
          }
        }
      }
    }
  }



}
