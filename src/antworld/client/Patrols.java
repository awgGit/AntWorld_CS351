package antworld.client;
import antworld.common.AntData;
import antworld.common.Direction;
import antworld.common.NestData;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ryanvary on 4/25/17.
 */
public class Patrols
{
  public ArrayList<AntData> ants;
  private int antsInPatrol;
  protected ArrayList<Patrol> patrols;
  private int spawnOffset = 5;

  private NestData nest;

  // Patrols object consists of a list of patrols which consist of a list of ant IDs.
  public Patrols(int numAnts, int antsInPatrol, ArrayList<AntData> patrolAnts, NestData nest)
  {
    this.antsInPatrol = antsInPatrol;
    this.nest = nest;
    patrols = new ArrayList<>();
    ants = new ArrayList(numAnts);

    setPatrolAnts(patrolAnts);
    createPatrols(numAnts, antsInPatrol);
  }

  // why have this as its own function?
  public void setPatrolAnts(ArrayList<AntData> patrolAnts)
  {
    for(AntData ant : patrolAnts) ants.add(ant);
  }

  // Divide ants into patrols, set their exit positions.
  private void createPatrols(int numAnts, int antsInPatrol)
  {
    ArrayList<AntData> assignAntsToPatrol;
    for (int j = 0; j < (numAnts / (antsInPatrol)); j++)
    {
      patrols.add(j, new Patrol(j, antsInPatrol));
      setPatrolNestExitPositions(patrols.get(j), spawnOffset);
      assignAntsToPatrol = new ArrayList<>(ants.subList(0, antsInPatrol));
      patrols.get(j).setPatrolAnts(assignAntsToPatrol);
      ants.subList(0, antsInPatrol).clear();
    }
    assignPatrolHeadings();
  }

  // make patrols radiate outwards
  public void assignPatrolHeadings()
  {
    int cnt;
    for(Patrol p : patrols)
    {
      cnt = 0;
      p.setHeading(Direction.values()[2*p.patrolNumber]);
      for(AntData ant : p.antPatrol)
      {
        ant.action.direction = p.heading;
        ant.gridX = p.spawnPositions[cnt][0];
        ant.gridY = p.spawnPositions[cnt][1];
        cnt++;
      }
      p.associateAntWithPosition();
    }
  }

  // Set position for anchor and in patrol
  private void setPatrolNestExitPositions(Patrol patrol, int offsetFromNestCenter)
  {
    double angle, phi;
    phi = patrol.patrolNumber*90;
    angle = (90 - phi)*(Math.PI/180);
    ArrayList<int[]> shifts = setAntPositionsInPatrol(phi);

    for(int j = 0; j < antsInPatrol; j++)
    {
      patrol.spawnPositions[j][0] = (int) (Math.round(offsetFromNestCenter*Math.cos(angle)) +
              shifts.get(j)[0]);
      patrol.spawnPositions[j][1] = (int) (Math.round(offsetFromNestCenter*Math.sin(angle - Math.PI)) +
              shifts.get(j)[1]);
    }
  }

  // Set the position of each ant in a patrol relative to the anchor ant
  private ArrayList<int[]> setAntPositionsInPatrol(double rotationAngle)
  {
    ArrayList<int[]> shifts = new ArrayList();
    int[] rotatedPosition;
    for(Positions p : Positions.values())
    {
      ArrayList<int[]> pShifts = p.values()[0].shifts;
      for(int j = 0; j < pShifts.size(); j++)
      {
        rotatedPosition = rotatePatrols(rotationAngle, pShifts.get(j));
        shifts.add(rotatedPosition);
      }
    }
    return shifts;
  }

  // define the shape of the patrol
  private enum Positions
  {
    //Triangle Formation
    /*
    Head (0,-1),
    FrontLine(-1,0,0, 0, 1, 0),
    SecondLine(-2,1,-1, 1, 0, 1, 1, 1, 2,1);
    */

    //Defensive Formation
    /*
    Head (0,-1),
    FrontLine(-1,0,0, 0, 1, 0),
    SecondLine(-2,1,-1, 1, 0, 1, 1, 1, 2,1),
    ThirdLine(-1, 2, 0 , 2, 1, 2),
    Tail(0,3);
    */

