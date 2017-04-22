package antworld.server;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import antworld.common.GameObject;
import antworld.common.PacketToClient;
import antworld.common.PacketToServer;
import antworld.common.Util;
import antworld.common.AntAction.AntActionType;
import antworld.common.AntAction.AntState;
import antworld.common.AntData;
import antworld.common.AntType;
import antworld.common.Constants;
import antworld.common.FoodData;
import antworld.common.NestData;
import antworld.common.NestNameEnum;

import static antworld.common.AntData.UNKNOWN_ANT_ID;


/**
 * Throwing uncaught exceptions causes the server to crash.
 * True, it is bad to have the server crash, however these
 * IllegalArgumentExceptions will never happen.<br>
 * So, why include them then?<br>
 * Having a function check for and throw  IllegalArgumentExceptions is a way to
 * document to future developers what the function is supposed to do (and not do).
 */
public class Nest extends NestData implements Serializable
{
  private static final long serialVersionUID = Constants.VERSION;
  private static final boolean DEBUG = false;
  private static Random random = Constants.random;

  public enum NestStatus {EMPTY, CONNECTED, DISCONNECTED, UNDERGROUND};

  private NestStatus status = NestStatus.EMPTY;
  private CommToClient client = null;
  private HashMap<Integer,AntData> antCollection = new HashMap<>();

  public Nest(NestNameEnum nestName, int x, int y)
  {
    super(nestName, null, x, y);
  }


  /**
   * Called by CommToClient when a new client socket connection is made.
   * @param client all socket and communication data an methods client being assigned this nest.
   * @param packetIn raw data packet from client which may or may not request to spawn new ants
   *                 on this first packet.
   */
  public synchronized void setClient(CommToClient client, PacketToServer packetIn)
  {
    System.out.println("AWG: SET CLIENT");
    team = packetIn.myTeam;

    if (status == NestStatus.EMPTY)
    {
      antCollection.clear();
      foodInNest  = Constants.INITIAL_FOOD_UNITS;
      waterInNest = Constants.INITIAL_NEST_WATER_UNITS;
    }

    int i = 0;
    for (AntData ant : packetIn.myAntList)
    {
      System.out.println("Spawning ant: " + i);
      if (ant.id != AntData.UNKNOWN_ANT_ID) continue;
      if (ant.action.type != AntActionType.BIRTH) continue;
      if (ant.antType == null) continue;
      spawnAnt(ant.antType);
      ant.id = i++; // Have to *set* ant ID to avoid spawning twice.
    }

    this.client = client;
    status = NestStatus.CONNECTED;
  }


  /**
   * DO NOT call this method from the mainGameLoop thread as client.closeSocket() sleeps for
   * 100s of ticks to make sure error message goes through before breaking socket.
   */
  public synchronized void disconnectClient()
  {
    if (client != null)
    {
      client.closeSocket("Server disconnecting");
      client = null;
    }
    //Do not change if status is EMPTY or UNDERGROUND
    if (status == NestStatus.CONNECTED) status = NestStatus.DISCONNECTED;
  }


  /**
   * Called by main game loop of AntWorld when a client has not sent a data package in a long time.
   * @param world
   */
  public synchronized void sendAllAntsUnderground(AntWorld world)
  {
    if (status == NestStatus.EMPTY) return;
    if (status == NestStatus.UNDERGROUND) return;

    status = NestStatus.UNDERGROUND;
    for (AntData ant : antCollection.values())
    {
      world.removeGameObject(ant);
      ant.state = AntState.UNDERGROUND;
      ant.gridX = centerX;
      ant.gridY = centerY;

    }
  }


  public int getFoodCount() { return foodInNest; }

  public int getWaterCount() { return waterInNest;}
  public int getAntCount() { return antCollection.size();}



  public HashMap<Integer,AntData> getAnts()
  {
    return antCollection;
  }


