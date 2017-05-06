package antworld.client;

import antworld.common.PacketToClient;
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
  public static PathNode[][] board = new PathNode[2500][1500];
  public static PathNode[][] nonTouchedBoard = new PathNode[2500][1500];

  private static BufferedImage loadedImage;
  static
  {
    try
    {
      URL fileURL = Util.class.getClassLoader().getResource("resources/AntWorld.png");
      loadedImage = ImageIO.read(fileURL);
    }
    catch( Exception e )
    {
      System.out.println(e);
    }
  }


  // AWG: I needed a simple getPath without the ant stuff still. Same idea as the other one but with fewer args, overload
  public static Map<PathNode,PathNode> getPath(PathNode start_position, PathNode end_position)
  {
    System.out.printf("Called A*: (%d,%d) : (%d,%d)\n", start_position.x, start_position.y, end_position.x, end_position.y);
    if( start_position == null)
    {
      System.out.println("A* : Invalid starting position");
      return null;
    }
    if( end_position == null)
    {
      System.out.println("A*: Invalid ending position");
      return null;
    }

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
      if( current == end_position )
      {
        break;
      }

      neighbors.removeAll(neighbors);

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
          if( Math.abs(neighbor.x-current.x) > 1 || Math.abs(neighbor.y-current.y) > 1 )
            System.out.println("Severely messed up A*");
        }
      }
    }

    return came_from;
  }


  /**
   * To make things simpler I say that the start postion is the nest and the end position is the ants location
   * @param start_position - the nests position!
   * @param end_position-  the ants postion!
   * @param ptc the information with regard to the ants.
   * @param antID the id of the ant to distinguish it from the other ants.
   * @return - the shortest path from the nest to the ant.
   */
  public static Map<PathNode,PathNode> getPath(PathNode start_position, PathNode end_position,PacketToClient ptc, int antID)
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

/**
 * Set all the locations of other ants to null so this ant cant go there.
 */
    for(int i =0; i< ptc.myAntList.size(); i++)
    {
      if(ptc.myAntList.size() == 1)
      {
        break;
      }
      if(ptc.myAntList.get(i).id != antID )
      {
        board[ptc.myAntList.get(i).gridX][ptc.myAntList.get(i).gridY] = null;
      }
    }

    while( !frontier.isEmpty() )
    {
      current = frontier.poll();
      if( current == end_position )
      {
        System.out.println("A* found a path.");
        break;
      }

      neighbors.removeAll(neighbors);
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
          came_from.put(neighbor, current); // Check if this is giving a bad node
          if( Math.abs( neighbor.x - current.x) > 1 || Math.abs(neighbor.y - current.y) > 1 ) System.out.println("AAUAHUhughg A* screwed up");
        }
      }
    }

    /**
     * Once this ants path has been created, set all the places that ants are on as available positions.
     */
    for(int i =0; i< ptc.myAntList.size(); i++)
    {
      if(ptc.myAntList.size() == 1)
      {
        break;
      }
      if(ptc.myAntList.get(i).id != antID)
      {
        board[ptc.myAntList.get(i).gridX][ptc.myAntList.get(i).gridY] =
                new PathNode(ptc.myAntList.get(i).gridX,ptc.myAntList.get(i).gridY,0);
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

  /**
   * This method builds 2 boards for us to use A_Star on. We will exclude water on the map because it is not a valid space.
   */
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
          nonTouchedBoard[x][y] = new PathNode(x,y,0); // AWG: what's the point of this? It seems to just get used once - here.
        }
      }
    }
  }

}
