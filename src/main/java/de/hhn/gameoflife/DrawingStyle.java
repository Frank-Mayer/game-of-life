package de.hhn.gameoflife;



public enum DrawingStyle {


  BLOCK("Block",DrawingStyleCategory.STILL_LIFES, new Boolean[][]{
      {false, false, false, false},
      {false, true, true, false},
      {false, true, true, false},
      {false, false, false, false}
  }),


  BEEHIVE("Beehive",DrawingStyleCategory.STILL_LIFES,new Boolean[][]{
    {false, false, false, false,false,false},
    {false, false, true, true,false,false},
    {false, true, false, false,true,false},
    {false, false, true, true,false,false},
    {false, false, false, false,false,false}
  }),

  LOAF("Loaf",DrawingStyleCategory.STILL_LIFES,new Boolean[][]{
      {false, false, false, false,false,false},
      {false, false, true, true,false,false},
      {false, true, false, false,true,false},
      {false, false, true, false,true,false},
      {false, false, false, true,false,false},
      {false, false, false, false,false,false}
  }),

  BOAT("Boat",DrawingStyleCategory.STILL_LIFES,new Boolean[][]{
      {false, false, false, false, false},
      {false, true, true, false, false},
      {false, true, false, true, false},
      {false, false, true, false, false},
      {false, false, false, false, false}
  }),

  TUB("Tub",DrawingStyleCategory.STILL_LIFES,new Boolean[][]{
      {false, false, false, false, false},
      {false, false, true, false, false},
      {false, true, false, true, false},
      {false, false, true, false, false},
      {false, false, false, false, false}
  });





  private final Boolean [][] structure;
  private final String name;

  private final DrawingStyleCategory category;
  DrawingStyle(String name, DrawingStyleCategory category, Boolean [][] structure){
    this.structure = structure;
    this.name = name;
    this.category = category;

  }

  public Boolean[][] getStructure() {
    return this.structure;
  }

  public String getName() {
    return this.name;
  }

  public DrawingStyleCategory getCategory() {
    return this.category;
  }


}
