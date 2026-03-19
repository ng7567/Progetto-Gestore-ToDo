package gui.style;

import java.awt.*;
import javax.swing.*;

/**
 * Rappresenta un'estensione specializzata di {@link FlowLayout} progettata per consentire
 * ai componenti di disporsi su più righe qualora lo spazio orizzontale
 * del contenitore risulti esaurito.
 * <p>
 * Risolve la criticità del {@code FlowLayout} standard all'interno di uno {@link JScrollPane},
 * dove il layout originale tenderebbe a espandersi infinitamente in orizzontale.
 * Questa implementazione calcola dinamicamente l'altezza necessaria in base alla larghezza
 * attuale, garantendo una visualizzazione a griglia fluida e responsiva.
 *
 * @author Nunzio Grasso (Matricola: N86005509)
 * @version 1.0
 */
public class WrapLayout extends FlowLayout {

    /**
     * Inizializza un nuovo {@code WrapLayout} con allineamento a sinistra e spaziature
     * orizzontali e verticali predefinite di 5 pixel.
     */
    public WrapLayout() {
        super();
    }

    /**
     * Inizializza un nuovo {@code WrapLayout} applicando l'allineamento specificato
     * e mantenendo le spaziature predefinite di 5 pixel.
     *
     * @param align Il valore di allineamento desiderato (es. {@link FlowLayout#LEFT}).
     */
    public WrapLayout(int align) {
        super(align);
    }

    /**
     * Inizializza un nuovo {@code WrapLayout} configurando esplicitamente l'allineamento
     * e le distanze millimetriche tra i componenti.
     *
     * @param align Il valore di allineamento desiderato.
     * @param hgap  Lo spazio orizzontale in pixel tra i componenti adiacenti.
     * @param vgap  Lo spazio verticale in pixel tra le righe sovrapposte.
     */
    public WrapLayout(int align, int hgap, int vgap) {
        super(align, hgap, vgap);
    }

    /**
     * {@inheritDoc}
     * Calcola le dimensioni preferite per il contenitore target eseguendo una scansione
     * dei componenti visibili e determinando il numero di righe necessarie in base
     * alla larghezza attuale della gerarchia grafica.
     *
     * @param target Il contenitore i cui componenti devono essere disposti.
     * @return Un oggetto {@link Dimension} rappresentante la larghezza e l'altezza preferite.
     */
    @Override
    public Dimension preferredLayoutSize(Container target) {
        return layoutSize(target, true);
    }

    /**
     * {@inheritDoc}
     * Determina l'ingombro minimo per il contenitore, applicando una
     * correzione sulla larghezza per prevenire la comparsa di scrollbar orizzontali
     * ridondanti nei contesti di scorrimento.
     *
     * @param target Il contenitore i cui componenti devono essere disposti.
     * @return Un oggetto {@link Dimension} rappresentante la larghezza e l'altezza minime.
     */
    @Override
    public Dimension minimumLayoutSize(Container target) {
        Dimension minimum = layoutSize(target, false);
        minimum.width -= (getHgap() + 1);
        return minimum;
    }

    /**
     * Esegue l'algoritmo di calcolo delle dimensioni del layout basandosi sulla
     * larghezza effettiva del target.
     * Il metodo opera in mutua esclusione tramite il lock dell'albero dei componenti
     * ({@code getTreeLock}) per garantire la coerenza dei calcoli durante le mutazioni della UI.
     *
     * @param target    Il contenitore oggetto della misurazione.
     * @param preferred Valore booleano: {@code true} per richiedere la dimensione preferita,
     * {@code false} per la minima.
     * @return L'ingombro volumetrico calcolato per la disposizione dei componenti.
     */
    private Dimension layoutSize(Container target, boolean preferred) {
        synchronized (target.getTreeLock()) {
            int targetWidth = target.getSize().width;

            // Gestisce la fase embrionale del ciclo di vita del componente in cui la larghezza è nulla
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

                    // Verifica se il componente eccede i confini della riga corrente per forzare l'andata a capo
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

            // Consolidamento dei dati relativi all'ultima riga elaborata
            addRow(dim, rowWidth, rowHeight);

            dim.width += horizontalInsetsAndGap;
            dim.height += insets.top + insets.bottom + (vgap * 2);

            // Ottimizzazione specifica per l'integrazione con JScrollPane
            Container scrollPane = SwingUtilities.getAncestorOfClass(JScrollPane.class, target);
            if (scrollPane != null && target.isValid()) {
                dim.width -= (hgap + 1);
            }

            return dim;
        }
    }

    /**
     * Consolida le misure di una riga appena ultimata all'interno delle dimensioni
     * complessive del layout.
     * Aggiorna la larghezza massima del contenitore e incrementa l'altezza totale,
     * inserendo lo spazio verticale (Vgap) tra le righe successive.
     *
     * @param dim       L'istanza di {@link Dimension} che accumula le misurazioni aggregate.
     * @param rowWidth  L'ampiezza orizzontale occupata dalla riga conclusa.
     * @param rowHeight L'altezza del componente più voluminoso presente nella riga.
     */
    private void addRow(Dimension dim, int rowWidth, int rowHeight) {
        dim.width = Math.max(dim.width, rowWidth);

        if (dim.height > 0) {
            dim.height += getVgap();
        }

        dim.height += rowHeight;
    }
}