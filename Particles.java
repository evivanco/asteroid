import greenfoot.*;
import java.util.Random;

/** Utilidades para crear ráfagas de partículas. */
public class Particles {

    private Particles() {}

    /** 
     * Explosión mixta (chispas y humo) en (x,y).
     * @param scale 1..n ~ tamaño/energía de la explosión (10 recomendado para asteroide S, 14 M, 20 L).
     */
    public static void spawnExplosion(World w, int x, int y, int scale) {
        Random rng = (w instanceof AsteroidsWorld) ? ((AsteroidsWorld) w).rng() : new Random();

        int sparks = Math.max(6, (int)(scale * 1.2));
        int smoke  = Math.max(4, scale / 2);

        // CHISPAS (brillo blanco/amarillo, rápidas, TTL corto)
        for (int i = 0; i < sparks; i++) {
            double ang = rng.nextDouble() * Math.PI * 2.0;
            double spd = 2.0 + rng.nextDouble() * (0.9 + 0.25 * scale);
            double vx  = Math.cos(ang) * spd;
            double vy  = Math.sin(ang) * spd;
            int ttl    = 20 + rng.nextInt(20);
            int r      = 2 + rng.nextInt(2);
            Color c    = (rng.nextBoolean())
                       ? new Color(255, 240, 160, 255)
                       : new Color(255, 255, 255, 255);
            Particle p = new Particle(c, r, vx, vy, ttl, rngRange(rng, -6, 6));
            w.addObject(p, x, y);
        }

        // HUMO (gris translúcido, lento, TTL mayor)
        for (int i = 0; i < smoke; i++) {
            double ang = rng.nextDouble() * Math.PI * 2.0;
            double spd = 0.6 + rng.nextDouble() * 1.0;
            double vx  = Math.cos(ang) * spd;
            double vy  = Math.sin(ang) * spd;
            int ttl    = 40 + rng.nextInt(40);
            int r      = 5 + rng.nextInt(6);
            Color c    = new Color(180, 180, 180, 180);
            Particle p = new Particle(c, r, vx, vy, ttl, rngRange(rng, -2, 2));
            w.addObject(p, x, y);
        }
    }

    private static double rngRange(Random rng, double a, double b) {
        return a + rng.nextDouble() * (b - a);
    }
}
