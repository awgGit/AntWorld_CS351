package antworld.common;

import java.io.Serializable;
import java.util.ArrayList;




public class PacketToServer implements Serializable
{
  private static final long serialVersionUID = Constants.VERSION;

  public final TeamNameEnum myTeam;

  /** This list must contain every ant belonging to client that is going to attempt some action
   * in the current timestep.<br>
   * The client uses this list to tell the server which of its ants do what.<br>
   * There is no need to include ants with the NOOP or BUSY action.<br>  <br>
   *
   * The server will execute ant actions in the following order:<br>
   * 1) All actions of all ants from a team are executed before all actions of any team that
   * reported its moves after the this team reported.<br>
   * 2) Ant actions within a nest are executed in the list order. This is important when
   * one ant needs to move into the space another ant is moving out of or when one ant wants to
   * pick up an item dropped in the same tick by another ant.<br><br>
   *
   * If the client sends a message with myAntList = null, then the server's next
   * reply will include a full list of all ants belonging to the nest. This is useful
   * when the client reconnects after crashing.
   */
  public volatile ArrayList<AntData> myAntList = new ArrayList<>();


  /**
   * Time in seconds from the start of the game.
   */
  public double timeReceived;


  public PacketToServer(TeamNameEnum myTeam)
  {
    this.myTeam = myTeam;
  }

  public PacketToServer(PacketToServer source)
  {
    myTeam = source.myTeam;
    timeReceived = source.timeReceived;
    myAntList = new ArrayList<>();
    for (AntData ant : source.myAntList)
    {
      myAntList.add(new AntData(ant));
    }
  }



  public String toString()
  {
    String out = "PacketToServer["+timeReceived+"]: myTeam=" + myTeam +"\n     ";
    out = out+ "myAntList:";
    for (AntData ant : myAntList)
    { out = out + "\n     " + ant;
    }
    return out;
  }
}
