package gui.components;

import com.formdev.flatlaf.FlatClientProperties;
import controller.Controller;
import gui.style.GuiUtils;
import gui.dialogs.TodoDetailsDialog;
import model.Board;
import model.ToDo;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

/**
 * Rappresenta graficamente un singolo Task (ToDo) all'interno della lista.
 * Gestisce la visualizzazione dei dati del task, l'interazione per il completamento
 * tramite checkbox e il menu contestuale per le operazioni rapide.
 * Implementa una logica adattiva per colori e icone in base allo sfondo della card.
 */
public class TaskCard extends JPanel {

    private final transient ToDo todo;
    private final transient Controller controller;
    private final transient Runnable onUpdate; // Callback per aggiornare la UI madre

    private static final String ERROR = "Errore";

    // Componenti UI
    private JLabel lblTitle;
    private JLabel lblDesc;
    private JLabel lblDate;
    private JCheckBox chkDone;
    private JButton btnOptions;

    /**
     * Costruisce una nuova card per il task specificato.
     *
     * @param todo       L'oggetto ToDo contenente i dati del task.
     * @param controller Il controller per eseguire operazioni di persistenza.
     * @param onUpdate   Callback da eseguire quando lo stato del task cambia.
     */
    public TaskCard(ToDo todo, Controller controller, Runnable onUpdate) {
        this.todo = todo;
        this.controller = controller;
        this.onUpdate = onUpdate;

        initUI();
        refreshStyle();
    }

    /**
     * Inizializza i componenti grafici primari e il layout della card.
     */
    private void initUI() {
        setLayout(new BorderLayout(10, 0));
        setOpaque(false);
        setBorder(new EmptyBorder(10, 16, 20, 16));

        setupBackgroundColor();
        setupCheckbox();
        setupCenterPanel();
        setupRightPanel();

        configureMouseEvents();
    }

    /**
     * Configura il colore di sfondo della card, effettuando il parsing della stringa esadecimale.
     */
    private void setupBackgroundColor() {
        Color cardColor = GuiUtils.getCardBackground();
        try {
            if (todo.getBackgroundColor() != null && !todo.getBackgroundColor().isEmpty()) {
                cardColor = Color.decode(todo.getBackgroundColor());
            }
        } catch (NumberFormatException e) {
            // Se il colore non è valido, mantiene il default
        }
        setBackground(cardColor);
    }

    /**
     * Crea e configura la checkbox laterale per il completamento del task.
     */
    private void setupCheckbox() {
        chkDone = new JCheckBox();
        chkDone.setSelected(todo.isCompleted());
        chkDone.setBorder(new EmptyBorder(5, 5, 5, 10));
        GuiUtils.styleCheckbox(chkDone);

        chkDone.addActionListener(e -> handleCheckboxToggle());
        add(chkDone, BorderLayout.WEST);
    }

