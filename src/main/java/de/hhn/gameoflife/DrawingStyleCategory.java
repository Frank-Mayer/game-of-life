package de.hhn.gameoflife;

public enum DrawingStyleCategory {

  STILL_LIFES("Still Lives"),
  OSCILLATORS("Oscillators"),
  SPACESHIPS("Spaceships");




 private final String name;





  DrawingStyleCategory(String name){
    this.name = name;
  }

  public String getName(){
    return this.name;
  }

}


