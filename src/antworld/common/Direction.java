package antworld.common;


/**
 *  To check all directions around a location (x0, y0), use:<br>
 *     for (Direction dir : Direction.values())<br>
 *     {<br>
 *        int x = x0 + dir.deltaX();<br>
 *        int y = y0 + dir.deltaY();<br>
 *     }<br><br>
 *
 *  Most methods that expect a direction, if given null, will assume the current location is being
 *  indicated (deltaX and deltaY == 0).
 */


public enum Direction 
{ NORTH
  { public int deltaX() {return  0;}
    public int deltaY() {return -1;}
  }, 
  
  NORTHEAST
  { public int deltaX() {return  1;}
    public int deltaY() {return -1;}
  },
  
  EAST
  { public int deltaX() {return  1;}
    public int deltaY() {return  0;}
  },

  SOUTHEAST
  { public int deltaX() {return  1;}
    public int deltaY() {return  1;}
  },

  SOUTH
  { public int deltaX() {return  0;}
    public int deltaY() {return  1;}
  },
  
  SOUTHWEST
  { public int deltaX() {return -1;}
    public int deltaY() {return  1;}
  },
  
  WEST
  { public int deltaX() {return -1;}
    public int deltaY() {return  0;}
  },
 
  NORTHWEST
  { public int deltaX() {return -1;}
    public int deltaY() {return -1;}
  };
  

  public abstract int deltaX();
  public abstract int deltaY();
  public static final int SIZE = values().length;
  public static Direction getRandomDir() {return values()[Constants.random.nextInt(SIZE)];}
  public static Direction getLeftDir(Direction dir) {return values()[(dir.ordinal()+SIZE-1) % SIZE];}
  public static Direction getRightDir(Direction dir) {return values()[(dir.ordinal()+1) % SIZE];}
}
