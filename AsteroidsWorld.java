import greenfoot.*;           // World, Actor, GreenfootImage, Greenfoot
import java.util.List;
import java.util.Random;
import java.awt.Color;

/**
 * Mundo principal del juego Asteroids.
 * - Mantiene estado global: puntaje, vidas, oleada, inicio/fin de juego.
 * - Genera oleadas de asteroides con aparición segura respecto al jugador.
 * - Muestra HUD con score/vidas/oleada e instrucciones.
 * - Maneja respawn y reinicio.
 *
 * Requiere (siguientes pasos):
 *  - class PlayerShip extends Actor
 *  - class Asteroid extends Actor con enum Size { LARGE, MEDIUM, SMALL }
 */
public class AsteroidsWorld extends World {

    // --- Dimensiones del mundo ---
    public static final int WIDTH  = 900;
    public static final int HEIGHT = 700;
    public static final int CELL   = 1;

    // --- Estado de juego ---
    private boolean gameStarted = false;
    private boolean waveClearedBanner = false;

    private int score = 0;
    private int lives = 3;
    private int wave  = 0;

    // Retraso para lanzar la siguiente oleada (en frames)
    private int nextWaveDelayFrames = 0;

    // RNG
    private final Random rng = new Random();

    // Parámetros de balance (ajustables)
    private int   baseLargeAsteroids = 5;      // L en oleada 1
    private float waveBudgetFactor   = 1.25f;  // crecimiento de presupuesto por oleada
    private int   safeSpawnRadius    = 140;    // radio seguro alrededor del Player
    private int   respawnDelayFrames = 45;     // frames antes de respawnear Player

    // Control de respawn
    private int respawnTimer = 0;

    public AsteroidsWorld() {
        // bounded=false (warp visual “infinito” más cómodo para Asteroids)
        super(WIDTH, HEIGHT, CELL, false);
        buildStarfieldBackground();
        drawTitleScreen();
    }

    @Override
    public void act() {
        if (!gameStarted) {
            // Espera a que el usuario presione ENTER para comenzar/reiniciar
            if (Greenfoot.isKeyDown("enter")) {
                startGame();
            }
            return;
        }

        // HUD en cada frame
        drawHUD();

        // Respawn del jugador si está pendiente
        tickRespawn();

        // Si no quedan asteroides, preparar siguiente oleada
        if (getObjects(Asteroid.class).isEmpty() && respawnTimer == 0) {
            if (nextWaveDelayFrames == 0) {
                waveClearedBanner = true;
                nextWaveDelayFrames = 60; // ~1 segundo a 60 FPS
                showCenteredMessage("¡Oleada despejada!", 40);
            } else {
                nextWaveDelayFrames--;
                if (nextWaveDelayFrames == 0) {
                    clearCenterMessage();
                    waveClearedBanner = false;
                    spawnNextWave();
                }
            }
        }

        // Reinicio rápido: si Game Over, ENTER para reiniciar
        if (lives <= 0 && getObjects(PlayerShip.class).isEmpty()) {
            showCenteredMessage("GAME OVER — ENTER para reiniciar", 36);
            if (Greenfoot.isKeyDown("enter")) {
                startGame();
            }
        }
    }

    /* ===================== Ciclo de vida del juego ===================== */

    /** Comienza/Resetea una partida nueva. */
    private void startGame() {
        gameStarted = true;
        waveClearedBanner = false;
        score = 0;
        lives = 3;
        wave  = 0;
        nextWaveDelayFrames = 0;
        respawnTimer = 0;

        // Limpia todo lo que hubiera
        removeObjects(getObjects(Actor.class));
        buildStarfieldBackground();
        clearCenterMessage();

        // Crea jugador y primera oleada
        spawnPlayerSafely();
        spawnNextWave();
    }

    /** Avanza contador de respawn del jugador (si aplica). */
    private void tickRespawn() {
        if (respawnTimer > 0) {
            respawnTimer--;
            if (respawnTimer == 0 && lives > 0 && getObjects(PlayerShip.class).isEmpty()) {
                spawnPlayerSafely();
            }
        }
    }

