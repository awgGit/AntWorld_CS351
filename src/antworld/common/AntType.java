package antworld.common;

import antworld.common.AntAction.AntState;
/**
 * This game supports 3 types of ants: WORKER, EXPLORER and WARRIOR.
 * Each type of ant overrides different default capability values. For example,
 * WORKER ants can carry more food or water units than other ants.
 */
public enum AntType
{

  WORKER
  {
    public int getCarryCapacity() {return super.getCarryCapacity()*2;}
    public int getHealWaterUnitsPerTick(AntState state)
    { return super.getHealWaterUnitsPerTick(state)*2;
    }
  },

  EXPLORER
    {
      public int getBaseMovementTicksPerCell()
      { return Math.max(1,super.getBaseMovementTicksPerCell()/3);
      }

      public int getVisionRadius() {return super.getVisionRadius()*2;}
    },

  WARRIOR
  {
    public int getMaxHealth() {return super.getMaxHealth()*2;}
    public int getAttackDiceD4() {return super.getAttackDiceD4()*2;}
  };

  public static final int SIZE = AntType.values().length;
  public static final int TOTAL_FOOD_UNITS_TO_SPAWN = 4;
  public static final int SCORE_PER_ANT = 3;
  public int getMaxHealth() {return 25;}

  /**
   * @return  The number of 4-sided dice rolled and summed to calculate the damage.
   * That is, the actual damage is the sum of <i>n</i> uniformly distributed random
   * numbers from 1 through 4.
   */
  public int getAttackDiceD4() {return 2;}

  /**
   * Each turn an ant is out and about it has a chance of taking attrition damage.
   * Ants that are underground (inside the nest) never take attrition damage.
   * @return the probability that each ant takes 1 point of damage each time step it spends
   * outside the nest. The average damage rate is 3 points of damage per minute.
   */
  public double getAttritionDamageProbability() {return 0.005;}

  /**
   * This function returns the base movement speed of a non-EXPLORER ant. <br>
   * When moving to cell at a higher elevation, this base is multiplied by getUpHillMultiplier().<br>
   * When an ant is encumbered (carrying more than half its capacity), this base is
   *   multiplied by getEncumbranceMultiplier().
   * @return number of game ticks required to move unencumbered ant on flat terrain.
   */
  public int getBaseMovementTicksPerCell() {return 2;}


  /**
   * Each land pixel on the map has an elevation defined by the green channel of its color.
   * When an ant moves to an adjacent pixel at a higher elevation than its current pixel its
   * movement cost is multiplied by the factor returned by this function.
   * @return multiplier to getBaseMovementTicksPerCell()
   */
  public int getUpHillMultiplier() {return 2;}


  /**
   * Each ant has a carrying capacity. Any ant carrying more than half its capacity
   * has a reduced movement rate.
   * @return multiplier to getBaseMovementTicksPerCell()
   */
  public int getEncumbranceMultiplier() {return 2;}


  /**
   * Fog of War is a major mechanic of this game. <br>
   * Each tick, the server notifies the client of food and enemy ants that are within
   * the vision radius of one or more of its ants.<br>
   * An ant with a vision radius of 10 will perceive food and enemy ants within a
   * 21x21 pixel square area that is centered on the ant.<br><br>
   *
   * If your nest has at least one ant underground, then you can perceive all game objects
   * (ants, food or water droplets) within your nest radius.
   * @return Ant perception distance on the map in pixels.
   */
  public int getVisionRadius() {return 10;}


  /**
   * An ant can carry food or water, but not both at the same time.
   * @return Maximum number of food or water units an ant can carry.
   */
  public int getCarryCapacity() {return 7;}

  /**
   * An ant can heal itself or an ant that is 8-direction adjacent.
   * Each unit of water consumed by an ant heals that ant by one health point up
   * to its max health.
   * @return the maximum number of water units on one tick that this ant can consume
   * to heal target ant.
   */
  public int getHealWaterUnitsPerTick(AntState state)
  {
    if (state == AntState.OUT_AND_ABOUT) return 1;
    if (state == AntState.UNDERGROUND) return 4;
    return 0; //the dead cannot heal.
  }


  /**
   * When an ant dies, it is removed from the game and replaced
   * by a food object.
   * The number of food units it is replaced by is equal to the sum of
   * the value returned by this function plus any food units it was carrying.
   * Any water the ant was carrying is lost.
   * @return Units of food that an ant becomes when it dies.
   */
  public static final int getDeadAntFoodUnits() {return 2;}
}
