package antworld.client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.HashMap;
import java.awt.image.BufferedImage;
import java.net.URL;
import javax.imageio.ImageIO;

import antworld.common.*;
import antworld.common.AntAction.AntState;
import antworld.common.AntAction.AntActionType;


/**
 * This is a very simple example client that implements the following protocol:
 *   <ol>
 *     <li>The server must already be running (either on a networked or local machine) and
 *     listening on port 5555 for a client socket connection.
 *     The default host for the server is foodgame.cs.unm.edu on port 5555.</li>
 *     <li>The client opens a socket to the server.</li>
 *     <li>The client then sends a PacketToServer with PacketToServer.myTeam
 *     set to the client's team enum.<br>
 *
 *       <ul>
 *         <li>If this is the client's first connection this game: The client may spawn its
 *         initial ants in this first message or may choose to wait for a future turn to
 *         spawn the ants.</li>
 *         <li>If the client is reconnecting, then the client should set myAntList = null.
 *         This will cause the next message from the server to
 *         include a full list of the client's ants (including ants that are underground,
 *         busy, and noop).</li>
 *       </ul>
 *    </li>
 *
 *     <li>
 *       The server will then send a populated PacketToClient message to the client.
 *     </li>
 *     <li>
 *       Each tick of server, the server will send a PacketToClient message to each client.
 *       After receiving the server update, the client should choose an action for each of its
 *       ants and send a PacketToServer message back to the server.
 *     </li>
 *   </ol>
 */


public class ClientRandomWalk
{
  private static final boolean DEBUG = true;
  private final TeamNameEnum myTeam;
  private ObjectInputStream  inputStream = null;
  private ObjectOutputStream outputStream = null;
  private boolean isConnected = false;
  private NestNameEnum myNestName = null;
  private int centerX, centerY, numAnts;
  private Socket clientSocket;

  static int initialAntsInPatrols = 5;
  static int numPatrols = 4;
  private int gameTick = 0;
  private Patrols patrols;
  private HashMap<Integer, HashMap<Integer, AntData>> antGroups;
  private BufferedImage loadedImage;

  /**
  * A random number generator is created in Constants. Use it.
  * Do not create a new generator every time you want a random number nor
  * even in every class were you want a generator.
  */
  private static Random random = Constants.random;


  public ClientRandomWalk(String host, TeamNameEnum team, boolean reconnect)
  {
    myTeam = team;
    System.out.println("Starting " + team +" on " + host + " reconnect = " + reconnect);

    isConnected = openConnection(host, reconnect);
    if (!isConnected) System.exit(0);

    mainGameLoop();
    closeAll();
  }

  private boolean openConnection(String host, boolean reconnect)
  {
    try
    {
      URL fileURL = Util.class.getClassLoader().getResource("resources/AntWorld.png");
      loadedImage = ImageIO.read(fileURL);
    }
    catch(Exception e)
    {
      System.out.println(e.getMessage());
    };
    try
    {
      clientSocket = new Socket(host, Constants.PORT);
    }
    catch (UnknownHostException e)
    {
      System.err.println("ClientRandomWalk Error: Unknown Host " + host);
      e.printStackTrace();
      return false;
    }
    catch (IOException e)
    {
      System.err.println("ClientRandomWalk Error: Could not open connection to " + host
        + " on port " + Constants.PORT);
      e.printStackTrace();
      return false;
    }

    try
    {
      outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
      inputStream = new ObjectInputStream(clientSocket.getInputStream());

    }
    catch (IOException e)
    {
      System.err.println("ClientRandomWalk Error: Could not open i/o streams");
      e.printStackTrace();
      return false;
    }

    numAnts = initialAntsInPatrols * numPatrols;
    PacketToServer packetOut = new PacketToServer(myTeam);
    if (reconnect) packetOut.myAntList = null;
    else
    {
      //Spawn ants of whatever objType you want
      AntType type;
      AntData ant;
      for (int i=0; i<numAnts; i++)
      {
        type = AntType.EXPLORER;
        ant = new AntData(type,myTeam);
        packetOut.myAntList.add(ant); //default action is BIRTH.
      }
    }
    send(packetOut);
    return true;
  }

