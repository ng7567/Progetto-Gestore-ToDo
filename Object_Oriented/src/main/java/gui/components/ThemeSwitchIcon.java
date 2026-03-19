package gui.components;

import com.formdev.flatlaf.ui.FlatUIUtils;
import com.formdev.flatlaf.util.AnimatedIcon;
import com.formdev.flatlaf.util.ColorFunctions;
import com.formdev.flatlaf.util.UIScale;
import gui.style.GuiUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Rappresenta un'icona animata personalizzata deputata alla gestione visiva del pulsante di cambio tema (Switch).
 * Implementa l'interfaccia {@link AnimatedIcon} del framework FlatLaf per calcolare e renderizzare transizioni
 * fluide e interpolate tra lo stato "Sole" (Tema Chiaro) e "Luna" (Tema Scuro), offrendo un feedback visivo
 * moderno e immediato all'interazione dell'utente.
 *
 * @author Nunzio Grasso (Matricola: N86005509)
 * @version 1.0
 */
public class ThemeSwitchIcon implements AnimatedIcon {

    /** Il padding interno espresso in pixel applicato tra il perimetro del cursore e l'icona interna. */
    private static final int ICON_GAP = 3;

    /** La spaziatura orizzontale centrale che separa le due posizioni estreme del binario. */
    private static final int CENTER_SPACE = 5;

    /** Il riferimento precaricato in memoria alla risorsa vettoriale (SVG) raffigurante la Luna. */
    private final Icon darkIcon;

    /** Il riferimento precaricato in memoria alla risorsa vettoriale (SVG) raffigurante il Sole. */
    private final Icon lightIcon;

    /** La costante cromatica che definisce il colore del cursore (Thumb) quando posizionato sul tema scuro. */
    private final Color darkColor = new Color(80, 80, 80);

    /** La costante cromatica che definisce il colore del cursore (Thumb) quando posizionato sul tema chiaro. */
    private final Color lightColor = new Color(230, 230, 230);

    /** Il colore di riempimento per il binario di scorrimento (Track) durante la visualizzazione in modalità chiara. */
    private final Color trackColorLight = new Color(220, 220, 220);

    /** Il colore di riempimento per il binario di scorrimento (Track) durante la visualizzazione in modalità scura. */
    private final Color trackColorDark = new Color(60, 60, 60);

    /**
     * Inizializza il componente precaricando le risorse grafiche vettoriali (SVG) necessarie.
     * Questa strategia di caricamento anticipato (Eager Loading) è cruciale per prevenire operazioni
     * di I/O bloccanti sul disco durante la fase di animazione, evitando così rallentamenti
     * e garantendo un rendering fluido.
     */
    public ThemeSwitchIcon() {
        this.darkIcon = GuiUtils.loadSVG("icons/theme/moon.svg", 16, 16);
        this.lightIcon = GuiUtils.loadSVG("icons/theme/sun.svg", 16, 16);
    }

    /**
     * {@inheritDoc}
     * Definisce la finestra temporale per il completamento della transizione di stato.
     *
     * @return La durata dell'animazione espressa in millisecondi (impostata a 500ms per un bilanciamento ottimale tra reattività e fluidità).
     */
    @Override
    public int getAnimationDuration() {
        return 500;
    }

    /**
     * {@inheritDoc}
     * Esegue il rendering matematico del componente per un singolo fotogramma.
     * Gestisce dinamicamente la traslazione spaziale (asse X) del cursore e l'interpolazione
     * cromatica del suo colore in funzione della variabile di progresso.
     *
     * @param c             Il componente {@link Component} su cui l'icona viene disegnata.
     * @param g             Il contesto grafico {@link Graphics} primario fornito da Swing.
     * @param x             La coordinata X assoluta di origine.
     * @param y             La coordinata Y assoluta di origine.
     * @param animatedValue Variabile di stato (da 0.0f a 1.0f) che descrive il progresso lineare dell'animazione.
     */
    @Override
    public void paintIconAnimated(Component c, Graphics g, int x, int y, float animatedValue) {
        Graphics2D g2 = (Graphics2D) g.create();
        FlatUIUtils.setRenderingHints(g2);

        int size = getIconHeight();
        int width = getIconWidth();

        // Calcola la posizione X del cursore interpolando spazialmente in base al progresso
        float animatedX = x + (width - size) * animatedValue;

        // Disegna lo Sfondo (Binario) selezionando il colore basato sul tema globale
        g2.setColor(GuiUtils.isDarkMode() ? trackColorDark : trackColorLight);
        g2.fillRoundRect(x, y, width, size, size, size);

        // Disegna il Cursore (Thumb) miscelando il colore per garantire una transizione progressiva
        Color thumbColor = ColorFunctions.mix(darkColor, lightColor, animatedValue);
        g2.setColor(thumbColor);
        g2.fillRoundRect((int) animatedX, y, size, size, size, size);

        // Disegna le icone interne applicando un differenziale vettoriale sull'asse Y per l'effetto di comparsa/scomparsa
        float darkY = y - size + (animatedValue * size);
        float lightY = y + (animatedValue * size);

        // Disegna l'icona scura (transizione in entrata dall'alto verso il centro)
        paintSubIcon(c, (Graphics2D) g2.create(), animatedX, darkY, darkIcon, animatedValue);

        // Disegna l'icona chiara (transizione in uscita dal centro verso il basso, invertendo l'opacità)
        paintSubIcon(c, (Graphics2D) g2.create(), animatedX, lightY, lightIcon, 1f - animatedValue);

        g2.dispose();
    }

