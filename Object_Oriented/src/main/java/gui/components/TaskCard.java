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
 *
 * @author Nunzio Grasso (Matricola: N86005509)
 * @version 1.0
 */
public class TaskCard extends JPanel {

    /** L'oggetto di dominio contenente i dati del task rappresentato. */
    private final transient ToDo todo;

    /** Riferimento al controller per la gestione delle operazioni di business. */
    private final transient Controller controller;

    /** L'azione di callback da eseguire per notificare l'interfaccia madre di un aggiornamento. */
    private final transient Runnable onUpdate;

    /** Stringa costante utilizzata per i titoli delle finestre di dialogo di errore. */
    private static final String ERROR = "Errore";

    // --- Componenti UI ---
    /** L'etichetta testuale principale che mostra il titolo del task. */
    private JLabel lblTitle;

    /** L'etichetta testuale secondaria che mostra una versione troncata della descrizione. */
    private JLabel lblDesc;

    /** L'etichetta testuale che riporta la data e l'ora di scadenza (se prevista). */
    private JLabel lblDate;

    /** La casella di controllo laterale per impostare lo stato di completamento del task. */
    private JCheckBox chkDone;

    /** Il pulsante grafico (con icona a tre puntini) che apre il menu delle operazioni aggiuntive. */
    private JButton btnOptions;

    /**
     * Inizializza la rappresentazione grafica per lo specifico task fornito.
     *
     * @param todo       L'oggetto ToDo contenente i dati da visualizzare.
     * @param controller Il gestore della logica applicativa per le operazioni di persistenza.
     * @param onUpdate   La funzione di callback invocata in seguito a mutamenti di stato del task.
     */
    public TaskCard(ToDo todo, Controller controller, Runnable onUpdate) {
        this.todo = todo;
        this.controller = controller;
        this.onUpdate = onUpdate;

        initUI();
        refreshStyle();
    }

    /**
     * Struttura i componenti grafici primari e definisce il layout ({@link BorderLayout}) della card.
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
     * Configura dinamicamente il colore di sfondo della card, interpretando la stringa esadecimale
     * salvata nel database o ripiegando sul colore predefinito di sistema in caso di errore.
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
     * Inizializza e posiziona la checkbox laterale delegata alla gestione dello stato di completamento.
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
     * Intercetta l'interazione dell'utente sulla checkbox e richiede l'aggiornamento
     * dello stato di completamento del task a livello di database.
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
                    ERROR, JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Struttura e aggrega nel pannello centrale le informazioni testuali principali:
     * Titolo, Descrizione (se presente) e Data di Scadenza (se prevista).
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
     * Struttura il pannello contestuale destro, deputato a ospitare i badge informativi
     * (es. stato di condivisione, livello di priorità) e gli indicatori visivi di presenza allegati (link, immagini).
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
     * Inizializza un componente JLabel configurato per esporre un'icona vettoriale SVG
     * accoppiata a un suggerimento testuale (tooltip).
     *
     * @param path    Il percorso relativo della risorsa SVG all'interno del file system del progetto.
     * @param color   Il colore da applicare in sovrimpressione ai tracciati vettoriali dell'icona.
     * @param tooltip Il testo informativo visualizzato al passaggio del cursore del mouse.
     * @return L'oggetto {@code JLabel} interamente configurato.
     */
    private JLabel createIconLabel(String path, Color color, String tooltip) {
        JLabel lbl = new JLabel();
        lbl.setIcon(GuiUtils.loadSVG(path, 14, 14, color));
        lbl.setToolTipText(tooltip);
        return lbl;
    }

