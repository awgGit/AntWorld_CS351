package antworld.server;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.Timer;
import javax.swing.JFrame;
import javax.swing.JFileChooser;
import javax.imageio.ImageIO;

import antworld.common.AntAction;
import antworld.common.AntData;
import antworld.common.Constants;
import antworld.common.FoodData;
import antworld.common.GameObject;
import antworld.common.LandType;
import antworld.common.NestData;
import antworld.common.NestNameEnum;
import antworld.common.TeamNameEnum;
import antworld.common.Util;
import antworld.server.Nest.NestStatus;
import antworld.renderer.DataViewer;
import antworld.renderer.Renderer;

public class AntWorld implements ActionListener
{
  private static final boolean DEBUG = true;
  public static Random random = Constants.random;
  public static final int FRAME_WIDTH = 1200;
  public static final int FRAME_HEIGHT = 700;

  public final boolean showGUI;

  public static final String title = "AntWorld Version: " + Constants.VERSION;
  private Renderer drawPanel;
  private Timer gameTimer;
  private int gameTick = 0;
  private double gameTime;

  private final int worldWidth, worldHeight;
  private Cell[][] world;
  private ArrayList<Nest> nestList = new ArrayList<>();
  private Server server;
  private DataViewer dataViewer;

  private ArrayList<FoodSpawnSite> foodSpawnList;

  public ArrayList<FoodSpawnSite> getFoodSpawnList() {return foodSpawnList;}
  public ArrayList<Nest> getNestList() {return nestList;}

  public AntWorld(boolean showGUI)
  {
    this.showGUI = showGUI;
    System.out.println(title);

    JFrame window = null;
    if (showGUI)
    { drawPanel = new Renderer(this, title, FRAME_WIDTH, FRAME_HEIGHT);
      window = drawPanel.window;
    }

    //********************* Note On map replacement  **************************
    //The map must have at least a one pixel a boarder of water: LandType.WATER.getColor.
    BufferedImage map = Util.loadImage("resources/AntWorld.png", window);
    worldWidth = map.getWidth();
    worldHeight = map.getHeight();


    //smoothMap(map);
    //if (map!=null) System.exit(1);

    readAntWorld(map);


      foodSpawnList = new ArrayList<>();
      createFoodSpawnSite();
      //System.out.println("AntWorld.loadFoodSites()...."
      //  + foodSpawnList.size());

    System.out.println("World: " + worldWidth + " x " + worldHeight);

    for (Nest nest : nestList)
    {
      int x0 = nest.centerX;
      int y0 = nest.centerY;

      System.out.println(nest.nestName + ": " + x0+","+y0);

      for (int x = x0 - Constants.NEST_RADIUS; x <= x0
        + Constants.NEST_RADIUS; x++)
      {
        for (int y = y0 - Constants.NEST_RADIUS; y <= y0
          + Constants.NEST_RADIUS; y++)
        {
          if (nest.isNearNest(x, y))
          {
            world[x][y].setNest(nest);
          }
        }
      }
    }


    if (showGUI)
    {
      drawPanel.initWorld(world, worldWidth, worldHeight);
      drawPanel.repaint();
    }

    gameTimer = new Timer(Constants.TIME_STEP_MSEC, this);

    System.out.println("Done Initializing AntWorld");
    server = new Server(this, nestList);
    if (showGUI)
    {
      dataViewer = new DataViewer(nestList);
    }
    gameTimer.start();
    server.start();
  }