    /**
     * Gestisce l'evento di interazione sulla checkbox e aggiorna il database.
     */
    private void handleCheckboxToggle() {
        boolean isChecked = chkDone.isSelected();
        boolean success = controller.setTodoComplete(todo.getId(), isChecked);

        if (success) {
            todo.setCompleted(isChecked);
            refreshStyle();
            if (onUpdate != null) onUpdate.run();
        } else {
            chkDone.setSelected(!isChecked);
            JOptionPane.showMessageDialog(getBaseFrame(),
                    "Errore durante l'aggiornamento dello stato.",
                    "Errore Salvataggio", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Assembla il pannello centrale contenente Titolo, Descrizione e Data di Scadenza.
     */
    private void setupCenterPanel() {
        JPanel centerPanel = new JPanel(new GridLayout(0, 1, 0, 4));
        centerPanel.setOpaque(false);

        lblTitle = new JLabel(todo.getTitle());
        lblTitle.setFont(GuiUtils.FONT_BOLD);
        centerPanel.add(lblTitle);

        if (todo.getDescription() != null && !todo.getDescription().isEmpty()) {
            String shortDesc = truncateText(todo.getDescription());
            lblDesc = new JLabel(shortDesc);
            lblDesc.setFont(new Font(GuiUtils.getFontFamily(), Font.PLAIN, 12));
            lblDesc.setToolTipText(GuiUtils.formatTooltip(todo.getDescription(), 150));
            centerPanel.add(lblDesc);
        }

        if (todo.getExpiryDate() != null) {
            lblDate = new JLabel(); // L'inizializzazione del testo avviene in applyActiveStyle/applyCompletedStyle
            lblDate.setFont(new Font(GuiUtils.getFontFamily(), Font.ITALIC, 11));
            centerPanel.add(lblDate);
        }

        add(centerPanel, BorderLayout.CENTER);
    }

    /**
     * Assembla il pannello destro contenente i badge (Condiviso, Priorità) e le icone di stato.
     */
    private void setupRightPanel() {
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setOpaque(false);

        // A. Badge Superiori
        JPanel topBadges = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        topBadges.setOpaque(false);

        if ("SHARED".equalsIgnoreCase(todo.getRole())) {
            JLabel lblShared = new JLabel("CONDIVISO");
            lblShared.setFont(new Font(GuiUtils.getFontFamily(), Font.BOLD, 9));
            lblShared.setForeground(new Color(60, 100, 200));
            lblShared.setBorder(new EmptyBorder(2, 0, 0, 0));
            lblShared.setToolTipText("Proprietario: " + todo.getOwnerUsername());
            topBadges.add(lblShared);
        }

        topBadges.add(createPriorityBadge());
        rightPanel.add(topBadges, BorderLayout.NORTH);

        // B. Icone Inferiori e Menu
        JPanel bottomWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bottomWrapper.setOpaque(false);

        Color iconColor = GuiUtils.getContrastColor(getBackground());

        if (todo.getUrlLink() != null && !todo.getUrlLink().trim().isEmpty()) {
            bottomWrapper.add(createIconLabel("icons/link.svg", iconColor, "Contiene un collegamento web"));
        }

        if (todo.getImagePath() != null && !todo.getImagePath().trim().isEmpty()) {
            bottomWrapper.add(createIconLabel("icons/image.svg", iconColor, "Contiene un'immagine allegata"));
        }

        if (todo.getCollaborators() != null && !todo.getCollaborators().isEmpty()) {
            long count = todo.getCollaborators().size();
            JLabel lblCollab = createIconLabel("icons/user/user.svg", iconColor, "Condiviso con " + count + " utenti");
            lblCollab.setText("+" + count);
            lblCollab.setFont(new Font(GuiUtils.getFontFamily(), Font.BOLD, 11));
            lblCollab.setForeground(iconColor);
            lblCollab.setIconTextGap(2);
            bottomWrapper.add(lblCollab);
        }

        btnOptions = createThreeDotsButton();
        bottomWrapper.add(btnOptions);

        rightPanel.add(bottomWrapper, BorderLayout.SOUTH);
        add(rightPanel, BorderLayout.EAST);
    }

    /**
     * Crea un componente JLabel configurato con un'icona SVG e un tooltip.
     * Utilizza le utilità di sistema per il caricamento e il ridimensionamento
     * dell'icona vettoriale con il colore specificato.
     *
     * @param path    Il percorso relativo della risorsa SVG all'interno del progetto.
     * @param color   Il colore da applicare agli elementi grafici dell'icona.
     * @param tooltip Il testo informativo da visualizzare al passaggio del mouse.
     * @return Un oggetto {@code JLabel} pronto per essere inserito nell'interfaccia.
     */
    private JLabel createIconLabel(String path, Color color, String tooltip) {
        JLabel lbl = new JLabel();
        lbl.setIcon(GuiUtils.loadSVG(path, 14, 14, color));
        lbl.setToolTipText(tooltip);
        return lbl;
    }

    /**
     * Crea e configura il pannello contenente il badge colorato della priorità.
     * Estrae i dati direttamente dall'oggetto ToDo e applica uno stile
     * con angoli arrotondati tramite le proprietà client di FlatLaf.
     *
     * @return Un {@code JPanel} trasparente che agisce da contenitore per l'etichetta della priorità.
     */
    private JPanel createPriorityBadge() {
        JLabel lblPrio = new JLabel(todo.getPriority().getLabel());
        lblPrio.setFont(new Font(GuiUtils.getFontFamily(), Font.BOLD, 10));
        lblPrio.setForeground(Color.WHITE);
        lblPrio.setOpaque(false);
        lblPrio.setBackground(todo.getPriority().getColor());
        lblPrio.setHorizontalAlignment(SwingConstants.CENTER);
        lblPrio.putClientProperty(FlatClientProperties.STYLE, "arc: 10");
        lblPrio.setBorder(new EmptyBorder(2, 8, 2, 8));

        JPanel badgeWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        badgeWrapper.setOpaque(false);
        badgeWrapper.add(lblPrio);
        return badgeWrapper;
    }

    /**
     * Configura i listener del mouse per la gestione estetica della card.
     * Registra i listener necessari per l'aggiornamento visivo durante l'ingresso
     * e l'uscita del cursore (hover) e applica lo stile del cursore a mano a tutti
     * i componenti interni.
     * <p>
     * Nota: La logica di apertura dei dettagli tramite click è delegata al
     * {@link gui.events.TaskDragListener} per prevenire conflitti di eventi e
     * garantire la distinzione tra click e trascinamento.
     */
    private void configureMouseEvents() {
        MouseAdapter hoverListener = new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { repaint(); }
            @Override public void mouseExited(MouseEvent e) { repaint(); }
            // La gestione del mouseClicked è demandata al DragListener per evitare duplicazioni
        };
        this.addMouseListener(hoverListener);
        applyCursorToAll(this, btnOptions);
    }

    /**
     * Applica il cursore a forma di mano a tutti i componenti interni.
     * Evita di aggiungere listener ricorsivi per prevenire il rimbalzo degli eventi.
     *
     * @param container  Il contenitore padre da scansionare.
     * @param excludeBtn Il bottone da escludere dall'aggiornamento.
     */
    private void applyCursorToAll(Container container, JButton excludeBtn) {
        for (Component c : container.getComponents()) {
            if (c == chkDone || c == excludeBtn) continue;

            c.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            if (c instanceof Container childContainer) {
                applyCursorToAll(childContainer, excludeBtn);
            }
        }
    }

    /**
     * Applica ricorsivamente un listener a tutti i componenti interni della card.
     * Esclude pulsanti o checkbox specifici per evitare conflitti di click e imposta
     * il cursore a mano (HAND_CURSOR) per indicare l'interattività.
     *
     * @param container  Il contenitore padre da cui iniziare la scansione dei componenti.
     * @param listener   Il MouseListener da associare ai componenti validi.
     * @param excludeBtn Il bottone specifico (es. tre puntini) da escludere dall'associazione.
     */
    private void applyListenerToAll(Container container, java.awt.event.MouseListener listener, JButton excludeBtn) {
        for (Component c : container.getComponents()) {
            if (c == chkDone || c == excludeBtn) continue;
            c.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            c.addMouseListener(listener);

            if (c instanceof Container childContainer) {
                applyListenerToAll(childContainer, listener, excludeBtn);
            }
        }
    }

    /**
     * Crea il bottone circolare con l'icona dei tre puntini verticali.
     * Personalizza il rendering grafico per disegnare i punti centrali e gestire
     * l'effetto hover circolare.
     *
     * @return Un oggetto {@code JButton} configurato per l'apertura del menu opzioni.
     */
    private JButton createThreeDotsButton() {
        JButton btn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                if (getModel().isPressed() || getModel().isRollover()) {
                    g2.setColor(new Color(0, 0, 0, 20));
                    g2.fillOval(2, 2, w - 4, h - 4);
                }

                Color dotsColor = GuiUtils.getContrastColor(getBackground());
                if(dotsColor == Color.WHITE) dotsColor = Color.LIGHT_GRAY;
                else dotsColor = Color.GRAY;

                g2.setColor(dotsColor);
                int dotSize = 3;
                int gap = 3;
                int totalH = (dotSize * 3) + (gap * 2);
                int startY = (h - totalH) / 2;
                int centerX = (w - dotSize) / 2;

                for (int i = 0; i < 3; i++) {
                    g2.fillOval(centerX, startY + (i * (dotSize + gap)), dotSize, dotSize);
                }
                g2.dispose();
            }
        };

        btn.setPreferredSize(new Dimension(24, 24));
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> showOptionsMenu(btn, 0, btn.getHeight()));

