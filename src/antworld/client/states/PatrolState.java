package antworld.client.states;
import antworld.client.Patrols.Patrol;
import antworld.client.states.State;

/**
 * Created by ryanvary on 5/1/17.
 */
public class PatrolState extends State
{
  State state;
  Patrol patrol;

  PatrolState(State state, Patrol patrol)
  {
    this.state = state;
    this.patrol = patrol;
  }

  public void move(){}

  public boolean foodInSight(){ return false;}

  public boolean rejuvenateHealth() { return false; }

}
