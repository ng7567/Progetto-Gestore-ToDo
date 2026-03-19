package gui.frames;

import controller.Controller;
import gui.components.AppHeader;
import gui.components.TaskCard;
import gui.dialogs.BoardDetailsDialog;
import gui.dialogs.AddTodoDialog;
import gui.events.TaskDragListener;
import gui.style.GuiUtils;
import gui.style.WrapLayout;
import model.Board;
import model.ToDo;
import com.toedter.calendar.JDateChooser;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Rappresenta la finestra principale dedicata alla visualizzazione e alla gestione
 * dei task contenuti all'interno di una specifica bacheca (Board).
 * Fornisce strumenti per il filtraggio temporale, la ricerca testuale e l'interazione
 * mediante trascinamento (Drag &amp; Drop). Coordina l'aggiornamento dinamico della vista
 * in risposta ai mutamenti dei dati e gestisce la transizione verso i dialoghi di dettaglio
 * e creazione dei task.
 *
 * @author Nunzio Grasso (Matricola: N86005509)
 * @version 1.0
 */
public class BoardFrame extends JFrame {

    /** Il riferimento al gestore della logica di business per il recupero e la manipolazione dei dati. */
    private final transient Controller controller;

    /** L'entità bacheca attualmente visualizzata e gestita nel frame. */
    private final transient Board currentBoard;

    /** Il pannello personalizzato adibito all'ospitare la collezione verticale delle card dei task. */
    private ScrollablePanel todosPanel;

    // --- Filtri di Visualizzazione ---

    /** Il campo di testo per l'immissione della stringa di ricerca testuale. */
    private JTextField txtSearch;

    /** Il selettore booleano per filtrare esclusivamente i task in scadenza nella giornata odierna. */
    private JCheckBox chkToday;

    /** Il selettore booleano per attivare il filtraggio basato su una data limite specifica. */
    private JCheckBox chkByDate;

    /** Il componente calendario per la selezione della data target per il filtro temporale. */
    private JDateChooser filterDateChooser;

    // --- Collezioni Dati ---

    /** L'elenco integrale dei task associati alla bacheca, recuperato dal database. */
    private transient List<ToDo> allTodos;

    /** L'elenco filtrato dei task attualmente renderizzati a video. */
    private transient List<ToDo> displayedTodos;