  public void closeAll()
  {
    System.out.println("ClientRandomWalk.closeAll()");
    {
      try
      {
        if (outputStream != null) outputStream.close();
        if (inputStream != null) inputStream.close();
        clientSocket.close();
      }
      catch (IOException e)
      {
        System.err.println("ClientRandomWalk Error: Could not close");
        e.printStackTrace();
      }
    }
  }

  /**
   * This method is called ONCE after the socket has been opened.
   * The server assigns a nest to this client with an initial ant population.
   */
  public void setupNest(PacketToClient packetIn)
  {

    myNestName = packetIn.myNest;
    centerX = packetIn.nestData[myNestName.ordinal()].centerX;
    centerY = packetIn.nestData[myNestName.ordinal()].centerY;
    System.out.println("ClientRandomWalk: ==== Nest Assigned ===>: " + myNestName);
  }

  /**
   * Called after socket has been created.<br>
   * This simple example client runs in a single thread. <br>
   * The mainGameLoop() has the following structure:<br>
   * <ol>
   *   <li>Start a blocking listen for message from server.</li>
   *   <li>When server message is received, if a nest has not yet been set up,
   *   then setup the nest.</li>
   *   <li> Assign actions to all ants</li>
   *   <li> Send ant actions to server.</li>
   *   <li> Loop back to step 1.</li>
   * </ol>
   * This NOT a "tight loop" because the blocking socket read
   * will not return until the server sends the next message. Thus, this loop
   * uses the server as a timer.
   */
  public void mainGameLoop()
  {
    while (true)
    {
      PacketToClient packetIn = null;
      try
      {
        if (DEBUG) System.out.println("ClientRandomWalk: listening to socket....");
        packetIn = (PacketToClient) inputStream.readObject();

        if (DEBUG) System.out.println("ClientRandomWalk: received <<<<<<<<<"+inputStream.available()+"<...\n" + packetIn);

        if (packetIn.myNest == null)
        {
          System.err.println("ClientRandomWalk***ERROR***: Server returned NULL nest");
          System.exit(0);
        }
      }
      catch (IOException e)
      {
        System.err.println("ClientRandomWalk***ERROR***: client read failed");
        e.printStackTrace();
        System.exit(0);

      }
      catch (ClassNotFoundException e)
      {
        System.err.println("ServerToClientConnection***ERROR***: client sent incorrect common format");
        e.printStackTrace();
        System.exit(0);
      }

      if (myNestName == null) setupNest(packetIn);
      if (myNestName != packetIn.myNest)
      {
        System.err.println("ClientRandomWalk: !!!!ERROR!!!! " + myNestName);
      }

      if (DEBUG) System.out.println("ClientRandomWalk: chooseActions: " + myNestName);

      PacketToServer packetOut = chooseActionsOfAllAnts(packetIn);
      send(packetOut);
    }
  }

  private void associateAntWithPatrol(PacketToClient packet)
  {
    antGroups = new HashMap<>(numPatrols);
    HashMap<Integer, AntData> ants;
    int index;
    for(int j = 0; j < patrols.patrols.size(); j++)
    {
      ants = new HashMap<>(patrols.patrols.size());
      antGroups.put(j, ants);
    }
    for(AntData ant : packet.myAntList)
    {
      for(Patrols.Patrol p : patrols.patrols)
      {
        if(p.antPatrol.contains(ant))
        {
          index = p.antPatrol.indexOf(ant);
          ant.action.direction = p.antPatrol.get(index).action.direction;
          antGroups.get(p.patrolNumber).put(index, ant);
          break;
        }
      }
    }
  }

  private void send(PacketToServer packetOut)
  {
    try
    {
      System.out.println("ClientRandomWalk: Sending>>>>>>>: " + packetOut);
      outputStream.writeObject(packetOut);
      outputStream.flush();
      outputStream.reset();
    }

    catch (IOException e)
    {
      System.err.println("ClientRandomWalk***ERROR***: client write failed");
      e.printStackTrace();
      System.exit(0);
    }
  }

