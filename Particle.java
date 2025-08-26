import greenfoot.*;

/**
 * Partícula simple:
 * - Se mueve con velocidad inicial, aplica fricción leve y desvanece alpha.
 * - TTL en frames; al expirar se elimina.
 * - Wrapping para que no desaparezca de golpe en bordes.
 */
public class Particle extends Actor {

    private double x, y, vx, vy;
    private int ttl, age = 0;
    private double drag = 0.98;     // fricción leve
    private double spin;            // giro por frame
    private GreenfootImage base;

    /**
     * @param color  color inicial (incluye alpha)
     * @param radius radio del círculo (px)
     * @param vx     velocidad inicial X (px/frame)
     * @param vy     velocidad inicial Y (px/frame)
     * @param ttl    frames de vida
     * @param spin   grados de giro por frame (puede ser 0)
     */
    public Particle(Color color, int radius, double vx, double vy, int ttl, double spin) {
        this.vx = vx; this.vy = vy; this.ttl = Math.max(1, ttl); this.spin = spin;

        int d = Math.max(2, radius * 2);
        base = new GreenfootImage(d, d);
        base.setColor(color);
        base.fillOval(0, 0, d-1, d-1);
        // halo suave
        base.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 80));
        base.drawOval(0, 0, d-1, d-1);
        setImage(base);
    }

    @Override
    protected void addedToWorld(World w) {
        x = getX(); y = getY();
    }

    @Override
    public void act() {
        // física
        vx *= drag; vy *= drag;
        x += vx; y += vy;
        wrap();
        setLocation((int)Math.round(x), (int)Math.round(y));

        // giro (visual)
        if (spin != 0) setRotation(getRotation() + (int)Math.round(spin));

        // desvanecer alpha
        age++;
        int alpha = (int)Math.max(0, 255 * (1.0 - (double)age / ttl));
        getImage().setTransparency(alpha);

        if (age >= ttl) {
            if (getWorld() != null) getWorld().removeObject(this);
        }
    }

    private void wrap() {
        World w = getWorld();
        int W = w.getWidth(), H = w.getHeight();
        if (x < 0) x += W;
        if (x >= W) x -= W;
        if (y < 0) y += H;
        if (y >= H) y -= H;
    }
}

