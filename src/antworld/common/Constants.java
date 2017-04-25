package antworld.common;

import java.util.Random;

public class Constants
{
  public static final long VERSION = 20170422L;
  public static final int NEST_RADIUS = 15;

  public static final int INITIAL_FOOD_UNITS = 100*AntType.TOTAL_FOOD_UNITS_TO_SPAWN;
  public static final int INITIAL_NEST_WATER_UNITS = 100;
  public static final int TIME_STEP_MSEC = 40;
  public static final double NANO = 1e-9;
  

  
  public static Random random = new Random();
  
  public static final int PORT = 5555;
}

