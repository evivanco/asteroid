import greenfoot.*;   // Actor, World, GreenfootImage, Greenfoot
import java.util.List;
import java.util.Random;

/**
 * OVNI enemigo:
 * - Se desplaza lateralmente (L->R o R->L) con leve deriva vertical.
 * - Dispara hacia el Player con precisión configurable (ruido angular).
 * - TTL (desaparece tras un tiempo) y wrapping opcional.
 * - Muere si lo golpea una Bullet del jugador (otorga puntos).
 * - Si choca con un Asteroid, ambos se destruyen (sin puntos).
 *
 * Requiere:
 *  - AsteroidsWorld con rng() y addScore(int).
 *  - PlayerShip (para apuntar) y Bullet (del jugador).
 *  - EnemyBullet (definida más abajo) para golpear al jugador.
 */
public class UFO extends Actor {

    public static enum Type { LARGE, SMALL } // clásico: LARGE=200 pts, SMALL=1000 pts

    // ---- Parámetros de balance ----
    private static final int PTS_LARGE = 200;
    private static final int PTS_SMALL = 1000;

    private static final double SPEED_X = 3.0;     // px/frame horizontal
    private static final double DRIFT_Y = 0.8;     // deriva vertical máxima
    private static final int    TTL_FRAMES = 12 * 60; // 12 s a 60 FPS

    private static final int FIRE_MIN = 45;        // intervalo de disparo (frames)
    private static final int FIRE_MAX = 95;
    private static final double ENEMY_BULLET_SPEED = 7.0;
    private static final int    ENEMY_BULLET_TTL   = 120;

    // Precisión: 0.0=aleatorio, 1.0=perfecto. El ruido angular se escala con (1-acc).
    // Ruido base en grados: LARGE ~25°, SMALL ~8° (aprox)
    private static final double NOISE_DEG_LARGE = 25.0;
    private static final double NOISE_DEG_SMALL = 8.0;

    // ---- Estado dinámico ----
    private final Type type;
    private double accuracy;       // 0..1
    private int ttl = TTL_FRAMES;

    private double x, y;           // posición subpíxel
    private double vx, vy;         // velocidad
    private int fireCd = 0;        // cooldown de disparo

    // Direcciones y RNG
    private boolean leftToRight;   // true si entra por la izquierda
    private Random rng;

    public UFO(Type type, double accuracy) {
        this.type = type;
        this.accuracy = clamp01(accuracy);
        buildSprite();
    }

    @Override
    protected void addedToWorld(World w) {
        AsteroidsWorld world = (AsteroidsWorld) w;
        this.rng = world.rng();

        this.x = getX();
        this.y = getY();

        // Si nos añadieron sin fijar X, elegimos borde aleatorio y Y aleatoria
        if (x <= 0 || x >= w.getWidth()-1) {
            leftToRight = (rng.nextBoolean());
            this.x = leftToRight ? 1 : (w.getWidth()-2);
            this.y = 40 + rng.nextInt(w.getHeight() - 80);
            setLocation((int)Math.round(x), (int)Math.round(y));
        } else {
            leftToRight = (x < w.getWidth()/2);
        }

        vx = leftToRight ? SPEED_X : -SPEED_X;
        vy = rngRange(-DRIFT_Y, DRIFT_Y);

        // Primer disparo en un rango aleatorio inicial
        fireCd = rng.nextInt(FIRE_MAX - FIRE_MIN + 1) + FIRE_MIN;
    }

