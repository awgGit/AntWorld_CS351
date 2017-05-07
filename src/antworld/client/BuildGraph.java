package antworld.client;

import antworld.common.Util;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;

public class BuildGraph
{
  // Read in the image.
  private static BufferedImage loadedImage;
  static
  {
    try
    {
      URL fileURL = Util.class.getClassLoader().getResource("resources/AntWorld.png");
      loadedImage = ImageIO.read(fileURL);
    }
    catch( Exception e )
    {
      System.out.println(e);
    }
  }

  static int resolution = 20;
  public static GraphNode[][] graphNodes = new GraphNode[2500][1500];
  public static double[][] densityMap = new double[2500/resolution][1500/resolution];

  public BuildGraph()
  {

    // Build a density map to figure out where to make the map fine grained or poor grained.
    int count;
    for(int x = 0; x < 2500/resolution - 1; x++)
    {
      for(int y = 0; y < 1500/resolution - 1; y++)
      {
        count = 0;
        for(int i = 0; i < resolution; i++)
        {
          for(int j = 0; j < resolution; j++)
          {
            //System.out.println(x*resolution+i + " " + y*resolution+j );
            if ((loadedImage.getRGB(x*resolution+i,y*resolution+j) & 0xff) != 255)
            {
            }
            else { count++; }
          }
        }
        //densityMap[x][y] = ((double) count) / ((double) (resolution*resolution));
        densityMap[x][y] = Math.sqrt( ((double) count ) / ((double) (resolution*resolution)));
      }
    }

    // Not being used at the moment
    //<editor-fold desc="Variable Density Method">
    /*
    double chance_of_spawning;
    for(int x = 0; x < 2500; x++)
    {
      for(int y = 0; y < 1500; y++)
      {
        // If this pixel is a nest, assure it a node on the graph.
        if((loadedImage.getRGB(x,y) & 0x00FFFFFF) == 0)
        {
          System.out.println("Nest at: " + x + " " + y);
          graphNodes[x][y] = new GraphNode(x,y);
          continue;
        }
        if ((loadedImage.getRGB(x,y) & 0xff) == 255) continue;
        if(densityMap[(int) Math.floor(x/resolution)][(int) Math.floor(y/resolution)] == 1) continue;

        chance_of_spawning = 0.002 + densityMap[x/resolution][y/resolution];

        // Higher probability of spawning if there is not much land.
        if( chance_of_spawning > Constants.random.nextDouble())
        {
          graphNodes[x][y] = new GraphNode(x,y);
        }
      }
    }

    // Find nearest neighbors.
    int distance_threshold, distance;
    int start_x2, start_y2, end_x2, end_y2;
    for(int x1 = 0; x1 < graphNodes.length; x1++)
    {
      for(int y1 = 0; y1 < graphNodes[0].length; y1++)
      {
        if(graphNodes[x1][y1] == null) continue;
        chance_of_spawning = 0.002 + densityMap[x1/resolution][y1/resolution];
        distance_threshold = (int) (resolution + ((1-chance_of_spawning) * resolution * 5));

        // If this is the location of a nest, give it a huge distance threshold for finding connections.
        if((loadedImage.getRGB(x1,y1) & 0x00FFFFFF) == 0) { distance_threshold = 100; }

        // Find the start and finish x2 and y2 positions so that we only search for nodes close to us.
        // This should greatly cut down on the processing time involved in finding nearest neighbors.
        start_x2 = x1-distance_threshold > 0? x1-distance_threshold : 0;
        start_y2 = y1-distance_threshold > 0? y1-distance_threshold : 0;
        end_x2 = x1+distance_threshold < graphNodes.length? x1+distance_threshold : graphNodes.length;
        end_y2 = y1+distance_threshold < graphNodes[0].length? y1+distance_threshold : graphNodes[0].length;

        // Loop through the nearby nodes, adding as neighbors if we can access them with a straight line;
        // that is, if the distance to water in the direction of the target node is less than the distance to the node
        // itself, then we mark that node as a neighbor.
        for( int x2 = start_x2; x2 < end_x2; x2++ )
        {
          for (int y2 = start_y2; y2 < end_y2; y2++)
          {
            if (graphNodes[x2][y2] == null) continue;
            distance = Math.abs(x1-x2)+Math.abs(y1-y2);
            if ( Raycasting.getDistanceToWaterUsingVector(graphNodes[x1][y1].x, graphNodes[x1][y1].y, graphNodes[x2][y2].x, graphNodes[x2][y2].y) >= distance)
            {
              graphNodes[x1][y1].neighbors.add(graphNodes[x2][y2]);
            }
          }
        }
      }
      System.out.println("x:" + x1);
    }*/
    //</editor-fold>

    // Seeing as A* is now fixed, this is more viable.
    // Still can be slow under particular conditions (2-5 s for long searches)
    //<editor-fold desc="Uniform Density Method">
    for(int x = 0; x < 2500/resolution; x++)
    {
      for(int y = 0; y < 1500/resolution; y++)
      {
        if ((loadedImage.getRGB(x*resolution,y*resolution) & 0xff) == 255){}
        else { graphNodes[x*resolution][y*resolution] = new GraphNode(x*resolution,y*resolution); }
      }
    }

    // Find neighbors now, connecting with diagonals as the threshold.
    for(int x = 0; x < 2500/resolution; x++)
    {
      for(int y = 0; y < 1500/resolution; y++)
      {
        if( graphNodes[x*resolution][y*resolution] == null) continue;
        //Search nearby nodes for nearest neighbor.
        for(int a = -5; a < +5; a++) /// Was +/- 10
        {
          for( int b = -5; b < +5; b++) // Was +/- 10
          {
            if( (x+a) * resolution >= 2500  || (x+a) < 0) continue;
            if( (y+b)* resolution >= 1500 || (y+b) < 0 ) continue;
            if( graphNodes[(x+a)*resolution][(y+b)*resolution] == null) continue;

            // Experimental : (May need review once integrated with other code)
            // Raycast towards the other graph node - if there's water in the way, cancel the neighbor.
            int distance_to_water = Raycasting.getDistanceToWaterUsingVector( x*resolution, y*resolution, (x+a)*resolution, (y+b)*resolution );
            if( distance_to_water < (Math.abs(a*resolution) + Math.abs(b*resolution)) ) continue;

            graphNodes[x*resolution][y*resolution].neighbors.add(graphNodes[(x+a)*resolution][(y+b)*resolution]);
          }
        }
      }
    }

    for(int x = 0; x < 2500; x++)
    {
      for(int y = 0; y < 1500; y++)
      {
        // Create the nests as nodes.
        if((loadedImage.getRGB(x,y) & 0x00FFFFFF) == 0)
        {
          graphNodes[x][y] = new GraphNode(x, y);
          // Then connect them to the graph.
          for (int a = -resolution*3; a < +resolution*3; a++)
          {
            for (int b = -100; b < +100; b++)
            {
              if ((x + a) >= 2500 || (x + a) < 0) continue;
              if ((y + b) >= 1500 || (y + b) < 0) continue;
              if( graphNodes[x+a][y+b] == null) continue;

              // Experimental : (May need review once integrated with other code)
              // Raycast towards the other graph node - if there's water in the way, cancel the neighbor.
              int distance_to_water = Raycasting.getDistanceToWaterUsingVector( x*resolution, y*resolution, (x+a)*resolution, (y+b)*resolution );
              if( distance_to_water < (Math.abs(a*resolution) + Math.abs(b*resolution)) ) continue;

              graphNodes[x][y].neighbors.add(graphNodes[(x + a)][(y + b)]);
            }
          }
        }
      }
    }
    //</editor-fold>

    // Save image to a file.
    //drawImage();
  }

  // Debugging to see that things are working as intended.
  public void drawImage()
  {
    try
    {
      BufferedImage img = new BufferedImage(2500, 1500, BufferedImage.TYPE_INT_ARGB);
      Graphics2D ig2 = img.createGraphics();
      ig2.setColor(Color.BLACK);
      ig2.fillRect(0, 0, 2500, 1500);

      Color line_color = (new Color(255,255,255,20));

     //  Draw the connections map:
      for (GraphNode[] r_row : graphNodes)
      {
        for (GraphNode r : r_row)
        {
          if (r == null) continue;
          if (r.neighbors == null) continue;
          ig2.setColor( line_color );
          for (GraphNode n : r.neighbors)
          {
            if (n == null) continue;
            ig2.drawLine(r.x, r.y, n.x, n.y);
          }
          ig2.setColor(Color.RED);
          ig2.fillRect(r.x-1,r.y-1,2,2);
        }
      }

      System.out.println("Done making the image, now just trying to write it to a file.");
      ImageIO.write(img, "PNG", new File("resources/graph.png"));
      System.out.println("And now we're done writing to a file.");
    }
    catch( IOException ie )
    {
      ie.printStackTrace();
    }
  }


}
