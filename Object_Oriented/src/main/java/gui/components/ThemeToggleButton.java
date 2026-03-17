package gui.components;

import com.formdev.flatlaf.FlatClientProperties;
import gui.style.GuiUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Rappresenta un pulsante Toggle personalizzato per la gestione del tema dell'applicazione (Chiaro/Scuro).
 * <p>
 * Il componente utilizza un'icona animata {@link ThemeSwitchIcon} per fornire un feedback
 * visivo durante la transizione. Implementa un meccanismo di sicurezza
 * per prevenire sovraccarichi del motore grafico FlatLaf derivanti
 * da click multipli e ravvicinati.
 */
public class ThemeToggleButton extends JToggleButton {

    /**
     * Indica se &egrave; in corso una transizione di tema.
     * Impedisce l'attivazione di ulteriori timer o ricaricamenti della finestra.
     */
    private boolean isSwitching = false;

    /**
     * Costruisce un nuovo {@code ThemeToggleButton}.
     * <p>
     * Configura l'estetica del pulsante rimuovendo i bordi standard e applicando
     * uno stile arrotondato coerente con il design moderno dell'applicazione.
     *
     * @param onReload Callback da eseguire per aggiornare l'interfaccia utente
     * dopo il cambio del tema.
     */
    public ThemeToggleButton(Runnable onReload) {
        // Assegna l'icona personalizzata che gestisce l'animazione
        setIcon(new ThemeSwitchIcon());

        // Sincronizza lo stato iniziale con la preferenza salvata
        setSelected(GuiUtils.isDarkMode());

        // Definisce l'aspetto grafico del componente
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Applica lo stile arrotondato tramite proprietà FlatLaf
        putClientProperty(FlatClientProperties.STYLE, "arc: 999");

        // Gestisce l'evento di selezione con protezione contro i click multipli
        addActionListener(e -> {

            // Verifica se una transizione &egrave; gi&agrave; stata avviata
            if (isSwitching) {
                // Ripristina lo stato grafico precedente per evitare discrepanze visive
                setSelected(!isSelected());
                return;
            }

            // Attiva il blocco logico per ignorare nuovi input
            isSwitching = true;

            // Aggiorna la configurazione globale del tema
            GuiUtils.toggleTheme();

            // Avvia la procedura di ricaricamento della GUI
            if (onReload != null) {
                /*
                 * Utilizza un javax.swing.Timer per attendere il completamento dell'animazione
                 * (500ms). Il ritardo garantisce una transizione fluida tra i temi.
                 */
                Timer timer = new Timer(550, evt -> onReload.run());
                timer.setRepeats(false);
                timer.start();
            }
        });
    }
}