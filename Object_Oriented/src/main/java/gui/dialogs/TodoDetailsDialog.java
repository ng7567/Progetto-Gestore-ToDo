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
 * Finestra di dialogo per la visualizzazione e la modifica dettagliata di un Task.
 * <p>
 * Questa classe gestisce un form complesso che include:
 * <ul>
 * <li>Titolo e Descrizione modificabili.</li>
 * <li>Stato di completamento.</li>
 * <li>Scadenza (Data e Ora).</li>
 * <li>Priorità e Link URL.</li>
 * <li>Gestione collaboratori (Aggiunta/Rimozione).</li>
 * <li>Gestione allegati immagine (Preview e Caricamento).</li>
 * <li>Personalizzazione del colore della scheda.</li>
 * </ul>
 * Implementa anche un sistema di "Snapshot" per rilevare modifiche non salvate alla chiusura.
 */
public class TodoDetailsDialog extends JDialog {

    private static final int COMPONENT_HEIGHT = 30;

    private final transient ToDo todo;
    private final transient Controller controller;

    private static final String ERROR = "Errore";

    // Snapshot per rilevare modifiche
    private final String initTitle;
    private final String initDescription;
    private final boolean initCompleted;
    private final Date initExpiryDate;
    private final Priority initPriority;
    private final String initUrl;
    private final String initImagePath;
    private final String initBackgroundColor;
    private final List<String> initCollaborators;

    // Componenti UI
    private JPanel mainPanel;
    private JTextField txtTitle;
    private JTextArea txtDescription;
    private JScrollPane scrollDesc;
    private JCheckBox chkCompleted;

    // Data e Ora
    private JDateChooser dateChooser;
    private JSpinner timeSpinner;

    private JComboBox<Priority> cmbPriority;
    private JTextField txtUrl;
    private JPanel colorPreviewPanel;
    private String selectedColorHex;

    // Gestione Immagini
    private JLabel lblImagePreview;
    private JButton btnRemoveImage;
    private String currentImagePath;

    // Flag per finestre secondarie
    private boolean isSubDialogActive = false;

    // Callback
    private final transient Runnable onUpdateListener;