  private PacketToServer chooseActionsOfAllAnts(PacketToClient packetIn)
  {
    PacketToServer packetOut = new PacketToServer(myTeam);
    if(patrols == null)
    {
      patrols  = new Patrols(numAnts, initialAntsInPatrols,
              packetIn.myAntList, packetIn.nestData[myNestName.ordinal()]);
    }
    associateAntWithPatrol(packetIn);
    for (int j = 0; j < antGroups.size(); j++)
    {
      HashMap<Integer, AntData> patrol = antGroups.get(j);
      for (int k = 0; k < patrol.size(); k++)
      {
        AntData ant = patrol.get(k);
        AntAction action = chooseAction(packetIn, ant);
        if (action.type != AntActionType.NOOP)
        {
          ant.action = action;
          packetOut.myAntList.add(ant);
        }
      }
    }
    return packetOut;
  }

  //=============================================================================
  // This method sets the given action to EXIT_NEST if and only if the given
  //   ant is underground.
  // Returns true if an action was set. Otherwise returns false
  //=============================================================================
  private boolean exitNest(AntData ant, AntAction action)
  {

    if (ant.state == AntState.UNDERGROUND)
    {
      //Positions (gridX, gridY) relative to nest center and direction are assigned when patrols are formed.
      //Assign ant direction to action direction so that state is not lost by server
      action.type = AntActionType.EXIT_NEST;
      {
        action.x = ant.gridX + centerX;// - (Constants.NEST_RADIUS-1) + random.nextInt(2 * (Constants.NEST_RADIUS-1));
        action.y = ant.gridY + centerY;// - (Constants.NEST_RADIUS-1) + random.nextInt(2 * (Constants.NEST_RADIUS-1));
        action.direction = ant.action.direction;
      }
      return true;
    }
    return false;
  }

  private boolean attackAdjacent(AntData ant, AntAction action)
  {
    return false;
  }

  private boolean pickUpFoodAdjacent(AntData ant, AntAction action)
  {
    return false;
  }

  private boolean goHomeIfCarryingOrHurt(AntData ant, AntAction action)
  {
    return false;
  }

  private boolean pickUpWater(AntData ant, AntAction action)
  {
    return false;
  }

  private boolean goToEnemyAnt(AntData ant, AntAction action)
  {
    return false;
  }

  private boolean goToFood(AntData ant, AntAction action)
  {
    return false;
  }

  private boolean goToGoodAnt(AntData ant, AntAction action)
  {
    return false;
  }

  private boolean goExplore(AntData ant, AntAction action)
  {
    action.direction = ant.action.direction;
    action.type = AntActionType.MOVE;
    return true;
  }


  private AntAction chooseAction(PacketToClient data, AntData ant)
  {
    AntAction action = new AntAction(AntActionType.NOOP);

    if (ant.action.type == AntActionType.BUSY)
    {
      //TODO: Now that the server has told you this ant is BUSY,
      //   The server will stop including it in updates until its state changes
      //   from BUSY to NOOP. At that point, the ant will have wasted a turn in NOOP
      //   that it could have used to do something. Therefore,
      //   the client should save this ant in some structure (such as a HashSet).
      return action;
    }

    //This is simple example of possible actions in order of what you might consider
    //   precedence.

    if (exitNest(ant, action)) return action;
    if (attackAdjacent(ant, action)) return action;
    if (pickUpFoodAdjacent(ant, action)) return action;
    if (goHomeIfCarryingOrHurt(ant, action)) return action;
    if (pickUpWater(ant, action)) return action;
    if (goToEnemyAnt(ant, action)) return action;
    if (goToFood(ant, action)) return action;
    if (goToGoodAnt(ant, action)) return action;

    if (goExplore(ant, action)) return action;
    return ant.action;
  }

  private static String usage()
  {
    return "Usage:\n    [-h hostname] [-t teamname] [-r]\n\n"+
      "Each argument group is optional and can be in any order.\n" +
      "-r specifies that the client is reconnecting.";
  }


  /**
   * @param args Array of command-line arguments (See usage()).
   */
  public static void main(String[] args)
  {
    String serverHost = "localhost";
    boolean reconnection = false;
    if (args.length > 0) serverHost = args[args.length -1];

    //TeamNameEnum team = TeamNameEnum.RandomWalkers;
    TeamNameEnum team = TeamNameEnum.Army;
    if (args.length > 1)
    {
      team = TeamNameEnum.getTeamByString(args[0]);
    }

    new ClientRandomWalk(serverHost, team, reconnection);
  }

}