        return btn;
    }

    /**
     * Costruisce e mostra il menu contestuale con le opzioni del task.
     * Differenzia le voci disponibili (Sposta, Elimina, Lascia) in base al ruolo
     * dell'utente (OWNER o SHARED).
     *
     * @param invoker Il componente che ha richiesto l'apertura del menu.
     * @param x       La coordinata X relativa al componente invoker.
     * @param y       La coordinata Y relativa al componente invoker.
     */
    private void showOptionsMenu(Component invoker, int x, int y) {
        JPopupMenu popup = new JPopupMenu();
        JMenuItem itemOpen = new JMenuItem("Apri");
        itemOpen.addActionListener(e -> openDetails());
        popup.add(itemOpen);
        popup.add(createRenameMenuItem());

        boolean isOwner = "OWNER".equalsIgnoreCase(todo.getRole());
        popup.addSeparator();

        if (isOwner) {
            JMenuItem itemMove = new JMenuItem("Sposta in...");
            itemMove.addActionListener(e -> moveTodoToAnotherBoard());
            popup.add(itemMove);
            popup.addSeparator();
            popup.add(createDeleteMenuItem());
        } else {
            popup.addSeparator();
            popup.add(createLeaveMenuItem());
        }
        popup.show(invoker, x, y);
    }

    /**
     * Crea la voce di menu dedicata alla ridenominazione del task.
     * @return Un {@code JMenuItem} configurato con la logica di input per il nuovo titolo.
     */
    private JMenuItem createRenameMenuItem() {
        JMenuItem item = new JMenuItem("Rinomina");
        item.addActionListener(e -> {
            String newT = JOptionPane.showInputDialog(getBaseFrame(), "Nuovo titolo:", todo.getTitle());
            if (newT != null && !newT.trim().isEmpty() && !newT.equals(todo.getTitle())) {
                executeRename(newT.trim());
            }
        });
        return item;
    }

    /**
     * Esegue l'operazione di ridenominazione tramite il controller.
     * In caso di errore, ripristina il titolo originale e avvisa l'utente.
     * @param newTitle Il nuovo titolo testuale da assegnare al task.
     */
    private void executeRename(String newTitle) {
        String old = todo.getTitle();
        todo.setTitle(newTitle);
        try {
            if (controller.updateTodo(todo)) {
                if (onUpdate != null) onUpdate.run();
            } else {
                todo.setTitle(old);
                JOptionPane.showMessageDialog(getBaseFrame(), "Salvataggio fallito.", ERROR, JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            todo.setTitle(old);
            JOptionPane.showMessageDialog(getBaseFrame(), ex.getMessage(), ERROR, JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Crea la voce di menu per l'eliminazione definitiva del task.
     * @return Un {@code JMenuItem} configurato per la cancellazione (riservato al proprietario).
     */
    private JMenuItem createDeleteMenuItem() {
        JMenuItem itemDelete = new JMenuItem("Elimina");
        itemDelete.setForeground(Color.RED);
        itemDelete.setToolTipText("Elimina definitivamente il task per tutti");
        itemDelete.addActionListener(e -> {
            String msg = "Eliminare il task \"" + todo.getTitle() + "\"?\nOperazione irreversibile per TUTTI.";

            if (GuiUtils.showConfirmDialog(getBaseFrame(), msg, "Conferma Eliminazione", JOptionPane.ERROR_MESSAGE)) {
                if (controller.deleteTodo(todo.getId())) {
                    if (onUpdate != null) onUpdate.run();
                } else {
                    JOptionPane.showMessageDialog(getBaseFrame(), "Errore durante l'eliminazione.", ERROR, JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        return itemDelete;
    }

    /**
     * Crea la voce di menu per permettere a un collaboratore di abbandonare il task.
     * @return Un {@code JMenuItem} configurato per rimuovere la condivisione lato utente.
     */
    private JMenuItem createLeaveMenuItem() {
        JMenuItem itemLeave = new JMenuItem("Lascia");
        itemLeave.setForeground(Color.RED);
        itemLeave.setToolTipText("Smetti di collaborare a questo task");
        itemLeave.addActionListener(e -> {
            String msg = "Vuoi smettere di seguire questo task?\nNon verrà eliminato per gli altri.";

            if (GuiUtils.showConfirmDialog(getBaseFrame(), msg, "Conferma Uscita", JOptionPane.WARNING_MESSAGE)) {
                if (controller.deleteTodo(todo.getId())) {
                    if (onUpdate != null) onUpdate.run();
                } else {
                    JOptionPane.showMessageDialog(getBaseFrame(), "Errore durante l'uscita dal task.", ERROR, JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        return itemLeave;
    }

    /**
     * Gestisce la logica di spostamento del task verso un'altra bacheca dell'utente.
     */
    private void moveTodoToAnotherBoard() {
        List<Board> allBoards = controller.getUserBoards();
        List<Board> targets = allBoards.stream()
                .filter(b -> b.getId() != todo.getBoardId())
                .toList();

        if (targets.isEmpty()) {
            JOptionPane.showMessageDialog(getBaseFrame(), "Nessuna altra bacheca disponibile.");
            return;
        }

        Board selectedBoard = (Board) JOptionPane.showInputDialog(
                getBaseFrame(), "Seleziona bacheca destinazione:", "Sposta Task",
                JOptionPane.QUESTION_MESSAGE, null, targets.toArray(), targets.get(0)
        );

        if (selectedBoard != null) {
            if (controller.moveTodo(todo.getId(), selectedBoard.getId(), controller.getCurrentUser().getId())) {
                JOptionPane.showMessageDialog(getBaseFrame(), "Task spostato in \"" + selectedBoard.getTitle() + "\"");
                if (onUpdate != null) onUpdate.run();
            } else {
                JOptionPane.showMessageDialog(getBaseFrame(), "Errore spostamento.");
            }
        }
    }

    /**
     * Apre il dialogo dei dettagli avanzati del task.
     */
    public void openDetails() {
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        TodoDetailsDialog dialog = new TodoDetailsDialog(parentWindow, controller, todo, onUpdate);
        dialog.setVisible(true);
    }

    // --- DISEGNO PERSONALIZZATO E STILE ---

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        int gap = 10;
        int arc = 15;

        Color bg = getBackground();
        Point p = getMousePosition();

        if (p != null && contains(p)) {
            bg = darkenColor(bg);
        }

        g2.setColor(bg);
        g2.fillRoundRect(0, 0, width - 1, height - 1 - gap, arc, arc);

        g2.setColor(new Color(220, 220, 220));
        g2.drawRoundRect(0, 0, width - 1, height - 1 - gap, arc, arc);

        g2.dispose();
    }

    /**
     * Aggiorna dinamicamente font e colori della card in base allo stato del task
     * e al colore di sfondo impostato dall'utente.
     */
    private void refreshStyle() {
        boolean darkBackground = isDark(getBackground());

        if (todo.isCompleted()) {
            applyCompletedStyle(darkBackground);
        } else {
            applyActiveStyle(darkBackground);
        }

        revalidate();
        repaint();
    }

    /**
     * Applica lo stile visivo per i task completati (testo barrato e colori tenui).
     *
     * @param darkBackground {@code true} se lo sfondo della card è scuro.
     */
    private void applyCompletedStyle(boolean darkBackground) {
        Font originalFont = GuiUtils.FONT_BOLD;
        @SuppressWarnings("unchecked")
        Map<TextAttribute, Object> attributes = (Map<TextAttribute, Object>) originalFont.getAttributes();
        attributes.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
        lblTitle.setFont(originalFont.deriveFont(attributes));

        Color completedColor = darkBackground ? new Color(150, 150, 150) : Color.GRAY;

        lblTitle.setForeground(completedColor);
        setForegroundSafe(lblDesc, completedColor);
        setForegroundSafe(lblDate, completedColor);
    }

    /**
     * Applica lo stile visivo per i task attivi, calcolando i contrasti e
     * gestendo l'eventuale stato di scadenza.
     * @param darkBackground {@code true} se lo sfondo della card è scuro.
     */
    private void applyActiveStyle(boolean darkBackground) {
        lblTitle.setFont(GuiUtils.FONT_BOLD);

        Color titleColor = darkBackground ? Color.WHITE : Color.BLACK;
        Color descColor = darkBackground ? new Color(230, 230, 230) : new Color(60, 60, 60);
        Color dateColor = darkBackground ? new Color(200, 200, 200) : Color.GRAY;

        // Formattazione della data
        String formattedDate = "";
        if (todo.getExpiryDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            formattedDate = sdf.format(todo.getExpiryDate());
        }

        if (isOverdue()) {
            Color alertColor = darkBackground ? new Color(255, 100, 100) : Color.RED;
            lblTitle.setForeground(alertColor);
            setForegroundSafe(lblDate, alertColor);

            // Cambia il prefisso da "Scadenza" a "Scaduto"
            if (lblDate != null) lblDate.setText("Scaduto: " + formattedDate);
        } else {
            lblTitle.setForeground(titleColor);
            setForegroundSafe(lblDate, dateColor);

            // Imposta il prefisso standard "Scadenza"
            if (lblDate != null) lblDate.setText("Scadenza: " + formattedDate);
        }

        setForegroundSafe(lblDesc, descColor);
    }

    /**
     * Imposta il colore del testo solo se il componente grafico è stato istanziato.
     *
     * @param component Il componente JLabel target (può essere null).
     * @param color     Il colore da applicare.
     */
    private void setForegroundSafe(JLabel component, Color color) {
        if (component != null) {
            component.setForeground(color);
        }
    }

    /**
     * Determina se un colore è considerato scuro basandosi sul calcolo della sua luminanza.
     * Utilizza la formula pesata dei colori primari (0.299R + 0.587G + 0.114B).
     *
     * @param color Il colore da sottoporre ad analisi.
     * @return {@code true} se la luminanza calcolata è inferiore a 140, indicando un colore scuro.
     */
    private boolean isDark(Color color) {
        double luminance = (0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue());
        return luminance < 140;
    }

    /**
     * Verifica se il task corrente ha superato la data di scadenza.
     * Il controllo fallisce se non è impostata una data o se il task risulta già completato.
     *
     * @return {@code true} se la data di scadenza è antecedente al timestamp attuale.
     */
    private boolean isOverdue() {
        if (todo.getExpiryDate() == null || todo.isCompleted()) return false;
        Timestamp now = new Timestamp(System.currentTimeMillis());
        return todo.getExpiryDate().before(now);
    }

    /**
     * Accorcia una stringa di testo se questa supera la lunghezza massima consentita.
     * Aggiunge i puntini di sospensione al termine della sottostringa troncata.
     *
     * @param text Il testo originale da elaborare.
     * @return Il testo troncato a 80 caratteri seguito da "...", oppure il testo originale se entro i limiti.
     */
    private String truncateText(String text) {
        int maxLength = 80;
        if (text == null || text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }

    /**
     * Produce una versione leggermente più scura del colore fornito in input.
     * Applica un fattore di riduzione costante a tutte le componenti RGB.
     *
     * @param color Il colore di partenza da scurire.
     * @return Un nuovo oggetto {@code Color} con luminosità ridotta del 4%.
     */
    private Color darkenColor(Color color) {
        double factor = 0.96;
        int r = (int) Math.max(0, color.getRed() * factor);
        int g = (int) Math.max(0, color.getGreen() * factor);
        int b = (int) Math.max(0, color.getBlue() * factor);
        return new Color(r, g, b);
    }

    /**
     * Restituisce l'oggetto del dominio ToDo associato a questa componente grafica.
     *
     * @return L'entità {@code ToDo} rappresentata dalla card.
     */
    public ToDo getTodo() { return this.todo; }

    /**
     * Calcola la dimensione massima consentita per la card all'interno del contenitore.
     * Adatta dinamicamente la larghezza in base alle dimensioni della finestra principale.
     *
     * @return Un oggetto {@code Dimension} rappresentante la larghezza massima e l'altezza preferita.
     */
    @Override
    public Dimension getMaximumSize() {
        int prefHeight = getPreferredSize().height;
        int maxWidth = 1100;
        Container parent = getBaseFrame();
        if (parent != null) maxWidth = Math.min(1100, parent.getWidth() - 40);
        return new Dimension(maxWidth, prefHeight);
    }

    /**
     * Definisce la dimensione minima che la card deve occupare per garantire la leggibilità.
     *
     * @return Un oggetto {@code Dimension} con larghezza minima fissata a 200 pixel.
     */
    @Override
    public Dimension getMinimumSize() {
        return new Dimension(200, super.getMinimumSize().height);
    }

    /**
     * Specifica l'allineamento orizzontale della componente rispetto al suo contenitore.
     *
     * @return Il valore costante {@code Component.CENTER_ALIGNMENT}.
     */
    @Override
    public float getAlignmentX() { return Component.CENTER_ALIGNMENT; }

    /**
     * Recupera la finestra principale (Window) che contiene gerarchicamente questa card.
     * Viene utilizzata come riferimento per il posizionamento dei dialoghi modali.
     *
     * @return L'antenato di tipo {@code Window} della card, o {@code null} se non trovato.
     */
    private Window getBaseFrame() {
        return SwingUtilities.getWindowAncestor(this);
    }
}