package gui.components;

import com.formdev.flatlaf.ui.FlatUIUtils;
import com.formdev.flatlaf.util.AnimatedIcon;
import com.formdev.flatlaf.util.ColorFunctions;
import com.formdev.flatlaf.util.UIScale;
import gui.style.GuiUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Icona animata personalizzata per il pulsante di cambio tema (Switch).
 * Implementa l'interfaccia {@link AnimatedIcon} di FlatLaf per gestire transizioni fluide
 * tra lo stato "Sole" (Tema Chiaro) e "Luna" (Tema Scuro).
 */
public class ThemeSwitchIcon implements AnimatedIcon {

    private static final int ICON_GAP = 3;
    private static final int CENTER_SPACE = 5;

    // Icone caricate una sola volta
    private final Icon darkIcon;
    private final Icon lightIcon;

    // Colori per l'animazione del cursore (Thumb)
    private final Color darkColor = new Color(80, 80, 80);
    private final Color lightColor = new Color(230, 230, 230);

    // Colori per lo sfondo del binario (Track)
    private final Color trackColorLight = new Color(220, 220, 220);
    private final Color trackColorDark = new Color(60, 60, 60);

    /**
     * Costruisce l'icona animata caricando le risorse SVG necessarie.
     * Utilizza {@link GuiUtils} per il caricamento sicuro delle icone.
     */
    public ThemeSwitchIcon() {
        this.darkIcon = GuiUtils.loadSVG("icons/theme/moon.svg", 16, 16);
        this.lightIcon = GuiUtils.loadSVG("icons/theme/sun.svg", 16, 16);
    }

    /**
     * Definisce la durata complessiva dell'animazione di switch.
     *
     * @return La durata dell'animazione espressa in millisecondi ({@code 500}).
     */
    @Override
    public int getAnimationDuration() {
        return 500;
    }

    /**
     * Esegue il rendering dell'icona fotogramma per fotogramma durante l'animazione.
     * Calcola la posizione, il colore intermedio e l'opacità delle icone (Sole/Luna)
     * basandosi sul valore progressivo dell'animazione.
     *
     * @param c             Il componente {@link Component} su cui disegnare.
     * @param g             Il contesto grafico {@link Graphics}.
     * @param x             La coordinata X di partenza.
     * @param y             La coordinata Y di partenza.
     * @param animatedValue Valore {@code float} compreso tra {@code 0.0} e {@code 1.0} che rappresenta il progresso dell'animazione.
     */
    @Override
    public void paintIconAnimated(Component c, Graphics g, int x, int y, float animatedValue) {
        Graphics2D g2 = (Graphics2D) g.create();
        FlatUIUtils.setRenderingHints(g2);

        int size = getIconHeight();
        int width = getIconWidth();

        // Calcola la posizione X del cursore in base al progresso dell'animazione
        float animatedX = x + (width - size) * animatedValue;

        //Disegna lo Sfondo (Binario)
        // Seleziona il colore appropriato in base al tema globale corrente
        g2.setColor(GuiUtils.isDarkMode() ? trackColorDark : trackColorLight);
        g2.fillRoundRect(x, y, width, size, size, size);

        //Disegna il Cursore (Thumb)
        // Miscela il colore tra scuro e chiaro per garantire una transizione fluida
        Color thumbColor = ColorFunctions.mix(darkColor, lightColor, animatedValue);
        g2.setColor(thumbColor);
        g2.fillRoundRect((int) animatedX, y, size, size, size, size);

        //Disegna le icone (Sole/Luna)
        // Applica uno scostamento verticale per creare un effetto visivo di entrata/uscita
        float darkY = y - size + (animatedValue * size);
        float lightY = y + (animatedValue * size);

        // Disegna l'icona scura (transizione in entrata dall'alto)
        paintSubIcon(c, (Graphics2D) g2.create(), animatedX, darkY, darkIcon, animatedValue);

        // Disegna l'icona chiara (transizione in uscita verso il basso)
        // Viene utilizzato (1f - animatedValue) per invertire l'opacità
        paintSubIcon(c, (Graphics2D) g2.create(), animatedX, lightY, lightIcon, 1f - animatedValue);

        g2.dispose();
    }

    /**
     * Metodo di supporto per disegnare le singole icone (Sole o Luna) gestendo
     * la trasparenza (canale alpha) e il posizionamento esatto.
     *
     * @param c     Il componente ospite {@link Component}.
     * @param g     Il contesto grafico {@link Graphics2D} (deve essere un clone per evitare side-effect).
     * @param x     La posizione X calcolata.
     * @param y     La posizione Y calcolata.
     * @param icon  L'oggetto {@link Icon} da renderizzare.
     * @param alpha Il valore di opacità desiderato (tra {@code 0.0} e {@code 1.0}).
     */
    private void paintSubIcon(Component c, Graphics2D g, float x, float y, Icon icon, float alpha) {
        // Normalizza il valore alpha per prevenire IllegalArgumentException
        if (alpha < 0) alpha = 0;
        if (alpha > 1) alpha = 1;

        int gap = UIScale.scale(ICON_GAP);

        g.translate(x, y);
        g.setComposite(AlphaComposite.SrcOver.derive(alpha));

        // Disegna l'icona applicando il margine interno (gap) rispetto al cursore
        icon.paintIcon(c, g, gap, gap);

        g.dispose();
    }

    /**
     * Determina lo stato finale target per l'animazione, basandosi sullo stato di selezione del pulsante.
     *
     * @param c Il componente (tipicamente un {@link AbstractButton}) che incorpora l'icona.
     * @return {@code 1.0f} se il pulsante risulta selezionato, {@code 0.0f} altrimenti.
     */
    @Override
    public float getValue(Component c) {
        return ((AbstractButton) c).isSelected() ? 1f : 0f;
    }

    /**
     * Calcola la larghezza totale richiesta dall'icona switch.
     * La dimensione tiene conto dello spazio necessario per le due icone, lo spazio centrale e i margini.
     *
     * @return La larghezza totale in pixel.
     */
    @Override
    public int getIconWidth() {
        if (darkIcon == null) return 32; // Fallback di sicurezza

        // Larghezza = 2 icone + spazio centrale + 4 margini laterali
        return (darkIcon.getIconWidth() * 2) + UIScale.scale(CENTER_SPACE) + (UIScale.scale(ICON_GAP) * 4);
    }

    /**
     * Calcola l'altezza totale richiesta dall'icona switch.
     *
     * @return L'altezza totale in pixel.
     */
    @Override
    public int getIconHeight() {
        if (darkIcon == null) return 16; // Fallback di sicurezza

        // Altezza = altezza massima tra le due icone + margini superiore/inferiore (scalati per DPI)
        return Math.max(darkIcon.getIconHeight(), lightIcon.getIconHeight()) + (UIScale.scale(ICON_GAP) * 2);
    }
}