  /**
   * Adds (or subtracts if quantity is <0) the specified quantity of the
   * specified type to this nest.
   * @param type (must be FOOD or WATER, not ANT)
   * @param quantity (units to add or subtract if <0)
   */
  public void addResource(GameObject.GameObjectType type, int quantity)
  {
    if (type == GameObject.GameObjectType.WATER) waterInNest += quantity;
    if (type == GameObject.GameObjectType.FOOD) foodInNest += quantity;
  }


  /**
   * Calculates and sets the current score of this nest. Each client's goal is to maximize this value
   * by the end of the game. The score is based on the food in this nest and the number of
   * living ants belonging to this nest. Food carried by ants does not count towards the score.
   */
  public void calculateScore()
  {
    if (status == NestStatus.EMPTY) score = 0;
    score = foodInNest + antCollection.size()*AntType.SCORE_PER_ANT;
  }



  
  public double getTimeOfLastMessageFromClient()
  {
    if (client == null) return 0;
    return client.getTimeOfLastMessageFromClient();
  }


  /**
   * Attempts to spawn a new ant in this nest. The attempt fails if there is
   * insufficient food.<br><br>
   * On success, the newly spawned ant is added to this nest's ant collection.
   * @param antType to spawn
   * @return an instance of the newly spawned ant or null of attempt failed.
   */
  private AntData spawnAnt(AntType antType)
  {
    //System.out.println("Nest.spawnAnt(): " + this);
    if (foodInNest < antType.TOTAL_FOOD_UNITS_TO_SPAWN)
    {
      return null;
    }

    foodInNest -= antType.TOTAL_FOOD_UNITS_TO_SPAWN;
    AntData ant = AntMethods.createAnt(antType, nestName, team);

    ant.gridX = centerX;
    ant.gridY = centerY;

    antCollection.put(ant.id, ant);
    return ant;
  }


  /**
   * @return The status of this nest which may be:
   * <ul>
   *     <li>EMPTY (not yet assigned to any client), </li>
   *     <li>CONNECTED (assigned to and receiving updates from a client),</li>
   *     <li>DISCONNECTED (assigned to a client with a dropped socket connection),</li>
   *     <li>UNDERGROUND (assigned to a client that has been disconnected for a long time and its ants
   *     have been returned to its nest).</li>
   * </ul>
   */
  public NestStatus getStatus() {return status;}


  /**
   * @param x east/west pixel (== cell) location on world map.
   * @param y north/south pixel (== cell) location on world map.
   * @return true iff the specified location is within the yellow colored area surrounding this nest.
   */
  public boolean isNearNest(int x, int y)
  { if (Util.manhattanDistance(centerX, centerY, x, y) <= Constants.NEST_RADIUS) return true;
    return false;
  }


  /**
   * Update automatic ant events of all ants in this nest. Automatic events include:
   * <ol>
   *    <li>Attrition damage.</li>
   *    <li>Removing ants that died last tick from ant sets.</li>
   *    <li>Decrementing move/busy time.</li>
   *    <li>Changing last turn's actions to default NOOP</li>
   * </ol>
   */
  public void updateAutomaticAntEvents()
  {
    //Note: this iterator allows removing items without a ConcurrentModificationException
    Iterator iterator = antCollection.entrySet().iterator();
    while (iterator.hasNext())
    {
      Map.Entry pair = (Map.Entry)iterator.next();
      AntData ant = (AntData) pair.getValue();
      if (ant.state == AntState.DEAD)
      { iterator.remove();
        continue;
      }

      if (ant.action.type == AntActionType.MOVE || ant.action.type == AntActionType.BUSY)
      {
        if (ant.action.quantity > 0) ant.action.quantity--;
        if (ant.action.quantity > 0) ant.action.type = AntActionType.BUSY;
        else ant.action.type = AntActionType.NOOP;
      }
      else ant.action.type = AntActionType.NOOP;


      //Note: This must be done after removal of dead ants since it could cause an
      //  ant to die and newly dead ants are left in the list for one tick.
      if (ant.state == AntState.OUT_AND_ABOUT)
      { if (random.nextDouble() < ant.antType.getAttritionDamageProbability()) ant.health--;
        if (ant.health < 0) ant.state = AntState.DEAD;
      }
    }
  }


