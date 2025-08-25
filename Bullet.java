import greenfoot.*; // Actor, World, GreenfootImage, Greenfoot
/**
 * Proyectil del jugador (o de otras entidades si se desea).
 * - Movimiento rectilíneo uniforme con posición subpíxel.
 * - Wrapping toroidal.
 * - TTL (time-to-live) y autodestrucción.
 * - Breve "muzzle grace" inicial para no colisionar con su dueño.
 *
 * Contratos con otras clases:
 *  - Asteroid: debe implementar public void onHitBy(Bullet b)
 *              (se encargará de dividirse/otorgar puntos y removerse).
 *  - El mundo es AsteroidsWorld, pero esta clase no depende de detalles del mundo.
 */
public class Bullet extends Actor {

    // ----- Estado dinámico (dobles para precisión) -----
    private double x, y;
    private double vx, vy;

    // ----- Ciclo de vida -----
    private final int ttlFrames;     // vida útil en frames (~72 ≈ 1.2s @60FPS)
    private int ageFrames = 0;
    private final int muzzleGrace = 6; // frames de gracia para no golpear al owner

    // ----- Propiedad -----
    private final Actor owner;       // quien disparó (para ignorar colisión inicial)

    // ----- Visual -----
    private GreenfootImage sprite;

    /**
     * @param vx px/frame en X
     * @param vy px/frame en Y
     * @param ttlFrames frames de vida (si <=0, no expira)
     * @param owner referencia al actor que disparó (puede ser null)
     */
    public Bullet(double vx, double vy, int ttlFrames, Actor owner) {
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
        // 1) Avance físico
        x += vx;
        y += vy;
        wrapAround();
        setLocation((int)Math.round(x), (int)Math.round(y));

        // 2) Colisiones (tras breve gracia)
        if (ageFrames > muzzleGrace) {
            Actor a = getOneIntersectingObject(Asteroid.class);
            if (a != null) {
                // Delegar la lógica de destrucción/puntaje al asteroide
                ((Asteroid)a).onHitBy(this);
                // Remover la bala (si aún existe en el mundo)
                if (getWorld() != null) getWorld().removeObject(this);
                return;
            }
        }

        // 3) TTL
        ageFrames++;
        if (ageFrames >= ttlFrames) {
            if (getWorld() != null) getWorld().removeObject(this);
        }
    }

    /* =================== Utilidades =================== */

    private void wrapAround() {
        World w = getWorld();
        if (w == null) return;
        int W = w.getWidth();
        int H = w.getHeight();

        boolean wrapped = false;
        if (x < 0)      { x += W; wrapped = true; }
        if (x >= W)     { x -= W; wrapped = true; }
        if (y < 0)      { y += H; wrapped = true; }
        if (y >= H)     { y -= H; wrapped = true; }

        if (wrapped) setLocation((int)Math.round(x), (int)Math.round(y));
    }

    private void buildSprite() {
        // Pequeño óvalo brillante
        int r = 4;
        sprite = new GreenfootImage(2*r, 2*r);
        sprite.setColor(new Color(255, 255, 255));
        sprite.fillOval(0, 0, 2*r-1, 2*r-1);
        // Halo leve
        sprite.setColor(new Color(255, 255, 255, 120));
        sprite.drawOval(0, 0, 2*r-1, 2*r-1);
    }

    /* =================== Getters útiles =================== */
    public double getVX() { return vx; }
    public double getVY() { return vy; }
    public Actor getOwner() { return owner; }
}
