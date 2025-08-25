import greenfoot.*;  // Actor, World, GreenfootImage, Greenfoot
import java.util.Random;

/**
 * Asteroide con tres tamaños (L/M/S):
 * - Movimiento con velocidad aleatoria y rotación.
 * - Wrapping toroidal.
 * - División al recibir impacto de Bullet (L->2M, M->2S, S->nada).
 * - Asigna puntos al destruirse (L=20, M=50, S=100).
 *
 * Contratos:
 *  - Bullet: al colisionar llama a asteroid.onHitBy(this).
 *  - AsteroidsWorld: expone addScore(int) y rng() para coherencia de aleatoriedad.
 */
public class Asteroid extends Actor {

    public static enum Size { LARGE, MEDIUM, SMALL }

    // --------- Parámetros por tamaño (ajustables) ---------
    private static final int   R_L = 46, R_M = 28, R_S = 16;           // radio aprox (px)
    private static final double VMIN_L = 1.2,  VMAX_L = 2.0;           // px/frame
    private static final double VMIN_M = 1.8,  VMAX_M = 2.8;
    private static final double VMIN_S = 2.3,  VMAX_S = 3.5;
    private static final int   PTS_L = 20, PTS_M = 50, PTS_S = 100;    // puntaje

    private static final double ROT_MIN = -2.0, ROT_MAX = 2.0;         // grados/frame
    private static final double SPLIT_IMPULSE = 1.2;                    // impulso extra al dividirse

    // --------- Estado dinámico ---------
    private Size size;
    private double x, y;         // posición subpíxel
    private double vx, vy;       // velocidad
    private double rotSpeed;     // velocidad angular (grados/frame)
    private int radius;          // para colisiones simples y sprites

    // --------- Visual ---------
    private GreenfootImage sprite;

    public Asteroid(Size size) {
        this.size = size;
        this.radius = (size == Size.LARGE) ? R_L : (size == Size.MEDIUM ? R_M : R_S);
        buildSprite();      // crea el polígono rocoso
        setImage(sprite);
    }

    @Override
    protected void addedToWorld(World w) {
        // Inicializa posición y velocidad al entrar al mundo (spawn externo define X/Y)
        this.x = getX();
        this.y = getY();

        AsteroidsWorld world = (AsteroidsWorld) w;
        Random rng = world.rng();

        // Velocidad aleatoria según tamaño
        double spd = randomInRange(rng, vMinFor(size), vMaxFor(size));
        double ang = randomInRange(rng, 0, Math.PI * 2);
        this.vx = Math.cos(ang) * spd;
        this.vy = Math.sin(ang) * spd;

        // Rotación angular aleatoria
        this.rotSpeed = randomInRange(rng, ROT_MIN, ROT_MAX);
        if (Math.abs(rotSpeed) < 0.2) {
            rotSpeed = (rotSpeed < 0 ? -0.2 : 0.2); // evita valores casi cero
        }

        // Rotación inicial cualquiera
        setRotation((int) Math.toDegrees(ang));
    }

    @Override
    public void act() {
        // Movimiento + rotación
        x += vx;
        y += vy;
        wrapAround();
        setLocation((int) Math.round(x), (int) Math.round(y));
        setRotation((getRotation() + (int) Math.round(rotSpeed)) % 360);

        // Player colisiona consigo mismo (la nave se encarga en su clase),
        // por lo que aquí no hacemos nada con PlayerShip.
    }

    /* ================== Lógica de impacto ================== */

    /** Llamado por Bullet al colisionar. */
    public void onHitBy(Bullet b) {
        AsteroidsWorld world = (AsteroidsWorld) getWorld();
        if (world == null) return;

        // 1) Sumar puntos
        world.addScore(pointsFor(size));

        // 2) Dividir si corresponde
        if (size == Size.LARGE) {
            spawnChildren(Size.MEDIUM, 2, b);
        } else if (size == Size.MEDIUM) {
            spawnChildren(Size.SMALL, 2, b);
        }
        // 3) Remover este asteroide
        world.removeObject(this);

        // (Opcional: sonido/partículas)
        Greenfoot.playSound("rock-break.wav"); // si tienes el audio
    }

