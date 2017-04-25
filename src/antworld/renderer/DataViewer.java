package antworld.renderer;


import java.awt.*;
//import java.awt.Font;
import java.util.ArrayList;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import antworld.common.GameObject;
import antworld.common.TeamNameEnum;
import antworld.server.AntWorld;
import antworld.server.Nest;
import antworld.server.Nest.NestStatus;

@SuppressWarnings("serial")
public class DataViewer extends JFrame
{
  //private Font myFont = new Font ("SansSerif", Font.PLAIN , 16);
  public static JTable table_nestList, table_FoodList;
  
  public DataViewer(ArrayList<Nest> nestList)
  {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (ClassNotFoundException | InstantiationException | UnsupportedLookAndFeelException | IllegalAccessException ignored) {
    }
    this.setTitle(AntWorld.title);

    this.setBounds(0, 0, 768, 500);
    this.setVisible(true);
    this.setResizable(true);
    this.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
    Container contentPane = this.getContentPane();
    // contentPane.setLayout(null);
    JTabbedPane tabbedPane = new JTabbedPane();
    contentPane.add(tabbedPane);

    JPanel panel_NestList = new JPanel();
    
    tabbedPane.addTab("Nest List", panel_NestList);
    int outsideWidth = tabbedPane.getWidth();
    int outsideHeight = tabbedPane.getHeight();
    Insets insets = tabbedPane.getInsets();
    int panelWidth = outsideWidth - insets.left - insets.right;
    int panelHeight = outsideHeight - insets.top - insets.bottom;
    
    
    String[] columnNames =  new String[7];
    columnNames[0] = "Nest";
    columnNames[1] = "Team";
    columnNames[2] = "Status";
    columnNames[3] = "Ant Count";
    columnNames[4] = "Food Count";
    columnNames[5] = "Water Count";
    columnNames[6] = "Score";

   
    DefaultTableModel model_table_nestList = new DefaultTableModel(null,columnNames);  
    
    
    table_nestList = new JTable(model_table_nestList) { //Automatically sizes the columns to match the content
      @Override
      public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component component = super.prepareRenderer(renderer, row, column);
        int rendererWidth = component.getPreferredSize().width;
        TableColumn tableColumn = getColumnModel().getColumn(column);
        tableColumn.setPreferredWidth(Math.max(rendererWidth + getIntercellSpacing().width, tableColumn.getPreferredWidth()));
        return component;
      }
    };
    table_nestList.setFont(table_nestList.getFont().deriveFont(24f));
    table_nestList.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    table_nestList.setRowHeight(table_nestList.getRowHeight()*2);

 
    //table_nestList.setPreferredScrollableViewportSize(new Dimension(panelWidth, panelHeight));
    
    JScrollPane scrollPane = new JScrollPane(table_nestList);
    
    //table_nestList.setFillsViewportHeight(true);
    panel_NestList.setLayout(new BorderLayout());
    panel_NestList.add(scrollPane);
    
   
    model_table_nestList.setRowCount(nestList.size());
    //tabbedPane.setSelectedIndex(0);
    //////////////=============== Ant List Table ============================
    JPanel panel_Details = new JPanel();
    tabbedPane.addTab("Ant List", panel_Details);
    
//    String[] columnAntList =  new String[5+FoodType.SIZE-1];
//    columnAntList[0] = "Nest";
//    columnAntList[1] = "Team";
//    columnAntList[2] = "Center X";
//    columnAntList[3] = "Center Y";
//    columnAntList[4] = "Ant Count";
//    int i = 5;
//    for (FoodType objType : FoodType.values())
//    { if (objType != FoodType.UNKNOWN)
//      { columnNames[i] = objType.name();
//        i++;
//      }
//    }
//   
//    DefaultTableModel model_table_nestList = new DefaultTableModel(null,columnNames);  
//    
//    
//    table_nestList = new JTable(model_table_nestList);
//
// 
//    table_nestList.setPreferredScrollableViewportSize(new Dimension(panelWidth, panelHeight));
//    
//    JScrollPane scrollPane = new JScrollPane(table_nestList);
//    
//    table_nestList.setFillsViewportHeight(true);
//    panel_NestList.setLayout(new BorderLayout());
//    panel_NestList.add(scrollPane);
//    
//
//    JPanel panel_Details = new JPanel();
//    tabbedPane.addTab("Ant List", panel_Details);
//    
//   
//    model_table_nestList.setRowCount(nestList.size());
//    table_nestList.selectAll();
    
    //////////////=============== Food In World ============================
    
    JPanel panel_Food = new JPanel();
    tabbedPane.addTab("Food List", panel_Food);
    scrollPane.getVerticalScrollBar().setValue(0);
    pack();
  }

  public void update(ArrayList<Nest> nestList)
  {
    for (int row = 0; row < nestList.size(); row++)
    {
      Nest nest = nestList.get(row);

      table_nestList.setValueAt(nest.nestName, row, 0);
      table_nestList.setValueAt(nest.team, row, 1);

      table_nestList.setValueAt(nest.getStatus().getAsFriendlyString(), row, 2);
      table_nestList.setValueAt(nest.getAntCount(), row, 3);
      table_nestList.setValueAt(nest.getFoodCount(), row, 4);
      table_nestList.setValueAt(nest.getWaterCount(), row, 5);
      table_nestList.setValueAt(nest.score, row, 6);

      // for (int x=0; x<columnNames.length; x++)
      // {
      // table_nestList.setValueAt(nestData[x][y], y, x);
      // table_nestList.getCellRenderer(y,
      // x).getTableCellRendererComponent(table_nestList, nestData[x][y], true,
      // true, y, x).setFont(myFont);
      // }
    }
  }

}
