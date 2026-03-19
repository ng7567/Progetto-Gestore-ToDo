package gui.style;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import com.toedter.calendar.JDateChooser;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.util.Calendar;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.Date;

/**
 * Classe di utilità per la gestione dell'interfaccia grafica.
 * Centralizza l'applicazione dei temi (Light/Dark Mode), standardizza
 * l'istanziazione e lo stile dei componenti Swing (campi di testo, pulsanti, scrollbar),
 * gestisce il rendering di file SVG e offre metodi di supporto per le finestre.
 * Ha un costruttore privato per vietarne l'istanziazione.
 *
 * @author Nunzio Grasso (Matricola: N86005509)
 * @version 1.0
 */
public class GuiUtils {

    /** Logger per tracciare eventuali errori grafici (es. mancato caricamento SVG). */
    private static final Logger LOGGER = Logger.getLogger(GuiUtils.class.getName());

    /** Nodo delle preferenze locali per salvare le impostazioni dell'utente. */
    private static final Preferences prefs = Preferences.userNodeForPackage(GuiUtils.class);

    /** Chiave usata nelle preferenze per salvare lo stato del tema scuro. */
    private static final String PREF_DARK_MODE = "dark_mode_enabled";

    /** Chiave usata nelle preferenze per salvare gli ID delle bacheche già aperte. */
    private static final String PREF_SEEN_BOARDS = "seen_boards_ids_reset";

    /** Font predefinito per l'intera applicazione. */
    private static final String FONT_FAMILY = "Segoe UI";

    // --- COLORI BASE (COSTANTI) ---

    /** Colore di sfondo globale per il tema chiaro. */
    private static final Color LIGHT_BG     = new Color(255, 255, 255);
    /** Colore di sfondo per le Card nel tema chiaro. */
    private static final Color LIGHT_CARD   = new Color(245, 245, 245);
    /** Colore del testo principale nel tema chiaro. */
    private static final Color LIGHT_TEXT   = new Color(51, 51, 51);
    /** Colore dei bordi nel tema chiaro. */
    private static final Color LIGHT_BORDER = new Color(200, 200, 200);
    /** Colore di sfondo per i campi di input nel tema chiaro. */
    private static final Color LIGHT_INPUT_BG = new Color(255, 255, 255);
    /** Colore del cursore di testo nel tema chiaro. */
    private static final Color LIGHT_CARET    = new Color(0, 0, 0);

    /** Colore di sfondo globale per il tema scuro. */
    private static final Color DARK_BG      = new Color(30, 30, 30);
    /** Colore di sfondo per le Card nel tema scuro. */
    private static final Color DARK_CARD    = new Color(45, 45, 45);
    /** Colore del testo principale nel tema scuro. */
    private static final Color DARK_TEXT    = new Color(230, 230, 230);
    /** Colore dei bordi nel tema scuro. */
    private static final Color DARK_BORDER  = new Color(80, 80, 80);
    /** Colore di sfondo per i campi di input nel tema scuro. */
    private static final Color DARK_INPUT_BG  = new Color(50, 50, 50);
    /** Colore del cursore di testo nel tema scuro. */
    private static final Color DARK_CARET     = new Color(255, 255, 255);

    /** Colore di partenza per i gradient nel tema chiaro. */
    private static final Color LIGHT_GRADIENT_START = new Color(240, 242, 245);
    /** Colore di arrivo per i gradient nel tema chiaro. */
    private static final Color LIGHT_GRADIENT_END   = new Color(220, 225, 230);

    /** Colore di partenza per i gradient nel tema scuro. */
    private static final Color DARK_GRADIENT_START = new Color(44, 62, 80);
    /** Colore di arrivo per i gradient nel tema scuro. */
    private static final Color DARK_GRADIENT_END   = new Color(30, 30, 30);

    /** Colore primario dell'applicazione. */
    public static final Color PRIMARY_COLOR = new Color(52, 152, 219);
    /** Colore primario per lo stato hover. */
    public static final Color PRIMARY_HOVER = new Color(41, 128, 185);
    /** Colore di sfondo per l'header superiore. */
    public static final Color HEADER_COLOR = new Color(44, 62, 80);
    /** Colore secondario per pulsanti o azioni di annullamento. */
    public static final Color SECONDARY_COLOR = new Color(236, 240, 241);
    /** Colore secondario per lo stato hover. */
    public static final Color SECONDARY_HOVER = new Color(189, 195, 199);
    /** Colore utilizzato per i link. */
    public static final Color LINK_COLOR = new Color(52, 152, 219);
    /** Colore utilizzato per i link nello stato hover. */
    public static final Color LINK_HOVER = new Color(41, 128, 185);

    /** Font per i titoli principali. */
    public static final Font FONT_TITLE = new Font(FONT_FAMILY, Font.BOLD, 26);
    /** Font in grassetto per etichette standard. */
    public static final Font FONT_BOLD = new Font(FONT_FAMILY, Font.BOLD, 14);
    /** Font normale per testo e campi di input. */
    public static final Font FONT_NORMAL = new Font(FONT_FAMILY, Font.PLAIN, 14);

    /** Dimensione standard per i campi di testo a riga singola. */
    public static final Dimension FIELD_DIMENSION = new Dimension(200, 35);

    // --- VARIABILI DEL TEMA (DINAMICHE) ---

