package antworld.client;
import antworld.common.Util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;

public class Raycasting
{
  private static int num_rays = 20;          // How many rays are cast (how dense, angularly?)
  private static int max_distance = 1000;   // How many steps will rays travel (when is the cutoff?)
  private static double stride_length = 1;  // How far should each step go?
  private static BufferedImage loadedImage;

  static
  {
    try
    {
      URL fileURL = Util.class.getClassLoader().getResource("resources/AntWorld.png");
      loadedImage = ImageIO.read(fileURL);
    }
    catch( Exception e ) { System.out.println(e); }
  }

  /* getBearingToWater
      Returns the angle of the shortest vector to water.
      Returns -1 if there are no rays being cast.
   */
  public static double getBearingToWater( double x, double y )
  {
    ArrayList<Integer> ray_distances = castRays(x,y);
    double best_angle = -1;
    int shortest_dist = Integer.MAX_VALUE;

    for( int i = 0; i < ray_distances.size(); i++)
    {
      if( ray_distances.get(i) < shortest_dist )
      {
        shortest_dist = ray_distances.get(i);
        best_angle = (360.0/ray_distances.size()) * i;
      }
    }
    return best_angle;
  }

  /* getBearingToLand
      Returns the angle of the longest vector to water.
      Returns -1 if there are no rays being cast.
   */
  public static double getBearingToLand( double x, double y )
  {
    ArrayList<Integer> ray_distances = castRays(x,y);
    double best_angle = -1;
    int longest_dist = Integer.MIN_VALUE;

    for( int i = 0; i < ray_distances.size(); i++)
    {
      if( ray_distances.get(i) > longest_dist )
      {
        longest_dist = ray_distances.get(i);
        best_angle = (360.0/ray_distances.size()) * i;
      }
    }
    return best_angle;
  }

  /* castRays
      Returns an arraylist with the integer distance to water from a particular position.
   */
  public static ArrayList<Integer> castRays( double x, double y )
  {
    ArrayList<Integer> ray_lengths = new ArrayList<>(); // optimize to use constant size array

    double angle;
    double dx, dy;
    double sx = x;
    double sy = y;

    for( int i = 0; i < num_rays; i++ )
    {
      x = sx;
      y = sy;

      angle = (360.0/num_rays) * i;
      dx = Math.sin( Math.toRadians(angle));
      dy = -Math.cos( Math.toRadians(angle));

      int distance;
      for(distance = 0; distance < max_distance; distance++ )
      {
        x += dx * stride_length; // Adjust the x position.
        y += dy * stride_length; // Adjust the y position.

        // Find the closest source of water, or crash on going out of bounds.
        // We know there's water at the edge so returning that distance is accurate.
        try
        {
          if ((loadedImage.getRGB((int) x, (int) y) & 0xff) == 255)
          {
            ray_lengths.add(distance);
            break;
          }
        }
        catch( Exception e )
        {
          System.out.println(e);
          ray_lengths.add( distance );
          break;
        }
      }
      if( distance == max_distance) ray_lengths.add(distance);
    }
    return ray_lengths;
  }

}
