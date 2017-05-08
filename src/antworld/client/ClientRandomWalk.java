package antworld.client;

import antworld.common.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

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

  // Our variables
  private ExplorerMachine explorerMachine;
  private WorkerMachine workerMachine;
  private BuildGraph buildGraph;
  private PathCalculator pathCalculator;
  private boolean spawned_workers = false;

  // Each game tick / packet sent from the server ...
  private PacketToServer chooseActionsOfAllAnts(PacketToClient packetIn)
  {
    PacketToServer packetOut = new PacketToServer(myTeam); // AWG: Impersonation?

    for( AntData ant : packetIn.myAntList )
    {
      if( ant.antType == AntType.EXPLORER ) explorerMachine.addAnt(ant);
      if( ant.antType == AntType.WORKER ) workerMachine.addAnt(ant);
      if( !MiscFunctions.healToFull.containsKey(ant.id)) MiscFunctions.healToFull.put(ant.id, false);
    }
    FoodPiles.cullPiles(); // Remove food piles which have no food left.
    pathCalculator.calculatePathsWhenReady(); // Calculate food-nest paths when able.
    explorerMachine.setAntActions( packetIn ); // Set actions for the explorers.
    workerMachine.setAntActions( packetIn );  // Set actions for the workers.

    if (spawned_workers)
    {
      int count = 0;
      for( AntData a : packetIn.myAntList ) { if( a.antType == AntType.WORKER ) count++; }
      if(count < 20)
      {
        AntType type = AntType.WORKER;
        packetOut.myAntList.add(new AntData(type, myTeam));
      }
    }

    // Spawn some workers when we're ready for them.
    if( !spawned_workers && PathCalculator.paths_ready )
    {
      for (int i=0; i<20; i++)
      {
        AntType type = AntType.WORKER;
        packetOut.myAntList.add(new AntData(type, myTeam));
      }
      spawned_workers = true;
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
      int numAnts = 40;
      for (int i=0; i<numAnts; i++)
      {
        AntType type = AntType.EXPLORER;
        packetOut.myAntList.add(new AntData(type, myTeam));
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

    // Build the two graphs we need.
    A_Star.buildBoard(); // Dense (A*)
    buildGraph = new BuildGraph(); // Sparse (DFS interpolated with A*)

    explorerMachine = new ExplorerMachine( centerX, centerY );
    workerMachine = new WorkerMachine( centerX, centerY );
    pathCalculator = new PathCalculator( centerX, centerY );
  }

  //<editor-fold desc="Strictly for communication with the server. Don't need to tinker with this.">
  public ClientRandomWalk(String host, TeamNameEnum team, boolean reconnect)
  {
    myTeam = team;
    System.out.println("Starting " + team +" on " + host + " reconnect = " + reconnect);

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

    TeamNameEnum team = TeamNameEnum.Carpenter;
    if (args.length > 1)
    { team = TeamNameEnum.getTeamByString(args[0]);
    }

    new ClientRandomWalk(serverHost, team, reconnection);
  }
  //</editor-fold>

}
