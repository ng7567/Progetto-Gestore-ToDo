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
 * Centralizza la gestione dei temi (Chiaro/Scuro), la configurazione degli stili
 * dei componenti Swing, la gestione delle icone e delle palette colori.
 * Implementata con pattern costruttore privato per impedirne l'istanziazione.
 */
public class GuiUtils {

    private static final Logger LOGGER = Logger.getLogger(GuiUtils.class.getName());
    private static final Preferences prefs = Preferences.userNodeForPackage(GuiUtils.class);
    private static final String PREF_DARK_MODE = "dark_mode_enabled";
    private static final String PREF_SEEN_BOARDS = "seen_boards_ids_reset";

    private static final String FONT_FAMILY = "Segoe UI";

    // PALETTE COLORI BASE COSTANTI
    private static final Color LIGHT_BG     = new Color(255, 255, 255);
    private static final Color LIGHT_CARD   = new Color(245, 245, 245);
    private static final Color LIGHT_TEXT   = new Color(51, 51, 51);
    private static final Color LIGHT_BORDER = new Color(200, 200, 200);
    private static final Color LIGHT_INPUT_BG = new Color(255, 255, 255);
    private static final Color LIGHT_CARET    = new Color(0, 0, 0);

    private static final Color DARK_BG      = new Color(30, 30, 30);
    private static final Color DARK_CARD    = new Color(45, 45, 45);
    private static final Color DARK_TEXT    = new Color(230, 230, 230);
    private static final Color DARK_BORDER  = new Color(80, 80, 80);
    private static final Color DARK_INPUT_BG  = new Color(50, 50, 50);
    private static final Color DARK_CARET     = new Color(255, 255, 255);

    private static final Color LIGHT_GRADIENT_START = new Color(240, 242, 245);
    private static final Color LIGHT_GRADIENT_END   = new Color(220, 225, 230);

    private static final Color DARK_GRADIENT_START = new Color(44, 62, 80);
    private static final Color DARK_GRADIENT_END   = new Color(30, 30, 30);

    public static final Color PRIMARY_COLOR = new Color(52, 152, 219);
    public static final Color PRIMARY_HOVER = new Color(41, 128, 185);
    public static final Color HEADER_COLOR = new Color(44, 62, 80);
    public static final Color SECONDARY_COLOR = new Color(236, 240, 241);
    public static final Color SECONDARY_HOVER = new Color(189, 195, 199);
    public static final Color LINK_COLOR = new Color(52, 152, 219);
    public static final Color LINK_HOVER = new Color(41, 128, 185);

    public static final Font FONT_TITLE = new Font(FONT_FAMILY, Font.BOLD, 26);
    public static final Font FONT_BOLD = new Font(FONT_FAMILY, Font.BOLD, 14);
    public static final Font FONT_NORMAL = new Font(FONT_FAMILY, Font.PLAIN, 14);

    public static final Dimension FIELD_DIMENSION = new Dimension(200, 35);

    // VARIABILI TEMA CORRENTI
    private static boolean darkMode;
    private static Color backgroundColor;
    private static Color textColor;
    private static Color cardBackground;
    private static Color borderColor;
    private static Color inputBackground;
    private static Color caretColor;
    private static Color gradientStart;
    private static Color gradientEnd;

    static {
        darkMode = prefs.getBoolean(PREF_DARK_MODE, false);
        applyTheme();
    }

    /**
     * Costruttore privato che impedisce l'istanziazione di questa classe di utilità.
     * Lancia un'eccezione se invocato tramite reflection.
     */
    private GuiUtils() {
        throw new UnsupportedOperationException("Classe di utilità: istanziazione non consentita.");
    }

    // ==========================
    // GETTER DEL TEMA E COLORI
    // ==========================

    /**
     * Restituisce lo stato attuale del tema (Chiaro/Scuro).
     *
     * @return {@code true} se è attivo il tema scuro, {@code false} altrimenti.
     */
    public static boolean isDarkMode() { return darkMode; }

