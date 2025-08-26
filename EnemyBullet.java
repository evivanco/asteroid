import greenfoot.*;

/**
 * Bala enemiga (disparada por el UFO u otros enemigos).
 * - Movimiento rectilíneo, wrapping y TTL.
 * - Daña al PlayerShip al impacto (notifica al mundo).
 */
public class EnemyBullet extends Actor {

    private double x, y, vx, vy;
    private final int ttlFrames;
    private int age = 0;
    private final Actor owner;
    private GreenfootImage sprite;

    public EnemyBullet(double vx, double vy, int ttlFrames, Actor owner) {
        this.vx = vx;
        this.vy = vy;
        this.ttlFrames = ttlFrames > 0 ? ttlFrames : Integer.MAX_VALUE;
        this.owner = owner;
        buildSprite();
        setImage(sprite);
    }

    @Override
    protected void addedToWorld(World w) {
        this.x = getX();
        this.y = getY();
    }

    @Override
    public void act() {
        // Movimiento + wrapping
        x += vx;
        y += vy;
        wrapAround();
        setLocation((int)Math.round(x), (int)Math.round(y));

        // Colisión con el jugador
        Actor p = getOneIntersectingObject(PlayerShip.class);
        if (p != null) {
            AsteroidsWorld world = (AsteroidsWorld) getWorld();
            if (world != null) {
                world.loseLife();
            }
            if (getWorld() != null) getWorld().removeObject(this);
            
            if (p.getWorld() != null) { 
                Particles.spawnExplosion(getWorld(), p.getX(), p.getY(), 16);
                p.getWorld().removeObject(p); // PlayerShip se quita aquí
                Greenfoot.playSound("explode.wav"); // opcional
            }
            return;
            
        }

        // TTL
        age++;
        if (age >= ttlFrames && getWorld() != null) {
            getWorld().removeObject(this);
        }
    }

    private void wrapAround() {
        World w = getWorld();
        int W = w.getWidth(), H = w.getHeight();
        if (x < 0) x += W;
        if (x >= W) x -= W;
        if (y < 0) y += H;
        if (y >= H) y -= H;
    }

    private void buildSprite() {
        // Rojo tenue para diferenciar de la bala del jugador
        int r = 3;
        sprite = new GreenfootImage(2*r, 2*r);
        sprite.setColor(new Color(255, 80, 80));
        sprite.fillOval(0, 0, 2*r-1, 2*r-1);
    }

    public Actor getOwner() { return owner; }
}