    /** Indica se è attivo il tema scuro. */
    private static boolean darkMode;
    /** Colore di sfondo attuale. */
    private static Color backgroundColor;
    /** Colore del testo attuale. */
    private static Color textColor;
    /** Colore di sfondo attuale per le Card. */
    private static Color cardBackground;
    /** Colore attuale dei bordi. */
    private static Color borderColor;
    /** Colore di sfondo attuale per i campi di input. */
    private static Color inputBackground;
    /** Colore attuale del cursore nei campi di testo. */
    private static Color caretColor;
    /** Colore di partenza attuale per i gradient. */
    private static Color gradientStart;
    /** Colore di arrivo attuale per i gradient. */
    private static Color gradientEnd;

    // Blocco statico per applicare subito il tema all'avvio dell'app.
    static {
        darkMode = prefs.getBoolean(PREF_DARK_MODE, false);
        applyTheme();
    }

    /**
     * Costruttore privato.
     * Lancia un'eccezione se viene richiamato per sbaglio (es. tramite reflection).
     *
     * @throws UnsupportedOperationException Sempre lanciata al richiamo.
     */
    private GuiUtils() {
        throw new UnsupportedOperationException("Classe di utilità: istanziazione non consentita.");
    }

    // ==========================
    // GETTER DEL TEMA E COLORI
    // ==========================

    /**
     * Ritorna lo stato del tema attuale.
     *
     * @return {@code true} se il tema scuro è attivo, {@code false} altrimenti.
     */
    public static boolean isDarkMode() { return darkMode; }

    /**
     * Restituisce il colore di sfondo globale.
     *
     * @return L'oggetto {@link Color} dello sfondo globale.
     */
    public static Color getBackgroundColor() { return backgroundColor; }

    /**
     * Restituisce il colore del testo principale.
     *
     * @return L'oggetto {@link Color} del testo principale.
     */
    public static Color getTextColor() { return textColor; }

    /**
     * Restituisce il colore di sfondo delle Card.
     *
     * @return L'oggetto {@link Color} dello sfondo delle Card.
     */
    public static Color getCardBackground() { return cardBackground; }

    /**
     * Restituisce il colore di sfondo per i campi di input.
     *
     * @return L'oggetto {@link Color} di sfondo per i campi di input.
     */
    public static Color getInputBackground() { return inputBackground; }

    /**
     * Restituisce il nome del font principale in uso.
     *
     * @return Il nome del font principale in uso (es. "Segoe UI").
     */
    public static String getFontFamily() { return FONT_FAMILY; }

    /**
     * Alterna il tema tra chiaro e scuro.
     * Salva la preferenza nelle impostazioni locali e aggiorna i colori in memoria.
     */
    public static void toggleTheme() {
        darkMode = !darkMode;
        prefs.putBoolean(PREF_DARK_MODE, darkMode);
        applyTheme();
    }

    /**
     * Applica i colori corretti alle variabili in base al tema selezionato
     * e inizializza il motore FlatLaf per aggiornare l'interfaccia Swing.
     */
    private static void applyTheme() {
        try {
            if (darkMode) {
                backgroundColor = DARK_BG;
                cardBackground  = DARK_CARD;
                textColor       = DARK_TEXT;
                borderColor     = DARK_BORDER;
                inputBackground = DARK_INPUT_BG;
                caretColor      = DARK_CARET;
                gradientStart   = DARK_GRADIENT_START;
                gradientEnd     = DARK_GRADIENT_END;
                FlatMacDarkLaf.setup();
            } else {
                backgroundColor = LIGHT_BG;
                cardBackground  = LIGHT_CARD;
                textColor       = LIGHT_TEXT;
                borderColor     = LIGHT_BORDER;
                inputBackground = LIGHT_INPUT_BG;
                caretColor      = LIGHT_CARET;
                gradientStart   = LIGHT_GRADIENT_START;
                gradientEnd     = LIGHT_GRADIENT_END;
                FlatMacLightLaf.setup();
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, e, () -> "Errore nel caricamento del tema: " + e.getMessage());
        }
    }

    // ==========================
    // GRADIENT E BORDI
    // ==========================