    /**
     * Inizializza il frame della bacheca configurando lo stato iniziale e le dimensioni di visualizzazione.
     * Esegue il primo caricamento dei dati e predispone i parametri di chiusura e centratura.
     *
     * @param controller Il riferimento al Controller principale dell'applicazione.
     * @param board      La bacheca selezionata di cui mostrare i contenuti.
     */
    public BoardFrame(Controller controller, Board board) {
        super("ToDo App - " + board.getTitle());
        this.controller = controller;
        this.currentBoard = board;
        this.allTodos = new ArrayList<>();
        this.displayedTodos = new ArrayList<>();

        initUI();
        refreshDataFromDB();

        GuiUtils.setAppIcon(this);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(1000, 600));
    }

    /**
     * Struttura gerarchicamente i componenti grafici del frame.
     * Organizza l'interfaccia in macro-aree: testata navigabile, barra dei filtri,
     * area di scorrimento centrale per i task e sezione dei comandi inferiori.
     */
    private void initUI() {
        JPanel mainPanel = GuiUtils.createGradientPanel();
        mainPanel.setLayout(new BorderLayout());

        AppHeader header = createHeader();
        mainPanel.add(header, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setOpaque(false);

        contentPanel.add(createFilterBar(), BorderLayout.NORTH);

        todosPanel = new ScrollablePanel();
        todosPanel.setLayout(new BoxLayout(todosPanel, BoxLayout.Y_AXIS));
        todosPanel.setOpaque(false);
        todosPanel.setBorder(new EmptyBorder(10, 20, 20, 20));

        JScrollPane scroll = GuiUtils.createModernScrollPane(todosPanel);
        contentPanel.add(scroll, BorderLayout.CENTER);

        mainPanel.add(contentPanel, BorderLayout.CENTER);

        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 30, 20));
        footerPanel.setOpaque(false);

        JButton btnAddTodo = new JButton("+ Nuovo Task");
        GuiUtils.stylePrimaryButton(btnAddTodo);
        btnAddTodo.setPreferredSize(new Dimension(150, 45));
        btnAddTodo.addActionListener(e -> {
            AddTodoDialog dialog = new AddTodoDialog(this, controller, currentBoard.getId());
            dialog.setVisible(true);
            if (dialog.isSuccess()) {
                refreshDataFromDB();
            }
        });

        footerPanel.add(btnAddTodo);
        mainPanel.add(footerPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    /**
     * Configura l'intestazione personalizzata del frame integrando le funzioni di navigazione.
     * Associa le callback per il ripristino della bacheca, il ritorno alla schermata Home
     * e l'apertura dei metadati descrittivi della bacheca stessa.
     *
     * @return L'oggetto {@link AppHeader} configurato con le azioni di contesto.
     */
    private AppHeader createHeader() {
        Runnable reload = () -> GuiUtils.reloadWindow(this, () -> new BoardFrame(controller, currentBoard));
        Runnable goBack = () -> GuiUtils.transition(this, new HomeFrame(controller));

        Runnable onOpenDetails = () -> {
            BoardDetailsDialog dialog = new BoardDetailsDialog(this, controller, currentBoard, reload);
            dialog.setVisible(true);
        };

        return new AppHeader(currentBoard.getTitle(), goBack, reload, controller, this, onOpenDetails);
    }

    /**
     * Istanzia il pannello dei controlli deputati al filtraggio dinamico della collezione.
     * Implementa listener per la ricerca "real-time" e gestisce la logica di mutua esclusione
     * tra le selezioni temporali (Oggi vs Data specifica) per garantire la coerenza dell'input.
     *
     * @return Il pannello ({@code JPanel}) contenente gli strumenti di filtraggio.
     */
    private JPanel createFilterBar() {
        JPanel filterPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, 15, 15));
        filterPanel.setOpaque(false);
        filterPanel.setBorder(new EmptyBorder(5, 15, 5, 15));

        JLabel lblSearch = GuiUtils.createLabel("Cerca:");
        txtSearch = GuiUtils.createStandardTextField(20);
        txtSearch.setToolTipText("Cerca per titolo o descrizione...");

        DocumentListener docListener = new DocumentListener() {
            /** {@inheritDoc} */
            public void insertUpdate(DocumentEvent e) { applyFilters(); }
            /** {@inheritDoc} */
            public void removeUpdate(DocumentEvent e) { applyFilters(); }
            /** {@inheritDoc} */
            public void changedUpdate(DocumentEvent e) { applyFilters(); }
        };
        txtSearch.getDocument().addDocumentListener(docListener);

        chkToday = new JCheckBox("In scadenza Oggi");
        GuiUtils.styleFilterCheckBox(chkToday);
        chkToday.addItemListener(e -> {
            if (chkToday.isSelected()) {
                chkByDate.setSelected(false);
                filterDateChooser.setEnabled(false);
            }
            applyFilters();
        });

        chkByDate = new JCheckBox("Entro il:");
        GuiUtils.styleFilterCheckBox(chkByDate);

        filterDateChooser = new JDateChooser();
        GuiUtils.styleDateChooser(filterDateChooser);
        filterDateChooser.setPreferredSize(new Dimension(130, 30));
        filterDateChooser.setEnabled(false);

        chkByDate.addItemListener(e -> {
            boolean selected = (e.getStateChange() == ItemEvent.SELECTED);
            filterDateChooser.setEnabled(selected);
            if (selected) {
                chkToday.setSelected(false);
                if (filterDateChooser.getDate() == null) {
                    Calendar cal = Calendar.getInstance();
                    cal.add(Calendar.DAY_OF_MONTH, 1);
                    filterDateChooser.setDate(cal.getTime());
                }
            }
            applyFilters();
        });

        filterDateChooser.addPropertyChangeListener("date", evt -> {
            if (chkByDate.isSelected()) applyFilters();
        });

        filterPanel.add(lblSearch);
        filterPanel.add(txtSearch);
        filterPanel.add(chkToday);
        filterPanel.add(chkByDate);
        filterPanel.add(filterDateChooser);

        return filterPanel;
    }

    /**
     * Sincronizza lo stato locale recuperando la collezione aggiornata dei task dal database.
     * Innesca automaticamente il ricalcolo dei filtri in seguito all'acquisizione dei nuovi dati.
     */
    public void refreshDataFromDB() {
        allTodos = controller.getTodos(currentBoard.getTitle());
        applyFilters();
    }

    /**
     * Processa la collezione integrale dei task applicando i parametri di ricerca correnti.
     * Sfrutta le operazioni aggregate per isolare gli elementi conformi
     * e demanda la ricostruzione grafica al metodo di rendering.
     */
    private void applyFilters() {
        String query = txtSearch.getText().trim().toLowerCase();
        boolean filterToday = chkToday.isSelected();
        boolean filterDate = chkByDate.isSelected();
        Date targetDate = filterDateChooser.getDate();

        Calendar calToday = Calendar.getInstance();
        resetTime(calToday);
        Date todayZero = calToday.getTime();

        displayedTodos = allTodos.stream().filter(t -> {
            if (!matchesTextQuery(t, query)) return false;
            if (filterToday && !matchesTodayFilter(t, todayZero)) return false;
            return !filterDate || targetDate == null || matchesDateFilter(t, targetDate, todayZero);
        }).toList();

        renderTodos();
    }

    /**
     * Valuta se l'istanza del task soddisfa i requisiti della ricerca testuale.
     *
     * @param t     L'oggetto ToDo soggetto a verifica.
     * @param query La stringa di ricerca normalizzata in minuscolo.
     * @return {@code true} se il testo è presente nel titolo o nella descrizione.
     */
    private boolean matchesTextQuery(ToDo t, String query) {
        if (query.isEmpty()) return true;
        String title = t.getTitle().toLowerCase();
        String desc = t.getDescription() != null ? t.getDescription().toLowerCase() : "";
        return title.contains(query) || desc.contains(query);
    }

    /**
     * Accerta se il task scade precisamente nella giornata odierna.
     *
     * @param t         L'oggetto ToDo soggetto a verifica.
     * @param todayZero La data odierna normalizzata all'inizio del giorno.
     * @return {@code true} se la scadenza coincide con la data odierna.
     */
    private boolean matchesTodayFilter(ToDo t, Date todayZero) {
        if (t.getExpiryDate() == null) return false;
        Calendar calT = Calendar.getInstance();
        calT.setTime(t.getExpiryDate());
        resetTime(calT);
        return calT.getTime().equals(todayZero);
    }

    /**
     * Verifica se la scadenza del task ricade nel range temporale definito tra l'istante
     * attuale e la data target selezionata.
     *
     * @param t          L'oggetto ToDo soggetto a verifica.
     * @param targetDate La data limite superiore selezionata.
     * @param todayZero  Il riferimento temporale odierno.
     * @return {@code true} se il task scade entro la data limite e non è già scaduto rispetto a oggi.
     */
    private boolean matchesDateFilter(ToDo t, Date targetDate, Date todayZero) {
        if (t.getExpiryDate() == null) return false;
        Date expiry = t.getExpiryDate();
        Date endOfTarget = endOfDay(targetDate);
        return !expiry.before(todayZero) && !expiry.after(endOfTarget);
    }

    /**
     * Rigenera dinamicamente le componenti grafiche nel pannello dei task.
     * Determina se abilitare le funzioni di riordino (Drag &amp; Drop) in base all'assenza
     * di filtri attivi, garantendo che le operazioni di spostamento non avvengano su
     * una vista parziale della bacheca.
     */
    private void renderTodos() {
        todosPanel.removeAll();

        if (displayedTodos.isEmpty()) {
            JLabel lblEmpty = new JLabel("Nessun task trovato");
            lblEmpty.setFont(new Font("Segoe UI", Font.BOLD, 18));
            lblEmpty.setForeground(GuiUtils.isDarkMode() ? new Color(200, 200, 200) : Color.GRAY);
            lblEmpty.setAlignmentX(Component.CENTER_ALIGNMENT);

            todosPanel.add(Box.createVerticalGlue());
            todosPanel.add(lblEmpty);
            todosPanel.add(Box.createVerticalGlue());
        } else {
            TaskDragListener dragListener = new TaskDragListener(
                    todosPanel,
                    controller,
                    this::refreshDataFromDB
            );

            // Blocca il trascinamento se i task visualizzati sono un sottoinsieme di quelli totali
            boolean filtersActive = displayedTodos.size() != allTodos.size();
            dragListener.setEnabled(!filtersActive);

            for (ToDo todo : displayedTodos) {
                TaskCard card = new TaskCard(todo, controller, this::refreshDataFromDB);
                card.setAlignmentX(Component.CENTER_ALIGNMENT);

                applyDragListenerRecursively(card, dragListener);

                if (filtersActive) {
                    card.setCursor(Cursor.getDefaultCursor());
                    dragListener.setEnabled(false);
                } else {
                    card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    dragListener.setEnabled(true);
                }

                todosPanel.add(card);
            }
            todosPanel.add(Box.createVerticalGlue());
        }

        todosPanel.revalidate();
        todosPanel.repaint();
    }

    /**
     * Propaga ricorsivamente il listener degli eventi del mouse lungo l'albero dei componenti.
     * Esclude esplicitamente i controlli interattivi (bottoni, checkbox, combo box) per
     * non comprometterne l'usabilità nativa.
     *
     * @param container Il componente genitore da cui avviare la scansione.
     * @param listener  L'ascoltatore eventi per la gestione coordinata di drag e click.
     */
    private void applyDragListenerRecursively(Container container, TaskDragListener listener) {
        container.addMouseListener(listener);
        container.addMouseMotionListener(listener);

        for (Component c : container.getComponents()) {
            if (c instanceof JButton || c instanceof JCheckBox || c instanceof JComboBox) {
                continue;
            }

            if (c instanceof Container containerObj) {
                applyDragListenerRecursively(containerObj, listener);
            } else {
                c.addMouseListener(listener);
                c.addMouseMotionListener(listener);
            }
        }
    }

    /**
     * Normalizza un'istanza di calendario azzerando ora, minuto e secondo.
     *
     * @param cal L'oggetto {@link Calendar} da azzerare.
     */
    private void resetTime(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }

    /**
     * Genera una data corrispondente all'ultimo istante utile (23:59:59.999) del giorno fornito.
     *
     * @param date La data di base.
     * @return Un'istanza {@link Date} normalizzata alla fine della giornata, o {@code null}.
     */
    private Date endOfDay(Date date) {
        if (date == null) return null;
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTime();
    }

    /**
     * Attiva o disabilita un Glass Pane per bloccare
     * l'interazione con il frame principale. Gestisce opzionalmente la chiusura
     * automatica dei dialoghi in risposta al click sull'overlay.
     *
     * @param show          Flag per determinare lo stato di visibilità dell'oscuramento.
     * @param onClickAction L'azione di callback da eseguire alla pressione sull'area oscurata.
     */
    public void showOverlay(boolean show, Runnable onClickAction) {
        if (show) {
            JPanel overlay = new JPanel();
            overlay.setOpaque(false);
            overlay.setLayout(null);
            overlay.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (onClickAction != null) onClickAction.run();
                    e.consume(); // Interrompe la propagazione dell'evento verso il basso
                }
            });
            setGlassPane(overlay);
            getGlassPane().setVisible(true);
        } else {
            getGlassPane().setVisible(false);
            requestFocusInWindow();
        }
    }

    /**
     * Sotto-classe specializzata per il pannello dei task che implementa {@link Scrollable}.
     * Garantisce che il pannello segua la larghezza della viewport del JScrollPane,
     * prevenendo lo scorrimento orizzontale.
     */
    private static class ScrollablePanel extends JPanel implements Scrollable {
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