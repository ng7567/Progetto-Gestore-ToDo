package gui.dialogs;

import com.formdev.flatlaf.FlatClientProperties;
import controller.Controller;
import gui.style.GuiUtils;
import gui.frames.BoardFrame;
import model.Priority;
import model.ToDo;
import com.toedter.calendar.JDateChooser;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import javax.imageio.ImageIO;
import util.FileUtils;

/**
 * Rappresenta la finestra di dialogo modale adibita alla visualizzazione e alla modifica
 * di un singolo Task.
 * Gestisce un'interfaccia che integra la manipolazione dei metadati testuali,
 * il controllo temporale delle scadenze, la gestione delle relazioni di collaborazione e
 * l'elaborazione di allegati multimediali. Implementa un sistema per il
 * monitoraggio dello stato di modifica, garantendo l'integrità dei dati tramite avvisi
 * di conferma alla chiusura in caso di variazioni non consolidate.
 *
 * @author Nunzio Grasso (Matricola: N86005509)
 * @version 1.0
 */
public class TodoDetailsDialog extends JDialog {

    /** Altezza predefinita espressa in pixel per i componenti di input standard della sidebar. */
    private static final int COMPONENT_HEIGHT = 30;

    /** Il riferimento all'entità di dominio ToDo correntemente soggetta a ispezione o modifica. */
    private final transient ToDo todo;

    /** Il riferimento al Controller per l'esecuzione delle operazioni di persistenza e business logic. */
    private final transient Controller controller;

    /** Costante testuale utilizzata come intestazione per i messaggi di errore critico. */
    private static final String ERROR = "Errore";

    // --- Monitoraggio delle modifiche ---

    /** Copia di sicurezza del titolo originale al momento dell'apertura del dialogo. */
    private final String initTitle;

    /** Copia di sicurezza della descrizione originale. */
    private final String initDescription;

    /** Copia di sicurezza dello stato di completamento originale. */
    private final boolean initCompleted;

    /** Copia di sicurezza della data di scadenza originale. */
    private final Date initExpiryDate;

    /** Copia di sicurezza del livello di priorità originale. */
    private final Priority initPriority;

    /** Copia di sicurezza del collegamento URL originale. */
    private final String initUrl;

    /** Copia di sicurezza del percorso del file immagine originale. */
    private final String initImagePath;

    /** Copia di sicurezza del codice esadecimale del colore di sfondo originale. */
    private final String initBackgroundColor;

    /** Copia di sicurezza dell'elenco dei collaboratori originale. */
    private final List<String> initCollaborators;

    // --- Componenti UI ---

    /** Il pannello principale che funge da radice per l'intera gerarchia grafica del dialogo. */
    private JPanel mainPanel;

    /** Il campo di testo per l'editing del titolo del task. */
    private JTextField txtTitle;

    /** L'area di testo multi-riga per l'editing della descrizione. */
    private JTextArea txtDescription;

    /** Il pannello di scorrimento stilizzato che incapsula l'area della descrizione. */
    private JScrollPane scrollDesc;

    /** La casella di controllo per l'aggiornamento dello stato di completamento. */
    private JCheckBox chkCompleted;

    // --- Data e Ora ---

    /** Il componente grafico a calendario per la selezione della data di scadenza. */
    private JDateChooser dateChooser;

    /** Il selettore numerico a rotazione per l'impostazione fine dell'orario di scadenza. */
    private JSpinner timeSpinner;

    /** Il menu a tendina per la selezione della priorità. */
    private JComboBox<Priority> cmbPriority;

    /** Il campo di testo per l'inserimento o la modifica dell'URL ipertestuale. */
    private JTextField txtUrl;

    /** Il pannello di anteprima cromatica che riflette il colore di sfondo scelto. */
    private JPanel colorPreviewPanel;

    /** Il valore esadecimale del colore di sfondo attualmente selezionato dall'utente. */
    private String selectedColorHex;

    // --- Gestione Immagini ---

    /** L'etichetta utilizzata per visualizzare l'anteprima scalata dell'immagine allegata. */
    private JLabel lblImagePreview;

    /** Il pulsante deputato alla rimozione logica e fisica dell'allegato immagine. */
    private JButton btnRemoveImage;

    /** Il percorso assoluto o relativo dell'immagine correntemente visualizzata nell'anteprima. */
    private String currentImagePath;

    /** Flag di controllo per inibire la chiusura accidentale del dialogo durante l'uso di selettori secondari. */
    private boolean isSubDialogActive = false;

    /** La funzione di callback da eseguire in seguito a operazioni di salvataggio o eliminazione. */
    private final transient Runnable onUpdateListener;