    /**
     * Genera un contenitore grafico rappresentante il livello di priorità assegnato al task.
     * Sfrutta le estensioni client di FlatLaf per definire un raggio di curvatura
     * dinamico (arc) sui bordi del componente.
     *
     * @return Il pannello ({@code JPanel}) trasparente contenente l'etichetta formattata.
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
     * Associa i listener legati agli eventi del mouse per gestire il feedback visivo (hover).
     * <p>
     * Nota architetturale: L'operazione di visualizzazione dei dettagli mediante click
     * è demandata esternamente (al {@link gui.events.TaskDragListener}) per eliminare
     * i conflitti di interpretazione tra click singolo e inizio di un'operazione di trascinamento.
     */
    private void configureMouseEvents() {
        MouseAdapter hoverListener = new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { repaint(); }
            @Override public void mouseExited(MouseEvent e) { repaint(); }
        };
        this.addMouseListener(hoverListener);
        applyCursorToAll(this, btnOptions);
    }

    /**
     * Scandisce ricorsivamente la gerarchia dei componenti applicando il cursore a forma
     * di puntatore agli elementi interattivi.
     *
     * @param container  Il contenitore padre radice dell'albero di ricerca.
     * @param excludeBtn Il pulsante o componente specifico da preservare da questa alterazione.
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
     * Genera il pulsante opzioni customizzato raffigurante tre ellissi impilate verticalmente.
     * Sovrascrive il metodo di disegno (paintComponent) per tracciare manualmente
     * i punti e simulare un'ombra circolare semi-trasparente durante l'interazione (hover).
     *
     * @return L'oggetto {@code JButton} deputato all'attivazione del menu contestuale.
     */
    private JButton createThreeDotsButton() {
        JButton btn = new JButton() {
            /** {@inheritDoc} */
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
     * Istanzia e visualizza il menu flottante (JPopupMenu) contenente
     * le azioni eseguibili sul task. Le opzioni ("Elimina", "Sposta") vengono filtrate
     * dinamicamente in base ai privilegi associati al ruolo dell'utente corrente.
     *
     * @param invoker Il componente grafico scatenante che ha richiesto l'invocazione del menu.
     * @param x       L'offset sull'asse X per l'ancoraggio del popup.
     * @param y       L'offset sull'asse Y per l'ancoraggio del popup.
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
     * Inizializza l'elemento di menu associato alla funzionalità di ridenominazione.
     * Richiama una finestra di dialogo nativa per la raccolta del nuovo titolo.
     *
     * @return L'istanza di {@code JMenuItem} configurata.
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
     * Coordina l'interazione con il livello logico per salvare il nuovo titolo nel database.
     * Implementa un meccanismo di rollback locale qualora l'operazione di persistenza fallisca.
     *
     * @param newTitle La stringa alfanumerica rappresentante il nuovo titolo assegnato.
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
     * Inizializza l'elemento di menu associato alla funzionalità di distruzione fisica (DELETE).
     * Tale operazione, distruttiva e irreversibile, è preclusa ai ruoli non proprietari.
     *
     * @return L'istanza di {@code JMenuItem} configurata.
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
     * Inizializza l'elemento di menu che consente la dissociazione
     * di un collaboratore dal task (abbandono della condivisione).
     *
     * @return L'istanza di {@code JMenuItem} configurata.
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
     * Governa il flusso logico interattivo per consentire al proprietario di alterare
     * l'associazione relazionale del task, riallocandolo su una differente bacheca target.
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
     * Istanzia e rende visibile la finestra di dialogo modale adibita
     * all'ispezione ed editazione estesa dei metadati del task.
     */
    public void openDetails() {
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        TodoDetailsDialog dialog = new TodoDetailsDialog(parentWindow, controller, todo, onUpdate);
        dialog.setVisible(true);
    }

    /**
     * {@inheritDoc}
     * Intercetta la fase di disegno per rimpiazzare i bordi rettangolari standard
     * di Swing con una sagoma smussata, modulandone dinamicamente il colore
     * al passaggio del cursore.
     */
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
     * Forza la ricalibrazione e ridisegnazione dei parametri tipografici (font, colori)
     * basando le logiche di contrasto sull'indice di luminanza dello sfondo corrente.
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
     * Implementa i tratti stilistici del completamento (font depotenziato cromaticamente e barrato).
     *
     * @param darkBackground Variabile booleana, {@code true} se la luminanza calcolata risulta bassa.
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
     * Implementa i tratti stilistici predefiniti attivi, allocando cromie appropriate in
     * base allo sfondo e richiamando controlli condizionali per eventuali allarmi temporali (scadenze).
     *
     * @param darkBackground Variabile booleana, {@code true} se la luminanza calcolata risulta bassa.
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
     * Esegue in sicurezza l'assegnazione cromatica su un controllo Swing,
     * sopprimendo l'esecuzione qualora il componente non risulti istanziato (null-safe).
     *
     * @param component L'etichetta bersaglio della mutazione.
     * @param color     L'oggetto Colore designato per l'assegnazione in primo piano (foreground).
     */
    private void setForegroundSafe(JLabel component, Color color) {
        if (component != null) {
            component.setForeground(color);
        }
    }

    /**
     * Computa empiricamente l'indice di luminanza apparente ricorrendo all'equazione
     * fotometrica ITU-R BT.601 (0.299R + 0.587G + 0.114B).
     *
     * @param color L'entità cromatica soggetta all'estrazione delle componenti.
     * @return {@code true} se il valore di uscita risulta marcatamente inferiore alla soglia di mezzo.
     */
    private boolean isDark(Color color) {
        double luminance = (0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue());
        return luminance < 140;
    }

    /**
     * Valuta l'aderenza tra la data limite registrata e il tempo macchina corrente.
     * La violazione si verifica esclusivamente su task la cui spunta di completamento risulti vacante.
     *
     * @return {@code true} se il timestamp puntatore risulta inferiore alla misurazione di sistema.
     */
    private boolean isOverdue() {
        if (todo.getExpiryDate() == null || todo.isCompleted()) return false;
        Timestamp now = new Timestamp(System.currentTimeMillis());
        return todo.getExpiryDate().before(now);
    }

    /**
     * Riduce proattivamente la lunghezza della stringa per assicurare il contenimento
     * visivo nel modulo grafico (ellissi terminale per stringhe lunghe).
     *
     * @param text Il corpo testuale da conformare alle tolleranze metriche.
     * @return La stringa processata.
     */
    private String truncateText(String text) {
        int maxLength = 80;
        if (text == null || text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }

    /**
     * Esegue un oscuramento omogeneo tramite manipolazione in riduzione proporzionale
     * del vettore dei canali primari (RGB).
     *
     * @param color Il punto di genesi cromatica.
     * @return Il color derivato (intensità decrescente del 4%).
     */
    private Color darkenColor(Color color) {
        double factor = 0.96;
        int r = (int) Math.max(0, color.getRed() * factor);
        int g = (int) Math.max(0, color.getGreen() * factor);
        int b = (int) Math.max(0, color.getBlue() * factor);
        return new Color(r, g, b);
    }

    /**
     * Recupera l'astrazione concettuale primaria (Entity) vincolata a tale frammento dell'interfaccia.
     *
     * @return Il blocco {@link ToDo}.
     */
    public ToDo getTodo() { return this.todo; }

    /**
     * {@inheritDoc}
     * Computa attivamente le dimensioni limite, interrogando geometricamente il frame genitore.
     *
     * @return Costrutto dimensionale con un tetto elastico di larghezza.
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
     * {@inheritDoc}
     *
     * @return Costrutto dimensionale con sbarramento minimo inderogabile.
     */
    @Override
    public Dimension getMinimumSize() {
        return new Dimension(200, super.getMinimumSize().height);
    }

    /**
     * {@inheritDoc}
     *
     * @return Direttiva float statica.
     */
    @Override
    public float getAlignmentX() { return Component.CENTER_ALIGNMENT; }

    /**
     * Ricerca la cima dell'albero gerarchico grafico.
     * Necessario per garantire il blocco dei flussi (modalità) durante l'invocazione di dialog box.
     *
     * @return Modulo Window radice (può ricadere a null se l'albero è disconnesso).
     */
    private Window getBaseFrame() {
        return SwingUtilities.getWindowAncestor(this);
    }
}