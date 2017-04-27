package antworld.client;

import antworld.common.Util;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.PriorityQueue;

public class A_Star
{
  public static PathNode[][] board = new PathNode[2500][1500]; // Big array.
  private static BufferedImage loadedImage;

  // Read in the map.
  static
  {
    try
    {
      URL fileURL = Util.class.getClassLoader().getResource("resources/AntWorld.png");
      loadedImage = ImageIO.read(fileURL);
    }
    catch( Exception e ) { System.out.println(e); }
  }

  // Actually get the path between these two nodes.
  public static Map<PathNode,PathNode> getPath(PathNode start_position, PathNode end_position)
  {
    if( start_position == null) return null;
    if( end_position == null) return null;

    PriorityQueue<PathNode> frontier = new PriorityQueue<>();
    frontier.add( start_position );

    Map<PathNode,PathNode> came_from = new HashMap<>();
    Map<PathNode,Double> cost_so_far = new HashMap<>();

    cost_so_far.put(start_position,0.0);
    came_from.put(start_position,null);
    double new_cost;

    PathNode current = null;
    LinkedList<PathNode> neighbors = new LinkedList<>();

    double dist;
    while( !frontier.isEmpty() )
    {
      current = frontier.poll();
      if( current == end_position ) break;
      if( board[current.x+1][current.y] != null ){ neighbors.add( board[current.x+1][current.y]); }
      if( board[current.x-1][current.y] != null ){ neighbors.add( board[current.x-1][current.y]); }
      if( board[current.x][current.y+1] != null ){ neighbors.add( board[current.x][current.y+1]); }
      if( board[current.x][current.y-1] != null ){ neighbors.add( board[current.x][current.y-1]); }
      if( board[current.x-1][current.y+1] != null ){ neighbors.add( board[current.x-1][current.y+1]); }
      if( board[current.x-1][current.y-1] != null ){ neighbors.add( board[current.x-1][current.y-1]); }
      if( board[current.x+1][current.y+1] != null ){ neighbors.add( board[current.x+1][current.y+1]); }
      if( board[current.x+1][current.y-1] != null ){ neighbors.add( board[current.x+1][current.y-1]); }

      for (PathNode neighbor : neighbors)
      {
        // Cost of moving is 1, no matter the direction, unless going uphill, in which case cost is 2.
        dist = (loadedImage.getRGB(neighbor.x, neighbor.y) > loadedImage.getRGB(current.x, current.y)) ? 2 : 1;

        new_cost = cost_so_far.get(current) + dist;
        if (!came_from.containsKey(neighbor) || new_cost < cost_so_far.get(neighbor))
        {
          cost_so_far.put(neighbor, new_cost);
          neighbor.priority = new_cost + heuristic(end_position, neighbor);
          frontier.add(neighbor);
          came_from.put(neighbor, current);
        }
      }
    }
    return came_from;
  }

  // In general, we can assume manhattan distance is ideal.
  private static double heuristic( PathNode a, PathNode b )
  {
    if( a == null || b == null) return 0;
    else return( Math.abs(a.x-b.x)+Math.abs(a.y-b.y));
  }

  // Build a pathnode representation of the map.
  public static void buildBoard()
  {
    // Read through map and build the board:
    for( int y = 0; y < 1500; y++ )
    {
      for( int x = 0; x < 2500; x++ )
      {
        if ((loadedImage.getRGB(x,y) & 0xff) != 255) // If it's ground, make it a valid pathnode.
        {
          board[x][y] = new PathNode(x,y,0);
        }
      }
    }
  }

}