    /**
     * Inizializza il dialogo di dettaglio eseguendo contestualmente lo snapshot dello stato
     * attuale dell'oggetto {@link ToDo}. Configura i listener di chiusura per intercettare
     * variazioni non salvate e gestisce l'oscuramento della finestra madre.
     *
     * @param owner            La finestra proprietaria ({@link Window}) per il posizionamento e la modalit&agrave;.
     * @param controller       Il riferimento al gestore della logica di business.
     * @param todo             L'istanza del task da analizzare.
     * @param onUpdateListener La callback per notificare i cambiamenti al frame principale.
     */
    public TodoDetailsDialog(Window owner, Controller controller, ToDo todo, Runnable onUpdateListener) {
        super(owner, "Dettagli Task");
        this.controller = controller;
        this.todo = todo;
        this.onUpdateListener = onUpdateListener;

        // Inizializzazione dello stato di sicurezza per il rollback o la conferma
        this.initTitle = todo.getTitle() == null ? "" : todo.getTitle();
        this.initDescription = todo.getDescription() == null ? "" : todo.getDescription();
        this.initCompleted = todo.isCompleted();
        this.initExpiryDate = todo.getExpiryDate();
        this.initPriority = todo.getPriority();
        this.initUrl = todo.getUrlLink() == null ? "" : todo.getUrlLink();
        this.initImagePath = todo.getImagePath();
        this.initBackgroundColor = todo.getBackgroundColor();
        this.initCollaborators = new ArrayList<>(todo.getCollaborators());

        this.selectedColorHex = todo.getBackgroundColor();
        this.currentImagePath = todo.getImagePath();

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                attemptClose();
            }
        });

        initUI();

        if (owner instanceof BoardFrame boardFrame) {
            boardFrame.showOverlay(true, this::attemptClose);
        }

        setSize(750, 600);
        setLocationRelativeTo(owner);
    }

    /**
     * Controlla la chiusura del dialogo.
     * Verifica la divergenza tra lo stato attuale dei componenti UI e lo stato iniziale;
     * in caso di modifiche effettuate, richiede un'ulteriore conferma all'utente.
     */
    private void attemptClose() {
        if (isSubDialogActive) return;

        if (hasUnsavedChanges()) {
            this.toFront();
            String msg = "Hai modifiche non salvate.\nSei sicuro di voler annullare?";

            if (GuiUtils.showConfirmDialog(this, msg, "Conferma Chiusura", JOptionPane.WARNING_MESSAGE)) {
                todo.setCollaborators(new ArrayList<>(initCollaborators));
                closeDialog();
            }
        } else {
            closeDialog();
        }
    }

    /**
     * Finalizza la chiusura della finestra di dialogo.
     * Provvede alla disattivazione dell'overlay grafico sulla finestra madre prima di rilasciare le risorse.
     */
    private void closeDialog() {
        Window owner = getOwner();
        if (owner instanceof BoardFrame boardFrame) {
            boardFrame.showOverlay(false, null);
        }
        dispose();
    }

    /**
     * Esegue un controllo comparativo tra i valori correnti presenti nei componenti
     * dell'interfaccia utente e lo stato memorizzato nello snapshot iniziale.
     * Questa verifica permette di determinare se il task ha subito mutazioni
     * e di prevenire la perdita accidentale di dati durante la chiusura del dialog.
     *
     * @return {@code true} se viene rilevata una discrepanza logica in uno qualsiasi
     * dei campi monitorati; {@code false} se lo stato dell'UI coincide
     * perfettamente con i dati originali.
     */
    private boolean hasUnsavedChanges() {
        if (stringsDifferent(initTitle, txtTitle.getText())) return true;
        if (stringsDifferent(initDescription, txtDescription.getText())) return true;
        if (stringsDifferent(initUrl, txtUrl.getText())) return true;

        if (chkCompleted.isSelected() != initCompleted) return true;
        if (cmbPriority.getSelectedItem() != initPriority) return true;

        if (!Objects.equals(currentImagePath, initImagePath)) return true;
        if (!Objects.equals(selectedColorHex, initBackgroundColor)) return true;

        Date currentUiDate = getDateFromUI();
        if (areDatesEqual(initExpiryDate, currentUiDate)) return true;

        return !initCollaborators.equals(todo.getCollaborators());
    }

    /**
     * Valuta se due stringhe di testo presentano contenuti logici differenti, normalizzandole
     * tramite la rimozione degli spazi bianchi iniziali e finali.
     * Il metodo gestisce in sicurezza i valori {@code null}, trattandoli come stringhe vuote
     * per evitare eccezioni di tipo {@link NullPointerException}.
     *
     * @param s1 Il primo riferimento testuale da sottoporre a confronto.
     * @param s2 Il secondo riferimento testuale da sottoporre a confronto.
     * @return {@code true} se le stringhe, una volta normalizzate, risultano diverse;
     * {@code false} se i contenuti logici sono identici.
     */
    private boolean stringsDifferent(String s1, String s2) {
        String safe1 = s1 == null ? "" : s1.trim();
        String safe2 = s2 == null ? "" : s2.trim();
        return !safe1.equals(safe2);
    }

    /**
     * Esegue una comparazione temporale tra due oggetti {@link Date} applicando una tolleranza
     * di un secondo (1000 millisecondi).
     * Tale scostamento è necessario per ovviare alle diverse risoluzioni temporali dei
     * database rispetto alla precisione del sistema Java,
     * garantendo che discrepanze inferiori al secondo non vengano interpretate come modifiche utente.
     *
     * @param d1 Il primo riferimento temporale da confrontare.
     * @param d2 Il secondo riferimento temporale da confrontare.
     * @return {@code true} se la differenza tra le due date è pari o superiore a un secondo
     * o se solo una delle due è {@code null}; {@code false} se le date coincidono
     * entro il margine di tolleranza o sono entrambe {@code null}.
     */
    private boolean areDatesEqual(Date d1, Date d2) {
        if (d1 == null && d2 == null) return false;
        if (d1 == null || d2 == null) return true;
        long diff = Math.abs(d1.getTime() - d2.getTime());
        return diff >= 1000;
    }

    /**
     * Recupera l'unione dei valori logici impostati nei selettori di data e ora.
     *
     * @return Un oggetto {@link Date} normalizzato, o {@code null} se la data non è specificata.
     */
    private Date getDateFromUI() {
        Date date = dateChooser.getDate();
        if (date != null) {
            Date time = (Date) timeSpinner.getValue();
            return getCombinedDateTime(date, time);
        }
        return null;
    }

    /**
     * Sincronizza i campi anno/mese/giorno di una data con ore/minuti di un'altra.
     * Azzera proattivamente i millisecondi per garantire la coerenza dei confronti.
     *
     * @param date Riferimento per la data.
     * @param time Riferimento per l'orario.
     * @return L'istanza temporale consolidata.
     */
    private Date getCombinedDateTime(Date date, Date time) {
        Calendar calDate = Calendar.getInstance();
        calDate.setTime(date);
        Calendar calTime = Calendar.getInstance();
        calTime.setTime(time);
        calDate.set(Calendar.HOUR_OF_DAY, calTime.get(Calendar.HOUR_OF_DAY));
        calDate.set(Calendar.MINUTE, calTime.get(Calendar.MINUTE));
        calDate.set(Calendar.SECOND, 0);
        calDate.set(Calendar.MILLISECOND, 0);
        return calDate.getTime();
    }

    /**
     * Struttura gerarchicamente l'interfaccia utente costruendo un layout a due colonne (Main/Sidebar).
     * Alloca i controlli principali a sinistra e le opzioni di configurazione secondaria nel pannello laterale.
     */
    private void initUI() {
        mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();

        // Colonna sinistra: Contenuti principali
        JPanel leftPanel = new JPanel(new BorderLayout(0, 15));
        leftPanel.setOpaque(false);

        // Header
        JPanel titleContainer = new JPanel(new BorderLayout(10, 0));
        titleContainer.setOpaque(false);

        chkCompleted = new JCheckBox();
        chkCompleted.setSelected(todo.isCompleted());
        GuiUtils.styleCheckbox(chkCompleted);

        txtTitle = new JTextField(todo.getTitle());
        txtTitle.setFont(new Font(GuiUtils.getFontFamily(), Font.BOLD, 22));
        txtTitle.setOpaque(false);
        txtTitle.setCaretPosition(0);
        updateTitleBorder(GuiUtils.PRIMARY_COLOR);

        titleContainer.add(chkCompleted, BorderLayout.WEST);
        titleContainer.add(txtTitle, BorderLayout.CENTER);
        leftPanel.add(titleContainer, BorderLayout.NORTH);

        // Descrizione
        txtDescription = new JTextArea(todo.getDescription());
        txtDescription.setFont(new Font(GuiUtils.getFontFamily(), Font.PLAIN, 14));
        txtDescription.setLineWrap(true);
        txtDescription.setWrapStyleWord(true);
        txtDescription.setOpaque(false);

        // Aggiunge il supporto Undo/Redo
        GuiUtils.addUndoSupport(txtDescription);

        scrollDesc = GuiUtils.createModernScrollPane(txtDescription);
        scrollDesc.setOpaque(false);
        scrollDesc.getViewport().setOpaque(false);

        updateDescriptionStyle(Color.GRAY);
        scrollDesc.setPreferredSize(new Dimension(100, 100));

        leftPanel.add(scrollDesc, BorderLayout.CENTER);

        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 1.0; gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 0, 0, 20);
        mainPanel.add(leftPanel, gbc);

        // Colonna destra: Sidebar opzioni
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setBackground(new Color(255, 255, 255, 200));
        rightPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        rightPanel.putClientProperty(FlatClientProperties.STYLE, "arc: 15");

        addSidebarSection(rightPanel, "Scadenza", createDatePanel());
        rightPanel.add(Box.createVerticalStrut(15));
        addSidebarSection(rightPanel, "Priorità", createPriorityPanel());
        rightPanel.add(Box.createVerticalStrut(15));
        addSidebarSection(rightPanel, "Condivisione", createCollaboratorsPanel());
        rightPanel.add(Box.createVerticalStrut(15));
        addSidebarSection(rightPanel, "Link / URL", createUrlPanel());
        rightPanel.add(Box.createVerticalStrut(15));
        addSidebarSection(rightPanel, "Immagine", createImagePanel());
        rightPanel.add(Box.createVerticalStrut(15));
        addSidebarSection(rightPanel, "Colore Scheda", createColorPanel());

        rightPanel.add(Box.createVerticalGlue());

        JButton btnDelete = new JButton("Elimina Task");
        btnDelete.setBackground(new Color(255, 235, 235));
        btnDelete.setForeground(Color.RED);
        btnDelete.setBorderPainted(false);
        btnDelete.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        btnDelete.addActionListener(e -> deleteAction());
        rightPanel.add(btnDelete);

        JScrollPane scrollRight = GuiUtils.createModernScrollPane(rightPanel);
        scrollRight.setPreferredSize(new Dimension(320, 0));
        scrollRight.setMinimumSize(new Dimension(320, 0));
        rightPanel.setMaximumSize(new Dimension(300, Integer.MAX_VALUE));

        gbc.gridx = 1; gbc.gridy = 0;
        gbc.weightx = 0.0; gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.VERTICAL;
        mainPanel.add(scrollRight, gbc);

        // Barra inferiore: Bottoni azione
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.setOpaque(false);

        JButton btnCancel = new JButton("Annulla");
        GuiUtils.styleSecondaryButton(btnCancel);
        btnCancel.addActionListener(e -> attemptClose());

        JButton btnSave = new JButton("Salva Modifiche");
        GuiUtils.stylePrimaryButton(btnSave);
        btnSave.setPreferredSize(new Dimension(140, 40));
        btnSave.addActionListener(e -> saveAction());

        bottomPanel.add(btnCancel);
        bottomPanel.add(btnSave);

        gbc.gridx = 0; gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0; gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(15, 0, 0, 0);
        mainPanel.add(bottomPanel, gbc);

        applyBackgroundColor();
        add(mainPanel);
    }

    /**
     * Applica dinamicamente il colore di sfondo selezionato e ricalcola proattivamente
     * il contrasto per i componenti testuali onde garantire l'accessibilità visiva.
     */
    private void applyBackgroundColor() {
        Color bg;
        try {
            if (selectedColorHex != null && !selectedColorHex.isEmpty()) {
                bg = Color.decode(selectedColorHex);
            } else {
                bg = Color.WHITE;
            }
        } catch (Exception e) { bg = Color.WHITE; }

        if (mainPanel != null) {
            mainPanel.setBackground(bg);
            mainPanel.repaint();
        }

        Color textColor = GuiUtils.getContrastColor(bg);

        if (txtTitle != null) {
            txtTitle.setForeground(textColor);
            txtTitle.setCaretColor(textColor);
            updateTitleBorder(textColor);
        }

        updateDescriptionStyle(textColor);

        if (colorPreviewPanel != null) colorPreviewPanel.setBackground(bg);
    }

    /**
     * Aggiorna lo stile del modulo descrittivo (area di testo e bordi del pannello).
     *
     * @param color Il colore dinamico calcolato per il contrasto.
     */
    private void updateDescriptionStyle(Color color) {
        if (txtDescription != null) {
            txtDescription.setForeground(color);
            txtDescription.setCaretColor(color);
        }
        if (scrollDesc != null) {
            scrollDesc.setBorder(BorderFactory.createTitledBorder(
                    GuiUtils.createStandardBorder(),
                    "Descrizione",
                    TitledBorder.DEFAULT_JUSTIFICATION,
                    TitledBorder.DEFAULT_POSITION,
                    new Font(GuiUtils.getFontFamily(), Font.BOLD, 12),
                    color
            ));
        }
    }

    /**
     * Modifica l'estetica del bordo inferiore del campo titolo per riflettere il tema.
     *
     * @param borderColor Il colore da applicare alla linea di sottolineatura del titolo.
     */
    private void updateTitleBorder(Color borderColor) {
        if (txtTitle != null) {
            txtTitle.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 2, 0, borderColor),
                    new EmptyBorder(5, 5, 5, 5)
            ));
        }
    }

    /**
     * Aggrega una nuova sezione informativa alla sidebar, formattandone l'etichetta
     * e vincolando le dimensioni dei componenti interattivi.
     *
     * @param container Il pannello sidebar di destinazione.
     * @param title     Il titolo testuale della sezione.
     * @param component Il componente grafico da aggiungere.
     */
    private void addSidebarSection(JPanel container, String title, JComponent component) {
        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font(GuiUtils.getFontFamily(), Font.BOLD, 12));
        lbl.setForeground(Color.GRAY);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        container.add(lbl);
        container.add(Box.createVerticalStrut(5));
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (!(component instanceof JPanel)) {
            component.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        }
        container.add(component);
    }

    /**
     * Istanzia il pannello composito deputato alla selezione della data e dell'orario di scadenza.
     *
     * @return Il componente grafico pronto per l'integrazione sidebar.
     */
    private JComponent createDatePanel() {
        GuiUtils.DateTimePicker picker = GuiUtils.createDateTimePicker(todo.getExpiryDate());

        this.dateChooser = picker.dateChooser();
        this.timeSpinner = picker.timeSpinner();

        JPanel expandedPanel = new JPanel(new BorderLayout(10, 0));
        expandedPanel.setOpaque(false);

        this.dateChooser.setPreferredSize(null);
        this.dateChooser.setMaximumSize(new Dimension(Integer.MAX_VALUE, COMPONENT_HEIGHT));
        this.timeSpinner.setPreferredSize(new Dimension(80, COMPONENT_HEIGHT));

        expandedPanel.add(this.dateChooser, BorderLayout.CENTER);
        expandedPanel.add(this.timeSpinner, BorderLayout.EAST);
        expandedPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, COMPONENT_HEIGHT));

        return expandedPanel;
    }

    /**
     * Inizializza il selettore dropdown per la priorità, sincronizzandolo con lo stato attuale del task.
     *
     * @return L'oggetto {@link JComboBox} configurato.
     */
    private JComponent createPriorityPanel() {
        cmbPriority = new JComboBox<>(Priority.values());
        GuiUtils.stylePriorityComboBox(cmbPriority);
        cmbPriority.setSelectedItem(todo.getPriority());
        return cmbPriority;
    }

    /**
     * Istanzia il modulo di gestione degli URL, includendo un'azione di apertura nativa nel browser.
     *
     * @return Il pannello di input stilizzato.
     */
    private JComponent createUrlPanel() {
        JPanel p = new JPanel(new BorderLayout(5, 0));
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, COMPONENT_HEIGHT));

        txtUrl = new JTextField(todo.getUrlLink());
        txtUrl.setPreferredSize(new Dimension(100, COMPONENT_HEIGHT));
        txtUrl.setMinimumSize(new Dimension(50, COMPONENT_HEIGHT));

        JButton btnGo = new JButton();
        GuiUtils.styleSecondaryButton(btnGo);

        Icon icon = GuiUtils.loadSVG("icons/link.svg", 18, 18);
        if (icon != null) btnGo.setIcon(icon); else btnGo.setText("🔗");

        btnGo.setToolTipText("Apri nel browser");
        btnGo.setPreferredSize(new Dimension(40, COMPONENT_HEIGHT));

        btnGo.addActionListener(e -> {
            String url = txtUrl.getText().trim();
            if(!url.isEmpty()) GuiUtils.openWebpage(url);
            else JOptionPane.showMessageDialog(this, "Nessun link inserito.");
        });

        p.add(txtUrl, BorderLayout.CENTER);
        p.add(btnGo, BorderLayout.EAST);
        return p;
    }

    /**
     * Costruisce la sezione informativa riassuntiva relativa ai membri e alla paternità del task.
     *
     * @return Un componente grafico che riassume lo stato delle condivisioni.
     */
    private JComponent createCollaboratorsPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);

        String currentUser = controller.getCurrentUser().getUsername();
        String ownerName = todo.getOwnerUsername();
        boolean amIOwner = currentUser.equals(ownerName);

        long otherCollabsCount = todo.getCollaborators().stream()
                .filter(u -> !u.equals(currentUser))
                .count();

        String ownerLabelText = amIOwner
                ? "<html><b>Questo task è tuo</b></html>"
                : "<html>Creato da: <b>" + ownerName + "</b></html>";

        JLabel lblOwner = new JLabel(ownerLabelText);
        lblOwner.setFont(GuiUtils.FONT_NORMAL);
        lblOwner.setForeground(Color.BLACK);

        JLabel lblCount = new JLabel();
        lblCount.setFont(new Font(GuiUtils.getFontFamily(), Font.PLAIN, 12));
        lblCount.setForeground(Color.GRAY);

        if (otherCollabsCount == 0) lblCount.setText(amIOwner ? "Non condiviso" : "Nessun altro collaboratore");
        else lblCount.setText("Condiviso con " + otherCollabsCount + " altre persone");

        JPanel textPanel = new JPanel(new GridLayout(2, 1));
        textPanel.setOpaque(false);
        textPanel.add(lblOwner);
        textPanel.add(lblCount);

        JButton btnAction = new JButton();
        GuiUtils.styleSecondaryButton(btnAction);
        btnAction.setPreferredSize(new Dimension(40, COMPONENT_HEIGHT));

        Icon icon = GuiUtils.loadSVG("icons/user/user.svg", 20, 20);
        if (icon != null) btnAction.setIcon(icon); else btnAction.setText("👤");

        btnAction.addActionListener(e -> {
            isSubDialogActive = true;
            openCollaboratorManager(amIOwner);
            isSubDialogActive = false;

            long newCount = todo.getCollaborators().stream()
                    .filter(u -> !u.equals(currentUser))
                    .count();
            if (newCount == 0) lblCount.setText("Non condiviso");
            else lblCount.setText("Condiviso con " + newCount + " altre persone");
            TodoDetailsDialog.this.repaint();
        });

        p.add(textPanel, BorderLayout.CENTER);
        p.add(btnAction, BorderLayout.EAST);
        return p;
    }

    /**
     * Istanzia il modulo dedicato alla gestione degli allegati immagine, configurando
     * le anteprime scalate e i gestori per il caricamento o la rimozione.
     *
     * @return Il pannello di controllo multimediale.
     */
    private JComponent createImagePanel() {
        JPanel rootPanel = new JPanel(new BorderLayout(0, 5));
        rootPanel.setOpaque(false);

        lblImagePreview = new JLabel();
        lblImagePreview.setHorizontalAlignment(SwingConstants.CENTER);
        lblImagePreview.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        lblImagePreview.setPreferredSize(new Dimension(230, 140));

        lblImagePreview.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && currentImagePath != null) {
                    showFullSizeImage();
                }
            }
        });

        rootPanel.add(lblImagePreview, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 5, 0));
        btnPanel.setOpaque(false);

        JButton btnChange = new JButton("Cambia");
        GuiUtils.stylePrimaryButton(btnChange);
        btnChange.setPreferredSize(new Dimension(0, COMPONENT_HEIGHT));

        btnRemoveImage = new JButton("Rimuovi");
        GuiUtils.styleSecondaryButton(btnRemoveImage);
        btnRemoveImage.setForeground(Color.RED);
        btnRemoveImage.setPreferredSize(new Dimension(0, COMPONENT_HEIGHT));

        btnChange.addActionListener(e -> {
            isSubDialogActive = true;
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Immagini (JPG, PNG, GIF)", "jpg", "jpeg", "png", "gif"));
            if (fileChooser.showOpenDialog(TodoDetailsDialog.this) == JFileChooser.APPROVE_OPTION) {
                currentImagePath = fileChooser.getSelectedFile().getAbsolutePath();
                updateImagePreview();
            }
            isSubDialogActive = false;
        });

        btnRemoveImage.addActionListener(e -> {
            currentImagePath = null;
            updateImagePreview();
        });

        btnPanel.add(btnChange);
        btnPanel.add(btnRemoveImage);
        rootPanel.add(btnPanel, BorderLayout.SOUTH);

        updateImagePreview();
        return rootPanel;
    }

    /**
     * Esegue il rinfresco grafico dell'etichetta anteprima, gestendo gli stati di errore
     * o l'assenza di allegati in modo da fornire un feedback testuale coerente.
     */
    private void updateImagePreview() {
        lblImagePreview.setIcon(null);
        if (btnRemoveImage != null) btnRemoveImage.setEnabled(false);

        if (currentImagePath != null && !currentImagePath.isEmpty()) {
            boolean loaded = loadIconFromFile();
            if (!loaded) {
                lblImagePreview.setText("File non trovato");
                lblImagePreview.setForeground(Color.RED);
            }
        } else {
            lblImagePreview.setText("Nessuna immagine");
            lblImagePreview.setForeground(GuiUtils.getTextColor());
            lblImagePreview.setCursor(Cursor.getDefaultCursor());
        }

        lblImagePreview.setOpaque(true);
        lblImagePreview.setBackground(GuiUtils.getInputBackground());
        lblImagePreview.revalidate();
        lblImagePreview.repaint();
    }

    /**
     * Tenta la decodifica e il ridimensionamento smooth dell'immagine a partire
     * dal percorso memorizzato, gestendo i fallback sui percorsi relativi.
     *
     * @return {@code true} se il caricamento e la scalatura dell'icona hanno successo.
     */
    private boolean loadIconFromFile() {
        try {
            File imgFile = new File(currentImagePath);
            if (!imgFile.exists()) {
                imgFile = new File(System.getProperty("user.dir"), currentImagePath);
            }

            if (imgFile.exists()) {
                Image originalImg = ImageIO.read(imgFile);
                if (originalImg != null) {
                    Image scaledImg = originalImg.getScaledInstance(230, 140, Image.SCALE_SMOOTH);
                    lblImagePreview.setIcon(new ImageIcon(scaledImg));
                    lblImagePreview.setText("");
                    lblImagePreview.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    if (btnRemoveImage != null) btnRemoveImage.setEnabled(true);
                    return true;
                }
            }
        } catch (Exception e) {
            lblImagePreview.setText("Errore caricamento");
        }
        return false;
    }

    /**
     * Istanzia un visualizzatore modale a schermo intero per l'ispezione dell'allegato immagine
     * a risoluzione nativa, integrando il supporto allo scorrimento.
     */
    private void showFullSizeImage() {
        if (currentImagePath == null) return;
        isSubDialogActive = true;

        JDialog viewerDialog = new JDialog(this, "Visualizza Immagine", true);
        viewerDialog.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        viewerDialog.getContentPane().setBackground(Color.BLACK);

        ImageIcon fullIcon = new ImageIcon(currentImagePath);
        if (fullIcon.getIconWidth() <= 0) {
            JOptionPane.showMessageDialog(this, "Impossibile caricare immagine.");
            isSubDialogActive = false;
            return;
        }

        JLabel lblFullImage = new JLabel(fullIcon);
        lblFullImage.setHorizontalAlignment(SwingConstants.CENTER);
        JScrollPane scroll = GuiUtils.createModernScrollPane(lblFullImage);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        viewerDialog.add(scroll);

        viewerDialog.pack();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        if (viewerDialog.getWidth() > screenSize.width * 0.9 || viewerDialog.getHeight() > screenSize.height * 0.9) {
            viewerDialog.setSize((int)(screenSize.width * 0.9), (int)(screenSize.height * 0.9));
        }
        viewerDialog.setLocationRelativeTo(this);
        viewerDialog.setVisible(true);
        isSubDialogActive = false;
    }

    /**
     * Inizializza il pannello interattivo per la scelta del colore di sfondo della scheda.
     *
     * @return Il componente cliccabile che innesca il selettore di colori di sistema.
     */
    private JComponent createColorPanel() {
        colorPreviewPanel = new JPanel();
        colorPreviewPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        try {
            if (selectedColorHex != null) colorPreviewPanel.setBackground(Color.decode(selectedColorHex));
            else colorPreviewPanel.setBackground(Color.WHITE);
        } catch (Exception e) { colorPreviewPanel.setBackground(Color.WHITE); }

        colorPreviewPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        colorPreviewPanel.setPreferredSize(new Dimension(0, COMPONENT_HEIGHT));
        colorPreviewPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, COMPONENT_HEIGHT));

        colorPreviewPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                isSubDialogActive = true;
                Color newColor = JColorChooser.showDialog(TodoDetailsDialog.this, "Scegli sfondo", colorPreviewPanel.getBackground());
                isSubDialogActive = false;
                if (newColor != null) {
                    colorPreviewPanel.setBackground(newColor);
                    selectedColorHex = String.format("#%02x%02x%02x", newColor.getRed(), newColor.getGreen(), newColor.getBlue());
                    applyBackgroundColor();
                    mainPanel.repaint();
                }
            }
        });
        return colorPreviewPanel;
    }

    /**
     * Apre il modulo di gestione gerarchico per i collaboratori. Consente l'aggiunta o l'estromissione
     * di membri dalla lista di condivisione a seconda dei permessi di proprietà posseduti dall'utente.
     *
     * @param canEdit Variabile booleana che stabilisce se l'utente possiede i diritti di modifica (Ruolo OWNER).
     */
    private void openCollaboratorManager(boolean canEdit) {
        JDialog dialog = new JDialog(this, "Collaboratori", true);
        dialog.setSize(350, 450);
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(GuiUtils.getBackgroundColor());

        JPanel headerPanel = createCollaboratorsHeader(canEdit);
        dialog.add(headerPanel, BorderLayout.NORTH);

        String currentUser = controller.getCurrentUser().getUsername();
        String ownerName = todo.getOwnerUsername();

        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (String user : todo.getCollaborators()) {
            if (!user.equals(currentUser) && !user.equals(ownerName)) listModel.addElement(user);
        }

        JList<String> list = new JList<>(listModel);
        list.setBackground(GuiUtils.getInputBackground());
        list.setForeground(GuiUtils.getTextColor());
        list.setFont(GuiUtils.FONT_NORMAL);
        JScrollPane scroll = GuiUtils.createModernScrollPane(list);
        scroll.setBorder(new EmptyBorder(10, 10, 10, 10));
        scroll.setColumnHeaderView(GuiUtils.createBoldLabel("Altri membri:"));
        dialog.add(scroll, BorderLayout.CENTER);

        if (canEdit) {
            JPanel btnPanel = createCollaboratorsEditPanel(dialog, list, listModel, currentUser);
            dialog.add(btnPanel, BorderLayout.SOUTH);
        } else {
            JButton btnClose = new JButton("Chiudi");
            GuiUtils.styleSecondaryButton(btnClose);
            btnClose.addActionListener(ev -> dialog.dispose());
            JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            p.setBackground(GuiUtils.getBackgroundColor());
            p.add(btnClose);
            dialog.add(p, BorderLayout.SOUTH);
        }
        dialog.setVisible(true);
    }

    /**
     * Struttura l'intestazione informativa del gestore collaboratori evidenziando l'attuale creatore del task.
     *
     * @param canEdit Flag amministrativo.
     * @return Il pannello informativo superiore.
     */
    private JPanel createCollaboratorsHeader(boolean canEdit) {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(GuiUtils.getInputBackground());
        headerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel lblOwnerInfo = new JLabel();
        lblOwnerInfo.setFont(GuiUtils.FONT_NORMAL);
        if (canEdit) lblOwnerInfo.setText("<html>Creatore: <b>Tu</b></html>");
        else lblOwnerInfo.setText("<html>Creato da: <b>" + todo.getOwnerUsername() + "</b></html>");

        headerPanel.add(lblOwnerInfo, BorderLayout.CENTER);
        return headerPanel;
    }

    /**
     * Costruisce il pannello dei comandi per la mutazione della lista dei collaboratori (Aggiunta/Rimozione).
     *
     * @param dialog      La finestra modale di riferimento.
     * @param list        La componente visuale della JList.
     * @param listModel   Il modello dei dati manipolato.
     * @param currentUser L'username del visualizzatore corrente.
     * @return Il pannello operativo configurato.
     */
    private JPanel createCollaboratorsEditPanel(JDialog dialog, JList<String> list, DefaultListModel<String> listModel, String currentUser) {
        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        btnPanel.setBackground(GuiUtils.getBackgroundColor());
        btnPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JButton btnRemove = new JButton("Rimuovi");
        GuiUtils.styleSecondaryButton(btnRemove);
        btnRemove.setForeground(Color.RED);

        JButton btnAdd = new JButton("Aggiungi");
        GuiUtils.stylePrimaryButton(btnAdd);

        btnRemove.addActionListener(ev -> {
            String selected = list.getSelectedValue();
            if (selected == null) return;
            listModel.removeElement(selected);
            todo.getCollaborators().remove(selected);
        });

        btnAdd.addActionListener(ev -> {
            UserSearchDialog searchDialog = new UserSearchDialog(dialog, controller);
            searchDialog.setVisible(true);
            String newUser = searchDialog.getSelectedUser();
            if (newUser != null) {
                if (newUser.equals(currentUser)) JOptionPane.showMessageDialog(dialog, "Non puoi aggiungere te stesso.");
                else if (listModel.contains(newUser)) JOptionPane.showMessageDialog(dialog, "Utente già presente.");
                else {
                    listModel.addElement(newUser);
                    todo.getCollaborators().add(newUser);
                }
            }
        });

        btnPanel.add(btnRemove);
        btnPanel.add(btnAdd);
        return btnPanel;
    }

    /**
     * Coordina le fasi finali per la persistenza delle modifiche. Esegue la convalida dell'input,
     * consolida lo stato sul modello locale, gestisce la sincronizzazione dei file immagine
     * e demanda l'operazione di aggiornamento al database.
     */
    private void saveAction() {
        String newTitle = txtTitle.getText().trim();
        Timestamp expiryTs = getExpiryTimestampFromUI();

        if (!isInputValid(newTitle, expiryTs)) {
            return;
        }

        updateTodoModel(newTitle, expiryTs);
        handleImagePersistence();
        performDatabaseUpdate();
    }

    /**
     * Valuta l'integrità e la coerenza dei dati immessi rispetto ai requisiti di sistema.
     *
     * @param title    Il titolo suggerito per il task.
     * @param expiryTs L'oggetto Timestamp della scadenza.
     * @return {@code true} se i dati sono considerati validi e conformi.
     */
    private boolean isInputValid(String title, Timestamp expiryTs) {
        if (title.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Titolo obbligatorio!");
            return false;
        }
        if (title.length() > 200) {
            JOptionPane.showMessageDialog(this, "Il titolo è troppo lungo (massimo 200 caratteri).", "Errore validazione", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        if (expiryTs != null) {
            boolean isDateChanged = areDatesEqual(initExpiryDate, expiryTs);
            if (isDateChanged && expiryTs.before(new Timestamp(System.currentTimeMillis()))) {
                JOptionPane.showMessageDialog(this, "Non puoi impostare una scadenza nel passato!", "Data non valida", JOptionPane.WARNING_MESSAGE);
                return false;
            }
        }
        return true;
    }

    /**
     * Trasferisce i dati convalidati prelevati dall'interfaccia utente all'interno
     * dello stato dell'entità ToDo correntemente gestita.
     *
     * @param title    Il titolo consolidato.
     * @param expiryTs La data di scadenza consolidata.
     */
    private void updateTodoModel(String title, Timestamp expiryTs) {
        todo.setTitle(title);
        todo.setDescription(txtDescription.getText());
        todo.setCompleted(chkCompleted.isSelected());
        todo.setPriority((Priority) cmbPriority.getSelectedItem());
        todo.setUrlLink(txtUrl.getText());
        todo.setBackgroundColor(selectedColorHex);
        todo.setExpiryDate(expiryTs);
    }

    /**
     * Innesca l'interazione con il database per il salvataggio dei cambiamenti.
     * Intercetta e gestisce le eccezioni specifiche per fornire messaggi di feedback
     * mirati all'utente finale (es. errori di vincolo temporale o auto-condivisione).
     */
    private void performDatabaseUpdate() {
        try {
            if (controller.updateTodo(todo)) {
                if (onUpdateListener != null) onUpdateListener.run();
                closeDialog();
            } else {
                JOptionPane.showMessageDialog(this, "Errore durante l'aggiornamento nel database.", ERROR, JOptionPane.ERROR_MESSAGE);
            }
        } catch (exception.SelfSharingException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Warning", JOptionPane.WARNING_MESSAGE);
        } catch (SQLException ex) {
            handleSqlException(ex);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Si è verificato un errore tecnico: " + ex.getMessage(), ERROR, JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Decodifica le eccezioni SQL mappando lo stato di errore restituito dal database
     * (es. violazione dei check constraint di PostgreSQL) in avvisi descrittivi per l'utente.
     *
     * @param ex L'eccezione SQL catturata durante l'operazione di persistenza.
     */
    private void handleSqlException(SQLException ex) {
        if ("23514".equals(ex.getSQLState())) {
            JOptionPane.showMessageDialog(this, "La data di scadenza non è coerente con la creazione del task.", "Errore Validazione DB", JOptionPane.WARNING_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Errore Database: " + ex.getMessage(), ERROR, JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Gestisce la persistenza fisica e logica delle immagini.
     * Provvede all'archiviazione delle nuove risorse, alla rimozione di quelle obsolete
     * e alla pulizia del file system per prevenire l'accumulo di dati orfani.
     */
    private void handleImagePersistence() {
        String oldPath = todo.getImagePath();

        if (currentImagePath == null) {
            if (oldPath != null) {
                todo.setImagePath(null);
                FileUtils.deleteFile(oldPath);
            }
            return;
        }

        if (!currentImagePath.equals(oldPath) && !currentImagePath.contains("saved_images")) {
            String savedPath = FileUtils.saveImage(currentImagePath);
            if (savedPath != null) {
                todo.setImagePath(savedPath);
                if (oldPath != null) FileUtils.deleteFile(oldPath);
            } else {
                JOptionPane.showMessageDialog(this, "Errore salvataggio immagine");
            }
        } else {
            todo.setImagePath(currentImagePath);
        }
    }

    /**
     * Estrae la combinazione di data e ora dai componenti UI restituendola sotto forma di Timestamp SQL.
     *
     * @return L'oggetto {@link Timestamp} normalizzato, o {@code null} se i campi sono vacanti.
     */
    private java.sql.Timestamp getExpiryTimestampFromUI() {
        Date date = dateChooser.getDate();
        if (date != null) {
            Date time = (Date) timeSpinner.getValue();
            return new java.sql.Timestamp(getCombinedDateTime(date, time).getTime());
        }
        return null;
    }

    /**
     * Innesca la procedura di cancellazione fisica del task.
     * Richiede un'ulteriore conferma di sicurezza data l'irreversibilità dell'operazione.
     */
    private void deleteAction() {
        String msg = "Vuoi eliminare definitivamente questo task?";

        if (GuiUtils.showConfirmDialog(this, msg, "Conferma Eliminazione", JOptionPane.WARNING_MESSAGE)) {
            if (controller.deleteTodo(todo.getId())) {
                if (onUpdateListener != null) onUpdateListener.run();
                closeDialog();
            } else {
                JOptionPane.showMessageDialog(this, "Errore durante l'eliminazione.", ERROR, JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}