    /**
     * Restituisce il colore di sfondo principale corrente.
     *
     * @return L'oggetto {@link Color} rappresentante lo sfondo.
     */
    public static Color getBackgroundColor() { return backgroundColor; }

    /**
     * Restituisce il colore principale del testo corrente.
     *
     * @return L'oggetto {@link Color} rappresentante il testo.
     */
    public static Color getTextColor() { return textColor; }

    /**
     * Restituisce il colore di sfondo utilizzato per le schede (Card).
     *
     * @return L'oggetto {@link Color} per lo sfondo delle schede.
     */
    public static Color getCardBackground() { return cardBackground; }

    /**
     * Restituisce il colore di sfondo per i campi di input.
     *
     * @return L'oggetto {@link Color} per i campi di input.
     */
    public static Color getInputBackground() { return inputBackground; }

    /**
     * Restituisce il nome della famiglia di font principale utilizzata dall'applicazione.
     *
     * @return La stringa contenente il nome del font.
     */
    public static String getFontFamily() { return FONT_FAMILY; }

    /**
     * Alterna lo stato del tema tra chiaro e scuro.
     * Salva la preferenza in modo persistente nelle impostazioni dell'utente
     * e aggiorna istantaneamente le variabili di colore globali.
     */
    public static void toggleTheme() {
        darkMode = !darkMode;
        prefs.putBoolean(PREF_DARK_MODE, darkMode);
        applyTheme();
    }

