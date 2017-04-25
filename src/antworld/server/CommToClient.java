package antworld.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import antworld.common.NestNameEnum;
import antworld.common.PacketToServer;
import antworld.common.PacketToClient;
import antworld.server.Nest.NestStatus;

public class CommToClient extends Thread
{
  private static final boolean DEBUG = false;
  private Server server = null;
  private Socket client = null;
  private boolean clientError = false;

  private ObjectInputStream clientReader = null;
  private ObjectOutputStream clientWriter = null;
  private Nest myNest = null;
  private volatile double timeOfLastMessageFromClient;
  private volatile PacketToServer currentPacketIn;
  private volatile PacketToClient currentPacketOut;
  private volatile int currentPacketInTick;
  private volatile int currentPacketOutTick;
  private String errorMsg;

  public CommToClient(Server server, Socket client)
  {
    this.server = server;
    this.client = client;
    
    
    try
    {
      clientReader = new ObjectInputStream(client.getInputStream());
      clientWriter = new ObjectOutputStream(client.getOutputStream());
    }
    catch (Exception e)
    {
      System.out.println("CommToClient.openConnectionToClient():"+
        " ***ERROR***: Could not open I/O streams");
    }
  }

  public void run()
  {
    assignNest();
    while (client != null)
    {
      if (myNest.getStatus() != NestStatus.CONNECTED)
      {
        closeSocket("Server / Client Connection Error.");
        break;
      }


      synchronized(this)
      {
        while (currentPacketOutTick <= currentPacketInTick)
        {
          try
          {
            wait();
          }
          catch (InterruptedException e) {}
        }
        send(currentPacketOut);
        currentPacketOutTick = 0;
      }

      //System.out.println("CommToClient.run(): nest=" + myNest.nestName + " calling read()");
      PacketToServer packetIn = read();
      pushPacketIn(packetIn);
      //System.out.println("CommToClient.run(): read() returned");
    }
  }


  public synchronized void pushPacketIn(PacketToServer packetIn)
  {
    if (packetIn == null) return;
    currentPacketIn = packetIn;
    timeOfLastMessageFromClient = server.getContinuousTime();
    packetIn.timeReceived = timeOfLastMessageFromClient;
    currentPacketOutTick = 0;
    currentPacketInTick  = server.getGameTick();
    //System.out.println("pushPacketIn(): " + currentPacketIn);
  }


  public synchronized PacketToServer popPacketIn(int gameTick)
  {
    //System.out.println("popPacketIn(): " + currentPacketIn);
    if (currentPacketIn == null) return null;

    PacketToServer packet = currentPacketIn;
    currentPacketIn = null;
    currentPacketInTick = gameTick;
    return packet;

  }


  public synchronized void pushPacketOut(PacketToClient packetOut, int gameTick)
  {
    if (DEBUG)System.out.println("CommToClient.pushPacketOut(gameTick="+gameTick+") " +
      currentPacketOutTick + " " + currentPacketInTick);
    currentPacketOutTick = gameTick;
    currentPacketOut = packetOut;
    if (currentPacketOutTick > currentPacketInTick) notify();
  }




  public double getTimeOfLastMessageFromClient() {return timeOfLastMessageFromClient;}


  /**
   * If this is the client's first connection, then assignNest() will assign the client
   * to an unused nest.<br>
   *
   * If this client is reconnecting, then this will update the required fields.<br><br>
   *
   * If assignNest() fails, then an error message is sent to the client,
   * the client socket is closed and this.client is set to null.
   */
  public void assignNest()
  {
    System.out.println("Server: Client has made connection. Now waiting for client's teamName....");
    PacketToServer packetIn = read();
    if (packetIn == null) return;

    synchronized (this)
    {
      myNest = server.assignNest(this, packetIn);
      if (myNest == null)
      {
        closeSocket(errorMsg);
        return;
      }
      pushPacketIn(packetIn);
      //currentPacketIn = packetIn;
      //timeOfLastMessageFromClient = server.getContinuousTime();
      //packetIn.timeReceived = timeOfLastMessageFromClient;
      //currentPacketOutTick = 0;
      //currentPacketInTick  = server.getGameTick();
    }


      
    System.out.println("Server: Client Accepted: nest="+myNest.nestName+", team="+myNest.team);
  }

  private PacketToServer read()
  {
    PacketToServer packetIn = null;
    try
    {
      packetIn = (PacketToServer) clientReader.readObject();
      if (DEBUG) System.out.println("CommToClient[Unknown]: received: <<<<<<======\n" + packetIn);
      if (packetIn == null)
      {
        closeSocket("!!REJECT!!: received NULL data");
        return null;
      }
    }
    catch (java.net.SocketTimeoutException e)
    {
      closeSocket("CommToClient***ERROR***: client timeout on read:");
      return null;
    }
    catch (IOException e)
    {
      clientError = true;
      closeSocket("CommToClient***ERROR***: client has disconnected");
      return null;
    }
    catch (Exception e)
    {
      e.printStackTrace();
      closeSocket("CommToClient***ERROR***: client read failed: " + e.getMessage());
      return null;
    }

    return packetIn;
  }

  
  


  public void setErrorMsg(String msg)
  {
    errorMsg = msg;
  }


  
  private void send(PacketToClient data)
  {
    if (clientError) return;
    try
    {
      if (myNest.getStatus() != NestStatus.CONNECTED)
      {
        System.out.println(myNest);

        clientError = true;
        closeSocket("NOT CONNECTED");
        return;
      }

      if (DEBUG) System.out.println("CommToClient.send:\n" + data);
      clientWriter.writeObject(data);
      clientWriter.flush();
      clientWriter.reset();
    }
    catch (Exception e)
    {
      clientError = true;
      closeSocket("CommToClient.send() " + e.getMessage());
    }
    
  }
  
  public void closeSocket(String msg)
  {
    NestNameEnum nestName = null;
    if (myNest != null) nestName = myNest.nestName;
    PacketToClient sendData = new PacketToClient(nestName);
    System.err.println(sendData.errorMsg);

    if (!clientError) send(sendData);
    if (myNest != null) myNest.disconnectClient();
    else return;
    myNest = null;
    sendData.errorMsg = msg + "\n Disconnecting.";
    System.err.println(msg);
    try
    {
      if (clientReader != null) clientReader.close();
      if (clientWriter != null) clientWriter.close();
      if (client != null) client.close();
    }
    catch (Exception e) {}
    client = null;
  }
}