    /** Lanza la siguiente oleada en función del número de oleada y presupuesto. */
    private void spawnNextWave() {
        wave++;
        // Presupuesto crece por oleada (L ~2 pts, M ~1 pt, S ~0.5 pt), aquí simplificado
        int budget = Math.max(1, Math.round((float)baseLargeAsteroids * (float)Math.pow(waveBudgetFactor, wave - 1)));

        // En oleadas bajas: más L; luego mezcla. S se generará al destruir L y M.
        int numLarge = Math.max(3, budget);
        for (int i = 0; i < numLarge; i++) {
            spawnAsteroidSafely(Asteroid.Size.LARGE);
        }

        showCenteredMessage("Oleada " + wave, 32);
        nextWaveDelayFrames = 45;
        waveClearedBanner = true;
    }

    /* ========================= API pública (desde actores) ========================= */

    /** Suma puntos (llamar desde Asteroid/UFO al destruirse). */
    public void addScore(int points) {
        score = Math.max(0, score + points);
    }

    /**
     * Llamar cuando el jugador pierde una vida (p.ej., PlayerShip al explotar).
     * El mundo gestiona respawn o game over.
     */
    public void loseLife() {
        if (lives <= 0) return; // ya estaba en game over
        lives--;
        if (lives > 0) {
            respawnTimer = respawnDelayFrames;
            showCenteredMessage("¡Has perdido una vida!", 32);
        } else {
            // Fin del juego
            respawnTimer = 0;
            showCenteredMessage("GAME OVER — ENTER para reiniciar", 36);
        }
    }

    /* ========================= Spawns seguros ========================= */

    /** Crea el jugador en el centro o en la posición segura más cercana. */
    private void spawnPlayerSafely() {
        int cx = WIDTH / 2;
        int cy = HEIGHT / 2;

        // Intenta el centro; si no es seguro, busca otra posición
        int[] pos = findSafeSpawnPosition(cx, cy, safeSpawnRadius);
        PlayerShip ship = new PlayerShip();
        addObject(ship, pos[0], pos[1]);
        clearCenterMessage();
    }

    /** Crea un asteroide del tamaño dado en una posición segura respecto al Player. */
    private void spawnAsteroidSafely(Asteroid.Size size) {
        // Intentar esquinas y bordes alejados del jugador
        int[][] candidates = new int[][] {
            {rng.nextInt(WIDTH), 0},
            {rng.nextInt(WIDTH), HEIGHT-1},
            {0, rng.nextInt(HEIGHT)},
            {WIDTH-1, rng.nextInt(HEIGHT)},
            {rng.nextInt(WIDTH), rng.nextInt(HEIGHT)} // extra
        };

        int[] pos = null;
        for (int[] c : candidates) {
            if (isSafeFromPlayer(c[0], c[1], safeSpawnRadius)) {
                pos = c;
                break;
            }
        }
        if (pos == null) {
            // Búsqueda aleatoria con límite de intentos
            pos = findSafeSpawnPosition(rng.nextInt(WIDTH), rng.nextInt(HEIGHT), safeSpawnRadius);
        }

        Asteroid a = new Asteroid(size);
        addObject(a, pos[0], pos[1]);
    }

    /** Devuelve si (x,y) está a una distancia segura del jugador actual. */
    private boolean isSafeFromPlayer(int x, int y, int radius) {
        List<PlayerShip> players = getObjects(PlayerShip.class);
        if (players.isEmpty()) return true; // si no hay player, es seguro
        PlayerShip p = players.get(0);
        int dx = x - p.getX();
        int dy = y - p.getY();
        return (dx*dx + dy*dy) >= (radius * radius);
    }