    /**
     * Applica i colori corretti alle variabili interne in base al tema selezionato
     * ed inizializza il Look and Feel FlatLaf corrispondente.
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
    // SEZIONE GRADIENT E BORDI
    // ==========================

    /**
     * Crea un pannello con uno sfondo sfumato lineare verticale basato sul tema corrente.
     *
     * @return Un'istanza personalizzata di {@link JPanel}.
     */
    public static JPanel createGradientPanel() {
        return new JPanel() {
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
     * Genera il bordo standard utilizzato per i componenti di input testuale.
     *
     * @return Un oggetto {@link Border} composto (linea e margine interno).
     */
    public static Border createStandardBorder() {
        return createCustomBorder(borderColor);
    }

    /**
     * Crea un bordo composto personalizzato basato su un colore specifico.
     * Combina una linea esterna solida con un margine interno (padding)
     * per migliorare la leggibilità del testo inserito nei campi.
     *
     * @param color Il colore della linea del bordo.
     * @return Un oggetto {@link Border} configurato.
     */
    private static Border createCustomBorder(Color color) {
        return BorderFactory.createCompoundBorder(
                new LineBorder(color, 1),
                new EmptyBorder(5, 5, 5, 5)
        );
    }

    // ==========================
    // SEZIONE COMPONENTI INPUT
    // ==========================

    /**
     * Genera un campo di testo standardizzato per l'applicazione, applicando colori e font del tema.
     *
     * @param columns Il numero di colonne (larghezza approssimativa) del campo.
     * @return Un {@link JTextField} formattato.
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
     * Genera un campo per l'inserimento della password standardizzato.
     *
     * @param columns Il numero di colonne (larghezza approssimativa) del campo.
     * @return Un {@link JPasswordField} formattato.
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
     * Applica uno stile moderno personalizzato a un componente JSpinner.
     *
     * @param spinner Lo {@link JSpinner} da stilizzare.
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
     * Avvolge un campo password in un pannello contenente un pulsante a forma di occhio
     * per visualizzare/nascondere il testo in chiaro.
     *
     * @param passField Il {@link JPasswordField} da inserire.
     * @return Un {@link JPanel} contenente il campo testuale e il pulsante.
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
    // SEZIONE PULSANTI
    // ==========================

    /**
     * Applica un effetto visivo dinamico di hover a un bottone.
     * Registra un listener che modifica il colore di sfondo quando il cursore
     * entra o esce dall'area del componente.
     *
     * @param btn    Il {@link JButton} a cui applicare l'effetto.
     * @param normal Il colore di sfondo in stato normale.
     * @param hover  Il colore di sfondo in stato attivo (mouse sopra).
     */
    private static void setButtonHoverEffect(JButton btn, Color normal, Color hover) {
        btn.setBackground(normal);
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.setBackground(hover); }
            @Override public void mouseExited(MouseEvent e) { btn.setBackground(normal); }
        });
    }

    /**
     * Applica lo stile primario (colore principale del brand) a un bottone.
     *
     * @param btn Il {@link JButton} da stilizzare.
     */
    public static void stylePrimaryButton(JButton btn) {
        configureBaseButton(btn, Color.WHITE, FONT_BOLD);
        setButtonHoverEffect(btn, PRIMARY_COLOR, PRIMARY_HOVER);
    }

    /**
     * Applica lo stile secondario (neutro) a un bottone.
     *
     * @param btn Il {@link JButton} da stilizzare.
     */
    public static void styleSecondaryButton(JButton btn) {
        configureBaseButton(btn, Color.BLACK, FONT_NORMAL);
        setButtonHoverEffect(btn, SECONDARY_COLOR, SECONDARY_HOVER);
    }

    /**
     * Configura le proprietà comuni per i bottoni dell'applicazione.
     * Imposta font, cursore a mano, margini e disabilita gli effetti
     * grafici predefiniti di Swing per consentire uno stile custom.
     *
     * @param btn  Il bottone da processare.
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
     * Formatta un bottone in modo che assomigli a un link ipertestuale testuale.
     *
     * @param btn Il {@link JButton} da stilizzare.
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
            @Override public void mouseEntered(MouseEvent e) { btn.setForeground(LINK_HOVER); }
            @Override public void mouseExited(MouseEvent e) { btn.setForeground(LINK_COLOR); }
        });
    }

    /**
     * Applica un bordo tratteggiato personalizzato a un bottone, utile per
     * azioni come l'inserimento di nuovi elementi o upload.
     *
     * @param btn Il {@link JButton} da stilizzare.
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
     * Aggiunge la funzionalità di Annulla (Ctrl+Z) e Ripristina (Ctrl+Y)
     * a un'area di testo specifica.
     *
     * @param textArea L'area di testo a cui applicare il supporto Undo/Redo.
     */
    public static void addUndoSupport(JTextArea textArea) {
        javax.swing.undo.UndoManager undoManager = new javax.swing.undo.UndoManager();

        // Registra ogni singola modifica del documento
        textArea.getDocument().addUndoableEditListener(e -> undoManager.addEdit(e.getEdit()));

        // Configura la combinazione Ctrl + Z (Annulla)
        textArea.getInputMap().put(KeyStroke.getKeyStroke("control Z"), "Undo");
        textArea.getActionMap().put("Undo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (undoManager.canUndo()) {
                    undoManager.undo();
                }
            }
        });

        // Configura la combinazione Ctrl + Y (Ripristina)
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
     * Configura una JTextArea allineandola al tema corrente.
     * Aggiunge automaticamente il supporto per Annulla/Ripristina.
     *
     * @param textArea La {@link JTextArea} da aggiornare.
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
     * Modifica lo stile di una ComboBox dedicata alle priorità.
     *
     * @param combo La {@link JComboBox} tipizzata su {@link model.Priority}.
     */
    public static void stylePriorityComboBox(JComboBox<model.Priority> combo) {
        configureComboBoxUI(combo, inputBackground, textColor);
        combo.setBorder(createStandardBorder());
    }

    /**
     * Configura i colori e i renderer per una ComboBox.
     * Gestisce la logica di ricolorazione dinamica del testo in base alla
     * priorità selezionata e associa la UI personalizzata per le frecce.
     *
     * @param combo      La ComboBox da configurare.
     * @param bgColor    Il colore di sfondo del componente.
     * @param arrowColor Il colore per l'indicatore di selezione (freccia).
     */
    private static void configureComboBoxUI(JComboBox<model.Priority> combo, Color bgColor, Color arrowColor) {
        combo.setBackground(bgColor);
        combo.setForeground(arrowColor);
        combo.setFont(FONT_NORMAL);

        // Assegna il Renderer personalizzato (estratto)
        combo.setRenderer(new PriorityComboBoxRenderer(bgColor));

        // Assegna la UI personalizzata (estratta)
        combo.setUI(new PriorityComboBoxUI(bgColor, arrowColor));

        // Listener per il cambio di selezione
        combo.addActionListener(e -> {
            if (combo.getSelectedItem() instanceof model.Priority selected) {
                combo.setForeground(selected.getColor());
            }
        });

        // Colore iniziale
        if (combo.getSelectedItem() instanceof model.Priority selected) {
            combo.setForeground(selected.getColor());
        }
    }

    /**
     * Configura il componente di selezione della data (JDateChooser) per
     * integrarsi visivamente con il tema attivo.
     *
     * @param dateChooser Il componente {@link JDateChooser}.
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
     * Crea un'etichetta di testo standardizzata.
     *
     * @param text Il contenuto testuale.
     * @return Il {@link JLabel} generato.
     */
    public static JLabel createLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_NORMAL);
        lbl.setForeground(textColor);
        return lbl;
    }

    /**
     * Crea un'etichetta di testo standardizzata in grassetto.
     *
     * @param text Il contenuto testuale.
     * @return Il {@link JLabel} generato.
     */
    public static JLabel createBoldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_BOLD);
        lbl.setForeground(textColor);
        return lbl;
    }

    /**
     * Forza una dimensione fissa e uniforme per un array di bottoni forniti.
     *
     * @param buttons Un array o varargs di {@link JButton}.
     */
    public static void makeSameSize(JButton... buttons) {
        Dimension size = new Dimension(110, 40);
        for (JButton btn : buttons) {
            btn.setPreferredSize(size);
        }
    }

    /**
     * Carica un'immagine vettoriale SVG dal percorso di risorsa, ridimensionandola.
     *
     * @param path   Il percorso interno al progetto dell'SVG.
     * @param width  Larghezza in pixel.
     * @param height Altezza in pixel.
     * @return L'oggetto {@link Icon} renderizzato, o {@code null} se il caricamento fallisce.
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
     * Carica un'immagine vettoriale SVG applicando un filtro monocromatico di ricolorazione.
     *
     * @param path   Il percorso interno al progetto dell'SVG.
     * @param width  Larghezza in pixel.
     * @param height Altezza in pixel.
     * @param color  Il colore solido da applicare all'intera icona.
     * @return L'oggetto {@link Icon} ricolorato, o {@code null} se il caricamento fallisce.
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
     * Sostituisce il frame attuale ricaricando un'istanza fresca generata tramite callback.
     * Utilizzato solitamente per applicare i cambiamenti di tema globale.
     *
     * @param currentFrame Il frame attualmente visualizzato.
     * @param frameCreator La funzione lambda che genera il nuovo frame.
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
     * Gestisce la transizione visiva tra due Frame preservando la posizione o
     * lo stato di massimizzazione sullo schermo.
     *
     * @param fromFrame Il frame di partenza che verrà distrutto.
     * @param toFrame   Il frame di destinazione che verrà mostrato.
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
     * Calcola il colore ottimale del testo (Chiaro o Scuro) garantendo il miglior contrasto
     * basandosi sulla luminanza dello sfondo fornito.
     *
     * @param background Il colore di sfondo su cui stampare il testo.
     * @return {@link #DARK_TEXT} se lo sfondo è chiaro, {@link #LIGHT_TEXT} se è scuro.
     */
    public static Color getContrastColor(Color background){
        double brightness = (0.299 * background.getRed() + 0.587 * background.getGreen() + 0.114 * background.getBlue());
        return brightness < 128 ? DARK_TEXT : LIGHT_TEXT;
    }

    // ==========================
    // COMPONENTI GRAFICI CUSTOM
    // ==========================

    /**
     * Crea un pannello scrollabile con un'interfaccia utente personalizzata, minimalista e trasparente.
     *
     * @param content Il componente interno da scorrere.
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
     * UI personalizzata per le barre di scorrimento, progettata per essere invisibile finché
     * non viene attivata o finché non ci si passa sopra col mouse.
     */
    private static class ModernScrollBarUI extends BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            this.thumbColor = new Color(0, 0, 0, 0);
            this.trackColor = new Color(0, 0, 0, 0);
        }

        @Override
        protected JButton createDecreaseButton(int orientation) { return createZeroButton(); }

        @Override
        protected JButton createIncreaseButton(int orientation) { return createZeroButton(); }

        private JButton createZeroButton() {
            JButton btn = new JButton();
            btn.setPreferredSize(new Dimension(0, 0));
            btn.setMinimumSize(new Dimension(0, 0));
            btn.setMaximumSize(new Dimension(0, 0));
            return btn;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
            // Intenzionalmente vuoto: il design minimalista non prevede il disegno del binario di scorrimento
        }

        /**
         * Disegna il cursore (thumb) della barra di scorrimento arrotondato.
         */
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
         * Calcola la geometria e la posizione del cursore (thumb) della barra di scorrimento.
         * Adatta le dimensioni del cursore in base all'orientamento della barra
         * (verticale o orizzontale) e aggiunge un leggero padding.
         *
         * @param thumbBounds L'area rettangolare destinata al cursore.
         * @return Un oggetto {@link Rectangle} con le coordinate e dimensioni calcolate.
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
         * Determina il colore del cursore della barra di scorrimento.
         * Applica una trasparenza costante e scurisce la tonalità se l'utente
         * sta trascinando la barra o se il mouse si trova sopra di essa.
         *
         * @return L'oggetto {@link Color} risultante con canale Alpha impostato a 180.
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

        @Override
        public Dimension getPreferredSize(JComponent c) {
            return new Dimension(14, super.getPreferredSize(c).height);
        }
    }

    /**
     * Sostituisce il disegno standard di una CheckBox per fornire un look vettoriale moderno e rotondo.
     *
     * @param checkBox Il {@link JCheckBox} da modificare.
     */
    public static void styleCheckbox(JCheckBox checkBox) {
        checkBox.setOpaque(false);
        checkBox.setFocusPainted(false);
        checkBox.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        final int iconSize = 22;

        checkBox.setIcon(new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.GRAY);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(x + 1, y + 1, iconSize - 2, iconSize - 2);
                g2.dispose();
            }
            @Override public int getIconWidth() { return iconSize; }
            @Override public int getIconHeight() { return iconSize; }
        });

        checkBox.setSelectedIcon(new Icon() {
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
            @Override public int getIconWidth() { return iconSize; }
            @Override public int getIconHeight() { return iconSize; }
        });
    }

    /**
     * Verifica se l'id della bacheca passato non è mai stato "visto" o aperto dall'utente locale.
     * Controlla la stringa salvata nelle preferenze dell'applicazione locale.
     *
     * @param boardId L'ID numerico della bacheca.
     * @return {@code true} se risulta "nuova", {@code false} altrimenti.
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
     * Registra nelle preferenze locali che una specifica bacheca è stata vista.
     * Toglierà il badge visuale "Nuovo" alle successive aperture.
     *
     * @param boardId L'ID numerico della bacheca.
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
     * Applica uno stile squadrato alle checkbox utilizzate per il filtraggio (es. nel BoardFrame).
     *
     * @param checkBox La {@link JCheckBox} da alterare.
     */
    public static void styleFilterCheckBox(JCheckBox checkBox) {
        checkBox.setOpaque(false);
        checkBox.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        checkBox.setFont(FONT_NORMAL);
        checkBox.setForeground(textColor);
        checkBox.setFocusPainted(false);

        int size = 20;

        checkBox.setIcon(new Icon() {
            @Override public int getIconWidth() { return size; }
            @Override public int getIconHeight() { return size; }
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
            @Override public int getIconWidth() { return size; }
            @Override public int getIconHeight() { return size; }
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
     * Implementazione personalizzata dell'interfaccia UI per la ComboBox delle priorità.
     */
    private static class PriorityComboBoxUI extends BasicComboBoxUI {
        private final Color bgColor;
        private final Color arrowColor;

        public PriorityComboBoxUI(Color bgColor, Color arrowColor) {
            this.bgColor = bgColor;
            this.arrowColor = arrowColor;
        }

        @Override
        protected JButton createArrowButton() {
            BasicArrowButton btn = new BasicArrowButton(
                    SwingConstants.SOUTH, bgColor, bgColor, arrowColor, bgColor
            );
            btn.setBorder(BorderFactory.createEmptyBorder());
            return btn;
        }

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
     * Implementazione personalizzata per renderizzare i singoli elementi della ComboBox,
     * consentendo ad ogni voce di mostrare il proprio colore.
     */
    private static class PriorityComboBoxRenderer extends DefaultListCellRenderer {
        private final Color bgColor;

        public PriorityComboBoxRenderer(Color bgColor) {
            this.bgColor = bgColor;
        }

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
     * Apre un indirizzo URL nel browser predefinito del sistema operativo.
     * Gestisce automaticamente l'aggiunta del protocollo di sicurezza (HTTPS)
     * qualora non fosse specificato nella stringa di input.
     *
     * @param urlString L'indirizzo web testuale da visualizzare nel browser.
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
     * Imposta l'icona dell'applicazione per il frame specificato.
     *
     * @param frame Il {@link JFrame} a cui applicare l'icona personalizzata.
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
     * Mostra un dialogo di conferma standardizzato con i pulsanti "Sì" e "No".
     *
     * @param parent      Il componente genitore per il posizionamento.
     * @param message     Il messaggio da visualizzare.
     * @param title       Il titolo della finestra.
     * @param messageType La tipologia del messaggio definita da JOptionPane.
     * @return {@code true} se l'utente seleziona "Sì", {@code false} altrimenti.
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
     * DTO per trasportare i componenti collegati alla selezione combinata di data e ora.
     *
     * @param panel       Il pannello padre contenente gli elementi.
     * @param dateChooser Il controllo grafico della selezione della data.
     * @param timeSpinner Il controllo a spin dell'orario.
     */
    public record DateTimePicker(JPanel panel, JDateChooser dateChooser, JSpinner timeSpinner) {}

    /**
     * Costruisce e assembla un modulo unificato per la selezione di Data e Ora.
     *
     * @param initialDate L'eventuale data preselezionata in partenza.
     * @return L'oggetto {@link DateTimePicker} per accedere ai componenti grafici originati.
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

        // Logica Iniziale
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

        // Listener per attivazione dinamica
        dc.addPropertyChangeListener("date", evt ->
                spinner.setEnabled(dc.getDate() != null)
        );

        panel.add(dc);
        panel.add(Box.createHorizontalStrut(10));
        panel.add(spinner);

        return new DateTimePicker(panel, dc, spinner);
    }

    /**
     * Formatta un testo lungo per i tooltip, troncandolo e aggiungendo i puntini di sospensione.
     * Forza una larghezza massima per evitare tooltip che occupano tutto lo schermo.
     *
     * @param text      Il testo originale.
     * @param maxLength Il numero massimo di caratteri prima del troncamento.
     * @return La stringa formattata in HTML pronta per setToolTipText.
     */
    public static String formatTooltip(String text, int maxLength) {
        if (text == null || text.trim().isEmpty()) return null;

        String cleanText = text.replace("\n", " "); // Rimuove gli a capo
        if (cleanText.length() > maxLength) {
            cleanText = cleanText.substring(0, maxLength) + "...";
        }

        // Il tag <p width='250'> forza il tooltip ad andare a capo se necessario,
        // evitando che si estenda orizzontalmente all'infinito.
        return "<html><p width='250'>" + cleanText + "</p></html>";
    }
}