  /**
   * Advance simulation one gameTick
   * @param e ignored since the only event is the timer.
   */
  public void actionPerformed(ActionEvent e)
  {
    gameTick++;
    gameTime = server.getContinuousTime();

    if (DEBUG) System.out.println("AntWorld =====> Tick=" + gameTick + " (" + gameTime + ")");

    if (random.nextDouble() < 0.9) // AWG: Changed to 0.9 so it happens frequently...
    {
      int foodSiteIdx = random.nextInt(foodSpawnList.size());
      foodSpawnList.get(foodSiteIdx).spawn(this);
    }

    //Update non-interactive events of all ants in world. Non-interactive events include:
    //    Attrition damage
    //    Removing ants that died last tick from ant sets.
    //    Decrementing move/busy time.
    //    Changing last turn's actions to default NOOP
    for (Nest myNest : nestList)
    {
      if (myNest.getStatus() == NestStatus.EMPTY) continue;
      if (myNest.getStatus() == NestStatus.UNDERGROUND) continue;
      myNest.updateAutomaticAntEvents();
    }


    //This loop applies client ant action requests. Note: no changes in this loop are made to
    //   ants belonging to a client that the client does not include in their return package nor
    //   for which a valid action is not provided.
    //TODO: visit in receive order
    for (Nest myNest : nestList)
    {
      if (myNest.getStatus() == NestStatus.EMPTY) continue;
      if (myNest.getStatus() == NestStatus.UNDERGROUND) continue;

      if (gameTime > myNest.getTimeOfLastMessageFromClient() + Server.TIMEOUT_CLIENT_TO_UNDERGROUND)
      {
        myNest.sendAllAntsUnderground(this);
        continue;
      }
      if (myNest.getStatus() != NestStatus.CONNECTED) continue;

      myNest.updateReceivePacket(this);
    }

    //Newly dead ants are left for one turn in the ant list, but in the world,
    //  they are immediately replaced with food piles.
    for (Nest myNest : nestList)
    {
      myNest.calculateScore();
      myNest.updateRemoveDeadAntsFromWorld(this);
    }

    //The server's Nest array is of Nest objects (which extend NestData items).
    //This copies, by value, all data from the server's array to the array sent to clients.
    NestData[] nestDataList = buildNestDataList();



    //Build and mark as ready, PacketToClient for each client.
    // Note: the actual sending of this packet is done in each client's worker thread.
    for (Nest myNest : nestList)
    {
      myNest.updateSendPacket(this, nestDataList);
    }

    //Update display, if not headless.
    if (drawPanel != null)
    { drawPanel.update();
      dataViewer.update(nestList);
    }
  }

  /*
  public NestData[] createNestDataList()
  {
    NestData[] nestDataList = new NestData[nestList.size()];
    for (int i = 0; i < nestList.size(); i++)
    {
      Nest nest = nestList.get(i);
      nestDataList[i] = nest.createNestData();
    }
    return nestDataList;
  }

*/

  public Nest getNest(NestNameEnum name)
  {
    return nestList.get(name.ordinal());
  }
  public Nest getNest(TeamNameEnum name)
  {
    for (Nest nest : nestList)
    {
      if (nest.team == name) return nest;
    }
    return null;
  }

  /**
   * gameTime is the time in seconds from the start of the game to the start of the current gameTick.
   * @return time in seconds
   */
  public double getGameTime() {return gameTime;}
  public int getGameTick()
  {
    return gameTick;
  }


  /**
   * Uses the given map image to create the world including world size, nest
   * locations and all terrain.
   * @param map Must have the following properties:
   * <ol>
   *     <li>The map must have at least a one pixel a boarder of water:
   *              LandType.WATER.getColor</li>
   *
   *     <li>The map must have at least one pixel of LandType.to define the nest.</li>
   *     <li>Each nest (pixel with 0x0 color) must be at least 2xNEST_RADIUS distant from each other nest.</li>
   *     <li>Map images must be resized using nearest neighbor NOT any type of interpolation or
   *            averaging which will create shades that are undefined.</li>
   *     <li>Map images must be saved in a lossless format (i.e. png).</li>
   * </ol>
   */
  private void readAntWorld(BufferedImage map)
  {
    world = new Cell[worldWidth][worldHeight];
    int nestCount = 0;
    for (int x = 0; x < worldWidth; x++)
    {
      for (int y = 0; y < worldHeight; y++)
      {
        int rgb = (map.getRGB(x, y) & 0x00FFFFFF);
        LandType landType;
        int height = 0;
        if (rgb == 0x0)
        {
          landType = LandType.NEST;
          NestNameEnum nestName = NestNameEnum.values()[nestCount];
          nestList.add(new Nest(nestName, x, y));
          nestCount++;
        }
        else if (rgb == 0xF0E68C)
        {
          landType = LandType.NEST;
        }
        else if (rgb == LandType.WATER.getMapColor())
        {
          landType = LandType.WATER;
        }
        else
        { landType = LandType.GRASS;
          height=LandType.getMapHeight(rgb);
        }
        world[x][y] = new Cell(landType, height, x, y);
      }
    }
  }





