package antworld.renderer;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.*;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.*;

import antworld.common.AntData;
import antworld.server.AntWorld;
import antworld.server.Cell;
import antworld.server.FoodSpawnSite;
import antworld.server.Nest;
import antworld.common.AntAction.AntState;

public class Renderer extends JPanel implements KeyListener, MouseListener, MouseMotionListener,
    MouseWheelListener, ComponentListener, ActionListener
{

  /* +- GLOBAL VARS --------------------------------------------+ */

  private static final long serialVersionUID = 1L;
  private BufferedImage panel, world;
  public Graphics2D gfx;
  public JFrame window;
  
  private Timer animationTimer;

  public String windowTitle;
  private int windowWidth, windowHeight;

  private boolean mouseDragging = false;
  private double translateX = 0, translateY = 0;
  private double translateDX = 0, translateDY = 0;
  private double translateGX = 0, translateGY = 0;
  
  private double mouseX, mouseY, mouseDownX, mouseDownY;
  private final int MIN_REPAINT_MSEC = 100;
  private static final double ZOOM_IN_MAX = 8;
  private static final double ZOOM_OUT_MIN = 0.42;
  private static final Color[] COLOR_ANT = {new Color(160,0,0, 120), new Color(204, 85, 0, 120), new Color(218, 165, 32, 120)};
  private static final int ANT_PIXEL_SIZE = 16;
  
  private double scale = ZOOM_OUT_MIN, scaleG = ZOOM_OUT_MIN;
  
  private final Font fontNest = new Font ("SansSerif", Font.BOLD , 28);
  private final Font fontAnt = new Font ("SansSerif", Font.BOLD , 3);
  private FontMetrics fontMetrics;

  private static final Color PURPLE = new Color(120, 81, 169);
  
  //private ArrayList<Nest> nestList;
  private AntWorld antworld;


  /* +- INITIALIZATION -----------------------------------------+ */
  public Renderer(AntWorld antworld, String windowTitle, int windowWidth, int windowHeight)
  {
    this.antworld = antworld;
    this.windowTitle = windowTitle;
    this.windowWidth = windowWidth;
    this.windowHeight = windowHeight;

    addMouseListener(this);
    addMouseMotionListener(this);
    addMouseWheelListener(this);
    addKeyListener(this);
    addComponentListener(this);

    window = new JFrame(windowTitle);
    window.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
    window.setPreferredSize(new Dimension(windowWidth, windowHeight));
    window.setFocusable(true);
    window.add(this);
    window.pack();
    window.setLocationRelativeTo(null);
    window.setVisible(true);

    animationTimer = new Timer(MIN_REPAINT_MSEC, this);
  }

  public void initWorld(Cell[][] world, int worldWidth, int worldHeight)
  {
    this.world = new BufferedImage(worldWidth, worldHeight, BufferedImage.TYPE_INT_RGB);

    for (int x = 0; x < worldWidth; x++)
    {
      for (int y = 0; y < worldHeight; y++)
      {
        this.world.setRGB(x, y, world[x][y].getRGB());
      }
    }
    animationTimer.start();
  }

  /* +- CONTROLS/EVENTS ----------------------------------------+ */

  public void reshape(int width, int height)
  {
    windowWidth = width;
    windowHeight = height;
    panel = new BufferedImage(windowWidth, windowHeight, BufferedImage.TYPE_INT_ARGB);
    gfx = (Graphics2D) panel.getGraphics();
    gfx.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
    
    gfx.setFont(fontNest);
    fontMetrics = gfx.getFontMetrics(fontNest);

    // System.out.println("Renderer.reshape(): translate = ("+translateX+", "
    // +translateY+
    // "), translateD = ("+translateDX+", "
    // +translateDY+"), translateG = ("+translateGX+", " +translateGY+")");
  }

  @Override
  public void componentResized(ComponentEvent e)
  {
    Component c = e.getComponent();
    reshape(c.getSize().width, c.getSize().height);
  }
  
  
  @Override
  public void mouseMoved(MouseEvent e)
  {
    mouseX = e.getX();
    mouseY = e.getY();
  }
  

  @Override
  public void mousePressed(MouseEvent e)
  {
    if (e.getButton() == MouseEvent.BUTTON1)
    {
      mouseDragging = true;
      mouseDownX = e.getX();
      mouseDownY = e.getY();
      translateDX = translateGX;
      translateDY = translateGY;
    }
  }

  @Override
  public void mouseDragged(MouseEvent e)
  {

    if (mouseDragging)
    {
      translateGX = translateDX + e.getX() - mouseDownX;
      translateGY = translateDY + e.getY() - mouseDownY;
    }
    // System.out.println("Renderer.mouseDragged(): translateG = ("+translateGX+", "
    // +translateGY+")");
  }

  @Override
  public void mouseReleased(MouseEvent e)
  {
    if (e.getButton() == MouseEvent.BUTTON1) mouseDragging = false;
  }

  @Override
  public void mouseWheelMoved(MouseWheelEvent e)
  {
    scaleG *= e.getWheelRotation() == -1 ? 2 : 0.5;
    if (scaleG > ZOOM_IN_MAX)
    {
      scaleG = ZOOM_IN_MAX;
      return;
    }
    if (scaleG < ZOOM_OUT_MIN)
    {
      scaleG = ZOOM_OUT_MIN;
      return;
    }
    double zoomMult = e.getWheelRotation() == -1 ? 1 : -0.5;
    translateGX += (translateGX - e.getX()) * zoomMult;
    translateGY += (translateGY - e.getY()) * zoomMult;
    translateDX += (translateDX - e.getX()) * zoomMult;
    translateDY += (translateDY - e.getY()) * zoomMult;
  }

  @Override
  public void keyReleased(KeyEvent e)
  {}

  /* +- RENDER -------------------------------------------------+ */

  public void drawCell(Cell myCell)
  {

    int x = myCell.getLocationX();
    int y = myCell.getLocationY();
    this.world.setRGB(x, y, myCell.getRGB());
  }

  long lastRepaintTick = 0;

  @Override
  public void paintComponent(Graphics g)
  {
    g.drawImage(panel, 0, 0, this);
  }
  
  public void update()
  {
    
    if (gfx == null) return;
    long tick = System.currentTimeMillis();
    if (tick < lastRepaintTick + MIN_REPAINT_MSEC) return;
    int elapsed = (int) (tick - lastRepaintTick);
    lastRepaintTick = tick;
    
    window.setTitle(windowTitle + "  " + antworld.getGameTick());


    gfx.setTransform(new AffineTransform());
    gfx.setColor(new Color(0, 0, 0));
    gfx.fillRect(0, 0, windowWidth, windowHeight);

    // if(translateGX > windowWidth /2)translateGX = windowWidth /2;
    // if(translateGY > windowHeight/2)translateGY = windowHeight/2;
    // if(translateGX < -worldWidth *scaleG+windowWidth /2)translateGX =
    // -worldWidth *scaleG+windowWidth /2;
    // if(translateGY < -worldHeight*scaleG+windowHeight/2)translateGY =
    // -worldHeight*scaleG+windowHeight/2;
    translateX += (translateGX - translateX) * elapsed * 0.005;
    translateY += (translateGY - translateY) * elapsed * 0.005;
    scale += (scaleG - scale) * elapsed * 0.005;

    AffineTransform mat = new AffineTransform();
    mat.translate(translateX, translateY);
    mat.scale(scale, scale);
    gfx.setTransform(mat);
    gfx.drawImage(world, 0, 0, this);

    ArrayList<Nest> nestList = antworld.getNestList();
    if (nestList != null)
    { 
      int renderNestCount = -1;
      //mat.scale(1.0/scale, 1.0/scale);
      //gfx.setTransform(mat);
      
      double antScale = ANT_PIXEL_SIZE / scale;
      for (Nest nest: nestList)
      { 
        if (DataViewer.table_nestList.isRowSelected(nest.nestName.ordinal()))
        { 
          renderNestCount++;
          gfx.setColor(COLOR_ANT[renderNestCount % COLOR_ANT.length]);
          
          
          HashMap<Integer,AntData> antCollection = nest.getAnts();
          for (AntData ant : antCollection.values())
          {
            if (ant.state != AntState.OUT_AND_ABOUT) continue;
            double x = ant.gridX - (int) (antScale / 2);
            double y = ant.gridY - (int) (antScale / 2);
            Rectangle2D.Double shape = new Rectangle2D.Double(x, y, antScale, antScale);
            gfx.fill(shape);

            // int xx = (int)((mouseX - translateX)/scale);
            // int yy = (int)((mouseY - translateY)/scale);
            // if (nest.isNearNest(x,y))
            // int xx = (int)((ant.gridX*scale) + translateX);
            // int yy = (int)((ant.gridY*scale) + translateY);
            if (!mouseDragging && scale >= ZOOM_OUT_MIN/2)
            {
              int mouseXX = (int) ((mouseX - translateX) / scale);
              int mouseYY = (int) ((mouseY - translateY) / scale);
              if (mouseXX >= x && mouseXX <= x + antScale && mouseYY >= y && mouseYY <= y + antScale)
              {
                gfx.setColor(Color.BLACK);
                // int nameWidth =
                // fontMetrics.stringWidth(nest.nestName.toString());
                // int nameHeight = fontMetrics.getHeight();

                // int xx = (int)((nest.centerX*scale) + translateX) -
                // nameWidth/2;
                // int yy = (int)((nest.centerY*scale) + translateY) +
                // nameHeight/2;

                gfx.setFont(fontAnt);
                gfx.drawString(ant.toStringShort(), (int) x, (int) y);
                gfx.setColor(COLOR_ANT[renderNestCount % COLOR_ANT.length]);
              }
            }
            
          }
        }
      }





      //Render food
      gfx.setColor(PURPLE);
      double foodScale = ANT_PIXEL_SIZE / scale;
      ArrayList<FoodSpawnSite> foodSpawnList = antworld.getFoodSpawnList();
      for (FoodSpawnSite site : foodSpawnList)
      {

        double x = site.getLocationX() - (int) (foodScale / 2);
        double y = site.getLocationY() - (int) (foodScale / 2);
        Rectangle2D.Double shape = new Rectangle2D.Double(x, y, foodScale, foodScale);
        gfx.fill(shape);

        if (!mouseDragging && scale >= ZOOM_OUT_MIN/2)
        {
           int mouseXX = (int) ((mouseX - translateX) / scale);
           int mouseYY = (int) ((mouseY - translateY) / scale);
           if (mouseXX >= x && mouseXX <= x + foodScale && mouseYY >= y && mouseYY <= y + foodScale)
           {
              gfx.setColor(Color.BLACK);

              gfx.setFont(fontAnt);
              gfx.setColor(PURPLE);
            }
          }
        }



      
      if (!mouseDragging)
      { 
        int x = (int)((mouseX - translateX)/scale);
        int y = (int)((mouseY - translateY)/scale);
        for (Nest nest: nestList)
        { 
          //System.out.println("nest: ("+nest.centerX+", "+nest.centerY+")  mouse: ("+x+", "+y+")");
          if (nest.isNearNest(x,y))
          {
            gfx.setFont(fontNest);
            AffineTransform transform = new AffineTransform();
            gfx.setTransform(transform);
            gfx.setColor(Color.BLACK);
            
            int nameWidth = fontMetrics.stringWidth(nest.nestName.toString());
            int nameHeight = fontMetrics.getHeight();
            
            int xx = (int)((nest.centerX*scale) + translateX) - nameWidth/2;
            int yy = (int)((nest.centerY*scale) + translateY) + nameHeight/2;
            gfx.drawString(nest.nestName.toString(), xx, yy);
            break;
          }
        }
      }
    }
    repaint();
  }
  

  @Override
  public void actionPerformed(ActionEvent e)
  {
    // System.out.println("Renderer.actionPerformed((): translate = ("+translateX+", "
    // +translateY+
    // "), translateD = ("+translateDX+", "
    // +translateDY+"), translateG = ("+translateGX+", " +translateGY+")");
    update();
    repaint();
  }

  /* +- UNUSED EVENTS ------------------------------------------+ */

  @Override
  public void componentHidden(ComponentEvent e)
  {}

  @Override
  public void componentMoved(ComponentEvent e)
  {}

  @Override
  public void componentShown(ComponentEvent e)
  {}

  @Override
  public void mouseClicked(MouseEvent e)
  {}

  @Override
  public void mouseEntered(MouseEvent e)
  {}

  @Override
  public void mouseExited(MouseEvent e)
  {}



  @Override
  public void keyPressed(KeyEvent e)
  {}

  @Override
  public void keyTyped(KeyEvent e)
  {}
}
