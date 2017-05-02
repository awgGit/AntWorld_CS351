package antworld.client;
import java.util.ArrayList;

public class GraphNode
{
  public int x;
  public int y;
  public boolean explored = false;
  ArrayList<GraphNode> neighbors;

  public GraphNode(int x, int y)
  {
    neighbors = new ArrayList<GraphNode>();
    this.x = x;
    this.y = y;
  }
  public ArrayList<GraphNode> getUnexploredNeighbors()
  {
    ArrayList<GraphNode> unexploredNeighbors = new ArrayList<GraphNode>();
    for( GraphNode r : neighbors )
    {
      if(!r.explored)
      {
        unexploredNeighbors.add(r);
      }
    }
    return unexploredNeighbors;
  }

}
