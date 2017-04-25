package antworld.server;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import antworld.common.Constants;
import antworld.common.PacketToServer;
import antworld.server.Nest.NestStatus;

import static antworld.common.Constants.random;

public class Server extends Thread
{
  private static final boolean DEBUG = false;

  /**
   * If the server does not receive a PacketToServer from a particular client
   * in greater than TIMEOUT_CLIENT_TO_UNDERGROUND seconds, then the client's ants
   * are sent back to its nest. <br><br>
   * Note: this is a few minutes longer than SOCKET_READ_TIMEOUT. Thus, a client that
   * loses a connection due to timeout, can reconnect within a few minutes to find
   * the ants still in the world with NOOP commands assumed during the time when
   * no client directions were received.
   */
  public static final double TIMEOUT_CLIENT_TO_UNDERGROUND = 60*5;


  /**
   * SOCKET_READ_TIMEOUT specifies the maximum time in milliseconds that a read()
   * call on the InputStream associated with the Socket will block. <br>
   * If the timeout expires, a java.net.SocketTimeoutException is raised,
   * though the Socket is still valid. If this exception is raised,
   * the server will send an error message to that client and close the socket.
   */
  public static final int SOCKET_READ_TIMEOUT = 60*2*1000;

  /**
   * The maximum queue length for incoming connection indications
   * (a request to connect) is set to the backlog parameter.
   * If a connection indication arrives when the queue is
   * full, the connection is refused.
   */
  private static final int SERVER_SOCKET_BACKLOG = 10;
  private long timeStartOfGameNano;
  
  private ServerSocket serverSocket = null;
  private ArrayList<Nest> nestList;
  private AntWorld world;


  public Server(AntWorld world, ArrayList<Nest> nestList)
  {
    timeStartOfGameNano = System.nanoTime();
    this.world = world;
    this.nestList = nestList;
    //System.out.println("Server: Opening socket to listen for client connections....");
    try
    {
      serverSocket = new ServerSocket(Constants.PORT, SERVER_SOCKET_BACKLOG);
    }
    catch (Exception e)
    {
      System.err.println("Server: ***ERROR***: Opening socket failed.");
      e.printStackTrace();
      System.exit(-1);
    }
    if (DEBUG) System.out.println("Server: socket opened on port "+Constants.PORT);
  }
  
  
  public void run()
  {
    while (true)
    {
      Socket clientSocket = null;
      CommToClient client = null;
      System.out.println("Server: waiting for client connection.....");
      try
      {
        clientSocket = serverSocket.accept();
        clientSocket.setSoTimeout(SOCKET_READ_TIMEOUT);

        System.out.println("Server: Client attempting to connect.....");
        client = new CommToClient(this, clientSocket);
        client.start();
      }
      catch (Exception e)
      {
        String msg = "Server ***ERROR***: Failed to connect to client.";
        if (client != null) client.closeSocket(msg);
      }
    }
  }


  /**
   * ContinuousTime is the time in seconds from the start of the game to the current moment.
   * By contrast, getGameTime returns the time to the current game tick.
   * @return time in seconds
   */
  public double getContinuousTime()
  {
    return (System.nanoTime() - timeStartOfGameNano)*Constants.NANO;
  }



  public double getGameTime() {return world.getGameTime();}
  public int getGameTick() {return world.getGameTick();}

  
  public synchronized Nest assignNest(CommToClient client, PacketToServer packetIn)
  {
    if (packetIn.myTeam == null) return null;

    Nest assignedNest = world.getNest(packetIn.myTeam);

    if (assignedNest != null)
    {
      if (assignedNest.getStatus() == NestStatus.CONNECTED)
      {
        String msg = "Already connected: team="+packetIn.myTeam + " to nest "+ assignedNest.nestName;
        client.setErrorMsg(msg);
        return null;
      }
      System.out.println("Server() Reconnecting " + packetIn.myTeam + " to nest " + assignedNest.nestName);
      assignedNest.setClient(client, packetIn);
      return assignedNest;
    }

    int assignedNestCount = 0;
    int centerOfMassX = 0;
    int centerOfMassY = 0;
    for (Nest nest : nestList)
    {
      if (nest.team != null)
      {
        assignedNestCount++;
        centerOfMassX += nest.centerX;
        centerOfMassY += nest.centerY;
      }
    }

    if (assignedNestCount < 1)
    {
      assignedNest = nestList.get(random.nextInt(nestList.size()));
      if (DEBUG) System.out.println("Server: Totally Random Nest: "+assignedNest.nestName);
    }
    else
    {
      centerOfMassX /= assignedNestCount;
      centerOfMassY /= assignedNestCount;
      if (DEBUG) System.out.println("Server: Nest Center of Mass: ("+
        centerOfMassX + ","+centerOfMassY+")");

      int minDistance1 = Integer.MAX_VALUE;
      int minDistance2 = Integer.MAX_VALUE;
      Nest nearEmptyNest1 = null;
      Nest nearEmptyNest2 = null;
      for (Nest nest : nestList)
      {
        if (nest.team != null) continue;

        int dx = nest.centerX - centerOfMassX;
        int dy = nest.centerY - centerOfMassY;
        int distance = Math.abs(dx) + Math.abs(dy);
        if (distance < minDistance1)
        {
          minDistance1 = distance;
          nearEmptyNest1 = nest;
        }
        else if (distance < minDistance2)
        {
          minDistance2 = distance;
          nearEmptyNest2 = nest;
        }
      }
      assignedNest = nearEmptyNest1;
      if (nearEmptyNest2 != null)
      { if (random.nextBoolean()) assignedNest = nearEmptyNest2;
        if (DEBUG) System.out.println("Server: Nearest Nests: "+ nearEmptyNest1.nestName +
          " & " + nearEmptyNest2.nestName);
      }
    }

    if (assignedNest != null) assignedNest.setClient(client, packetIn);

    return assignedNest;
  }
}
