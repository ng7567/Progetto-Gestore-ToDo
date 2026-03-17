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
 * Finestra principale per la visualizzazione e la gestione dei task all'interno
 * di una specifica bacheca. Fornisce funzionalità avanzate di filtraggio,
 * ricerca e interazione sugli elementi tramite drag &amp; drop.
 */
public class BoardFrame extends JFrame {

    private final transient Controller controller;
    private final transient Board currentBoard;
    private ScrollablePanel todosPanel;

    // Filtri
    private JTextField txtSearch;
    private JCheckBox chkToday;
    private JCheckBox chkByDate;
    private JDateChooser filterDateChooser;

    // Dati
    private transient List<ToDo> allTodos;
    private transient List<ToDo> displayedTodos;

    /**
     * Inizializza una nuova istanza della finestra BoardFrame.
     *
     * @param controller Il {@link Controller} dell'applicazione per le operazioni di business.
     * @param board      La bacheca ({@link Board}) correntemente selezionata per la visualizzazione.
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
     * Costruisce e posiziona tutti i componenti grafici dell'interfaccia utente.
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
     * Costruisce l'intestazione dell'applicazione e configura le azioni di navigazione.
     * Collega il bottone "Dettagli" alla finestra di dialogo per la modifica della bacheca
     * e definisce le callback per il ritorno alla Home e il ricaricamento della finestra.
     *
     * @return Il componente {@link AppHeader} configurato e pronto all'uso.
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
     * Costruisce il pannello degli strumenti per il filtraggio dinamico dei task.
     * Configura i listener per la ricerca testuale "real-time" e i selettori di data,
     * assicurando l'esclusione mutua tra il filtro giornaliero e quello per data specifica.
     *
     * @return Un {@link JPanel} contenente i controlli di ricerca e i filtri temporali.
     */
    private JPanel createFilterBar() {
        JPanel filterPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, 15, 15));
        filterPanel.setOpaque(false);
        filterPanel.setBorder(new EmptyBorder(5, 15, 5, 15));

        JLabel lblSearch = GuiUtils.createLabel("Cerca:");
        txtSearch = GuiUtils.createStandardTextField(20);
        txtSearch.setToolTipText("Cerca per titolo o descrizione...");

        DocumentListener docListener = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { applyFilters(); }
            public void removeUpdate(DocumentEvent e) { applyFilters(); }
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
     * Ricarica integralmente l'elenco dei task dal database.
     */
    public void refreshDataFromDB() {
        allTodos = controller.getTodos(currentBoard.getTitle());
        applyFilters();
    }

    /**
     * Applica i criteri di ricerca e i filtri temporali alla collezione dei task.
     * Utilizza le API Stream di Java per filtrare la lista originale e richiama
     * successivamente la procedura di rendering per aggiornare la vista grafica.
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
     * Verifica se un determinato task soddisfa i criteri della ricerca testuale.
     * Esegue il confronto, ignorando la differenza tra maiuscole e minuscole,
     * sul titolo e sulla descrizione dell'oggetto.
     *
     * @param t     Il task da sottoporre a verifica.
     * @param query La stringa di ricerca inserita dall'utente.
     * @return {@code true} se la query è vuota o se è contenuta nel titolo/descrizione.
     */
    private boolean matchesTextQuery(ToDo t, String query) {
        if (query.isEmpty()) return true;
        String title = t.getTitle().toLowerCase();
        String desc = t.getDescription() != null ? t.getDescription().toLowerCase() : "";
        return title.contains(query) || desc.contains(query);
    }

    /**
     * Accerta se la data di scadenza di un task coincide con la giornata odierna.
     *
     * @param t         Il task da analizzare.
     * @param todayZero La data di oggi azzerata nelle componenti di ora, minuti e secondi.
     * @return {@code true} se la scadenza del task cade nel giorno corrente.
     */
    private boolean matchesTodayFilter(ToDo t, Date todayZero) {
        if (t.getExpiryDate() == null) return false;
        Calendar calT = Calendar.getInstance();
        calT.setTime(t.getExpiryDate());
        resetTime(calT);
        return calT.getTime().equals(todayZero);
    }

    /**
     * Verifica se la scadenza di un task rientra nell'intervallo temporale tra oggi
     * e la data limite selezionata.
     *
     * @param t          Il task da controllare.
     * @param targetDate La data limite superiore del filtro.
     * @param todayZero  La data di oggi (limite inferiore).
     * @return {@code true} se il task scade entro il periodo specificato.
     */
    private boolean matchesDateFilter(ToDo t, Date targetDate, Date todayZero) {
        if (t.getExpiryDate() == null) return false;
        Date expiry = t.getExpiryDate();
        Date endOfTarget = endOfDay(targetDate);
        return !expiry.before(todayZero) && !expiry.after(endOfTarget);
    }

    /**
     * Aggiorna il pannello grafico ricostruendo le card dei task attualmente visibili.
     * In caso di lista vuota, visualizza un messaggio informativo; altrimenti, istanzia
     * le {@link TaskCard} associando loro i listener per il drag & drop e i menu contestuali.
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

            // Abilita il drag solo se non ci sono filtri attivi
            // Confronta la dimensione della lista filtrata con quella totale
            boolean filtersActive = displayedTodos.size() != allTodos.size();
            dragListener.setEnabled(!filtersActive);

            for (ToDo todo : displayedTodos) {
                TaskCard card = new TaskCard(todo, controller, this::refreshDataFromDB);
                card.setAlignmentX(Component.CENTER_ALIGNMENT);

                applyDragListenerRecursively(card, dragListener);

                // Se i filtri sono attivi, cambia il cursore per far capire all'utente che il drag è disabilitato
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
     * Applica in maniera ricorsiva il listener per le operazioni di trascinamento.
     * Analizza la gerarchia dei componenti per garantire che il drag & drop funzioni
     * cliccando su qualsiasi area della card, escludendo però i componenti interattivi
     * come bottoni o checkbox per non inibirne il funzionamento.
     *
     * @param container Il contenitore o componente da processare.
     * @param listener  L'ascoltatore eventi per il movimento e il click del mouse.
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
     * Azzera le componenti di ora, minuto, secondo e millisecondo in un oggetto {@link Calendar}.
     *
     * @param cal L'istanza di calendario da normalizzare.
     */
    private void resetTime(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
    }

    /**
     * Calcola un oggetto Date rappresentante l'ultimo istante della giornata indicata.
     *
     * @param date La data di riferimento.
     * @return Un'istanza {@link Date} impostata alle ore 23:59:59 del giorno fornito.
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
     * Visualizza o nasconde un velo semitrasparente (Glass Pane) sopra l'interfaccia.
     * Viene impiegato per inibire le interazioni con la finestra principale durante
     * l'apertura di dialoghi modali e per gestire la chiusura automatica al click esterno.
     *
     * @param show          Specifica se attivare o disattivare l'oscuramento.
     * @param onClickAction L'azione di callback da eseguire al click sull'overlay.
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
                    e.consume();
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
     * Sotto-classe progettata per ottimizzare la visualizzazione dei componenti
     * all'interno di un {@link JScrollPane}.
     * Forza il pannello a seguire la larghezza della viewport, prevenendo la
     * comparsa della barra di scorrimento orizzontale e favorendo il wrap dei contenuti.
     */
    private static class ScrollablePanel extends JPanel implements Scrollable {
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) { return 16; }
        @Override public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) { return 16; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }
}