    /**
     * Esegue il rendering isolato di una singola sotto-icona (Sole o Luna) sul piano grafico bidimensionale.
     * Manipola il canale alpha (trasparenza) tramite costrutti {@link AlphaComposite} per generare
     * un effetto di dissolvenza incrociata in sincronia con lo spostamento fisico.
     *
     * @param c     Il componente ospite {@link Component}.
     * @param g     Il contesto grafico {@link Graphics2D} (ricevuto come clone per garantire l'isolamento delle trasformazioni a matrice).
     * @param x     La posizione X interpolata ove ancorare l'icona.
     * @param y     La posizione Y interpolata ove ancorare l'icona.
     * @param icon  L'oggetto {@link Icon} sorgente da proiettare.
     * @param alpha Il coefficiente scalare di opacità richiesto per il fotogramma corrente (ristretto nel range 0.0 - 1.0).
     */
    private void paintSubIcon(Component c, Graphics2D g, float x, float y, Icon icon, float alpha) {
        // Normalizzazione di sicurezza per prevenire IllegalArgumentException sul canale Alpha
        if (alpha < 0) alpha = 0;
        if (alpha > 1) alpha = 1;

        int gap = UIScale.scale(ICON_GAP);

        g.translate(x, y);
        g.setComposite(AlphaComposite.SrcOver.derive(alpha));

        // Disegna l'icona applicando il margine interno simmetrico rispetto alle pareti del cursore
        icon.paintIcon(c, g, gap, gap);

        g.dispose();
    }

    /**
     * {@inheritDoc}
     * Interroga il componente logico sottostante per determinare la polarità di arrivo dell'animazione.
     *
     * @param c Il componente interattivo (generalmente un'istanza derivata da {@link AbstractButton}) che incorpora lo switch.
     * @return Il target posizionale: {@code 1.0f} (scuro) se il controllo risulta attivato, {@code 0.0f} (chiaro) in caso contrario.
     */
    @Override
    public float getValue(Component c) {
        return ((AbstractButton) c).isSelected() ? 1f : 0f;
    }

    /**
     * {@inheritDoc}
     * Computa l'ingombro orizzontale totale aggregando dinamicamente la larghezza
     * delle singole icone, degli spazi intermedi e dei margini, applicando preventivamente
     * il fattore di scala (DPI-awareness) del sistema host.
     *
     * @return L'estensione orizzontale espressa in pixel logici.
     */
    @Override
    public int getIconWidth() {
        if (darkIcon == null) return 32; // Fallback di sicurezza protettivo

        // Larghezza calcolata = diametro di 2 icone + volume centrale + (4 * margini laterali isolanti)
        return (darkIcon.getIconWidth() * 2) + UIScale.scale(CENTER_SPACE) + (UIScale.scale(ICON_GAP) * 4);
    }

    /**
     * {@inheritDoc}
     *
     * @return L'ingombro verticale in pixel, allineato dinamicamente alla maggiore tra le altezze delle icone vettoriali caricate, comprensivo dei margini di respiro.
     */
    @Override
    public int getIconHeight() {
        if (darkIcon == null) return 16; // Fallback di sicurezza protettivo

        return Math.max(darkIcon.getIconHeight(), lightIcon.getIconHeight()) + (UIScale.scale(ICON_GAP) * 2);
    }
}