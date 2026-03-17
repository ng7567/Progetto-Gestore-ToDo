package gui.style;

import java.awt.*;
import javax.swing.*;

/**
 * Estensione di {@link FlowLayout} che permette ai componenti di andare a capo
 * automaticamente quando lo spazio orizzontale è esaurito.
 * <p>
 * È particolarmente utile quando il contenitore è inserito in uno {@link JScrollPane},
 * poiché il FlowLayout standard tenderebbe a espandersi infinitamente in orizzontale.
 */
public class WrapLayout extends FlowLayout {

    /**
     * Costruisce un WrapLayout con allineamento a sinistra e gap predefiniti.
     */
    public WrapLayout() {
        super();
    }

    /**
     * Costruisce un WrapLayout con l'allineamento specificato e gap di 5 pixel.
     * @param align l'allineamento (es. {@code FlowLayout.LEFT})
     */
    public WrapLayout(int align) {
        super(align);
    }

    /**
     * Costruisce un WrapLayout con allineamento e spaziature specificate.
     *
     * @param align l'allineamento (es. {@code FlowLayout.LEFT})
     * @param hgap  lo spazio orizzontale tra i componenti
     * @param vgap  lo spazio verticale tra le righe
     */
    public WrapLayout(int align, int hgap, int vgap) {
        super(align, hgap, vgap);
    }

    /**
     * Calcola la dimensione preferita per il contenitore utilizzando questo layout.
     * Rispetto al FlowLayout standard, calcola l'altezza necessaria basandosi sulla
     * larghezza attuale del contenitore.
     *
     * @param target il contenitore da disporre
     * @return le dimensioni preferite
     */
    @Override
    public Dimension preferredLayoutSize(Container target) {
        return layoutSize(target, true);
    }

    /**
     * Calcola la dimensione minima per il contenitore utilizzando questo layout.
     *
     * @param target il contenitore da disporre
     * @return le dimensioni minime
     */
    @Override
    public Dimension minimumLayoutSize(Container target) {
        Dimension minimum = layoutSize(target, false);
        minimum.width -= (getHgap() + 1);
        return minimum;
    }

    /**
     * Calcola la dimensione del layout in base alla larghezza del target.
     *
     * @param target    il contenitore
     * @param preferred true per calcolare la dimensione preferita, false per la minima
     * @return la dimensione calcolata
     */
    private Dimension layoutSize(Container target, boolean preferred) {
        synchronized (target.getTreeLock()) {
            int targetWidth = target.getSize().width;

            // Se la larghezza è 0, il componente non è ancora stato visualizzato.
            // Usiamo un valore molto alto per evitare calcoli errati iniziali.
            if (targetWidth == 0) {
                targetWidth = Integer.MAX_VALUE;
            }

            int hgap = getHgap();
            int vgap = getVgap();
            Insets insets = target.getInsets();
            int horizontalInsetsAndGap = insets.left + insets.right + (hgap * 2);
            int maxWidth = targetWidth - horizontalInsetsAndGap;

            Dimension dim = new Dimension(0, 0);
            int rowWidth = 0;
            int rowHeight = 0;

            int nmembers = target.getComponentCount();

            for (int i = 0; i < nmembers; i++) {
                Component m = target.getComponent(i);
                if (m.isVisible()) {
                    Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();

                    // Se il componente corrente eccede la larghezza della riga, si va a capo
                    if (rowWidth + d.width > maxWidth) {
                        addRow(dim, rowWidth, rowHeight);
                        rowWidth = 0;
                        rowHeight = 0;
                    }

                    if (rowWidth != 0) {
                        rowWidth += hgap;
                    }

                    rowWidth += d.width;
                    rowHeight = Math.max(rowHeight, d.height);
                }
            }

            // Aggiunge l'ultima riga
            addRow(dim, rowWidth, rowHeight);

            dim.width += horizontalInsetsAndGap;
            dim.height += insets.top + insets.bottom + (vgap * 2);

            // Ottimizzazione per JScrollPane: forza la larghezza per evitare scrollbar orizzontali inutili
            Container scrollPane = SwingUtilities.getAncestorOfClass(JScrollPane.class, target);
            if (scrollPane != null && target.isValid()) {
                dim.width -= (hgap + 1);
            }

            return dim;
        }
    }

    /**
     * Aggiorna la dimensione complessiva del contenitore aggiungendo i dati di una riga appena completata.
     * Incrementa l'altezza totale includendo lo spazio verticale (Vgap) tra le righe e calcola
     * la larghezza massima raggiunta rispetto alle righe precedenti.
     *
     * @param dim       L'oggetto {@link Dimension} che accumula le misure totali del layout.
     * @param rowWidth  La larghezza calcolata della riga appena terminata.
     * @param rowHeight L'altezza massima rilevata tra i componenti della riga appena terminata.
     */
    private void addRow(Dimension dim, int rowWidth, int rowHeight) {
        // Aggiorna la larghezza complessiva mantenendo il valore massimo riscontrato
        dim.width = Math.max(dim.width, rowWidth);

        // Aggiunge lo spazio verticale solo se non si tratta della prima riga
        if (dim.height > 0) {
            dim.height += getVgap();
        }

        // Incrementa l'altezza totale con l'altezza della riga corrente
        dim.height += rowHeight;
    }
}