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

import antworld.common.AntAction.AntState;
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
  private static final boolean DEBUG = false;
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
    BufferedImage map = Util.loadImage("AntWorld.png", window);
    worldWidth = map.getWidth();
    worldHeight = map.getHeight();


    //smoothMap(map);
    //if (map!=null) System.exit(1);

    readAntWorld(map);



    foodSpawnList = new ArrayList<>();
    createFoodSpawnSite(true);
    System.out.println("World: " + worldWidth + " x " + worldHeight);

    for (Nest nest : nestList)
    {
      int x0 = nest.centerX;
      int y0 = nest.centerY;

      if (DEBUG) System.out.println(nest.nestName + ": " + x0+","+y0);

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

    gameTimer = new Timer(Constants.TIME_STEP_MSEC, this);

    System.out.println("Done Initializing AntWorld");
    server = new Server(this, nestList);
    if (showGUI)
    {
      drawPanel.initWorld(world, worldWidth, worldHeight);
      drawPanel.repaint();
      dataViewer = new DataViewer(nestList);
    }

    for (FoodSpawnSite site : foodSpawnList)
    {
      site.spawn(this);
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

    if (random.nextDouble() < 0.005)
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
   *     <li>Map images must be resized using nearest neighbor NOT any objType of interpolation or
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
    if (ant.state != AntState.OUT_AND_ABOUT) return;
    int x = ant.gridX;
    int y = ant.gridY;

    world[x][y].setGameObject(ant);

    if (drawPanel != null) drawPanel.drawCell(world[x][y]);
  }

  public void addFood(FoodData food)
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


  public void appendVisibleObjects(AntData myAnt, ArrayList<AntData> antList, ArrayList<FoodData> foodList)
  {
    int radius = myAnt.antType.getVisionRadius();
    int xmin = Math.max(1,myAnt.gridX - radius);
    int ymin = Math.max(1,myAnt.gridY - radius);
    int xmax = Math.min(worldWidth-2,myAnt.gridX + radius);
    int ymax = Math.min(worldHeight-2, myAnt.gridY + radius);


    for (int y=ymin; y <= ymax; y++)
    {
      for (int x=xmin; x <= xmax; x++)
      {
        if (Util.manhattanDistance(x, y, myAnt.gridX, myAnt.gridY) > radius) continue;
        GameObject obj = world[x][y].getGameObject();
        if (obj == null) continue;
        if ((world[x][y].lookedAtNest == myAnt.nestName) && (world[x][y].lookedAtTick == gameTick)) continue;

        world[x][y].lookedAtNest = myAnt.nestName;
        world[x][y].lookedAtTick = gameTick;

        if (obj.objType == GameObject.GameObjectType.ANT)
        {
          AntData ant = (AntData) obj;
          if (ant.nestName == myAnt.nestName) continue;
          antList.add(ant);
        }
        else
        {
          foodList.add((FoodData) obj);
        }
      }
    }
  }






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

  private void createFoodSpawnSite(boolean spawnNearNests)
  {
    if (spawnNearNests)
    {
      for (Nest nest : nestList)
      {
        int x0 = nest.centerX + 25;
        int y0 = nest.centerY + 25;

        int x1 = nest.centerX - 25;
        int y1 = nest.centerY - 25;

        if (world[x0][y0].getLandType() == LandType.GRASS)
        {
          foodSpawnList.add(new FoodSpawnSite(this, x0, y0));
          System.out.println("FoodSpawnSite: [ " + x0 + ", " + y0 + "]");

        }
        if (world[x1][y1].getLandType() == LandType.GRASS)
        {
          foodSpawnList.add(new FoodSpawnSite(this, x1, y1));
          System.out.println("FoodSpawnSite: [ " + x1 + ", " + y1 + "]");
        }
      }
      return;
    }





    int totalSitesToSpawn = 3 + random.nextInt(4);
    if (spawnNearNests) totalSitesToSpawn = 30;

    //int xRange = worldWidth/totalSitesToSpawn;
    int minDistanceToNest = 150;
    int minDistanceToSpawnSite = 500;
    int attemptCount = 0;
    while (totalSitesToSpawn > 0)
    {
      attemptCount++;
      int spawnX = random.nextInt(worldWidth-4)+2;
      int spawnY = random.nextInt(worldHeight-4)+2;

      if (world[spawnX][spawnY].getLandType() != LandType.GRASS) continue;
      {
        boolean locationOK = true;
        for (Nest nest : nestList)
        {
          int x0 = nest.centerX;
          int y0 = nest.centerY;
          if ((Math.abs(x0-spawnX) < minDistanceToNest) && (Math.abs(y0-spawnY) < minDistanceToNest))
          {
            locationOK = false;
            break;
          }
        }
        if (!locationOK) continue;

        for (FoodSpawnSite existingSite : foodSpawnList)
        {
          int x0 = existingSite.getLocationX();
          int y0 = existingSite.getLocationY();
          if ((Math.abs(x0-spawnX) < minDistanceToSpawnSite) && (Math.abs(y0-spawnY) < minDistanceToSpawnSite))
          {
            locationOK = false;
            break;
          }
        }



        if (locationOK)
        { foodSpawnList.add(new FoodSpawnSite(this, spawnX, spawnY));
          System.out.println("FoodSpawnSite: [ " + spawnX + ", " + spawnY + "] attempts="+attemptCount);
          attemptCount = 0;
          totalSitesToSpawn--;
        }
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
