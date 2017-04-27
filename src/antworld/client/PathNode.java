package antworld.client;

  /**
   * @author Andre' Green
   * This class is used so we can mess with priority queues in the pathfinding class.
   */
public class PathNode implements Comparable<PathNode>
{
  public int x;
  public int y;
  double priority;

  @Override
  public String toString()
    {
      return this.x + " " + this.y;
    }

  public PathNode( int x, int y, double priority )
  {
    this.x = x;
    this.y = y;
    this.priority = priority;
  }

  @Override // Just to be sure we're overriding the right method.
  public int compareTo( PathNode pn )
    {
      return (int)(this.priority - pn.priority);
    }
}