/*
  Reads in precalculated paths from within resources to allow WarriorMachine to travel between nests without
  having to calculate the A* path.
 */
package antworld.client;

import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class PrecalculatedPaths
{

  // Given the pathnodes at two nests, find the between the two.
  static HashMap<String,HashMap<PathNode,PathNode>> nest_paths = new HashMap<>();

  // The function takes a path and turns it into a file.
  public static void writePath( Map<PathNode,PathNode> write_path, String filename, PathNode start_node )
  {
    PathNode nextNode = start_node;
    try
    {
      //PrintWriter writer = new PrintWriter( String.format("resources/paths/%s.txt", filename), "UTF-8");
      PrintWriter writer = new PrintWriter( String.format("resources/resources/paths/%s.txt",filename), "UTF-8");
      while( write_path.get(nextNode) != null)
      {
        writer.printf("%d,%d<-", nextNode.x, nextNode.y);
        nextNode = write_path.get(nextNode);
        writer.printf("%d,%d\n", nextNode.x, nextNode.y);
      }
      writer.close();
      System.out.println("SUCCESSFULLY WROTE FILE");
    }
    catch( IOException e)
    {
      e.printStackTrace();
    }
  }
  public static HashMap<PathNode,PathNode> getPath( PathNode end_node, PathNode start_node)
  {
    // Instead of running A*, pull from our calculated paths.
    if( nest_paths.containsKey(String.format("%s->%s",start_node,end_node)))
    {
      System.out.println("Found precalculated path.");
      return nest_paths.get(String.format("%s->%s",start_node,end_node));
    }
    System.out.println("Failed to find precalculated path.");
    return null; // Otherwise, return null if we don't have the path precalculated.

  }
  public static void getPath(String filename)
  {
    // DEBUG make some path for us to parse
    // Make up a path.
//    Map<PathNode,PathNode> write_path = A_Star.getPath(A_Star.board[200][300],A_Star.board[210][310]);
//    PathNode nextNode = A_Star.board[210][310];
//    // Then write that path to a file.
//    try
//    {
//      PrintWriter writer = new PrintWriter("resources/test.txt", "UTF-8");
//
//      while( write_path.get(nextNode) != null)
//      {
//        writer.printf("%d,%d<-", nextNode.x, nextNode.y);
//        nextNode = write_path.get(nextNode);
//        writer.printf("%d,%d\n", nextNode.x, nextNode.y);
//      }
//      writer.close();
//    }
//    catch( IOException e)
//    {
//      e.printStackTrace();
//    }

    try
    {
      PathNode start_node = null;
      PathNode end_node = null;
      HashMap<PathNode,PathNode> path = new HashMap<>();

      Scanner in = new Scanner(new FileReader(String.format("%s%s.txt", "resources/resources/paths/", filename)));
      while (in.hasNext())
      {
        // Read in each line.
        String[] nodes = in.next().split("<-");
        String[] first_node_xy = nodes[0].split(",");
        String[] second_node_xy = nodes[1].split(",");

        // DEBUG printing to check we parsed it right.
        // Print out the connection to make sure it was done right.
//        System.out.printf("(%d,%d)<-(%d,%d)\n",
//                Integer.parseInt(first_node_xy[0]),
//                Integer.parseInt(first_node_xy[1]),
//                Integer.parseInt(second_node_xy[0]),
//                Integer.parseInt(second_node_xy[1]));

        if( start_node == null)
        {
          start_node = A_Star.board[Integer.parseInt(first_node_xy[0])][Integer.parseInt(first_node_xy[1])];
        }

        // Add the node connection to our hashmap.
        path.put( A_Star.board[Integer.parseInt(first_node_xy[0])]
                        [Integer.parseInt(first_node_xy[1])],
                A_Star.board[Integer.parseInt(second_node_xy[0])]
                        [Integer.parseInt(second_node_xy[1])] );

        if( !in.hasNext() )
        {
          end_node = A_Star.board[Integer.parseInt(second_node_xy[0])][Integer.parseInt(second_node_xy[1])];
        }
      }


      // DEBUG printing to check we parsed right.
//      nextNode = start_node;
//      while( path.get(nextNode) != null)
//      {
//        System.out.printf("%d,%d<-", nextNode.x, nextNode.y);
//        nextNode = path.get(nextNode);
//        System.out.printf("%d,%d\n", nextNode.x, nextNode.y);
//      }

      // this is sort of stupid way to index but w/e, strings are immutable
      nest_paths.put(String.format("%s->%s",start_node,end_node),path);
      System.out.println("SUCCESSFULLY READ FILE " + nest_paths.size() );
    }
    catch( IOException ioe )
    {
      System.out.println("Couldn't find file, probably.");
      ioe.printStackTrace();
    }
  }

}