    /**
     * Costruisce il dialogo di dettaglio per un task specifico.
     * Salva immediatamente uno snapshot dello stato attuale del {@link ToDo} per gestire
     * il controllo delle modifiche non salvate al momento della chiusura.
     *
     * @param owner            La finestra proprietaria (utilizzata per centrare il dialogo).
     * @param controller       Il controller per l'interazione con il database.
     * @param todo             L'oggetto {@link ToDo} da visualizzare e modificare.
     * @param onUpdateListener La callback eseguita in caso di salvataggio o eliminazione avvenuti con successo.
     */
    public TodoDetailsDialog(Window owner, Controller controller, ToDo todo, Runnable onUpdateListener) {
        super(owner, "Dettagli Task");
        this.controller = controller;
        this.todo = todo;
        this.onUpdateListener = onUpdateListener;

        // Salvataggio stato iniziale
        this.initTitle = todo.getTitle() == null ? "" : todo.getTitle();
        this.initDescription = todo.getDescription() == null ? "" : todo.getDescription();
        this.initCompleted = todo.isCompleted();
        this.initExpiryDate = todo.getExpiryDate();
        this.initPriority = todo.getPriority();
        this.initUrl = todo.getUrlLink() == null ? "" : todo.getUrlLink();
        this.initImagePath = todo.getImagePath();
        this.initBackgroundColor = todo.getBackgroundColor();
        this.initCollaborators = new ArrayList<>(todo.getCollaborators());

        // Stato corrente modificabile
        this.selectedColorHex = todo.getBackgroundColor();
        this.currentImagePath = todo.getImagePath();

        // Configurazione chiusura finestra
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                attemptClose();
            }
        });

        initUI();

        // Gestione Overlay
        if (owner instanceof BoardFrame boardFrame) {
            boardFrame.showOverlay(true, this::attemptClose);
        }

        setSize(750, 600);
        setLocationRelativeTo(owner);
    }

    /**
     * Tenta la chiusura della finestra di dialogo.
     * Verifica la presenza di modifiche non salvate tramite lo snapshot e, in caso affermativo,
     * richiede una conferma esplicita all'utente prima di scartarle.
     */
    private void attemptClose() {
        if (isSubDialogActive) return;

        if (hasUnsavedChanges()) {
            this.toFront();
            String msg = "Hai modifiche non salvate.\nSei sicuro di voler annullare?";

            if (GuiUtils.showConfirmDialog(this, msg, "Conferma Chiusura", JOptionPane.WARNING_MESSAGE)) {
                // Ripristina i collaboratori nel caso siano stati modificati live nella lista
                todo.setCollaborators(new ArrayList<>(initCollaborators));
                closeDialog();
            }
        } else {
            closeDialog();
        }
    }

    /**
     * Chiude definitivamente il dialogo corrente.
     * Rimuove l'overlay di oscuramento dalla finestra madre, se presente, e distrugge le risorse grafiche.
     */
    private void closeDialog() {
        Window owner = getOwner();
        if (owner instanceof BoardFrame boardFrame) {
            boardFrame.showOverlay(false, null);
        }
        dispose();
    }

    /**
     * Confronta i valori attualmente inseriti nei componenti UI con lo snapshot iniziale.
     *
     * @return {@code true} se rileva una qualsiasi modifica ai campi o allo stato, {@code false} altrimenti.
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
     * Verifica se due stringhe di testo differiscono, ignorando gli spazi bianchi laterali e i valori nulli.
     *
     * @param s1 La prima stringa da confrontare.
     * @param s2 La seconda stringa da confrontare.
     * @return {@code true} se le stringhe contengono un valore logico differente.
     */
    private boolean stringsDifferent(String s1, String s2) {
        String safe1 = s1 == null ? "" : s1.trim();
        String safe2 = s2 == null ? "" : s2.trim();
        return !safe1.equals(safe2);
    }

    /**
     * Verifica se due oggetti Date differiscono di almeno un secondo logico.
     *
     * @param d1 La prima data.
     * @param d2 La seconda data.
     * @return {@code true} se le date sono diverse in maniera significativa.
     */
    private boolean areDatesEqual(Date d1, Date d2) {
        if (d1 == null && d2 == null) return false;
        if (d1 == null || d2 == null) return true;
        long diff = Math.abs(d1.getTime() - d2.getTime());
        return diff >= 1000;
    }

    /**
     * Estrae e combina i valori di data e ora dagli appositi componenti dell'interfaccia utente.
     *
     * @return L'oggetto {@link Date} completo combinato, o {@code null} se l'utente non ha impostato alcuna data.
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
     * Unisce la componente di data di un oggetto Date con la componente oraria di un altro.
     * Azzera preventivamente i secondi e i millisecondi per evitare asimmetrie nel database.
     *
     * @param date La data sorgente (giorno/mese/anno).
     * @param time Il riferimento orario sorgente (ore/minuti).
     * @return L'istanza combinata risultante.
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
     * Inizializza l'interfaccia utente costruendo il layout strutturato a due colonne.
     * Posiziona i campi testuali principali a sinistra e i pannelli delle opzioni aggiuntive a destra.
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
     * Applica il colore di sfondo selezionato all'intero pannello.
     * Calcola dinamicamente il colore in contrasto per aggiornare la tinta dei testi
     * e garantire sempre un'ottima leggibilità.
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
     * Aggiorna lo stile cromatico del componente di testo dedicato alla descrizione.
     *
     * @param color Il colore dinamico da applicare al font e ai bordi.
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
     * Sostituisce il bordo inferiore del campo titolo con il colore specificato.
     *
     * @param borderColor Il colore da assegnare alla linea inferiore del campo testuale.
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
     * Aggiunge una nuova sezione tematica alla sidebar laterale del dialogo.
     * Fornisce un'etichetta di formattazione standard al componente passato come parametro.
     *
     * @param container Il contenitore genitore in cui inserire la sezione.
     * @param title     Il titolo descrittivo della sezione.
     * @param component Il componente interattivo da abbinare al titolo.
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
     * Configura il pannello orizzontale per la selezione della data e dell'orario di scadenza.
     *
     * @return Il {@link JComponent} composito pronto per l'inserimento nell'interfaccia.
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
     * Crea il selettore a discesa per l'impostazione della priorità del task.
     *
     * @return Il componente {@link JComboBox} stilizzato e preimpostato.
     */
    private JComponent createPriorityPanel() {
        cmbPriority = new JComboBox<>(Priority.values());
        GuiUtils.stylePriorityComboBox(cmbPriority);
        cmbPriority.setSelectedItem(todo.getPriority());
        return cmbPriority;
    }

    /**
     * Crea il pannello per la gestione degli URL allegati, corredato di pulsante di reindirizzamento al browser.
     *
     * @return Il pannello configurato contenente campo testuale e pulsante azione.
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
     * Crea la sezione riassuntiva per mostrare lo stato di condivisione del task attuale.
     *
     * @return Il pannello grafico aggiornato in base ai permessi dell'utente corrente.
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
     * Crea il riquadro destinato al caricamento, alla rimozione e alla visualizzazione dell'anteprima dell'immagine.
     *
     * @return Il pannello di controllo completo per gli allegati multimediali.
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
     * Aggiorna graficamente il contenitore dell'anteprima dell'immagine nella sidebar.
     * Attiva o disattiva i controlli correlati in base all'effettiva presenza di un file.
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
     * Tenta di localizzare, leggere e ridimensionare il file immagine associato al percorso corrente.
     *
     * @return {@code true} se l'operazione di decodifica va a buon fine, {@code false} in caso di disco non raggiungibile o file corrotto.
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
     * Istanzia una nuova finestra di dialogo a schermo intero dedicata alla visualizzazione dell'immagine.
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
     * Crea il pulsante di selezione visiva per la scelta del colore di sfondo.
     *
     * @return Il pannello interattivo che lancia il color chooser di sistema.
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
     * Apre il modulo di gestione dedicato per consultare ed eventualmente aggiungere/rimuovere i collaboratori.
     *
     * @param canEdit Flag che stabilisce se l'utente corrente possiede i diritti amministrativi sul task.
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
     * Compone l'intestazione testuale indicando la paternità del task nel gestore dei collaboratori.
     *
     * @param canEdit Flag amministrativo derivato dal proprietario della risorsa.
     * @return Il pannello destinato al titolo di appartenenza.
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
     * Costruisce il pannello dei comandi per aggiungere ed estromettere gli utenti dalla lista.
     *
     * @param dialog      Il dialogo padre che gestisce la ricerca.
     * @param list        La componente visuale della JList.
     * @param listModel   Il modello di dati su cui operare.
     * @param currentUser L'username di colui che compie l'azione.
     * @return Il pannello coi tasti azione (Aggiungi/Rimuovi).
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
     * Coordina le fasi necessarie al salvataggio definitivo delle modifiche al task.
     * Attiva le funzioni di convalida per prevenire corruzioni, consolida lo stato sul modello
     * locale e delega l'invio finale della transazione logica al database.
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
     * Ispeziona i valori di input principali per stabilirne l'idoneità rispetto alle regole.
     * Garantisce che il titolo inserito rispetti i limiti di lunghezza del database (varchar)
     * e valuta la coerenza temporale dell'eventuale scadenza, interrompendo precocemente il flusso in caso di scarto.
     *
     * @param title    Il nuovo testo suggerito come titolo.
     * @param expiryTs La scadenza calcolata in oggetto Timestamp.
     * @return {@code true} se non vengono intercettati difetti formali, {@code false} se si evidenziano violazioni logiche.
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
     * Scrive i dati convalidati prelevati dai controlli dell'interfaccia utente nei campi interni del modello ToDo.
     * Allinea integralmente lo stato logico della classe con le istruzioni espresse dall'utente prima dell'archiviazione.
     *
     * @param title    Il titolo consolidato.
     * @param expiryTs La scadenza calcolata consolidata.
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
     * Esegue il dialogo a livello applicativo con la risorsa database tramite il controller.
     * Implementa i meccanismi predefiniti di intercettazione per gestire malfunzionamenti di tipo strutturale, relazionale (vincoli violati)
     * ed eccezioni derivate dalle logiche restrittive in vigore per il sistema (es. tentata auto-condivisione).
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
     * Processa le eccezioni originate dalla comunicazione col database PostgreSQL tradotte dal controller.
     * Decodifica il livello d'errore mappandolo allo stato SQL restituito per garantire una restituzione a video human-readable del problema originario.
     *
     * @param ex L'oggetto eccezione contenente il payload informativo prodotto dai driver JDBC.
     */
    private void handleSqlException(SQLException ex) {
        if ("23514".equals(ex.getSQLState())) {
            JOptionPane.showMessageDialog(this, "La data di scadenza non è coerente con la creazione del task.", "Errore Validazione DB", JOptionPane.WARNING_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Errore Database: " + ex.getMessage(), ERROR, JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Gestisce la logica pre-persistenza per le immagini allegate al task.
     * Valuta l'esigenza di copiare i nuovi file nella cartella protetta del progetto
     * ed esegue il processo di dismissione ed eliminazione logico-fisica per le risorse oramai de-sincronizzate o respinte in toto.
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
     * Converte i dati immessi selettivamente nei controlli UI designati (Data e Ora) all'interno di un univoco oggetto {@link Timestamp}.
     *
     * @return Una strutturazione del timestamp formalmente aderente allo standard SQL preteso dal database, oppure {@code null} per task senza scadenza designata.
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
     * Avvia una richiesta d'eliminazione totale ed irreversibile per il task in gestione.
     * Chiede conferma all'utente prima di avviare l'azione nel database.
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