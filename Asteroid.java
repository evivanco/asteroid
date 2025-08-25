import greenfoot.*;  // (World, Actor, GreenfootImage, Greenfoot and MouseInfo)
enum Size {
  SMALL,
  MEDIUM,
  LARGE
}

public class Asteroid extends Actor
{
    private Size size;
    public void act()
    {
        // Add your action code here.
    }
    public Asteroid(Size size){
        this.size = size;
    }
}