    private void spawnChildren(Size childSize, int count, Bullet source) {
        AsteroidsWorld world = (AsteroidsWorld) getWorld();
        Random rng = world.rng();

        // Vector del impacto para darles impulso de separación
        double ivx = 0, ivy = 0;
        if (source != null) { ivx = source.getVX(); ivy = source.getVY(); }
        // Normaliza (si no nulo)
        double norm = Math.sqrt(ivx*ivx + ivy*ivy);
        double nx = (norm > 0.0001) ? (ivx / norm) : Math.cos(randomInRange(rng, 0, Math.PI*2));
        double ny = (norm > 0.0001) ? (ivy / norm) : Math.sin(randomInRange(rng, 0, Math.PI*2));

        // Crea hijos con pequeña variación angular
        for (int i = 0; i < count; i++) {
            Asteroid child = new Asteroid(childSize);
            world.addObject(child, (int)Math.round(x), (int)Math.round(y));

            // Velocidad base aleatoria del hijo en su rango
            double baseSpd = randomInRange(rng, vMinFor(childSize), vMaxFor(childSize));
            double baseAng = randomInRange(rng, 0, Math.PI * 2);
            double bvx = Math.cos(baseAng) * baseSpd;
            double bvy = Math.sin(baseAng) * baseSpd;

            // Impulso de separación a partir del vector de impacto, con signo alterno
            double sign = (i % 2 == 0) ? 1.0 : -1.0;
            double jx = nx * SPLIT_IMPULSE * sign;
            double jy = ny * SPLIT_IMPULSE * sign;

            child.setVelocity(bvx + jx + this.vx * 0.2,  // hereda un poco de la velocidad madre
                              bvy + jy + this.vy * 0.2);
            child.randomizeRotation(world.rng());
        }
    }

    /* ================== Movimiento y utilidades ================== */

    private void wrapAround() {
        World w = getWorld();
        int W = w.getWidth();
        int H = w.getHeight();

        boolean wrapped = false;
        if (x < 0)   { x += W; wrapped = true; }
        if (x >= W)  { x -= W; wrapped = true; }
        if (y < 0)   { y += H; wrapped = true; }
        if (y >= H)  { y -= H; wrapped = true; }

        if (wrapped) setLocation((int)Math.round(x), (int)Math.round(y));
    }

    public void setVelocity(double vx, double vy) {
        this.vx = vx;
        this.vy = vy;
    }

    private void randomizeRotation(Random rng) {
        this.rotSpeed = randomInRange(rng, ROT_MIN, ROT_MAX);
        if (Math.abs(rotSpeed) < 0.2) rotSpeed = (rotSpeed < 0 ? -0.2 : 0.2);
    }

    private static double vMinFor(Size s) {
        switch (s) {
            case LARGE:  return VMIN_L;
            case MEDIUM: return VMIN_M;
            default:     return VMIN_S;
        }
    }

    private static double vMaxFor(Size s) {
        switch (s) {
            case LARGE:  return VMAX_L;
            case MEDIUM: return VMAX_M;
            default:     return VMAX_S;
        }
    }

    private static int pointsFor(Size s) {
        switch (s) {
            case LARGE:  return PTS_L;
            case MEDIUM: return PTS_M;
            default:     return PTS_S;
        }
    }

    private static double randomInRange(Random rng, double a, double b) {
        return a + rng.nextDouble() * (b - a);
    }

    /* ================== Visual (polígono “rocoso”) ================== */

    private void buildSprite() {
        // Imagen cuadrada con tamaño acorde al radio
        int sz = radius * 2 + 2;
        GreenfootImage img = new GreenfootImage(sz, sz);

        // Polígono dentado: N vértices con jitter radial
        int verts = Math.max(8, (int) Math.round(radius / 2.5)); // más grande => más vértices
        int cx = sz / 2, cy = sz / 2;

        int[] xs = new int[verts];
        int[] ys = new int[verts];

        // Semilla simple: usar Math.random aquí no afecta la coherencia del juego
        for (int i = 0; i < verts; i++) {
            double t = (2 * Math.PI * i) / verts;
            double jitter = 0.75 + Math.random() * 0.4; // 0.75..1.15
            double r = radius * jitter;
            xs[i] = (int) Math.round(cx + Math.cos(t) * r);
            ys[i] = (int) Math.round(cy + Math.sin(t) * r);
        }

        // Relleno + contorno
        img.setColor(new Color(200, 200, 200));
        img.fillPolygon(xs, ys, verts);
        img.setColor(new Color(140, 140, 140));
        img.drawPolygon(xs, ys, verts);

        // Sombras simples
        img.setColor(new Color(255, 255, 255, 30));
        img.drawLine(cx, cy, cx + radius, cy); // brillo
        img.setColor(new Color(0, 0, 0, 40));
        img.drawLine(cx, cy, cx - radius, cy); // sombra

        this.sprite = img;
    }

    /* ================== Getters útiles ================== */
    public Size getSizeType() { return size; }
    public int  getRadius()   { return radius; }
    public double getVX()     { return vx; }
    public double getVY()     { return vy; }
}