    /**
     * Busca una posición segura alrededor de (seedX,seedY) probando varias muestras.
     * Si no encuentra, devuelve la última probada.
     */
    private int[] findSafeSpawnPosition(int seedX, int seedY, int radius) {
        int attempts = 80;
        int bestX = seedX;
        int bestY = seedY;
        for (int i = 0; i < attempts; i++) {
            int x = rng.nextInt(WIDTH);
            int y = rng.nextInt(HEIGHT);
            if (isSafeFromPlayer(x, y, radius)) {
                return new int[]{x, y};
            }
            bestX = x; bestY = y;
        }
        return new int[]{bestX, bestY};
    }

    /* ========================= HUD y UI ========================= */

    private void drawHUD() {
        // Bordes superiores
        showText("Puntaje: " + score, 90, 20);
        showText("Vidas: " + lives,   90, 40);
        showText("Oleada: " + wave,   90, 60);

        // Mensajes del centro se dibujan con showCenteredMessage/clearCenterMessage
        // Nota: showText reasigna por coordenadas; no necesita limpiar manual.
    }

    private void drawTitleScreen() {
        buildStarfieldBackground();
        showCenteredMessage("ASTEROIDS\nENTER para comenzar", 42);
        // Limpia HUD
        showText("", 90, 20);
        showText("", 90, 40);
        showText("", 90, 60);
    }

    private void showCenteredMessage(String msg, int fontSize) {
        // Dibuja un cartel/transparencia sobre el fondo (usando showText multilínea simplificada)
        // Greenfoot no soporta multilínea con showText, así que separamos manualmente
        String[] lines = msg.split("\\n");
        int lineHeight = fontSize + 6;
        int startY = HEIGHT/2 - (lines.length * lineHeight)/2;

        // Para resaltar, también podemos dar sombra manual (simple).
        for (int i = 0; i < lines.length; i++) {
            // Sombra
            showText(lines[i], WIDTH/2 + 1, startY + i*lineHeight + 1);
            // Texto principal
            showText(lines[i], WIDTH/2,     startY + i*lineHeight);
        }
    }

    private void clearCenterMessage() {
        // Borra con cadenas vacías en varias líneas alrededor del centro
        for (int dy = -3; dy <= 3; dy++) {
            showText("", WIDTH/2, HEIGHT/2 + dy*18);
        }
    }

    /* ========================= Fondo (estrellas) ========================= */

    private void buildStarfieldBackground() {
        GreenfootImage bg = new GreenfootImage(WIDTH, HEIGHT);
        bg.setColor(greenfoot.Color.BLACK);
        bg.fill();

        // Estrellas
        int stars = 420;
        for (int i = 0; i < stars; i++) {
            int x = rng.nextInt(WIDTH);
            int y = rng.nextInt(HEIGHT);
            int b = 150 + rng.nextInt(106); // brillo 150-255
            bg.setColor(greenfoot.Color.WHITE);
            bg.drawRect(x, y, 1, 1);
            // Algunas estrellas más grandes
            if (rng.nextFloat() < 0.07f) {
                bg.drawRect(x+1, y, 1, 1);
            }
        }

        // Nebulosidad ligera
        for (int i = 0; i < 12; i++) {
            int cx = rng.nextInt(WIDTH);
            int cy = rng.nextInt(HEIGHT);
            int rad = 80 + rng.nextInt(140);
            int alpha = 20 + rng.nextInt(30);
            greenfoot.Color haze = new greenfoot.Color(120 + rng.nextInt(80), 120 + rng.nextInt(80), 200, alpha);
            drawFilledCircle(bg, cx, cy, rad, haze);
        }

        setBackground(bg);
    }

    /** Utilidad: círculo relleno (pincel barato para nebulosas). */
    private void drawFilledCircle(GreenfootImage img, int cx, int cy, int r, greenfoot.Color col) {
        img.setColor(col);
        for (int y = -r; y <= r; y++) {
            int span = (int)Math.sqrt(r*r - y*y);
            img.drawLine(cx - span, cy + y, cx + span, cy + y);
        }
    }

    /* ========================= Utilidades generales ========================= */

    public int getScore() { return score; }
    public int getLives() { return lives; }
    public int getWave()  { return wave;  }

    /** Acceso al RNG del mundo (para actores que quieran variabilidad coherente). */
    public Random rng() { return rng; }
}
