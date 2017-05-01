package antworld.client;

import antworld.common.*;

import java.util.Map;

/* This class holds functions which aren't associated with a particular task (yet).
 * Sort of like the 'actions' class I made previously. */
public class MiscStateTransitions
{

  /**
   *This method is in charge of helping the ant find its way back home once it is carrying food!
   * @param ant - this current ant
   * @param action - sets this ants actions depending if it is at the nest or not.
   * @param ptc - information regarding the ants for our group
   * @return - if the ant has hit its carry capacity go home, or keep doing what it is doing.
   */
  static  boolean goHomeIfCarryingOrHurt(AntData ant, AntAction action, PacketToClient ptc)
  {
    return  enterNest(ant,action,ptc) // Enter the nest if we're close enough to it.
            || goHomeIfCarryingFood(ant,action,ptc,ant.antType.getCarryCapacity()) // Return if full with food.
            || goHomeIfCarryingWater(ant,action,ptc,ant.antType.getCarryCapacity()) // Return if full with water.
            || goHomeIfHurt(ant,action,ptc,ant.antType.getMaxHealth()/2 ); // Return if below or at 50% health.
  }

  // Split the logic into multiple functions
  static boolean goHomeIfCarryingWater( AntData ant, AntAction action, PacketToClient ptc, int carry_threshold)
  {
    return ant.carryType == GameObject.GameObjectType.FOOD && ant.carryUnits > carry_threshold && goHome(ant, action, ptc);
  }
  static boolean goHomeIfCarryingFood( AntData ant, AntAction action, PacketToClient ptc, int carry_threshold)
  {
    return ant.carryType == GameObject.GameObjectType.FOOD && ant.carryUnits > carry_threshold && goHome(ant, action, ptc);
  }
  static boolean goHomeIfHurt( AntData ant, AntAction action, PacketToClient ptc, int health_threshold )
  {
    return ant.health < health_threshold && goHome(ant, action, ptc);
  }
  static boolean goHome( AntData ant, AntAction action, PacketToClient ptc )
  {
    int nest_y = ptc.nestData[ptc.myNest.ordinal()].centerY;
    int nest_x = ptc.nestData[ptc.myNest.ordinal()].centerX;

    PathNode antSpot = A_Star.board[ant.gridX][ant.gridY];
    PathNode nestSpot = A_Star.board[nest_x][nest_y];

    Map<PathNode,PathNode> path = A_Star.getPath( nestSpot, antSpot, ptc, ant.id);
    return moveAlongPath( ant, action, path );
  }
  static boolean moveAlongPath( AntData ant, AntAction action, Map<PathNode,PathNode> path)
  {
    Direction dir;
    PathNode antSpot = A_Star.board[ant.gridX][ant.gridY];
    PathNode nextStep = path.get(antSpot);

    if((ant.gridY-1 == nextStep.y)&& (ant.gridX == nextStep.x)) { dir = Direction.NORTH; }
    else if((ant.gridY-1 == nextStep.y)&& (ant.gridX+1 == nextStep.x)) { dir = Direction.NORTHEAST; }
    else if((ant.gridY == nextStep.y)&& (ant.gridX+1 == nextStep.x)) { dir = Direction.EAST; }
    else if ((ant.gridY+1 == nextStep.y)&& (ant.gridX+1 == nextStep.x)) { dir = Direction.SOUTHEAST; }
    else if ((ant.gridY+1 == nextStep.y)&& (ant.gridX == nextStep.x)) { dir = Direction.SOUTH; }
    else if ((ant.gridY+1 == nextStep.y)&& (ant.gridX-1 == nextStep.x)) { dir = Direction.SOUTHWEST; }
    else if ((ant.gridY-1 == nextStep.y)&& (ant.gridX-1 == nextStep.x)) { dir = Direction.NORTHWEST; }
    else { dir = Direction.WEST; }

    action.type = AntAction.AntActionType.MOVE;
    action.direction = dir;
    return true;
  }
  static boolean enterNest( AntData ant, AntAction action, PacketToClient ptc )
  {
    int nestY = ptc.nestData[ptc.myNest.ordinal()].centerY;
    int nestX = ptc.nestData[ptc.myNest.ordinal()].centerX;

    if( (Math.abs(ant.gridX-nestX)+Math.abs(ant.gridY-nestY) < 15)&& ant.carryUnits !=0)
    {
      action.direction = null;
      action.type = AntAction.AntActionType.ENTER_NEST;
      action.quantity = ant.carryUnits;
      return true;
    }
    return false;
  }

}