    @Override
    public void act() {
        // 1) Movimiento
        x += vx;
        y += vy;

        // Rebotito vertical sutil para no salir
        if (y < 20 || y > getWorld().getHeight() - 20) vy = -vy;

        // Wrapping horizontal opcional: si lo prefieres, elimínalo y usa TTL
        if (x < 0)      x += getWorld().getWidth();
        if (x >= getWorld().getWidth()) x -= getWorld().getWidth();

        setLocation((int)Math.round(x), (int)Math.round(y));

        // 2) Disparo
        if (fireCd > 0) fireCd--;
        if (fireCd == 0) {
            shootAtPlayer();
            fireCd = rng.nextInt(FIRE_MAX - FIRE_MIN + 1) + FIRE_MIN;
        }

        // 3) Ser golpeado por balas del jugador
        Actor maybeBullet = getOneIntersectingObject(Bullet.class);
        if (maybeBullet != null) {
            Bullet b = (Bullet) maybeBullet;
            if (b.getOwner() instanceof PlayerShip) {
                onKilledByPlayerBullet(b);
                return;
            }
        }

        // 4) Choque contra asteroides (ambos fuera, sin puntos)
        Actor rock = getOneIntersectingObject(Asteroid.class);
        if (rock != null) {
            if (getWorld() != null) {
                getWorld().removeObject(rock); // no hace split: caos emergente
                Particles.spawnExplosion(getWorld(), getX(), getY(), 16);                
                getWorld().removeObject(this);
            }
            return;
        }

        // 5) TTL
        ttl--;
        if (ttl <= 0 && getWorld() != null) {
            getWorld().removeObject(this);
        }
    }

    /* ====================== Disparo y muerte ====================== */

    private void shootAtPlayer() {
        List<PlayerShip> players = getWorld().getObjects(PlayerShip.class);
        if (players.isEmpty()) return;

        PlayerShip p = players.get(0);
        double tx = p.getX();
        double ty = p.getY();

        // Vector al jugador
        double dx = tx - x;
        double dy = ty - y;
        double ang = Math.atan2(dy, dx); // en radianes

        // Aplica ruido angular según precisión y tipo
        double noiseDegBase = (type == Type.SMALL) ? NOISE_DEG_SMALL : NOISE_DEG_LARGE;
        double noiseDeg = noiseDegBase * (1.0 - accuracy);
        double noiseRad = Math.toRadians(rngRange(-noiseDeg, noiseDeg));

        double shootAng = ang + noiseRad;

        double bvx = Math.cos(shootAng) * ENEMY_BULLET_SPEED;
        double bvy = Math.sin(shootAng) * ENEMY_BULLET_SPEED;

        EnemyBullet eb = new EnemyBullet(bvx, bvy, ENEMY_BULLET_TTL, this);
        getWorld().addObject(eb, (int)Math.round(x), (int)Math.round(y));
        Greenfoot.playSound("ufo_shoot.wav"); // opcional
    }

    private void onKilledByPlayerBullet(Bullet playerBullet) {
        AsteroidsWorld world = (AsteroidsWorld) getWorld();
        if (world == null) return;

        // Puntaje según tipo
        world.addScore(type == Type.SMALL ? PTS_SMALL : PTS_LARGE);

        // Quita bala y OVNI
        if (playerBullet.getWorld() != null) playerBullet.getWorld().removeObject(playerBullet);
        Particles.spawnExplosion(getWorld(), getX(), getY(), (type == Type.SMALL) ? 14 : 18);        
        world.removeObject(this);

        Greenfoot.playSound("ufo_explode.wav"); // opcional
    }

    /* ====================== Visual ====================== */

    private void buildSprite() {
        // Saucer simple: disco + cúpula; SMALL más compacto
        boolean small = (type == Type.SMALL);
        int w = small ? 28 : 40;
        int h = small ? 14 : 18;

        GreenfootImage img = new GreenfootImage(w, h);
        img.setColor(new Color(190, 190, 190));
        img.fillOval(0, h/3, w-1, h-1);             // plato
        img.setColor(new Color(230, 230, 230));
        img.fillOval(w/4, 0, w/2, h/2);             // cúpula
        img.setColor(new Color(120, 120, 120));
        img.drawOval(0, h/3, w-1, h-1);
        img.drawOval(w/4, 0, w/2, h/2);
        setImage(img);
    }

    /* ====================== Utilidades ====================== */

    private double rngRange(double a, double b) {
        return a + rng.nextDouble() * (b - a);
    }
    private static double clamp01(double v) { return Math.max(0.0, Math.min(1.0, v)); }
}
