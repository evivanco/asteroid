import greenfoot.*;   // Actor, World, GreenfootImage, Greenfoot
import java.util.List;

/**
 * Nave del jugador:
 * - Movimiento inercial con thrust y giro.
 * - Wrapping toroidal.
 * - Disparo con cooldown y límite de balas activas.
 * - Invulnerabilidad temporal al (re)aparecer (parpadeo).
 * - Explosión al colisionar (avisa al mundo para perder vida).
 *
 * Requiere:
 *  - class AsteroidsWorld extends World (con loseLife() y addScore() ya definidos).
 *  - class Bullet extends Actor con constructor Bullet(double vx, double vy, int ttlFrames, Actor owner).
 *  - class Asteroid extends Actor (para detección de colisión).
 */
public class PlayerShip extends Actor {

    // ------------ Parámetros de balance (ajustables) ------------
    private static final double THRUST_POWER = 0.35;  // px/frame^2 (aceleración)
    private static final double DRAG         = 0.992; // fricción leve (1 = sin fricción)
    private static final double MAX_SPEED    = 8.5;   // px/frame (tope)
    private static final double ANGULAR_SPEED= 4.0;   // grados/frame

    private static final int    FIRE_COOLDOWN_FRAMES = 15; // ~0.25s @60FPS
    private static final int    MAX_BULLETS          = 4;  // activas a la vez
    private static final double BULLET_SPEED         = 12.0;
    private static final int    BULLET_TTL_FRAMES    = 72; // ~1.2s @60FPS

    private static final int    INVULN_FRAMES        = 120; // ~2s
    private static final int    SHIP_RADIUS_PX       = 16;  // para cálculos simples

    private static final int    HYPERSPACE_CD_FRAMES = 120; // opcional

    // ------------ Estado dinámico ------------
    private double x, y;       // posición subpíxel
    private double vx, vy;     // velocidad
    private int fireCooldown = 0;
    private int invulnTimer  = INVULN_FRAMES;
    private int hyperCD      = 0;

    // Sprites simples (con y sin llama)
    private GreenfootImage imgBase;
    private GreenfootImage imgThrust;

    @Override
    protected void addedToWorld(World w) {
        // Posición inicial (usar la del World)
        this.x = getX();
        this.y = getY();
        buildSprites();
        setImage(imgBase);
        setRotation(270); // 270° = "mirando hacia arriba" visualmente (opcional)
    }

    @Override
    public void act() {
        readInput();
        physicsStep();
        wrapAround();
        updateInvulnerabilityVisual();
        checkCollisions();
    }

    /* ==================== Entrada del jugador ==================== */

    private void readInput() {
        boolean left  = Greenfoot.isKeyDown("left")  || Greenfoot.isKeyDown("a");
        boolean right = Greenfoot.isKeyDown("right") || Greenfoot.isKeyDown("d");
        boolean up    = Greenfoot.isKeyDown("up")    || Greenfoot.isKeyDown("w");
        boolean fire  = Greenfoot.isKeyDown("space");
        boolean hyper = Greenfoot.isKeyDown("shift"); // hipersalto opcional

        // Giro
        if (left)  setRotation((int)(getRotation() - ANGULAR_SPEED + 360) % 360);
        if (right) setRotation((int)(getRotation() + ANGULAR_SPEED) % 360);

        // Thrust
        if (up) {
            double rad = Math.toRadians(getRotation());
            vx += Math.cos(rad) * THRUST_POWER;
            vy += Math.sin(rad) * THRUST_POWER;
            setImage(imgThrust);
        } else {
            setImage(imgBase);
        }

        // Disparo
        if (fireCooldown > 0) fireCooldown--;
        if (fire && fireCooldown == 0 && canFireAnotherBullet()) {
            shoot();
            fireCooldown = FIRE_COOLDOWN_FRAMES;
        }

        // Hipersalto (opcional, simple: teletransporte aleatorio con cooldown)
        if (hyperCD > 0) hyperCD--;
        if (hyper && hyperCD == 0) {
            hyperspace();
            hyperCD = HYPERSPACE_CD_FRAMES;
        }
    }

    private boolean canFireAnotherBullet() {
        // Si solo hay un jugador, vale contar balas totales
        List<Bullet> bullets = getWorld().getObjects(Bullet.class);
        return bullets.size() < MAX_BULLETS;
    }

    private void shoot() {
        double rad = Math.toRadians(getRotation());
        // Punto de salida (nariz de la nave)
        double noseX = x + Math.cos(rad) * (SHIP_RADIUS_PX + 10);
        double noseY = y + Math.sin(rad) * (SHIP_RADIUS_PX + 10);

        // Velocidad de la bala = vel nave + vector hacia adelante
        double bvx = vx + Math.cos(rad) * BULLET_SPEED;
        double bvy = vy + Math.sin(rad) * BULLET_SPEED;

        Bullet b = new Bullet(bvx, bvy, BULLET_TTL_FRAMES, this);
        getWorld().addObject(b, (int)Math.round(noseX), (int)Math.round(noseY));
        Greenfoot.playSound("shoot.wav"); // opcional (si tienes el audio)
    }

