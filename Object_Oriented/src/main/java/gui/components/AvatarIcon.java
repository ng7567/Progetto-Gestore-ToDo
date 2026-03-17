package gui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;

/**
 * Implementazione personalizzata dell'interfaccia {@link Icon} per creare avatar utente.
 * Genera dinamicamente un'icona circolare contenente l'iniziale dell'utente
 * su uno sfondo colorato determinato in base all'hash del nome.
 */
public class AvatarIcon implements Icon {

    private final int size;
    private final String initial;
    private final Color color;

    /**
     * Crea un nuovo AvatarIcon.
     *
     * @param username Il nome utente da cui estrarre l'iniziale e generare il colore.
     * @param size     Le dimensioni (larghezza e altezza) dell'icona in pixel.
     */
    public AvatarIcon(String username, int size) {
        this.size = size;

        if (username != null && !username.trim().isEmpty()) {
            this.initial = username.substring(0, 1).toUpperCase();

            // Genera un colore unico e gradevole basato sull'hash del nome.
            // Utilizza HSB (Hue, Saturation, Brightness) per evitare colori troppo scuri o troppo chiari.
            int hash = username.hashCode();
            // Math.abs gestisce hash negativi, % 360 definisce l'angolo per la tonalità
            float hue = (Math.abs(hash) % 360) / 360f;

            // Saturation 0.6f (colori non troppo accesi), Brightness 0.85f (luminosi ma non bianchi)
            this.color = Color.getHSBColor(hue, 0.6f, 0.85f);
        } else {
            // Fallback per utenti anonimi o in caso di errore
            this.initial = "?";
            this.color = Color.GRAY;
        }
    }

    /**
     * Disegna l'icona sul componente specificato.
     * Utilizza primitive grafiche 2D con anti-aliasing per una resa visiva ottimale.
     *
     * @param c Il componente su cui disegnare (non utilizzato in questa implementazione).
     * @param g Il contesto grafico {@link Graphics}.
     * @param x La coordinata X dove disegnare.
     * @param y La coordinata Y dove disegnare.
     */
    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();

        // Attiva l'anti-aliasing per cerchi e testo smussati
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 1. Disegna il cerchio di sfondo
        g2.setColor(color);
        g2.fill(new Ellipse2D.Double(x, y, size, size));

        // 2. Disegna l'iniziale
        g2.setColor(Color.WHITE);
        // Dimensione font proporzionale all'icona (50%)
        g2.setFont(new Font("Segoe UI", Font.BOLD, size / 2));

        // Calcolo preciso per centrare il testo verticalmente e orizzontalmente
        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(initial);
        int textHeight = fm.getAscent(); // Altezza visiva dal baseline

        // Centratura Orizzontale: x + metà spazio rimanente
        int tx = x + (size - textWidth) / 2;

        // Centratura Verticale: y + metà altezza icona - metà altezza testo + correzione baseline
        int ty = y + (size / 2) + (textHeight / 2) - 2; // -2 è una correzione ottica fine

        g2.drawString(initial, tx, ty);

        g2.dispose();
    }

    /**
     * Restituisce la larghezza dell'icona.
     *
     * @return La larghezza in pixel.
     */
    @Override
    public int getIconWidth() {
        return size;
    }

    /**
     * Restituisce l'altezza dell'icona.
     * Per garantire la coerenza dell'icona circolare, restituisce il medesimo
     * valore della larghezza.
     *
     * @return L'altezza in pixel, corrispondente alla larghezza.
     */
    @Override
    public int getIconHeight() {
        return getIconWidth();
    }
}