  /**
   * Applies client ant action requests. Note: no changes in this loop are made to
   *   ants belonging to a client that the client does not include in their return package nor
   *   for which a valid action is not provided.
   * @param world
   */
  public void updateReceivePacket(AntWorld world)
  {
    PacketToServer packetIn = client.popPacketIn(world.getGameTick());

    if (packetIn == null) return;
    
    if (packetIn.myAntList == null) return;


    //Loop through and execute legal actions of each ant in the ant list sent by the client.<br>
    // Ant actions are applied in the ant list order.
    for (AntData clientAnt : packetIn.myAntList)
    {
      //Birth action
      if (clientAnt.id == UNKNOWN_ANT_ID)
      {
        if (clientAnt.action.type != AntActionType.BIRTH) continue;
        spawnAnt(clientAnt.antType); //Only succeeds is needed food exists.
      }

      else
      {
        //Search the server's copy of this nest's ant collection for an ant with
        //id matching the clientAnt id.<br>
        //Ignore the request if the client attempts to command an ant not in its collection.
        AntData serverAnt = antCollection.get(clientAnt.id);

        if (serverAnt == null)
        {
          if (DEBUG) System.out.println("Nest.updateReceivePacket(): Illegal Ant =" + clientAnt);
          continue;
        }
        serverAnt.action.type = AntMethods.update(world, serverAnt, clientAnt.action);

        // AWG: Added this but it doesn't seem to do anything : because we're updating the *sent* client ant.
        //clientAnt.gridX = serverAnt.gridX;
        //clientAnt.gridY = serverAnt.gridY;

      }
    }
  }


  /**
   * Ants that die are left in the ant collection for one game tick, but are instantly
   * replaced in the world by a food stack.
   * @param world
   */
  public void updateRemoveDeadAntsFromWorld(AntWorld world)
  {
    for (AntData ant : antCollection.values())
    {
      if (ant.state == AntState.DEAD)
      {
        world.removeGameObject(ant);
        int foodUnits = AntType.getDeadAntFoodUnits();
        if (ant.carryUnits > 0 && ant.carryType == GameObject.GameObjectType.FOOD)
        {
          foodUnits += ant.carryUnits;
        }
  
        FoodData droppedFood = new FoodData(GameObject.GameObjectType.FOOD, ant.gridX, ant.gridY, foodUnits);
        world.addFood(null, droppedFood);
        if (DEBUG) System.out.println("Nest[" + nestName +
          "] Ant died: Current Population = " + antCollection.size());
        //Note: an ant may have done some action this tick before dieing.
        //   This will have been recorded in ant.action.type.
      }
    }
  }

  //public AntData getAntByID(int antId)
  //{
  //  tmpAntData.id = antId;
  //  int index = Collections.binarySearch(antList, tmpAntData);
  //  return antList.get(index);
  //}

  public void updateSendPacket(AntWorld world, NestData[] nestDataList)
  {
    if (team == null) return;
    if (status != NestStatus.CONNECTED) return;

    PacketToClient packetOut = new PacketToClient(nestName);

    packetOut.nestData = nestDataList;

    packetOut.tick = world.getGameTick();
    packetOut.tickTime = world.getGameTime();
    //commData.enemyAntList = new ArrayList<>();
    //commData.foodSet = new ArrayList();

    // AWG: what we're sending is the values for antCollection.
    for (AntData ant : antCollection.values())
    {
      if (ant.action.type == AntActionType.BUSY) continue;
      packetOut.myAntList.add(ant);

      //TODO
      //world.appendAntsInProximity(ant, commData.enemyAntList);
      //world.appendFoodInProximity(ant, commData.foodSet);
    }

    client.pushPacketOut(packetOut, world.getGameTick());
  }
}