    /**
     * Crea un pannello con uno sfondo sfumato (Gradient) verticale,
     * che si adatta automaticamente ai colori del tema attivo.
     *
     * @return Un'istanza preconfigurata di {@link JPanel}.
     */
    public static JPanel createGradientPanel() {
        return new JPanel() {
            /** {@inheritDoc} */
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                GradientPaint gp = new GradientPaint(
                        0, 0, gradientStart,
                        0, getHeight(), gradientEnd
                );
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
    }

    /**
     * Crea il bordo standard utilizzato per i campi di input di testo.
     *
     * @return Un oggetto {@link Border} che combina una linea esterna e un padding.
     */
    public static Border createStandardBorder() {
        return createCustomBorder(borderColor);
    }

    /**
     * Crea un bordo personalizzato unendo una linea colorata esterna
     * e un padding di 5 pixel.
     *
     * @param color Il colore della linea del bordo.
     * @return Un oggetto {@link Border}.
     */
    private static Border createCustomBorder(Color color) {
        return BorderFactory.createCompoundBorder(
                new LineBorder(color, 1),
                new EmptyBorder(5, 5, 5, 5)
        );
    }

    // ==========================
    // COMPONENTI INPUT
    // ==========================

    /**
     * Crea un campo di testo (JTextField) standardizzato, applicando
     * il font e i colori del tema corrente.
     *
     * @param columns La larghezza approssimativa in numero di colonne.
     * @return Un oggetto {@link JTextField} istanziato e formattato.
     */
    public static JTextField createStandardTextField(int columns) {
        JTextField txt = new JTextField(columns);
        txt.setPreferredSize(FIELD_DIMENSION);
        txt.setFont(FONT_NORMAL);
        txt.setForeground(textColor);
        txt.setBackground(inputBackground);
        txt.setCaretColor(caretColor);
        txt.setBorder(createStandardBorder());
        return txt;
    }

    /**
     * Crea un campo per la password (JPasswordField) standardizzato.
     *
     * @param columns La larghezza approssimativa in numero di colonne.
     * @return Un oggetto {@link JPasswordField} istanziato e formattato.
     */
    public static JPasswordField createPasswordField(int columns) {
        JPasswordField pf = new JPasswordField(columns);
        pf.setFont(FONT_NORMAL);
        pf.setForeground(textColor);
        pf.setBackground(inputBackground);
        pf.setCaretColor(caretColor);
        pf.setBorder(createStandardBorder());
        return pf;
    }

    /**
     * Applica uno stile arrotondato e personalizzato a un JSpinner
     * utilizzando le proprietà di FlatLaf.
     *
     * @param spinner Il modulo {@link JSpinner} da stilizzare.
     */
    public static void styleSpinner(JSpinner spinner) {
        if (spinner == null) return;

        spinner.putClientProperty("FlatLaf.style",
                "buttonStyle: button;" +
                        "arrowType: chevron;" +
                        "arc: 10;" +
                        "borderWidth: 1;" +
                        "focusWidth: 0"
        );

        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor defaultEditor) {
            JFormattedTextField textField = defaultEditor.getTextField();
            textField.setBackground(inputBackground);
            textField.setForeground(textColor);
            textField.setCaretColor(caretColor);
            textField.setFont(FONT_NORMAL);
            textField.setHorizontalAlignment(SwingConstants.CENTER);
            textField.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        }
        spinner.setBackground(inputBackground);
    }

