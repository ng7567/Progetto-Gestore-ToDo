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
 * Rappresenta la finestra principale dell'applicazione, fungendo da punto
 * d'accesso centrale per l'utente autenticato.
 * Espone in modo dinamico la collezione delle bacheche personali, consentendo la
 * creazione di nuovi spazi di lavoro, l'ispezione dei metadati tramite tooltip
 * informativi e la navigazione verso i contenuti specifici di ogni bacheca.
 *
 * @author Nunzio Grasso (Matricola: N86005509)
 * @version 1.0
 */
public class HomeFrame extends JFrame {

    /** Il riferimento al gestore della logica di business per il recupero delle bacheche. */
    private final transient Controller controller;

    /** Il contenitore grafico flessibile che ospita la collezione delle card delle bacheche. */
    private JPanel boardsContainer;

    /**
     * Inizializza una nuova istanza della Dashboard principale.
     * Configura i parametri di visualizzazione standard e innesca il primo caricamento
     * dei dati dal database.
     *
     * @param controller Il riferimento al Controller principale dell'applicazione.
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
     * Struttura gerarchicamente l'interfaccia utente della dashboard.
     * Integra l'header di sistema nella parte superiore e predispone un'area di
     * scorrimento centrale dotata di {@link WrapLayout}, permettendo alle card
     * di disporsi fluidamente su più righe in base alla larghezza della finestra.
     */
    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(GuiUtils.getBackgroundColor());

        // Callback per il rinfresco completo della finestra
        Runnable reload = () -> GuiUtils.reloadWindow(this, () -> new HomeFrame(controller));

        AppHeader header = new AppHeader(
                "Le mie Bacheche",
                null,
                reload,
                controller,
                this
        );

        mainPanel.add(header, BorderLayout.NORTH);

        // Inizializza il contenitore con supporto allo scorrimento e andata a capo automatica
        boardsContainer = new ScrollablePanel(new WrapLayout(FlowLayout.LEFT, 20, 20));
        boardsContainer.setBackground(GuiUtils.getBackgroundColor());

