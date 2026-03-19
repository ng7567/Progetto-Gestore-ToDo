package gui.components;

import com.formdev.flatlaf.FlatClientProperties;
import gui.style.GuiUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;

/**
 * Pulsante grafico (Toggle Button) usato per cambiare il tema dell'applicazione.
 * Usa l'icona animata {@link ThemeSwitchIcon} e blocca l'intera finestra durante
 * la transizione per evitare click ripetuti o l'apertura di finestre multiple.
 *
 * @author Nunzio Grasso (Matricola: N86005509)
 * @version 1.0
 */
public class ThemeToggleButton extends JToggleButton {

    /**
     * Crea il pulsante per il cambio tema.
     * Applica un glass pane (pannello invisibile) sopra la finestra
     * durante l'animazione per "congelare" l'interfaccia e ignorare i click.
     *
     * @param onReload La funzione che verrà eseguita alla fine dell'animazione
     * per applicare il nuovo tema all'intera finestra.
     */
    public ThemeToggleButton(Runnable onReload) {
        // Assegna l'icona animata personalizzata
        setIcon(new ThemeSwitchIcon());

        // Imposta lo stato iniziale in base al tema attuale
        setSelected(GuiUtils.isDarkMode());

        // Rimuove bordi e sfondi per un look pulito
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);

        // Impedisce l'attivazione ripetuta tramite il tasto Spazio o Invio
        setFocusable(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Rende il pulsante completamente arrotondato
        putClientProperty(FlatClientProperties.STYLE, "arc: 999");

        // Azione eseguita quando l'utente clicca il pulsante
        addActionListener(e -> {

            // Trova la finestra principale in cui si trova questo bottone
            Window window = SwingUtilities.getWindowAncestor(this);

            if (window instanceof JFrame frame) {
                // Recupera il GlassPane
                Component glassPane = frame.getGlassPane();

                // Mostra il cursore di caricamento (la clessidra o rotellina)
                glassPane.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                // Aggiunge un "muro" che assorbe tutti i click del mouse a vuoto
                glassPane.addMouseListener(new MouseAdapter() {});

                // Rende attivo il GlassPane: da questo momento l'app è congelata
                glassPane.setVisible(true);
            }

            // Cambia le variabili di sistema per il tema
            GuiUtils.toggleTheme();

            // Avvia il timer per l'animazione
            if (onReload != null) {
                Timer timer = new Timer(550, evt -> {
                    onReload.run();

                    // NOTA: Non serve sbloccare il GlassPane, perché la vecchia
                    // finestra verrà distrutta e sostituita da una nuova
                });
                timer.setRepeats(false);
                timer.start();
            }
        });
    }
}