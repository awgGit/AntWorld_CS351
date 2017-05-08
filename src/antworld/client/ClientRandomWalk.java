package antworld.client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import antworld.common.*;

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
  //<editor-fold desc="Joel's variables">
  private static final boolean DEBUG = true;
  private final TeamNameEnum myTeam;
  private ObjectInputStream  inputStream = null;
  private ObjectOutputStream outputStream = null;
  private boolean isConnected = false;
  private NestNameEnum myNestName = null;
  private int centerX, centerY;
  private Socket clientSocket;
  //</editor-fold>

  // Our exploration variables.
  private BuildGraph buildGraph;
  private ExploreGraph exploreGraph;

  Map<Integer, HashMap<PathNode, PathNode>> copy;

  // Each game tick / packet sent from the server ...
  private PacketToServer chooseActionsOfAllAnts(PacketToClient packetIn)
  {
    PacketToServer packetOut = new PacketToServer(myTeam);
    exploreGraph.setAntActions( packetIn ); // Update all of the ants.

    for(int j = 0; j < exploreGraph.food_sites_to_broadcast; j++)
    {
      int convoyAnts = 2;
      if(exploreGraph.pheromone_path_generated[j]
              && !exploreGraph.convoy_sent[j]
              && exploreGraph.t1.getState() == Thread.State.TERMINATED)
      {
        if(exploreGraph.nest_to_food.get(j) == null)
        {
          exploreGraph.setNestToFoodPath(j);
          Map<PathNode, PathNode> nest_to_food = exploreGraph.nest_to_food.get(j);
          copy.put(j, new HashMap<>(nest_to_food));
          Iterator it = nest_to_food.entrySet().iterator();
          while(it.hasNext())
          {
            Map.Entry pair = (Map.Entry)(it.next());
            pair.setValue(null);
          }
          exploreGraph.t1 = new Thread();
          PathNode temp = A_Star.board[exploreGraph.path_generator.end_position.x][exploreGraph.path_generator.end_position.y];

          exploreGraph.path_generator.end_position = A_Star.board[exploreGraph.path_generator.start_position.x][exploreGraph.path_generator.start_position.y];
          exploreGraph.path_generator.start_position = temp;

          exploreGraph.t1.start();
        }
        else
        {
          exploreGraph.setFoodToNestPath(j);
          exploreGraph.convoy_sent[j] = true;
          exploreGraph.nest_to_food.put(j, copy.get(j));
          for (int k = 0; k < convoyAnts; k++)
          {
            AntType type = AntType.WORKER;
            packetOut.myAntList.add(new AntData(type, myTeam));
          }
        }
      }
    }
    for( AntData ant : packetIn.myAntList ) { packetOut.myAntList.add(ant); }
    return packetOut;
  }

  // For spawning ants on connection ...
  private boolean openConnection(String host, boolean reconnect)
  {
    //<editor-fold desc="Necessary communication with server.">
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
    //</editor-fold>
    PacketToServer packetOut = new PacketToServer(myTeam);
    if (reconnect) packetOut.myAntList = null;
    else
    {
      int numAnts = 10; // Was 70
      for (int i=0; i<numAnts; i++)
      {
        AntType type = AntType.EXPLORER;
        AntData temp_antdata = new AntData(type, myTeam);
        packetOut.myAntList.add( temp_antdata );
      }
    }
    send(packetOut);
    return true;
  }

  // For building components on obtaining a nest ...
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
    A_Star board = new A_Star();
    board.buildBoard();
    //A_Star.buildBoard(); // AWG: Transform the board into interconnected graph nodes.
    buildGraph = new BuildGraph(); // Build a more heavily discretized graph to explore.
    exploreGraph = new ExploreGraph(board, centerX, centerY); // Explore the discretized graph using DFS & local A*.
    for( AntData ant : packetIn.myAntList ) { exploreGraph.addAnt( ant ); } // Actually get the ants on the list so
                                                                            // that they'll explore.
  }

  //<editor-fold desc="Strictly for communication with the server. Don't need to tinker with this.">
  public ClientRandomWalk(String host, TeamNameEnum team, boolean reconnect)
  {
    myTeam = team;
    System.out.println("Starting " + team +" on " + host + " reconnect = " + reconnect);

    copy = new HashMap<>();

    isConnected = openConnection(host, reconnect);
    if (!isConnected) System.exit(0);

    mainGameLoop();
    closeAll();
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
  //</editor-fold>

}