    /**
     * Inserisce un JPasswordField all'interno di un pannello che include
     * un pulsante a forma di occhio per mostrare o nascondere la password in chiaro.
     *
     * @param passField Il campo password base da inglobare.
     * @return Un {@link JPanel} contenente il campo e il pulsante.
     */
    public static JPanel createPasswordPanelWithEye(JPasswordField passField) {
        JPanel wrapper = new JPanel(new BorderLayout(0, 0));
        wrapper.setBackground(backgroundColor);

        passField.setPreferredSize(FIELD_DIMENSION);
        passField.setFont(FONT_NORMAL);
        passField.setForeground(textColor);

        passField.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(1, 1, 1, 0, borderColor),
                new EmptyBorder(0, 10, 0, 10)
        ));

        wrapper.add(passField, BorderLayout.CENTER);

        JToggleButton btnEye = new JToggleButton();
        Icon iconClosed = loadSVG("icons/password/eye-close.svg", 18, 18);
        Icon iconOpen = loadSVG("icons/password/eye-open.svg", 18, 18);

        if (iconClosed != null) btnEye.setIcon(iconOpen); else btnEye.setText("O");
        if (iconOpen != null) btnEye.setSelectedIcon(iconClosed);

        btnEye.setBackground(Color.WHITE);
        btnEye.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnEye.setContentAreaFilled(true);
        btnEye.setFocusPainted(false);
        btnEye.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 1, borderColor));
        btnEye.setPreferredSize(new Dimension(40, 35));

        wrapper.add(btnEye, BorderLayout.EAST);

        char defaultChar = passField.getEchoChar();
        btnEye.addActionListener(e -> {
            if (btnEye.isSelected()) {
                passField.setEchoChar((char) 0);
            } else {
                passField.setEchoChar(defaultChar);
            }
        });

        return wrapper;
    }

    // ==========================
    // PULSANTI
    // ==========================

    /**
     * Applica un effetto hover al pulsante, cambiandone il colore di sfondo
     * quando il cursore del mouse ci passa sopra.
     *
     * @param btn    Il pulsante a cui applicare l'effetto.
     * @param normal Il colore di sfondo in stato normale.
     * @param hover  Il colore di sfondo in stato hover.
     */
    private static void setButtonHoverEffect(JButton btn, Color normal, Color hover) {
        btn.setBackground(normal);
        btn.addMouseListener(new MouseAdapter() {
            /** {@inheritDoc} */
            @Override public void mouseEntered(MouseEvent e) { btn.setBackground(hover); }
            /** {@inheritDoc} */
            @Override public void mouseExited(MouseEvent e) { btn.setBackground(normal); }
        });
    }

    /**
     * Applica lo stile primario (colore principale) a un pulsante,
     * rendendolo l'azione principale della vista.
     *
     * @param btn Il componente {@link JButton} da formattare.
     */
    public static void stylePrimaryButton(JButton btn) {
        configureBaseButton(btn, Color.WHITE, FONT_BOLD);
        setButtonHoverEffect(btn, PRIMARY_COLOR, PRIMARY_HOVER);
    }

    /**
     * Applica lo stile secondario (neutro) a un pulsante,
     * usato per azioni come "Annulla" o opzioni meno importanti.
     *
     * @param btn Il componente {@link JButton} da formattare.
     */
    public static void styleSecondaryButton(JButton btn) {
        configureBaseButton(btn, Color.BLACK, FONT_NORMAL);
        setButtonHoverEffect(btn, SECONDARY_COLOR, SECONDARY_HOVER);
    }

    /**
     * Configura le proprietà di base per i pulsanti (font, cursore a forma di mano, margini)
     * e rimuove gli stili predefiniti di Swing.
     *
     * @param btn  Il pulsante da configurare.
     * @param fg   Il colore del testo.
     * @param font Il font da utilizzare.
     */
    private static void configureBaseButton(JButton btn, Color fg, Font font) {
        btn.setFont(font);
        btn.setForeground(fg);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(true);
        btn.setOpaque(true);
        btn.setBorder(new EmptyBorder(10, 20, 10, 20));
    }

    /**
     * Rende un normale pulsante Swing simile a un collegamento (link) web,
     * rimuovendo lo sfondo e cambiando il colore del testo al passaggio del mouse.
     *
     * @param btn Il pulsante da modificare.
     */
    public static void styleLinkButton(JButton btn) {
        btn.setFont(FONT_NORMAL);
        btn.setForeground(LINK_COLOR);
        btn.setBackground(backgroundColor);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setOpaque(false);
        btn.addMouseListener(new MouseAdapter() {
            /** {@inheritDoc} */
            @Override public void mouseEntered(MouseEvent e) { btn.setForeground(LINK_HOVER); }
            /** {@inheritDoc} */
            @Override public void mouseExited(MouseEvent e) { btn.setForeground(LINK_COLOR); }
        });
    }

    /**
     * Applica un bordo tratteggiato al pulsante, utile per indicare
     * azioni come la creazione di un nuovo elemento o l'upload di file.
     *
     * @param btn Il bottone da formattare.
     */
    public static void styleDashedButton(JButton btn) {
        Color bgNormal = new Color(245, 245, 245);
        Color bgHover = new Color(230, 230, 230);

        btn.setForeground(Color.GRAY);
        btn.setFont(FONT_NORMAL);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        float[] dashPattern = { 10.0f, 10.0f };
        BasicStroke dashedStroke = new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dashPattern, 0.0f);
        Border dashedBorder = BorderFactory.createStrokeBorder(dashedStroke, Color.LIGHT_GRAY);
        btn.setBorder(new CompoundBorder(dashedBorder, new EmptyBorder(10, 10, 10, 10)));
        btn.setBorderPainted(true);

        setButtonHoverEffect(btn, bgNormal, bgHover);
    }

    /**
     * Aggiunge la funzionalità Annulla (CTRL+Z) e Ripeti (CTRL+Y)
     * a un'area di testo tramite tastiera.
     *
     * @param textArea La {@link JTextArea} a cui aggiungere i listener.
     */
    public static void addUndoSupport(JTextArea textArea) {
        javax.swing.undo.UndoManager undoManager = new javax.swing.undo.UndoManager();

        textArea.getDocument().addUndoableEditListener(e -> undoManager.addEdit(e.getEdit()));

        textArea.getInputMap().put(KeyStroke.getKeyStroke("control Z"), "Undo");
        textArea.getActionMap().put("Undo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (undoManager.canUndo()) {
                    undoManager.undo();
                }
            }
        });

        textArea.getInputMap().put(KeyStroke.getKeyStroke("control Y"), "Redo");
        textArea.getActionMap().put("Redo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (undoManager.canRedo()) {
                    undoManager.redo();
                }
            }
        });
    }

    /**
     * Configura lo stile di una JTextArea con i colori del tema e
     * aggiunge automaticamente il supporto per Annulla/Ripeti.
     *
     * @param textArea La {@link JTextArea} da formattare.
     */
    public static void styleTextArea(JTextArea textArea) {
        textArea.setBackground(inputBackground);
        textArea.setForeground(textColor);
        textArea.setCaretColor(caretColor);
        textArea.setBorder(createStandardBorder());
        textArea.setFont(FONT_NORMAL);
        addUndoSupport(textArea);
    }

    // ==========================
    // COMBO BOX E DATE CHOOSER
    // ==========================

    /**
     * Applica lo stile personalizzato a una JComboBox utilizzata per selezionare la priorità.
     *
     * @param combo L'oggetto dropdown parametrizzato sulle classi enumerative Priorità.
     */
    public static void stylePriorityComboBox(JComboBox<model.Priority> combo) {
        configureComboBoxUI(combo, inputBackground, textColor);
        combo.setBorder(createStandardBorder());
    }

    /**
     * Sostituisce la UI standard della JComboBox per applicare colori
     * personalizzati sia allo sfondo che alla freccia (dropdown).
     *
     * @param combo      La JComboBox da modificare.
     * @param bgColor    Il colore di sfondo.
     * @param arrowColor Il colore per la freccia.
     */
    private static void configureComboBoxUI(JComboBox<model.Priority> combo, Color bgColor, Color arrowColor) {
        combo.setBackground(bgColor);
        combo.setForeground(arrowColor);
        combo.setFont(FONT_NORMAL);

        combo.setRenderer(new PriorityComboBoxRenderer(bgColor));
        combo.setUI(new PriorityComboBoxUI(bgColor, arrowColor));

        combo.addActionListener(e -> {
            if (combo.getSelectedItem() instanceof model.Priority selected) {
                combo.setForeground(selected.getColor());
            }
        });

        if (combo.getSelectedItem() instanceof model.Priority selected) {
            combo.setForeground(selected.getColor());
        }
    }

    /**
     * Applica i colori del tema corrente al componente esterno JDateChooser.
     *
     * @param dateChooser Il componente calendario.
     */
    public static void styleDateChooser(com.toedter.calendar.JDateChooser dateChooser) {
        if (dateChooser == null) return;

        dateChooser.setBackground(inputBackground);
        dateChooser.setForeground(textColor);
        dateChooser.setBorder(null);

        JComponent editorComp = dateChooser.getDateEditor().getUiComponent();
        if (editorComp instanceof JTextField editor) {
            editor.setBackground(inputBackground);
            editor.setForeground(textColor);
            editor.setCaretColor(caretColor);
            editor.setBorder(createStandardBorder());

            editor.addPropertyChangeListener("foreground", evt -> {
                if (!evt.getNewValue().equals(textColor) && !evt.getNewValue().equals(Color.RED)) {
                    editor.setForeground(textColor);
                }
            });
        }

        JButton calendarButton = dateChooser.getCalendarButton();
        calendarButton.setBackground(inputBackground);
        calendarButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        calendarButton.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));

        com.toedter.calendar.JCalendar calendar = dateChooser.getJCalendar();
        if (calendar != null) {
            calendar.setBackground(cardBackground);
            calendar.setDecorationBackgroundColor(inputBackground);
            calendar.setSundayForeground(new Color(231, 76, 60));
            calendar.setWeekdayForeground(textColor);

            if (calendar.getDayChooser() != null && calendar.getDayChooser().getDayPanel() != null) {
                calendar.getDayChooser().getDayPanel().setBackground(cardBackground);
            }
        }
    }

    // ==========================
    // UTILITY VARIE
    // ==========================

    /**
     * Istanzia una JLabel standard con il font e il colore del tema corrente.
     *
     * @param text Il testo da mostrare.
     * @return La {@link JLabel} creata.
     */
    public static JLabel createLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_NORMAL);
        lbl.setForeground(textColor);
        return lbl;
    }

    /**
     * Istanzia una JLabel in grassetto con il colore del tema corrente.
     *
     * @param text Il testo da mostrare.
     * @return La {@link JLabel} in grassetto.
     */
    public static JLabel createBoldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_BOLD);
        lbl.setForeground(textColor);
        return lbl;
    }

    /**
     * Imposta la stessa dimensione fissa per tutti i pulsanti passati
     * come parametro, in modo da renderli uniformi nel layout.
     *
     * @param buttons Variabile (varargs) di moduli {@link JButton}.
     */
    public static void makeSameSize(JButton... buttons) {
        Dimension size = new Dimension(110, 40);
        for (JButton btn : buttons) {
            btn.setPreferredSize(size);
        }
    }

    /**
     * Carica un file SVG dal filesystem e lo ridimensiona in base alle misure fornite.
     *
     * @param path   Il percorso del file.
     * @param width  Larghezza desiderata in pixel.
     * @param height Altezza desiderata in pixel.
     * @return L'oggetto {@link Icon} creato, o {@code null} in caso di errore.
     */
    public static Icon loadSVG(String path, int width, int height) {
        try {
            return new FlatSVGIcon(path, width, height);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, e, () -> "Errore caricamento SVG: " + path);
            return null;
        }
    }

    /**
     * Carica un file SVG e ne cambia il colore, riempiendolo con una tinta unita.
     *
     * @param path   Il percorso del file.
     * @param width  Larghezza desiderata in pixel.
     * @param height Altezza desiderata in pixel.
     * @param color  Il colore da usare per ricolorare l'icona.
     * @return L'oggetto {@link Icon} ricolorato, o {@code null} in caso di errore.
     */
    public static Icon loadSVG(String path, int width, int height, Color color) {
        try {
            FlatSVGIcon icon = new FlatSVGIcon(path, width, height);
            icon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> color));
            return icon;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, e, () -> "Errore caricamento SVG: " + path);
            return null;
        }
    }

    /**
     * Chiude il frame attuale e ne istanzia uno nuovo passato tramite la funzione Supplier,
     * mantenendone le dimensioni e la posizione sullo schermo (utile per cambiare tema senza far saltare la finestra).
     *
     * @param currentFrame Il frame attualmente aperto.
     * @param frameCreator La funzione (Supplier) che restituisce il nuovo frame.
     */
    public static void reloadWindow(JFrame currentFrame, Supplier<JFrame> frameCreator) {
        Point location = currentFrame.getLocation();
        Dimension size = currentFrame.getSize();
        int state = currentFrame.getExtendedState();
        currentFrame.dispose();
        JFrame newFrame = frameCreator.get();
        newFrame.setSize(size);
        newFrame.setLocation(location);
        newFrame.setExtendedState(state);
        newFrame.setVisible(true);
    }

    /**
     * Passa da una finestra all'altra mantenendo la stessa dimensione e posizione,
     * per poi chiudere il vecchio frame.
     *
     * @param fromFrame Il frame di partenza che verrà chiuso.
     * @param toFrame   Il frame di destinazione che verrà aperto.
     */
    public static void transition(JFrame fromFrame, JFrame toFrame) {
        int state = fromFrame.getExtendedState();
        if((state & Frame.MAXIMIZED_BOTH) == 0) {
            toFrame.setBounds(fromFrame.getBounds());
        } else {
            toFrame.setExtendedState(state);
        }
        toFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        toFrame.setVisible(true);
        fromFrame.dispose();
    }

    /**
     * Calcola se usare il colore del testo chiaro o scuro in base alla luminosità
     * del colore di sfondo passato come parametro.
     *
     * @param background Il colore di sfondo da analizzare.
     * @return Il colore per il testo che garantisce il miglior contrasto.
     */
    public static Color getContrastColor(Color background){
        double brightness = (0.299 * background.getRed() + 0.587 * background.getGreen() + 0.114 * background.getBlue());
        return brightness < 128 ? DARK_TEXT : LIGHT_TEXT;
    }

    // ==========================
    // COMPONENTI GRAFICI CUSTOM
    // ==========================

    /**
     * Crea un JScrollPane trasparente e minimalista, nascondendo la barra
     * di scorrimento finché non viene utilizzata.
     *
     * @param content Il componente interno che deve essere scorrevole.
     * @return Il {@link JScrollPane} configurato.
     */
    public static JScrollPane createModernScrollPane(JComponent content) {
        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.setViewportBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        scrollPane.getVerticalScrollBar().setOpaque(false);
        scrollPane.getHorizontalScrollBar().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        scrollPane.getHorizontalScrollBar().setUI(new ModernScrollBarUI());
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return scrollPane;
    }

    /**
     * UI personalizzata per le scrollbar che le rende minimali.
     * Rimuove i pulsanti freccia e disegna solo un cursore arrotondato
     * che diventa visibile al passaggio del mouse o al trascinamento.
     */
    private static class ModernScrollBarUI extends BasicScrollBarUI {
        /** {@inheritDoc} */
        @Override
        protected void configureScrollBarColors() {
            this.thumbColor = new Color(0, 0, 0, 0);
            this.trackColor = new Color(0, 0, 0, 0);
        }

        /** {@inheritDoc} */
        @Override
        protected JButton createDecreaseButton(int orientation) { return createZeroButton(); }

        /** {@inheritDoc} */
        @Override
        protected JButton createIncreaseButton(int orientation) { return createZeroButton(); }

        /**
         * Crea un pulsante invisibile (0x0 pixel).
         * @return Un bottone vuoto.
         */
        private JButton createZeroButton() {
            JButton btn = new JButton();
            btn.setPreferredSize(new Dimension(0, 0));
            btn.setMinimumSize(new Dimension(0, 0));
            btn.setMaximumSize(new Dimension(0, 0));
            return btn;
        }

        /** {@inheritDoc} */
        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
            // Intenzionalmente vuoto per non disegnare la traccia di scorrimento
        }

        /** {@inheritDoc} */
        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
            if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) return;

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Rectangle geom = calculateThumbGeometry(thumbBounds);
            Color drawColor = calculateThumbColor();

            g2.setColor(drawColor);
            g2.fillRoundRect(geom.x, geom.y, geom.width, geom.height, 10, 10);
            g2.dispose();
        }

        /**
         * Calcola le dimensioni e la posizione del cursore della scrollbar.
         *
         * @param thumbBounds Area rettangolare destinata al cursore.
         * @return Il rettangolo ricalcolato.
         */
        private Rectangle calculateThumbGeometry(Rectangle thumbBounds) {
            final int thumbSize = 8;
            int x;
            int y;
            int w;
            int h;

            if (scrollbar.getOrientation() == Adjustable.VERTICAL) {
                x = thumbBounds.x + (thumbBounds.width - thumbSize) / 2;
                y = thumbBounds.y + 2;
                w = thumbSize;
                h = thumbBounds.height - 4;
            } else {
                x = thumbBounds.x + 2;
                y = thumbBounds.y + (thumbBounds.height - thumbSize) / 2;
                w = thumbBounds.width - 4;
                h = thumbSize;
            }
            return new Rectangle(x, y, w, h);
        }

        /**
         * Determina il colore del cursore aggiungendo trasparenza e
         * scurendolo se l'utente sta trascinando la barra.
         *
         * @return Il colore finale per il cursore.
         */
        private Color calculateThumbColor() {
            Color baseColor = UIManager.getColor("ScrollBar.thumb");
            if (baseColor == null) {
                baseColor = Color.GRAY;
            }
            if (isDragging || isThumbRollover()) {
                baseColor = baseColor.darker();
            }
            return new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 180);
        }

        /** {@inheritDoc} */
        @Override
        public Dimension getPreferredSize(JComponent c) {
            return new Dimension(14, super.getPreferredSize(c).height);
        }
    }

    /**
     * Sostituisce il disegno predefinito della JCheckBox con uno stile personalizzato
     * circolare, disegnando il segno di spunta a mano con Java2D.
     *
     * @param checkBox La JCheckBox da modificare.
     */
    public static void styleCheckbox(JCheckBox checkBox) {
        checkBox.setOpaque(false);
        checkBox.setFocusPainted(false);
        checkBox.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        final int iconSize = 22;

        checkBox.setIcon(new Icon() {
            /** {@inheritDoc} */
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.GRAY);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(x + 1, y + 1, iconSize - 2, iconSize - 2);
                g2.dispose();
            }
            /** {@inheritDoc} */
            @Override public int getIconWidth() { return iconSize; }
            /** {@inheritDoc} */
            @Override public int getIconHeight() { return iconSize; }
        });

        checkBox.setSelectedIcon(new Icon() {
            /** {@inheritDoc} */
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(PRIMARY_COLOR);
                g2.fillOval(x + 1, y + 1, iconSize - 2, iconSize - 2);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                java.awt.geom.Path2D.Float checkMark = new java.awt.geom.Path2D.Float();
                checkMark.moveTo(x + 6f, y + 11f);
                checkMark.lineTo(x + 10f, y + 15f);
                checkMark.lineTo(x + 16f, y + 7f);

                g2.draw(checkMark);
                g2.dispose();
            }
            /** {@inheritDoc} */
            @Override public int getIconWidth() { return iconSize; }
            /** {@inheritDoc} */
            @Override public int getIconHeight() { return iconSize; }
        });
    }

    /**
     * Controlla nelle preferenze locali se l'ID della bacheca è già stato aperto
     * dall'utente su questo PC. Serve per mostrare il badge "Nuovo".
     *
     * @param boardId L'ID della bacheca.
     * @return {@code true} se la bacheca non è mai stata aperta, {@code false} altrimenti.
     */
    public static boolean isBoardNew(int boardId) {
        String seenIds = prefs.get(PREF_SEEN_BOARDS, "");
        String[] ids = seenIds.split(",");
        String idStr = String.valueOf(boardId);
        for (String id : ids) {
            if (id.equals(idStr)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Salva l'ID della bacheca nelle preferenze locali in modo che non venga
     * più considerata come "Nuova" ai successivi avvii.
     *
     * @param boardId L'ID della bacheca da segnare come letta.
     */
    public static void markBoardAsSeen(int boardId) {
        if (!isBoardNew(boardId)) return;
        String seenIds = prefs.get(PREF_SEEN_BOARDS, "");
        if (seenIds.isEmpty()) {
            seenIds = String.valueOf(boardId);
        } else {
            seenIds += "," + boardId;
        }
        prefs.put(PREF_SEEN_BOARDS, seenIds);
    }

    /**
     * Applica uno stile arrotondato personalizzato alle checkbox usate nei menu dei filtri.
     *
     * @param checkBox La checkbox da formattare.
     */
    public static void styleFilterCheckBox(JCheckBox checkBox) {
        checkBox.setOpaque(false);
        checkBox.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        checkBox.setFont(FONT_NORMAL);
        checkBox.setForeground(textColor);
        checkBox.setFocusPainted(false);

        int size = 20;

        checkBox.setIcon(new Icon() {
            /** {@inheritDoc} */
            @Override public int getIconWidth() { return size; }
            /** {@inheritDoc} */
            @Override public int getIconHeight() { return size; }
            /** {@inheritDoc} */
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(borderColor);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(x, y, size-1, size-1, 4, 4);
                g2.dispose();
            }
        });

        checkBox.setSelectedIcon(new Icon() {
            /** {@inheritDoc} */
            @Override public int getIconWidth() { return size; }
            /** {@inheritDoc} */
            @Override public int getIconHeight() { return size; }
            /** {@inheritDoc} */
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(PRIMARY_COLOR);
                g2.fillRoundRect(x, y, size-1, size-1, 4, 4);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(x + 4, y + 10, x + 8, y + 14);
                g2.drawLine(x + 8, y + 14, x + 16, y + 6);
                g2.dispose();
            }
        });
    }

    /**
     * Classe che disegna la UI personalizzata per la combobox delle priorità.
     */
    private static class PriorityComboBoxUI extends BasicComboBoxUI {
        /** Colore di sfondo del menu a tendina. */
        private final Color bgColor;
        /** Colore della freccia per l'apertura del menu. */
        private final Color arrowColor;

        /**
         * Crea la UI personalizzata.
         * @param bgColor Il colore di sfondo.
         * @param arrowColor Il colore per la freccia.
         */
        public PriorityComboBoxUI(Color bgColor, Color arrowColor) {
            this.bgColor = bgColor;
            this.arrowColor = arrowColor;
        }

        /** {@inheritDoc} */
        @Override
        protected JButton createArrowButton() {
            BasicArrowButton btn = new BasicArrowButton(
                    SwingConstants.SOUTH, bgColor, bgColor, arrowColor, bgColor
            );
            btn.setBorder(BorderFactory.createEmptyBorder());
            return btn;
        }

        /** {@inheritDoc} */
        @Override
        public void paintCurrentValue(Graphics g, Rectangle bounds, boolean hasFocus) {
            g.setColor(bgColor);
            g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);

            ListCellRenderer<Object> renderer = comboBox.getRenderer();

            boolean renderFocus = hasFocus && !isPopupVisible(comboBox);
            Component c = renderer.getListCellRendererComponent(listBox, comboBox.getSelectedItem(), -1, renderFocus, false);

            c.setBackground(bgColor);
            c.setFont(comboBox.getFont());

            if (comboBox.getSelectedItem() instanceof model.Priority p) {
                c.setForeground(p.getColor());
            }

            currentValuePane.paintComponent(g, c, comboBox, bounds.x, bounds.y, bounds.width, bounds.height, hasFocus);
        }
    }

    /**
     * Renderizza ogni singola voce della combobox applicando il colore
     * specifico della priorità associata.
     */
    private static class PriorityComboBoxRenderer extends DefaultListCellRenderer {
        /** Colore di sfondo base per le voci non selezionate. */
        private final Color bgColor;

        /**
         * Istanzia il renderer.
         * @param bgColor Il colore di sfondo.
         */
        public PriorityComboBoxRenderer(Color bgColor) {
            this.bgColor = bgColor;
        }

        /** {@inheritDoc} */
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof model.Priority p) {
                setText(p.getLabel());
                if (isSelected && index != -1) {
                    setBackground(PRIMARY_COLOR);
                    setForeground(Color.WHITE);
                } else {
                    setBackground(bgColor);
                    setForeground(p.getColor());
                }
            }
            return this;
        }
    }

    /**
     * Apre il link nel browser web predefinito di sistema.
     * Aggiunge "https://" in automatico se l'URL passato non lo contiene.
     *
     * @param urlString Il link da aprire.
     */
    public static void openWebpage(String urlString) {
        if (urlString == null || urlString.trim().isEmpty()) {
            return;
        }

        try {
            String finalUrl = urlString;

            String httpsPrefix = "https" + "://";
            String httpPrefix = "http" + "://";

            if (!finalUrl.startsWith(httpsPrefix) && !finalUrl.startsWith(httpPrefix)) {
                finalUrl = httpsPrefix + finalUrl;
            }

            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(finalUrl));
            } else {
                LOGGER.log(Level.WARNING, "L''azione di browsing non è supportata su questo sistema.");
            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, e, () -> "Impossibile aprire l''URL specificato: " + urlString);

            JOptionPane.showMessageDialog(null,
                    "Errore durante l'apertura del link: " + e.getMessage(),
                    "Errore Browser",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Imposta l'icona dell'applicazione per la finestra specificata,
     * che diventerà visibile nella barra delle applicazioni del sistema operativo.
     *
     * @param frame Il JFrame a cui assegnare l'icona.
     */
    public static void setAppIcon(JFrame frame) {
        try {
            com.formdev.flatlaf.extras.FlatSVGIcon icon = new com.formdev.flatlaf.extras.FlatSVGIcon("icons/app/app_icon.svg");

            Image image = icon.derive(128, 128).getImage();

            if (image != null) {
                frame.setIconImage(image);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, e, () -> "Errore nel caricamento icona: " + e.getMessage());
        }
    }

    /**
     * Mostra un pop-up di conferma standard con pulsanti "Sì" e "No".
     *
     * @param parent      Il componente genitore da cui dipende il pop-up.
     * @param message     Il messaggio da mostrare all'utente.
     * @param title       Il titolo della finestra.
     * @param messageType Il tipo di icona (es. Error, Warning, Information).
     * @return {@code true} se l'utente clicca "Sì", {@code false} altrimenti.
     */
    public static boolean showConfirmDialog(Component parent, String message, String title, int messageType) {
        Object[] options = {"Sì", "No"};
        int result = JOptionPane.showOptionDialog(
                parent,
                message,
                title,
                JOptionPane.YES_NO_OPTION,
                messageType,
                null,
                options,
                options[1]
        );

        return result == 0;
    }

    /**
     * Oggetto (Record) usato per restituire in blocco i componenti grafici che
     * costituiscono il selettore combinato di data e ora.
     *
     * @param panel       Il pannello contenitore di base.
     * @param dateChooser Il componente per scegliere la data.
     * @param timeSpinner Il componente per scegliere l'orario.
     */
    public record DateTimePicker(JPanel panel, JDateChooser dateChooser, JSpinner timeSpinner) {}

    /**
     * Istanzia un pannello che contiene sia un selettore di date (JDateChooser)
     * che uno spinner per l'orario. Sincronizza i due componenti in modo che
     * l'orario sia disabilitato se non è stata scelta una data.
     *
     * @param initialDate La data e l'ora da mostrare all'avvio (se presenti).
     * @return Il record {@link DateTimePicker} contenente i componenti creati.
     */
    public static DateTimePicker createDateTimePicker(Date initialDate) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.setOpaque(false);

        // Configurazione DateChooser
        JDateChooser dc = new JDateChooser();
        dc.getJCalendar().setMinSelectableDate(new Date());
        dc.setDateFormatString("dd/MM/yyyy");
        styleDateChooser(dc);
        dc.setPreferredSize(new Dimension(150, 30));

        // Configurazione Spinner Ora
        SpinnerDateModel model = new SpinnerDateModel();
        JSpinner spinner = new JSpinner(model);
        JSpinner.DateEditor editor = new JSpinner.DateEditor(spinner, "HH:mm");
        spinner.setEditor(editor);
        spinner.setPreferredSize(new Dimension(80, 30));
        styleSpinner(spinner);

        // Imposta i valori iniziali
        if (initialDate != null) {
            dc.setDate(initialDate);
            spinner.setValue(initialDate);
            spinner.setEnabled(true);
        } else {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            spinner.setValue(cal.getTime());
            spinner.setEnabled(false);
        }

        // Abilita lo spinner dell'orario solo se la data è stata impostata
        dc.addPropertyChangeListener("date", evt ->
                spinner.setEnabled(dc.getDate() != null)
        );

        panel.add(dc);
        panel.add(Box.createHorizontalStrut(10));
        panel.add(spinner);

        return new DateTimePicker(panel, dc, spinner);
    }

    /**
     * Formatta un testo lungo per un tooltip, troncandolo dopo una certa lunghezza
     * e usando tag HTML per forzare l'andata a capo automatica, in modo che
     * il testo non esca dallo schermo.
     *
     * @param text      Il testo da mostrare nel tooltip.
     * @param maxLength Il numero massimo di caratteri prima di aggiungere i tre punti (...).
     * @return La stringa formattata con tag HTML.
     */
    public static String formatTooltip(String text, int maxLength) {
        if (text == null || text.trim().isEmpty()) return null;

        String cleanText = text.replace("\n", " ");
        if (cleanText.length() > maxLength) {
            cleanText = cleanText.substring(0, maxLength) + "...";
        }

        return "<html><p width='250'>" + cleanText + "</p></html>";
    }
}