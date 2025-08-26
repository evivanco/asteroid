import greenfoot.*;  // Actor, GreenfootImage, World

/**
 * HUD compacto que muestra Puntaje, Vidas y Oleada.
 * Se auto-actualiza leyendo el estado del AsteroidsWorld.
 */
public class HUD extends Actor {

    private int lastScore = -1, lastLives = -1, lastWave = -1;
    private GreenfootImage canvas;

    @Override
    protected void addedToWorld(World w) {
        canvas = new GreenfootImage(220, 68);
        setImage(canvas);
        redraw((AsteroidsWorld) w); // primera pintura
    }

    @Override
    public void act() {
        AsteroidsWorld world = (AsteroidsWorld) getWorld();
        int s = world.getScore();
        int l = world.getLives();
        int wv = world.getWave();
        if (s != lastScore || l != lastLives || wv != lastWave) {
            redraw(world);
        }
    }

    private void redraw(AsteroidsWorld world) {
        lastScore = world.getScore();
        lastLives = world.getLives();
        lastWave  = world.getWave();

        canvas.clear();
        canvas.setColor(new Color(0, 0, 0, 120));
        fillRoundedRect(canvas, 0, 0, canvas.getWidth(), canvas.getHeight(), 14);
        
        // (opcional) borde suave:
        canvas.setColor(new Color(255, 255, 255, 40));
        drawRoundedRect(canvas, 0, 0, canvas.getWidth(), canvas.getHeight(), 14);
        
        // Texto
        canvas.setColor(new Color(255,255,255));
        canvas.drawString("Puntaje: " + lastScore, 10, 22);
        canvas.drawString("Vidas:   " + lastLives, 10, 40);
        canvas.drawString("Oleada:  " + lastWave,  10, 58);
        setImage(canvas);
    }
    
    /** Rellena un rectángulo con esquinas redondeadas usando fillRect + fillOval. */
    private static void fillRoundedRect(GreenfootImage g, int x, int y, int w, int h, int r) {
        // centros
        g.fillRect(x + r, y,         w - 2*r, h);         // banda horizontal
        g.fillRect(x,     y + r,     w,       h - 2*r);   // banda vertical
        // esquinas
        g.fillOval(x,           y,           2*r, 2*r);
        g.fillOval(x + w - 2*r, y,           2*r, 2*r);
        g.fillOval(x,           y + h - 2*r, 2*r, 2*r);
        g.fillOval(x + w - 2*r, y + h - 2*r, 2*r, 2*r);
    }
    
    /** Traza el borde de un rectángulo redondeado con drawLine + drawOval. */
    private static void drawRoundedRect(GreenfootImage g, int x, int y, int w, int h, int r) {
        // lados rectos
        g.drawLine(x + r,     y,         x + w - r - 1, y);
        g.drawLine(x + r,     y + h - 1, x + w - r - 1, y + h - 1);
        g.drawLine(x,         y + r,     x,             y + h - r - 1);
        g.drawLine(x + w - 1, y + r,     x + w - 1,     y + h - r - 1);
        // esquinas
        g.drawOval(x,           y,           2*r, 2*r);
        g.drawOval(x + w - 2*r, y,           2*r, 2*r);
        g.drawOval(x,           y + h - 2*r, 2*r, 2*r);
        g.drawOval(x + w - 2*r, y + h - 2*r, 2*r, 2*r);
    }
    
}
