package antworld.client;

import antworld.common.FoodData;
import java.util.ArrayList;

public class FoodPiles
{
  static ArrayList<FoodData> foodPiles = new ArrayList<>();
  public static void cullPiles ( )
  {
    ArrayList<FoodData> emptyFoodPiles = new ArrayList<>();
    for (FoodData foodPile : foodPiles) { if (foodPile.quantity == 0) emptyFoodPiles.add(foodPile); }
    foodPiles.removeAll( emptyFoodPiles );
  }
}
