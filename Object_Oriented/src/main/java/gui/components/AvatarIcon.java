package gui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;

/**
 * Rappresenta un'implementazione personalizzata dell'interfaccia {@link Icon} progettata
 * per generare dinamicamente avatar utente all'interno dell'interfaccia grafica.
 * Crea un'icona circolare contenente l'iniziale dell'utente sovrapposta a uno sfondo colorato,
 * calcolato a partire dall'hash dello username.
 *
 * @author Nunzio Grasso (Matricola: N86005509)
 * @version 1.0
 */
public class AvatarIcon implements Icon {

    /** Il diametro in pixel dell'icona circolare. */
    private final int size;

    /** Il carattere testuale, tipicamente l'iniziale maiuscola dello username, renderizzato al centro dell'avatar. */
    private final String initial;

    /** Il colore di sfondo dell'avatar, generato dinamicamente per garantire varietà visiva e perfetta leggibilità. */
    private final Color color;

    /**
     * Inizializza l'icona dell'avatar calcolando istantaneamente l'iniziale e il colore di sfondo.
     * Utilizza lo spazio colore HSB (Hue, Saturation, Brightness) per assicurare che le tinte
     * generate casualmente abbiano sempre un contrasto sufficiente con il testo bianco in sovrimpressione.
     *
     * @param username Il nome utente da cui estrarre l'iniziale e derivare matematicamente il colore.
     * @param size     Il diametro (larghezza e altezza) dell'icona espresso in pixel.
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
     * Esegue il rendering grafico dell'avatar sul componente di destinazione.
     * Sfrutta le primitive grafiche 2D e attiva esplicitamente l'anti-aliasing per garantire
     * bordi smussati e un'elevata qualità visiva sia per il perimetro del cerchio che per il testo.
     *
     * @param c Il componente grafico su cui l'icona viene disegnata (non utilizzato attivamente in questa implementazione autonoma).
     * @param g Il contesto grafico {@link Graphics} fornito dal framework Swing.
     * @param x La coordinata X di partenza per l'operazione di disegno.
     * @param y La coordinata Y di partenza per l'operazione di disegno.
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
     * Essendo l'avatar perfettamente circolare, questo valore corrisponde al diametro
     * specificato in fase di inizializzazione.
     *
     * @return La larghezza totale in pixel.
     */
    @Override
    public int getIconWidth() {
        return size;
    }

    /**
     * Restituisce l'altezza dell'icona.
     * Per preservare le proporzioni geometriche del cerchio, il valore restituito
     * è vincolato a coincidere esattamente con la larghezza.
     *
     * @return L'altezza totale in pixel.
     */
    @Override
    public int getIconHeight() {
        return getIconWidth();
    }
}