    /* ==================== Física y envolvente ==================== */

    private void physicsStep() {
        // Drag leve + clamp de velocidad
        vx *= DRAG;
        vy *= DRAG;

        double speed = Math.sqrt(vx*vx + vy*vy);
        if (speed > MAX_SPEED) {
            double k = MAX_SPEED / speed;
            vx *= k;
            vy *= k;
        }

        // Integración de posición
        x += vx;
        y += vy;

        setLocation((int)Math.round(x), (int)Math.round(y));
    }

    private void wrapAround() {
        World w = getWorld();
        int W = w.getWidth();
        int H = w.getHeight();

        boolean wrapped = false;
        if (x < 0)      { x += W; wrapped = true; }
        if (x >= W)     { x -= W; wrapped = true; }
        if (y < 0)      { y += H; wrapped = true; }
        if (y >= H)     { y -= H; wrapped = true; }

        if (wrapped) setLocation((int)Math.round(x), (int)Math.round(y));
    }

    /* ==================== Colisiones y estado ==================== */

    private void checkCollisions() {
        // Durante invulnerabilidad, no colisiona
        if (invulnTimer > 0) return;

        // Solo comprobamos Asteroid para compilar sin clases extra
        Actor hit = getOneIntersectingObject(Asteroid.class);
        if (hit != null) {
            explode();
        }
    }

    private void explode() {
        // Efecto simple (puedes reemplazar por partículas)
        Greenfoot.playSound("explode.wav"); // opcional

        AsteroidsWorld world = (AsteroidsWorld)getWorld();
        // Notificar pérdida de vida ANTES de eliminar el actor
        world.loseLife();
        Particles.spawnExplosion(getWorld(), getX(), getY(), 16); // nave ~mediana
        // Sacar la nave
        getWorld().removeObject(this);
        // El respawn lo gestiona el mundo.
    }

    private void hyperspace() {
        World w = getWorld();
        int W = w.getWidth();
        int H = w.getHeight();

        // Teletransporte aleatorio (no garantiza 100% seguridad; el Mundo ya intenta spawns seguros)
        this.x = Greenfoot.getRandomNumber(W);
        this.y = Greenfoot.getRandomNumber(H);

        // Pierde algo de velocidad (mareo post salto)
        this.vx *= 0.3;
        this.vy *= 0.3;

        setLocation((int)Math.round(x), (int)Math.round(y));
        // Pequeña invulnerabilidad tras salto
        invulnTimer = Math.max(invulnTimer, 24);
    }

    /* ==================== Visuales y utilidades ==================== */

    private void updateInvulnerabilityVisual() {
        if (invulnTimer > 0) {
            invulnTimer--;
            // Parpadeo: alterna transparencia
            int phase = (invulnTimer / 6) % 2;
            getImage().setTransparency(phase == 0 ? 110 : 230);
            if (invulnTimer == 0) {
                getImage().setTransparency(255);
            }
        }
    }

    private void buildSprites() {
        // Triángulo apuntando a la derecha (rot 0) para que el heading coincida con getRotation()
        imgBase = new GreenfootImage(40, 40);
        imgBase.setColor(new Color(255, 255, 255));
        int cx = 20, cy = 20;
        int[] xs = { cx + 18, cx - 12, cx - 12 };
        int[] ys = { cy,      cy - 8,   cy + 8  };
        imgBase.drawPolygon(xs, ys, 3);
        imgBase.drawLine(cx - 5, cy - 4, cx - 5, cy + 4); // cabina
        // contorno más visible
        imgBase.setColor(new Color(200, 200, 200));
        imgBase.drawOval(cx-16, cy-12, 8, 8); // detalle decorativo

        imgThrust = new GreenfootImage(imgBase); // copia
        // llama trasera
        imgThrust.setColor(new Color(255, 180, 60));
        imgThrust.fillPolygon(
            new int[]{cx - 12, cx - 20, cx - 12},
            new int[]{cy - 5,  cy,      cy + 5},
            3
        );
    }

    /* ==================== API opcional ==================== */

    /** Devuelve la velocidad actual (módulo). */
    public double getSpeed() {
        return Math.sqrt(vx*vx + vy*vy);
    }

    /** Reaplicar invulnerabilidad (útil si activas powerups de escudo). */
    public void grantInvulnerability(int frames) {
        invulnTimer = Math.max(invulnTimer, frames);
    }
}