    //Queue
    Queue(0, 0, 0, 1, 0, 2, 0, 3, 0, 4, 0, 5, 0, 6, 0, 7, 0, 8);

    ArrayList<int[]> shifts = new ArrayList();
    int[] pos;
    Positions(int...args)
    {
      shifts.clear();
      for(int j = 0; j < args.length/2; j++)
      {
        pos = new int[2];
        pos[0] = args[j*2];
        pos[1] = args[j*2 + 1];
        shifts.add(j, pos);
      }
    }
  }

  // rorate about the anchor ant
  private int[] rotatePatrols(double angle, int[] pos)
  {
    int[] transformedPositions = new int[2];
    angle = angle*(Math.PI/180);
    transformedPositions[0] = (int)(Math.round(pos[0]*Math.cos(angle) + pos[1]*Math.sin(angle)));
    transformedPositions[1] = (int)(Math.round(-pos[0]*Math.sin(angle) + pos[1]*Math.cos(angle)));
    return transformedPositions;
  }

  public int getNumberDeadAnts()
  {
    int deadAntCnt = 0;
    for (Patrol patrol : patrols)
    {
      deadAntCnt += patrol.numDeadPatrolAnts();
    }
    return deadAntCnt;
  }

  public void replaceDeadAntsInPatrols(ArrayList<AntData> enlistedAnts)
  {
    ArrayList<AntData> recruitedAnts = new ArrayList();
    for(Patrol patrol : patrols)
    {
      for(int j = 0; j < patrol.numDeadAnts; j++)
      {
        recruitedAnts.add(enlistedAnts.get(0));
        enlistedAnts.remove(enlistedAnts.get(0));
      }
      patrol.replaceDeadAnts(recruitedAnts);
      recruitedAnts.clear();
    }
  }

  // AWG: how come this is in patrols.java?
  class Patrol
  {
    protected ArrayList<AntData> antPatrol;
    protected ArrayList<AntData> deadAntsInPatrol;
    protected int numDeadAnts = 0;
    protected int[][] spawnPositions;
    protected int patrolNumber;
    protected int head;
    protected int radius;

    private Direction heading;
    private State state;

    Patrol(int patrolNumber, int antsInPatrol)
    {
      this.patrolNumber = patrolNumber;
      antPatrol = new ArrayList<>();
      deadAntsInPatrol = new ArrayList();
      spawnPositions = new int[antsInPatrol][2];
      radius = 50;
    }

    // why not just do p.heading = dir?
    private void setHeading(Direction dir)
    {
      heading = dir;
    }

    // this depends on the orientation of the line of ants
    protected void associateAntWithPosition()
    {
      switch (heading)
      {
        case SOUTH:
        case NORTH:
          head = antPatrol.get(0).id;
          break;
        case EAST:
        case WEST:
          tailBecomesHead();
          break;
      }
    }

    private void tailBecomesHead()
    {
      ArrayList<AntData> ptr = new ArrayList<>();
      for(int j = 0; j < antsInPatrol; j++)
      {
        ptr.add(j,antPatrol.get(antsInPatrol-(1+j)));
      }
      head = ptr.get(0).id;
      antPatrol = ptr;
    }

    private void replaceDeadAnts(ArrayList<AntData> newRecruits)
    {
      int cnt = 0, index;
      for(AntData ant : deadAntsInPatrol)
      {
        index = antPatrol.indexOf(ant);
        newRecruits.get(cnt).action.direction = heading;
        newRecruits.get(cnt).gridX = spawnPositions[index][0];
        newRecruits.get(cnt).gridY = spawnPositions[index][1];
        antPatrol.set(index, newRecruits.get(cnt));
        cnt++;
      }
    }

    // AWG: why not just do antPatrol.addAll(ants)?
    // e.g. patrols.get(j).antPatrol.addAll(assignAntsToPatrol);
    private void setPatrolAnts(List<AntData> ants)
    {
      for(AntData ant: ants)
      {
        antPatrol.add(ant);
      }
    }

    private int numDeadPatrolAnts()
    {
      numDeadAnts = 0;
      for (AntData ant : antPatrol)
      {
        if(ant.health == 0)
        {
          deadAntsInPatrol.add(ant);
          numDeadAnts++;
        }
      }
      return numDeadAnts;
    }
  }
}