  public Cell getCell(int x, int y)
  {
    if (x < 0 || y < 0 || x >= worldWidth || y >= worldHeight)
    {
      // System.out.println("AntWorld().getCell(" + x + ", " + y +
      // ") worldWidth=" + worldWidth + ", worldHeight="
      // + worldHeight);
      return null;
    }
    return world[x][y];
  }


  public void addAnt(AntData ant)
  {
    if (ant.state != AntAction.AntState.OUT_AND_ABOUT) return;
    int x = ant.gridX;
    int y = ant.gridY;

    world[x][y].setGameObject(ant);

    if (drawPanel != null) drawPanel.drawCell(world[x][y]);
  }

  public void addFood(FoodSpawnSite foodSpawnSite, FoodData food)
  {
    int x = food.gridX;
    int y = food.gridY;

    world[x][y].setGameObject(food);

    if (drawPanel != null) drawPanel.drawCell(world[x][y]);
  }

  public void removeGameObject(GameObject obj)
  {
    if (obj == null) return;
    int x = obj.gridX;
    int y = obj.gridY;

    world[x][y].setGameObject(null);
    if (drawPanel != null) drawPanel.drawCell(world[x][y]);
  }


  public void moveAnt(AntData ant, Cell from, Cell to)
  {


    from.setGameObject(null);
    to.setGameObject(ant);

    ant.gridX = to.getLocationX();
    ant.gridY = to.getLocationY();

    if (drawPanel != null)
    {  drawPanel.drawCell(from);
       drawPanel.drawCell(to);
    }

  }
/*
  public void appendAntsInProximity(AntData myAnt, HashSet<AntData> antSet)
  {
    double x = myAnt.gridX;
    double y = myAnt.gridY;
    double radius = myAnt.antType.getVisionRadius();
    NestNameEnum nestExclude = myAnt.nestName;

      }
    }
  }
*/
  // public boolean isAntInProximity(double x, double y, double radius)
  // {
  // for (int i = (int) Math.max(Math.floor(x / BLOCK_SIZE - radius /
  // BLOCK_SIZE), 0); i <= Math.min(
  // Math.ceil(x / BLOCK_SIZE + radius / BLOCK_SIZE), antBlocks.length - 1);
  // ++i)
  // {
  // for (int j = (int) Math.max(Math.floor(y / BLOCK_SIZE - radius /
  // BLOCK_SIZE), 0); j <= Math.min(
  // Math.ceil(y / BLOCK_SIZE + radius / BLOCK_SIZE), antBlocks[i].length -
  // 1); ++j)
  // {
  // for (AntData ant : antBlocks[i][j])
  // {
  //
  // if (Util.manhattanDistance((int) x, (int) y, ant.gridX, ant.gridY) <=
  // radius) return true;
  // }
  // }
  // }
  // return false;
  // }
/*
  public void appendFoodInProximity(AntData myAnt, HashSet<FoodData> foodSet)
  {
    double x = myAnt.gridX;
    double y = myAnt.gridY;
    double radius = myAnt.antType.getVisionRadius();


    }
  }
*/