        JScrollPane scrollPane = GuiUtils.createModernScrollPane(boardsContainer);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        add(mainPanel);
    }

    /**
     * Sincronizza la visualizzazione con lo stato attuale del database.
     * Svuota il contenitore, rigenera il pulsante di creazione e istanzia una
     * nuova card interattiva per ogni bacheca associata all'utente corrente.
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
     * Genera il pulsante delegato all'attivazione del processo di creazione bacheca.
     * Applica uno stile visivo differenziato (tratteggio) per distinguerlo
     * dalle card informative esistenti.
     *
     * @return L'oggetto {@code JButton} configurato con stile "dashed".
     */
    private JButton createAddButton() {
        JButton btn = new JButton("<html><center><font size='10'>+</font><br>Crea nuova</center></html>");
        btn.setPreferredSize(new Dimension(160, 100));
        GuiUtils.styleDashedButton(btn);
        btn.addActionListener(e -> showCreateBoardDialog());
        return btn;
    }

    /**
     * Costruisce la card rappresentante un'istanza di bacheca.
     * Sfrutta un {@link OverlayLayout} per sovrapporre strategicamente diversi livelli informativi:
     * il titolo centrale, i controlli amministrativi (menu) e gli indicatori di stato (badge).
     *
     * @param board L'entità bacheca da cui estrarre i dati per il rendering.
     * @return Il pannello ({@code JPanel}) interattivo e formattato.
     */
    private JPanel createBoardCard(Board board) {
        // 1. SETUP STRUTTURALE: Configura le dimensioni e l'arrotondamento dei bordi
        JPanel cardPanel = new JPanel();
        cardPanel.setLayout(new OverlayLayout(cardPanel));
        cardPanel.setPreferredSize(new Dimension(160, 100));
        cardPanel.setBackground(GuiUtils.SECONDARY_COLOR);

        int cornerArc = 15;
        cardPanel.putClientProperty("FlatLaf.style", "arc: " + cornerArc);
        cardPanel.setOpaque(false);

        // 2. LOGICA VISIVA DI STATO: Gestisce il bordo evidenziato per le bacheche non ancora lette
        boolean isNew = GuiUtils.isBoardNew(board.getId());
        if (isNew) {
            cardPanel.setBorder(new com.formdev.flatlaf.ui.FlatLineBorder(
                    new Insets(0, 0, 0, 0), new Color(231, 76, 60), 2, cornerArc));
        } else {
            cardPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        }

        // --- LAYER 1 (SUPERIORE): Controlli interattivi e indicatori ---
        JPanel controlsPanel = new JPanel(new BorderLayout());
        controlsPanel.setOpaque(false);
        controlsPanel.setAlignmentX(0.5f);
        controlsPanel.setAlignmentY(0.5f);

        // Posizionamento del pulsante opzioni in alto a destra
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        headerPanel.setOpaque(false);
        headerPanel.setBorder(new EmptyBorder(2, 0, 0, 2));

        JPopupMenu popupMenu = createBoardPopupMenu(board);
        JButton btnOptions = createOptionsButton(popupMenu);

        headerPanel.add(btnOptions);
        controlsPanel.add(headerPanel, BorderLayout.NORTH);

        // Inserimento condizionale del badge "NUOVO" nella sezione inferiore
        if (isNew) {
            controlsPanel.add(createNewBadge(), BorderLayout.SOUTH);
        }

        // --- LAYER 2 (INFERIORE): Titolo descrittivo centrato ---
        JPanel titleContainer = createTitlePanel(board.getTitle());

        cardPanel.add(controlsPanel);
        cardPanel.add(titleContainer);

        // Configura la risposta agli eventi del mouse per l'intera card
        configureCardMouseEvents(cardPanel, board, isNew, btnOptions);

        // Configura la descrizione estesa come tooltip con propagazione ricorsiva
        String desc = board.getDescription();
        if (desc != null && !desc.trim().isEmpty()) {
            String tooltipHtml = GuiUtils.formatTooltip(desc, 120);
            applyToolTipRecursively(cardPanel, tooltipHtml);
        }

        return cardPanel;
    }

    /**
     * Istanzia il menu popup contenente le azioni rapide di gestione.
     * Include una logica di monitoraggio temporale sulla chiusura per prevenire
     * conflitti di visualizzazione con il pulsante di attivazione.
     *
     * @param board L'entità bacheca associata al menu contestuale.
     * @return L'oggetto {@link JPopupMenu} configurato.
     */
    private JPopupMenu createBoardPopupMenu(Board board) {
        JPopupMenu popupMenu = new JPopupMenu();

        popupMenu.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            /** {@inheritDoc} */
            @Override public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {}
            /** {@inheritDoc} */
            @Override public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {}
            /** {@inheritDoc} */
            @Override
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {
                // Traccia l'istante di disattivazione per gestire il debouncing del click
                popupMenu.putClientProperty("lastCloseTime", System.currentTimeMillis());
            }
        });

        popupMenu.add(createDetailsMenuItem(board));
        popupMenu.addSeparator();
        popupMenu.add(createDeleteMenuItem(board));

        return popupMenu;
    }

    /**
     * Crea l'opzione di menu dedicata all'ispezione dei dettagli o alla modifica della bacheca.
     *
     * @param board La bacheca bersaglio dell'azione.
     * @return L'oggetto {@code JMenuItem} configurato.
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
     * Crea l'opzione di menu dedicata alla cancellazione della bacheca.
     * Implementa la richiesta di conferma e la gestione del feedback di pericolo.
     *
     * @param board La bacheca destinata alla rimozione.
     * @return L'oggetto {@code JMenuItem} configurato con stile "alert".
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
     * Genera il pulsante delle opzioni caratterizzato da un'icona a tre punti.
     * Sovrascrive il disegno del componente per integrare effetti di hover circolari
     * mediante primitive Java2D.
     *
     * @param popupMenu Il menu contestuale da attivare.
     * @return L'oggetto {@code JButton} personalizzato.
     */
    private JButton createOptionsButton(JPopupMenu popupMenu) {
        JButton btnOptions = new JButton() {
            /** {@inheritDoc} */
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
            // Implementa un controllo anti-rimbalzo per prevenire riaperture istantanee
            if (lastClose == null || System.currentTimeMillis() - lastClose > 200) {
                popupMenu.show(btnOptions, 0, btnOptions.getHeight());
            }
        });

        return btnOptions;
    }

    /**
     * Genera il pannello indicatore "NUOVO" composto da un'icona stellata e un'etichetta testuale.
     *
     * @return Il pannello ({@code JPanel}) informativo per le nuove bacheche.
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
     * Istanzia il pannello deputato all'esposizione del titolo della bacheca.
     * Sfrutta un {@link JTextPane} per garantire il centraggio del testo e il
     * supporto a stringhe multiriga.
     *
     * @param title La stringa testuale del titolo.
     * @return L'oggetto {@code JPanel} formattato.
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

        // Applica l'attributo di centraggio del paragrafo al documento testuale
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
     * Gestisce la registrazione dei listener per la risposta agli eventi di interazione
     * sulla card. Coordina il feedback visivo (cambio colore) e gestisce la
     * transizione verso la visualizzazione dei task interni alla bacheca.
     *
     * @param cardPanel  Il pannello della card bersaglio.
     * @param board      L'entità bacheca associata.
     * @param isNew      Flag di novità della risorsa.
     * @param btnOptions Riferimento al pulsante opzioni per la gestione isolata dei click.
     */
    private void configureCardMouseEvents(JPanel cardPanel, Board board, boolean isNew, JButton btnOptions) {
        MouseAdapter openListener = new MouseAdapter() {
            /** {@inheritDoc} */
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1 && SwingUtilities.isLeftMouseButton(e)) {
                    if (isNew) GuiUtils.markBoardAsSeen(board.getId());
                    GuiUtils.transition(HomeFrame.this, new BoardFrame(controller, board));
                }
            }
            /** {@inheritDoc} */
            @Override
            public void mouseEntered(MouseEvent e) { cardPanel.setBackground(new Color(220, 220, 220)); }
            /** {@inheritDoc} */
            @Override
            public void mouseExited(MouseEvent e) {
                Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), cardPanel);
                if (!cardPanel.contains(p)) {
                    cardPanel.setBackground(GuiUtils.SECONDARY_COLOR);
                }
            }
        };

        setHandCursorToAll(cardPanel, openListener);

        // Svincola il pulsante opzioni dalla logica di apertura bacheca
        btnOptions.removeMouseListener(openListener);
        btnOptions.addMouseListener(new MouseAdapter() {
            /** {@inheritDoc} */
            @Override public void mouseEntered(MouseEvent e) { cardPanel.setBackground(new Color(220, 220, 220)); }
        });
    }

    /**
     * Propaga ricorsivamente il cursore a forma di mano e l'ascoltatore eventi a
     * tutti i componenti discendenti del contenitore, garantendo un'interattività uniforme.
     *
     * @param component Il componente genitore da scansionare.
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
     * Applica il testo del tooltip descrittivo lungo l'intera
     * gerarchia della card, assicurando che l'informazione rimanga visibile
     * indipendentemente dal componente puntato.
     *
     * @param comp    Il componente radice della card.
     * @param tooltip La stringa testuale della descrizione.
     */
    private void applyToolTipRecursively(JComponent comp, String tooltip) {
        if (!(comp instanceof JButton)) {
            comp.setToolTipText(tooltip);
        }

        for (Component child : comp.getComponents()) {
            if (child instanceof JComponent jComp) {
                applyToolTipRecursively(jComp, tooltip);
            }
        }
    }

    /**
     * Coordina l'apertura e la gestione logica del dialogo di creazione bacheca.
     * Implementa i protocolli di validazione locale e intercetta le eccezioni di
     * dominio per fornire feedback sulla disponibilità del nome scelto.
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

        // --- Sezione Nome ---
        gbc.gridy = 0;
        JLabel lblName = GuiUtils.createBoldLabel("Nome della Bacheca:");
        dialog.add(lblName, gbc);

        gbc.gridy = 1;
        JTextField txtName = GuiUtils.createStandardTextField(20);
        dialog.add(txtName, gbc);

        // --- Sezione Descrizione ---
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

        // --- Sezione Comandi ---
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
                JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Nome duplicato", JOptionPane.WARNING_MESSAGE);
            } catch (Exception ex) {
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
     * Record immutabile delegato al rendering grafico procedurale di un'icona stellata.
     * Sfrutta le primitive geometriche ({@link GeneralPath}) per disegnare una stella
     * a 5 punte calcolata trigonometricamente.
     * @param width  La larghezza complessiva dell'icona espressa in pixel.
     * @param height L'altezza complessiva dell'icona espressa in pixel.
     * @param color  L'oggetto {@link Color} utilizzato per il riempimento della geometria.
     */
    private record StarIcon(int width, int height, Color color) implements Icon {
        /** {@inheritDoc} */
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

            // Esegue il calcolo vettoriale dei 10 vertici (5 esterni e 5 interni)
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

        /** {@inheritDoc} */
        @Override public int getIconWidth() { return width; }
        /** {@inheritDoc} */
        @Override public int getIconHeight() { return height; }
    }

    /**
     * Estensione specializzata di JPanel integrata con l'interfaccia {@link Scrollable}.
     * Garantisce che layout dinamici come il WrapLayout mantengano un comportamento
     * fluido all'interno di un {@link JScrollPane}, vincolando la larghezza del pannello
     * alla viewport per innescare correttamente l'andata a capo degli elementi.
     */
    private static class ScrollablePanel extends JPanel implements Scrollable {
        /**
         * Inizializza il pannello con il gestore di layout specificato.
         *
         * @param layout Il LayoutManager da applicare.
         */
        public ScrollablePanel(LayoutManager layout) { super(layout); }

        /** {@inheritDoc} */
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        /** {@inheritDoc} */
        @Override public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) { return 16; }
        /** {@inheritDoc} */
        @Override public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) { return 16; }
        /** {@inheritDoc} */
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        /** {@inheritDoc} */
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }
}