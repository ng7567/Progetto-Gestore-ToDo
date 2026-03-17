package gui.frames;

import controller.Controller;
import gui.components.AppHeader;
import gui.dialogs.BoardDetailsDialog;
import gui.style.GuiUtils;
import gui.style.WrapLayout;
import model.Board;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.util.List;

/**
 * Finestra principale dell'applicazione (Dashboard).
 * Mostra l'elenco delle bacheche dell'utente e permette di crearne di nuove,
 * visualizzarne i dettagli tramite tooltip ricorsivi o accedere al loro contenuto.
 */
public class HomeFrame extends JFrame {

    private final transient Controller controller;
    private JPanel boardsContainer;

    /**
     * Costruisce la HomeFrame.
     *
     * @param controller Il controller per gestire la logica di business.
     */
    public HomeFrame(Controller controller) {
        super("Le mie bacheche");
        this.controller = controller;

        initUI();
        loadBoards();

        GuiUtils.setAppIcon(this);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1000, 800);
        setMinimumSize(new Dimension(800, 600));
        setLocationRelativeTo(null);
    }

    /**
     * Inizializza l'interfaccia utente principale della dashboard.
     * Configura il pannello principale con l'header di sistema e predispone un
     * JScrollPane che utilizza il {@code WrapLayout} per
     * disporre dinamicamente le card delle bacheche su più righe.
     */
    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(GuiUtils.getBackgroundColor());

        Runnable reload = () -> GuiUtils.reloadWindow(this, () -> new HomeFrame(controller));

        AppHeader header = new AppHeader(
                "Le mie Bacheche",
                null,
                reload,
                controller,
                this
        );

        mainPanel.add(header, BorderLayout.NORTH);

        // Usa WrapLayout per disporre le card a capo automaticamente
        boardsContainer = new ScrollablePanel(new WrapLayout(FlowLayout.LEFT, 20, 20));
        boardsContainer.setBackground(GuiUtils.getBackgroundColor());

        JScrollPane scrollPane = GuiUtils.createModernScrollPane(boardsContainer);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        add(mainPanel);
    }

    /**
     * Ricarica l'elenco delle bacheche interrogando il database tramite il controller.
     * Pulisce il contenitore visivo, reinserisce il pulsante di creazione e genera
     * una nuova card per ogni bacheca trovata, forzando infine il ridisegno della vista.
     */
    private void loadBoards() {
        boardsContainer.removeAll();
        boardsContainer.add(createAddButton());

        List<Board> boards = controller.getUserBoards();
        for (Board b : boards) {
            boardsContainer.add(createBoardCard(b));
        }

        boardsContainer.revalidate();
        boardsContainer.repaint();
    }

    /**
     * Crea il pulsante per l'aggiunta di una nuova bacheca.
     * Utilizza una formattazione HTML per il testo e applica uno stile tratteggiato
     * tramite le utilità grafiche per differenziarlo visivamente dalle card esistenti.
     *
     * @return Il {@code JButton} configurato per l'apertura del dialogo di creazione.
     */
    private JButton createAddButton() {
        JButton btn = new JButton("<html><center><font size='10'>+</font><br>Crea nuova</center></html>");
        btn.setPreferredSize(new Dimension(160, 100));
        GuiUtils.styleDashedButton(btn);
        btn.addActionListener(e -> showCreateBoardDialog());
        return btn;
    }

    /**
     * Crea il pannello grafico (Card) rappresentante una singola bacheca.
     * Implementa una struttura a livelli tramite {@code OverlayLayout} per sovrapporre
     * il titolo centrale ai controlli di gestione (menu opzioni e badge "Nuovo")
     * e applica gli effetti visivi di arrotondamento e bordi colorati.
     *
     * @param board L'oggetto {@link Board} contenente i dati da visualizzare.
     * @return Un {@code JPanel} configurato e interattivo.
     */
    private JPanel createBoardCard(Board board) {
        // 1. SETUP DELLA CARD PRINCIPALE
        JPanel cardPanel = new JPanel();
        cardPanel.setLayout(new OverlayLayout(cardPanel));
        cardPanel.setPreferredSize(new Dimension(160, 100));
        cardPanel.setBackground(GuiUtils.SECONDARY_COLOR);

        // Configura la curvatura degli angoli tramite le proprietà FlatLaf.
        int cornerArc = 15;
        cardPanel.putClientProperty("FlatLaf.style", "arc: " + cornerArc);

        // Imposta il pannello come non opaco per garantire la corretta resa degli angoli arrotondati.
        cardPanel.setOpaque(false);

        // 2. LOGICA VISIVA "NUOVO"
        boolean isNew = GuiUtils.isBoardNew(board.getId());
        if (isNew) {
            cardPanel.setBorder(new com.formdev.flatlaf.ui.FlatLineBorder(
                    new Insets(0, 0, 0, 0), new Color(231, 76, 60), 2, cornerArc));
        } else {
            cardPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        }

        // --- LAYER 1 (SOPRA): Controlli (Header con menu e Footer con badge) ---
        JPanel controlsPanel = new JPanel(new BorderLayout());
        controlsPanel.setOpaque(false);
        controlsPanel.setAlignmentX(0.5f);
        controlsPanel.setAlignmentY(0.5f);

        // HEADER: Posiziona il pulsante delle opzioni in alto a destra.
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        headerPanel.setOpaque(false);
        headerPanel.setBorder(new EmptyBorder(2, 0, 0, 2));

        JPopupMenu popupMenu = createBoardPopupMenu(board);
        JButton btnOptions = createOptionsButton(popupMenu);

        headerPanel.add(btnOptions);
        controlsPanel.add(headerPanel, BorderLayout.NORTH);

        // FOOTER: Aggiunge l'indicatore visivo solo se la bacheca è nuova.
        if (isNew) {
            controlsPanel.add(createNewBadge(), BorderLayout.SOUTH);
        }

        // --- LAYER 2 (SOTTO): Titolo della bacheca ---
        JPanel titleContainer = createTitlePanel(board.getTitle());

        cardPanel.add(controlsPanel);
        cardPanel.add(titleContainer);

        // GESTIONE INTERAZIONE
        configureCardMouseEvents(cardPanel, board, isNew, btnOptions);

        String desc = board.getDescription();
        if (desc != null && !desc.trim().isEmpty()) {
            String tooltipHtml = GuiUtils.formatTooltip(desc, 120); // 120 caratteri max
            applyToolTipRecursively(cardPanel, tooltipHtml);
        }

        return cardPanel;
    }

    /**
     * Crea il menu contestuale (popup) contenente le azioni rapide per la bacheca.
     * Include ascoltatori per gestire correttamente i tempi di chiusura e prevenire
     * conflitti con i click consecutivi sul pulsante di attivazione.
     *
     * @param board La bacheca a cui associare le azioni del menu.
     * @return L'oggetto {@code JPopupMenu} popolato con le voci di dettaglio ed eliminazione.
     */
    private JPopupMenu createBoardPopupMenu(Board board) {
        JPopupMenu popupMenu = new JPopupMenu();

        popupMenu.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {
                // Metodo intenzionalmente vuoto: nessuna azione preliminare all'apertura.
            }
            @Override
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {
                // Metodo intenzionalmente vuoto: nessuna azione specifica sull'annullamento.
            }
            @Override
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {
                // Memorizza l'istante di chiusura per evitare riaperture accidentali al click consecutivo.
                popupMenu.putClientProperty("lastCloseTime", System.currentTimeMillis());
            }
        });

        popupMenu.add(createDetailsMenuItem(board));
        popupMenu.addSeparator();
        popupMenu.add(createDeleteMenuItem(board));

        return popupMenu;
    }

    /**
     * Crea la voce di menu specifica per l'apertura dei dettagli della bacheca.
     *
     * @param board La bacheca da sottoporre a ispezione o modifica.
     * @return Un {@code JMenuItem} configurato con la logica di apertura del dialogo modale.
     */
    private JMenuItem createDetailsMenuItem(Board board) {
        JMenuItem itemDetails = new JMenuItem("Dettagli e Modifica");
        itemDetails.addActionListener(e -> {
            BoardDetailsDialog dialog = new BoardDetailsDialog(this, controller, board, this::loadBoards);
            dialog.setVisible(true);
        });
        return itemDetails;
    }

    /**
     * Crea la voce di menu per la rimozione definitiva della bacheca.
     * Gestisce la richiesta di conferma all'utente e la successiva propagazione
     * del comando di cancellazione al database tramite il controller.
     *
     * @param board La bacheca destinata all'eliminazione.
     * @return Un {@code JMenuItem} configurato con lo stile di avviso (colore rosso).
     */
    private JMenuItem createDeleteMenuItem(Board board) {
        JMenuItem itemDelete = new JMenuItem("Elimina bacheca");
        itemDelete.setForeground(Color.RED);
        itemDelete.addActionListener(e -> {
            String msg = "Vuoi davvero eliminare la bacheca?\nTutti i task al suo interno verranno persi.";

            if (GuiUtils.showConfirmDialog(this, msg, "Conferma Eliminazione", JOptionPane.WARNING_MESSAGE) && controller.deleteBoard(board)) {
                loadBoards();
            }
        });
        return itemDelete;
    }

    /**
     * Crea il pulsante delle opzioni caratterizzato dall'icona a tre punti verticali.
     * Implementa un rendering personalizzato in Java2D per disegnare i punti e gestire
     * l'effetto di evidenziazione (hover) circolare.
     *
     * @param popupMenu Il menu popup da visualizzare alla pressione del pulsante.
     * @return Il {@code JButton} configurato.
     */
    private JButton createOptionsButton(JPopupMenu popupMenu) {
        JButton btnOptions = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed() || getModel().isRollover() || popupMenu.isVisible()) {
                    g2.setColor(new Color(200, 200, 200, 100));
                    g2.fillOval(2, 2, 20, 20);
                }
                g2.setColor(Color.GRAY);
                g2.fillOval(10, 5, 3, 3);
                g2.fillOval(10, 10, 3, 3);
                g2.fillOval(10, 15, 3, 3);
                g2.dispose();
            }
        };
        btnOptions.setPreferredSize(new Dimension(24, 24));
        btnOptions.setBorderPainted(false);
        btnOptions.setContentAreaFilled(false);
        btnOptions.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btnOptions.addActionListener(e -> {
            Long lastClose = (Long) popupMenu.getClientProperty("lastCloseTime");
            // Apre il menu solo se è passato un tempo sufficiente dall'ultima chiusura
            if (lastClose == null || System.currentTimeMillis() - lastClose > 200) {
                popupMenu.show(btnOptions, 0, btnOptions.getHeight());
            }
        });

        return btnOptions;
    }

    /**
     * Crea il badge informativo per segnalare visivamente una bacheca non ancora consultata.
     * Combina un'icona a forma di stella disegnata proceduralmente con un testo di avviso.
     *
     * @return Un {@code JPanel} trasparente contenente l'indicatore "NUOVO".
     */
    private JPanel createNewBadge() {
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        footerPanel.setOpaque(false);
        footerPanel.setBorder(new EmptyBorder(0, 0, 5, 0));

        JLabel lblIcon = new JLabel(new StarIcon(10, 10, Color.RED));
        JLabel lblText = new JLabel("NUOVO");
        lblText.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblText.setForeground(Color.RED);

        footerPanel.add(lblIcon);
        footerPanel.add(lblText);
        return footerPanel;
    }

    /**
     * Crea il pannello dedicato alla visualizzazione del titolo della bacheca.
     * Utilizza un {@code JTextPane} configurato per il centraggio del testo e
     * inserito in un {@code GridBagLayout} per assicurarne il posizionamento perfetto.
     *
     * @param title La stringa testuale del titolo da mostrare.
     * @return Un {@code JPanel} non opaco contenente il titolo formattato.
     */
    private JPanel createTitlePanel(String title) {
        JPanel titleContainer = new JPanel(new GridBagLayout());
        titleContainer.setOpaque(false);
        titleContainer.setAlignmentX(0.5f);
        titleContainer.setAlignmentY(0.5f);
        titleContainer.setBorder(new EmptyBorder(25, 5, 20, 5));

        JTextPane titlePane = new JTextPane();
        titlePane.setText(title);
        titlePane.setEditable(false);
        titlePane.setFocusable(false);
        titlePane.setHighlighter(null);
        titlePane.setOpaque(false);
        titlePane.setFont(GuiUtils.FONT_BOLD);
        titlePane.setForeground(Color.BLACK);

        StyledDocument doc = titlePane.getStyledDocument();
        SimpleAttributeSet center = new SimpleAttributeSet();
        StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
        doc.setParagraphAttributes(0, doc.getLength(), center, false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        titleContainer.add(titlePane, gbc);
        return titleContainer;
    }

    /**
     * Configura gli eventi del mouse (click, hover, exit) per l'intera superficie della card.
     * Gestisce la transizione alla bacheca, l'aggiornamento dello stato di lettura
     * e i cambiamenti cromatici dello sfondo durante l'interazione.
     *
     * @param cardPanel  Il pannello della card su cui registrare i listener.
     * @param board      La bacheca associata.
     * @param isNew      Flag indicativo dello stato di novità della bacheca.
     * @param btnOptions Il riferimento al pulsante opzioni per escluderlo da determinate logiche di click.
     */
    private void configureCardMouseEvents(JPanel cardPanel, Board board, boolean isNew, JButton btnOptions) {
        MouseAdapter openListener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1 && SwingUtilities.isLeftMouseButton(e)) {
                    // Segna come letta rimuovendo il badge "Nuovo"
                    if (isNew) GuiUtils.markBoardAsSeen(board.getId());
                    // Transizione verso la schermata dei task della bacheca
                    GuiUtils.transition(HomeFrame.this, new BoardFrame(controller, board));
                }
            }
            @Override
            public void mouseEntered(MouseEvent e) { cardPanel.setBackground(new Color(220, 220, 220)); }
            @Override
            public void mouseExited(MouseEvent e) {
                Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), cardPanel);
                if (!cardPanel.contains(p)) {
                    cardPanel.setBackground(GuiUtils.SECONDARY_COLOR);
                }
            }
        };

        setHandCursorToAll(cardPanel, openListener);

        // Disaccoppia il bottone opzioni per permettere l'apertura del menu senza aprire la bacheca
        btnOptions.removeMouseListener(openListener);
        btnOptions.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { cardPanel.setBackground(new Color(220, 220, 220)); }
        });
    }

    /**
     * Applica ricorsivamente il cursore a mano e i listener del mouse a un componente e ai suoi discendenti.
     * Assicura che l'intera area della card risponda uniformemente alle interazioni,
     * filtrando i bottoni per non interferire con le loro funzionalità native.
     *
     * @param component Il componente genitore da cui avviare la propagazione.
     * @param listener  L'ascoltatore eventi del mouse da agganciare.
     */
    private void setHandCursorToAll(JComponent component, MouseAdapter listener) {
        component.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (!(component instanceof JButton)) {
            component.addMouseListener(listener);
        }

        for (Component child : component.getComponents()) {
            if (child instanceof JComponent jComponent) {
                setHandCursorToAll(jComponent, listener);
            }
        }
    }

    /**
     * Propaga in maniera ricorsiva il testo del tooltip a tutti i componenti interni della card.
     * Questa procedura garantisce che la descrizione della bacheca sia visibile anche
     * quando il puntatore si trova sopra etichette o pannelli annidati.
     *
     * @param comp    Il componente base da cui partire.
     * @param tooltip La stringa descrittiva da impostare.
     */
    private void applyToolTipRecursively(JComponent comp, String tooltip) {
        // Evitiamo di sovrascrivere eventuali tooltip specifici e intenzionali sui pulsanti
        if (!(comp instanceof JButton)) {
            comp.setToolTipText(tooltip);
        }

        // Propaga l'impostazione a tutti i componenti discendenti
        for (Component child : comp.getComponents()) {
            if (child instanceof JComponent jComp) {
                applyToolTipRecursively(jComp, tooltip);
            }
        }
    }

    /**
     * Gestisce l'apertura del dialogo modale per la creazione di una nuova bacheca.
     * Include i controlli di validazione lato client per la lunghezza del titolo
     * e intercetta le eccezioni di business logic (es. nomi duplicati) per
     * fornire feedback immediati all'utente.
     */
    private void showCreateBoardDialog() {
        JDialog dialog = new JDialog(this, "Nuova Bacheca", true);
        dialog.setSize(400, 360);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new GridBagLayout());
        dialog.getContentPane().setBackground(GuiUtils.getBackgroundColor());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 20, 5, 20);
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // --- TITOLO ---
        gbc.gridy = 0;
        JLabel lblName = GuiUtils.createBoldLabel("Nome della Bacheca:");
        dialog.add(lblName, gbc);

        gbc.gridy = 1;
        JTextField txtName = GuiUtils.createStandardTextField(20);
        dialog.add(txtName, gbc);

        // --- DESCRIZIONE ---
        gbc.gridy = 2;
        gbc.insets = new Insets(10, 20, 5, 20);
        JLabel lblDesc = GuiUtils.createBoldLabel("Descrizione (Opzionale):");
        dialog.add(lblDesc, gbc);

        gbc.gridy = 3;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;

        JTextArea txtDesc = new JTextArea();
        txtDesc.setLineWrap(true);
        txtDesc.setWrapStyleWord(true);
        GuiUtils.styleTextArea(txtDesc);

        JScrollPane scrollDesc = GuiUtils.createModernScrollPane(txtDesc);
        scrollDesc.setBorder(GuiUtils.createStandardBorder());
        dialog.add(scrollDesc, gbc);

        // --- BOTTONI ---
        gbc.gridy = 4;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(15, 20, 15, 20);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        btnPanel.setBackground(GuiUtils.getBackgroundColor());

        JButton btnCancel = new JButton("Annulla");
        GuiUtils.styleSecondaryButton(btnCancel);
        btnCancel.addActionListener(e -> dialog.dispose());

        JButton btnCreate = new JButton("Crea");
        GuiUtils.stylePrimaryButton(btnCreate);
        GuiUtils.makeSameSize(btnCancel, btnCreate);

        btnCreate.addActionListener(e -> {
            String title = txtName.getText().trim();
            String description = txtDesc.getText().trim();

            if (title.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Il titolo è obbligatorio!", "Errore validazione", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (title.length() > 100) {
                JOptionPane.showMessageDialog(this, "Il titolo è troppo lungo (massimo 100 caratteri).", "Errore validazione", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                if (controller.addBoard(title, description)) {
                    loadBoards();
                    dialog.dispose();
                } else {
                    JOptionPane.showMessageDialog(dialog, "Errore durante la creazione.", "Errore", JOptionPane.ERROR_MESSAGE);
                }
            } catch (exception.DuplicateBoardException ex) {
                // Catturiamo l'eccezione specifica e mostriamo il suo messaggio personalizzato
                JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Nome duplicato", JOptionPane.WARNING_MESSAGE);
            } catch (Exception ex) {
                // Gestione generica per altri tipi di errori imprevisti
                JOptionPane.showMessageDialog(dialog, "Si è verificato un errore inaspettato.", "Errore", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnPanel.add(btnCancel);
        btnPanel.add(btnCreate);
        dialog.add(btnPanel, gbc);

        dialog.getRootPane().setDefaultButton(btnCreate);
        dialog.setVisible(true);
    }

    /**
     * Record immutabile per il rendering procedurale di un'icona a forma di stella a 5 punte.
     * Utilizzato come indicatore grafico nel badge "Nuovo".
     */
    private record StarIcon(int width, int height, Color color) implements Icon {
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);

            GeneralPath star = new GeneralPath();
            double cx = x + (width / 2.0);
            double cy = y + (height / 2.0);
            double outerRadius = width / 2.0;
            double innerRadius = outerRadius / 2.5;

            // Disegna i 10 vertici della stella
            for (int i = 0; i < 5; i++) {
                double angleOuter = Math.toRadians(-90d + (i * 72));
                double xOuter = cx + Math.cos(angleOuter) * outerRadius;
                double yOuter = cy + Math.sin(angleOuter) * outerRadius;
                if (i == 0) star.moveTo(xOuter, yOuter);
                else star.lineTo(xOuter, yOuter);

                double angleInner = Math.toRadians(-54d + (i * 72));
                double xInner = cx + Math.cos(angleInner) * innerRadius;
                double yInner = cy + Math.sin(angleInner) * innerRadius;
                star.lineTo(xInner, yInner);
            }
            star.closePath();
            g2.fill(star);
            g2.dispose();
        }

        @Override public int getIconWidth() { return width; }
        @Override public int getIconHeight() { return height; }
    }

    /**
     * Implementazione di JPanel che supporta nativamente l'interfaccia Scrollable.
     * Necessario per garantire che layout fluidi come WrapLayout (che riposizionano
     * gli elementi a capo quando non c'è più spazio) funzionino correttamente
     * all'interno di un JScrollPane standard, ridimensionandosi dinamicamente.
     */
    private static class ScrollablePanel extends JPanel implements Scrollable {
        /**
         * Istanzia un nuovo pannello scrollabile con il layout specificato.
         *
         * @param layout Il LayoutManager da utilizzare (es. WrapLayout).
         */
        public ScrollablePanel(LayoutManager layout) { super(layout); }

        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) { return 16; }
        @Override public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) { return 16; }

        // Forza la larghezza del pannello ad adattarsi alla viewport per attivare l'andata a capo automatica
        @Override public boolean getScrollableTracksViewportWidth() { return true; }

        // L'altezza invece deve potersi espandere liberamente senza costrizioni
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }
}