  private NestData[] buildNestDataList()
  {
    NestData[] nestDataList = new NestData[NestNameEnum.SIZE];
    for (int i=0; i<NestNameEnum.SIZE; i++)
    {
      Nest myNest = nestList.get(i);
      nestDataList[i] = new NestData(myNest);
    }
    return nestDataList;
  }

  private void createFoodSpawnSite()
  {
    int totalSitesToSpawn = 30 + random.nextInt(3); // AWG: Increased number of spawn sites
    int xRange = worldWidth/totalSitesToSpawn;
    while (totalSitesToSpawn > 0)
    {
      int spawnX = random.nextInt(xRange);
      spawnX = spawnX + (totalSitesToSpawn-1)*xRange;
      int spawnY = random.nextInt(worldHeight);

      if (world[spawnX][spawnY].getLandType() == LandType.GRASS)
      {
        foodSpawnList.add(new FoodSpawnSite(spawnX, spawnY, nestList.size()));
        //System.out.println("FoodSpawnSite: [ " + spawnX + ", " + spawnY + "] " + foodType);
        totalSitesToSpawn--;
      }
    }
  }




  /**
   * This is a tool used to smooth the world map before being used in the simulation.
   * This method will never be called during a game.
   * @param map
   */
  private void smoothMap(BufferedImage map)
  {
    int[] DX={0,-1,0,1};
    int[] DY={-1,0,1,0};

    int n = worldWidth*worldHeight*22;
    for (int i = 0; i < n; i++)
    {
      int x = Constants.random.nextInt(worldWidth-3)+1;
      int y = Constants.random.nextInt(worldHeight-3)+1;

      int rgb0 = (map.getRGB(x, y) & 0x00FFFFFF);
      if (rgb0 == LandType.WATER.getMapColor()) continue;

      int r = (rgb0 & 0x00FF0000) >> 16;
      int g = (rgb0 & 0x0000FF00) >> 8;
      int b = rgb0 & 0x000000FF;

      int dist = 1;
      if (i < 10)
      { dist = Constants.random.nextInt(12)+Constants.random.nextInt(12)+1;
      }

      int count = 1;
      for (int k=0; k<DX.length; k++)
      {
        int xx = x+DX[k]*dist;
        int yy = y+DY[k]*dist;

        if (xx < 1) xx = 1;
        if (yy < 1) yy = 1;
        if (xx > worldWidth-3)  xx = worldWidth-3;
        if (yy > worldHeight-3) yy = worldHeight-3;

        int rgb = (map.getRGB(xx, yy) & 0x00FFFFFF);
        if (rgb == LandType.WATER.getMapColor()) rgb = rgb0;

        count++;
        r += (rgb & 0x00FF0000) >> 16;
        g += (rgb & 0x0000FF00) >> 8;
        b += rgb & 0x000000FF;

      }
      r /= count;
      g /= count;
      b /= count;
      rgb0 = (r<<16) | (g<<8) | b;
      map.setRGB(x, y, rgb0);

    }

    JFileChooser fileChooser = new JFileChooser();

    int returnValue = fileChooser.showSaveDialog(null);

    if (returnValue != JFileChooser.APPROVE_OPTION) return;

    File inputFile = fileChooser.getSelectedFile();
    String path = inputFile.getAbsolutePath();
    if ((path.endsWith(".png") == false) && (path.endsWith(".PNG") == false))
    { path = path+".png";
    }

    File myFile = new File(path);
    try
    { ImageIO.write(map, "png", myFile);
    }
    catch (Exception e){ e.printStackTrace();}
  }


  /**
   * Main entry point of the AntWorld Server.
   * @param args Specify to run with GUI (default) or headless (-nogui).
   */
  public static void main(String[] args)
  {
    boolean showGUI = true;
    if (args != null && args.length > 0)
    {
      for (String field : args)
      {
        if (field.equals("-nogui"))  showGUI = false;
      }
    }
    new AntWorld(showGUI);
  }
}
