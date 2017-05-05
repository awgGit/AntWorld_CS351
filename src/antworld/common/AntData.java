package antworld.common;

import antworld.common.AntAction.AntActionType;
import antworld.common.AntAction.AntState;
/**
 * AntData contains all data about an ant that is exchanged between
 * client and server.
 */

public class AntData extends GameObject implements Comparable<AntData>
{
  /** An ant can be in one of three states: UNDERGROUND, OUT_AND_ABOUT or DEAD.<br>
   * When an ant is UNDERGROUND, it is in its nest. An ant in the nest is totally safe:
   * it cannot be attacked and never takes attrition damage.
   * */

  private static final long serialVersionUID = Constants.VERSION;

  public static final int UNKNOWN_ANT_ID = -1;//use by client for ants being birthed

  /** The name of the nest to which this ant belongs. */
  public final NestNameEnum nestName;
  
  /** The name of the team to which this ant belongs. */
  public final TeamNameEnum teamName;

  /** Unique id. When the client uses the birth action to create an ant, the 
   * client should leave this set to the default value of Constants.UNKNOWN_ANT_ID.<br><br>
   * When the server accepts an ant's birth action, the server sets the id to a
   * unique value that monotonically increases with the tick.
   */
  public int id = UNKNOWN_ANT_ID;



  public AntState state = AntState.UNDERGROUND;

  /** The ant objType. */
  public AntType antType;

  
  /** The number of food units the ant is carrying. An ant may carry
   * either food or water.<br>
   * The maximum number of units an ant can
   * carry is given by this.antType.getCarryCapacity()
   *  */
  public int carryUnits = 0;


  /**
   * Ants cannot carry ants. The only valid carryType values are:
   * WATER and FOOD.
   */
  public GameObjectType carryType;

  /** The client sets myAction to tell the server what the ant wants to do.
   * The server returns this set to the action that was actually taken.
   * */
  public AntAction action;

  /** This ant's health. When an ant's health reaches 0, it dies.*/
  public int health;



  /**
   * Constructor used by client to birth new ants. It creates an
   * ant with id = UNKNOWN_ANT_ID.
   * This constructor sets nestName to null. The server will ignore this
   * as a client can only birth ants in its own nest.
   * @param antType the objType
   * @param teamName the team name
   */
  public AntData(AntType antType, TeamNameEnum teamName)
  {
    this.objType = GameObjectType.ANT;
    id = UNKNOWN_ANT_ID;
    this.antType = antType;
    nestName = null;
    this.teamName = teamName;
    health = antType.getMaxHealth();
    action = new AntAction(AntActionType.BIRTH);
  }


  /**
   * Constructor used by server.
   *
   * @param id the unique id
   * @param type the objType
   * @param nestName the nest name
   * @param teamName the team name
   */
  public AntData(int id, AntType type, NestNameEnum nestName, TeamNameEnum teamName)
  {
    this.objType = GameObjectType.ANT;
    this.id = id;
    antType = type;
    this.nestName = nestName;
    this.teamName = teamName;
    health = type.getMaxHealth();
    action = new AntAction(AntActionType.BIRTH);
  }
  
  
  /**
   * Instantiates a new ant common by creating a deep copy of the given source ant.
   *
   * @param source the source
   */
  public AntData(AntData source)
  {
    objType = GameObjectType.ANT;
    id = source.id;
    nestName = source.nestName;
    teamName = source.teamName;
    
    gridX = source.gridX;
    gridY = source.gridY;

    state = source.state;

    antType = source.antType;
    carryType = source.carryType;
    carryUnits = source.carryUnits;

    action = new AntAction(source.action);

    health = source.health;
  }

  /**
   * @return a formatted string showing many of this ant's fields.
   */
  public String toString()
  {
    String out = "AntData: [id=" + id + ", nest=" + nestName + ", team=" + teamName + ", " + antType + ", health="
        + health + ", " + action;
    if (carryUnits > 0) out += ", carry: [" + carryType + ", " + carryUnits + "]";

    if (state==AntState.UNDERGROUND) out += ", underground ]";
    else out += ", x=" + gridX + ", y=" + gridY + "]";

    return out;
  }


  /**
   * @return a formatted string showing some of this ant's fields for compact
   * display in the GUI mouse-over.
   */
  public String toStringShort()
  {
    String out = nestName.toString() +"[" + id + "]: " + health + " HP ";
    if (carryUnits > 0) out += "[" + carryType + ":" + carryUnits + "]";
    return out;
  }

  @Override
  public int compareTo(AntData otherAnt)
  {
    return id - otherAnt.id;
  }


  @Override
  public boolean equals(Object other)
  {
    if (other == this) return true;
    if (other == null) return false;
    if (!(other instanceof AntData))return false;
    if (id == ((AntData)other).id) return true;
    return false;
  }

  @Override
  public int hashCode()
  {
    return id;
  }
}
