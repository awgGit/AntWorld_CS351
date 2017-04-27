package antworld.client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Random;

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
  public double angle_to_sea = -1;
  private static final boolean DEBUG = true;
  private final TeamNameEnum myTeam;
  private ObjectInputStream  inputStream = null;
  private ObjectOutputStream outputStream = null;
  private boolean isConnected = false;
  private NestNameEnum myNestName = null;
  private int centerX, centerY;
  private Socket clientSocket;

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

    PacketToServer packetOut = new PacketToServer(myTeam); // AWG: Impersonation?

    if (reconnect) packetOut.myAntList = null;
    else
    {
      //Spawn ants of whatever objType you want
      int numAnts = 50;//Constants.INITIAL_FOOD_UNITS / AntType.TOTAL_FOOD_UNITS_TO_SPAWN;

      for (int i=0; i<numAnts; i++)
      {
        AntType type = AntType.EXPLORER; //AntType.values()[random.nextInt(AntType.SIZE)];
        packetOut.myAntList.add(new AntData(type, myTeam)); //default action is BIRTH.
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
    angle_to_sea = Raycasting.getBearingToWater(centerX,centerY);
    System.out.println("ClientRandomWalk: ==== Nest Assigned ===>: " + myNestName);
    A_Star.buildBoard(); // AWG: Transform the board into interconnected graph nodes.
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
    PacketToServer packetOut = new PacketToServer(myTeam); // AWG: Impersonation?
    for (AntData ant : packetIn.myAntList)
    {
      AntAction action = chooseAction(packetIn, ant);
      if (action.type != AntActionType.NOOP)
      {
         ant.action = action;
         packetOut.myAntList.add(ant);
      }
    }
    return packetOut;
  }

  //=============================================================================
  // This method sets the given action to EXIT_NEST if and only if the given
  //   ant is underground.
  // Returns true if an action was set. Otherwise returns false
  //=============================================================================

  // AWG: Added functionality to drop whatever we're carrying before we exit the nest.
  // We may want to seperate these two actions in case ants want to take water with them out of the nest.
  private boolean exitNest(AntData ant, AntAction action, PacketToClient ptc)
  {
    if (ant.state == AntState.UNDERGROUND)
    {
      if(ant.carryUnits != 0)
      {
        action.type = AntActionType.DROP;
        action.direction = null;
        action.quantity = ant.carryUnits;
        return true;
      }
      action.x = centerX;// - (Constants.NEST_RADIUS-1) + random.nextInt(2 * (Constants.NEST_RADIUS-1));
      action.y = centerY;// - (Constants.NEST_RADIUS-1) + random.nextInt(2 * (Constants.NEST_RADIUS-1));
      action.type = AntActionType.EXIT_NEST;
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

  // If we're carrying water, try to heal ourselves.
  // AWG: For whatever reasons, it only seems to heal 1 hp. Probably a bug on my side.
  private boolean healSelf( AntData ant, AntAction action )
  {
    if( ant.health < 25 && ant.carryUnits > 0)
    {
      action.type = AntActionType.HEAL;
      action.direction = null;
      action.quantity = ant.carryUnits;
      return true;
    }
    return false;
  }

  // If we're near water, try to pass it along.
  // AWG: Ultimately this should only be used by ants on water carring duty, not just any ant.
  private boolean passWater(AntData ant, AntAction action)
  {
    if( ant.carryUnits == 0) return false; // Don't try to pass unless we have stuff.
    // If we're near the nest, drop ourselves into the nest to add the resource.
    if( Math.abs(ant.gridX-centerX)+Math.abs(ant.gridY-centerY) < 15)
    {
      action.direction = null;
      action.type = AntActionType.ENTER_NEST;
      action.quantity = ant.carryUnits;
      return true;
    }

    // Pass back in the direction opposite to the water.
    action.direction = Direction.values()[ (4+(int) Math.floor(angle_to_sea /45.0))%8 ];
    action.type = AntActionType.DROP;
    action.quantity = ant.carryUnits;
    return true;
  }

  // If we're near water, try to pick it up.
  // AWG: Again this might be best left to ants on water duty.
  private boolean pickUpWater( AntData ant, AntAction action, PacketToClient ptc )
  {
    if( ant.carryUnits >= ant.antType.getCarryCapacity() ) return false;
    ArrayList<Integer> ray_distances = Raycasting.castRays(ant.gridX, ant.gridY);
    boolean adjacent_to_water = false;
    for( Integer i : ray_distances )
    {
      if (i == 0)
      {
        adjacent_to_water = true;
        break;
      }
    }
    int x_diff;
    int y_diff;

    if( ptc.foodList != null)
    {
      for (GameObject food : ptc.foodList)
      {
        x_diff = Math.abs(food.gridX - ant.gridX);
        y_diff = Math.abs(food.gridY - ant.gridY);
        if (food.objType == GameObject.GameObjectType.FOOD) continue;
        if ((x_diff + y_diff) <= 1 || (x_diff == 1 && y_diff == 1))
        {
          adjacent_to_water = true;
        }
      }
    }

    if( !adjacent_to_water ) return false;

    action.direction = Direction.values()[ (int) Math.floor(angle_to_sea /45.0)];
    action.type = AntActionType.PICKUP;
    action.quantity = ant.antType.getCarryCapacity();
    return true;
  }

  // Try to go to the coastline to get water, forming a queue along the way.
  private boolean goToSea(AntData ant, AntAction action, PacketToClient ptc)
  {
    // Point towards the nearest ocean.
    /* Travel in a straight instead of A* because queue is shortest this way,
    and with relaying, terrain is irrelevant anyway. */
    Direction dir = Direction.values()[ (int) Math.floor(angle_to_sea /45.0)];
    int x_diff;
    int y_diff;

    for( AntData a : ptc.myAntList )
    {
      if(a == ant) continue;
      if(a.state == AntState.UNDERGROUND) continue;
      x_diff = Math.abs(a.gridX-(ant.gridX+dir.deltaX()));
      y_diff = Math.abs(a.gridY-(ant.gridY+dir.deltaY()));
      if( (x_diff+y_diff)<=1 || (x_diff==1&&y_diff==1) )
      {
        dir = null;
        break;
      }
    }

    action.type = AntActionType.MOVE;
    action.direction = dir;
    return true;

  }

  // Experimental exploration method
  // AWG: changing the denominator's power varies the decay rate of the simulated repulsive field
  // i.e. if it's bigger then the closest ants have the greatest repulsion:
  // e.g. setting to 4 will roughly emulate blue noise
  private boolean goExplore( AntData ant, AntAction action, PacketToClient ptc)
  {
    double sx, sy, mag;
    //mag = Math.abs(centerY-ant.gridY) + Math.abs(centerX-ant.gridX);
    sx = 0;
    sy = 0;
    //sx = (centerX - ant.gridX);
    //sy = (centerY - ant.gridY);
    //sx = (centerX - ant.gridX)/Math.pow(mag,2);
    //sy = (centerY - ant.gridY)/Math.pow(mag,2);

    for( AntData a : ptc.myAntList )
    {
      if(a == ant) continue;
      //mag = Math.sqrt( Math.pow(a.gridX-ant.gridX,2) + Math.pow(a.gridY-ant.gridY,2));
      mag = Math.abs(ant.gridX-a.gridX)+Math.abs(ant.gridY-a.gridY);
      sx -= (ant.gridX-a.gridX)/ Math.pow(mag,3); // Power of 1: Spreading out thin
      sy -= (ant.gridY-a.gridY)/ Math.pow(mag,3); // Power of 4: Blue noise, almost! This is interesting.
    }

    //mg = Math.sqrt(sx*sx+sy*sy);
    mag = Math.abs(sx) + Math.abs(sy);
    sx /= mag;
    sy /= mag;

    double angle = Math.toDegrees(Math.atan2( sy, sx ));
    angle = (angle+270)%360;
    System.out.printf("\t%.2f -> %d\n", angle, (int) Math.floor(angle/45.0));
    Direction dir = Direction.values()[ (int) Math.floor(angle/45.0)];
    action.type = AntActionType.MOVE;
    action.direction = dir;
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

    // State transitions
    if (exitNest(ant, action, data)) return action;
    if( pickUpWater(ant,action, data))return action;
    if(healSelf(ant,action))return action;
    if( passWater(ant,action))return action;
    if (goToSea(ant, action, data)) return action;
    return action;

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
    TeamNameEnum team = TeamNameEnum.SimpleSolid_3;
    if (args.length > 1)
    { team = TeamNameEnum.getTeamByString(args[0]);
    }

    new ClientRandomWalk(serverHost, team, reconnection);
  }

}
