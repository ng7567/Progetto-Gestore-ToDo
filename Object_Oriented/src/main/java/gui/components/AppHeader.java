package gui.components;

import controller.Controller;
import gui.style.GuiUtils;
import gui.frames.LogInFrame;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Rappresenta il componente grafico dell'intestazione (Header) dell'applicazione.
 * Costituisce la barra di navigazione superiore, ospitando il titolo della vista corrente,
 * i controlli di navigazione (tasto indietro), l'interruttore per il tema visivo e
 * il menu contestuale legato al profilo dell'utente autenticato.
 *
 * @author Nunzio Grasso (Matricola: N86005509)
 * @version 1.0
 */
public class AppHeader extends JPanel {

    /** Il titolo testuale visualizzato al centro dell'intestazione. */
    private final String title;

    /** L'azione di callback da eseguire alla pressione del pulsante di ritorno. */
    private final transient Runnable onBack;

    /** L'azione di callback per ricaricare dinamicamente il tema grafico (Light/Dark mode). */
    private final transient Runnable onReloadTheme;

    /** Il riferimento al Controller principale per la gestione della logica di business (es. logout). */
    private final transient Controller controller;

    /** Il riferimento al frame genitore, necessario per l'ancoraggio e il posizionamento dei dialoghi. */
    private final JFrame parentFrame;

    /** L'azione di callback opzionale invocata al click sul collegamento dei dettagli. */
    private final transient Runnable onOpenDetails;

    /**
     * Inizializza l'intestazione completa, includendo il collegamento interattivo per i dettagli.
     * Utilizzato tipicamente nelle schermate che richiedono opzioni aggiuntive (come la vista della singola bacheca).
     *
     * @param title         Il titolo testuale della schermata corrente.
     * @param onBack        La funzione di callback per la navigazione a ritroso (può essere {@code null} se disabilitata).
     * @param onReloadTheme La funzione di callback per l'aggiornamento del tema visivo.
     * @param controller    Il riferimento al gestore della logica di business.
     * @param parentFrame   Il frame grafico contenitore, utile per ancorare i popup.
     * @param onOpenDetails La funzione di callback attivata dalla pressione del pulsante "Dettagli bacheca".
     */
    public AppHeader(String title, Runnable onBack, Runnable onReloadTheme, Controller controller, JFrame parentFrame, Runnable onOpenDetails) {
        this.title = title;
        this.onBack = onBack;
        this.onReloadTheme = onReloadTheme;
        this.controller = controller;
        this.parentFrame = parentFrame;
        this.onOpenDetails = onOpenDetails;

        initUI();
    }

    /**
     * Inizializza l'intestazione in modalità semplificata, omettendo il pulsante dei dettagli.
     * Utilizzato nelle schermate di navigazione principale (come la Home) in cui non sono richieste azioni secondarie.
     *
     * @param title         Il titolo testuale della schermata corrente.
     * @param onBack        La funzione di callback per la navigazione a ritroso.
     * @param onReloadTheme La funzione di callback per l'aggiornamento del tema visivo.
     * @param controller    Il riferimento al gestore della logica di business.
     * @param parentFrame   Il frame grafico contenitore.
     */
    public AppHeader(String title, Runnable onBack, Runnable onReloadTheme, Controller controller, JFrame parentFrame) {
        this(title, onBack, onReloadTheme, controller, parentFrame, null);
    }

    /**
     * Struttura e posiziona tutti i componenti grafici all'interno dell'intestazione.
     * Applica un layout a griglia ({@link GridBagLayout}) suddividendo lo spazio in tre sezioni logiche:
     * area di navigazione (sinistra), area informativa (centro) e area utente (destra).
     */
    private void initUI() {
        setLayout(new GridBagLayout());
        setOpaque(true);
        setBackground(GuiUtils.HEADER_COLOR);
        setBorder(new EmptyBorder(10, 20, 10, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.insets = new Insets(0, 0, 0, 0);

        // --- 1. SINISTRA (Tasto Indietro) ---
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftPanel.setOpaque(false);

        if (onBack != null) {
            JButton btnBack = new JButton("<html><font size='5'>&#8592;</font></html>");
            btnBack.setForeground(Color.WHITE);
            btnBack.setContentAreaFilled(false);
            btnBack.setBorderPainted(false);
            btnBack.setFocusPainted(false);
            btnBack.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnBack.addActionListener(e -> onBack.run());
            leftPanel.add(btnBack);
        }

        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.WEST;
        add(leftPanel, gbc);

        // --- 2. CENTRO (Titolo e Dettagli) ---
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);

        // Inizializzazione del campo di classe
        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(GuiUtils.FONT_TITLE);
        lblTitle.setForeground(Color.WHITE);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(lblTitle);

        // Aggiunge il link "Dettagli" solo se la callback è stata fornita
        if (onOpenDetails != null) {
            JButton btnDetails = new JButton("Dettagli bacheca");
            btnDetails.setFont(new Font(GuiUtils.getFontFamily(), Font.PLAIN, 12));
            btnDetails.setForeground(new Color(200, 220, 255)); // Azzurrino per sembrare un link
            btnDetails.setContentAreaFilled(false);
            btnDetails.setBorderPainted(false);
            btnDetails.setFocusPainted(false);
            btnDetails.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnDetails.setAlignmentX(Component.CENTER_ALIGNMENT);

            // Effetto hover
            btnDetails.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    btnDetails.setForeground(Color.WHITE);
                }
                @Override
                public void mouseExited(java.awt.event.MouseEvent evt) {
                    btnDetails.setForeground(new Color(200, 220, 255));
                }
            });

            btnDetails.addActionListener(e -> onOpenDetails.run());
            centerPanel.add(btnDetails);
        }

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 15, 0, 15);
        add(centerPanel, gbc);

        // --- 3. DESTRA (Tema + Avatar) ---
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        rightPanel.setOpaque(false);

        ThemeToggleButton themeSwitch = new ThemeToggleButton(onReloadTheme);
        rightPanel.add(themeSwitch);

        String username = (controller != null && controller.getCurrentUser() != null)
                ? controller.getCurrentUser().getUsername() : "Guest";

        JPopupMenu popupMenu = createProfilePopupMenu(username);

        JButton btnAvatar = new JButton();
        btnAvatar.setIcon(new AvatarIcon(username, 34));
        btnAvatar.setContentAreaFilled(false);
        btnAvatar.setBorderPainted(false);
        btnAvatar.setFocusPainted(false);
        btnAvatar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnAvatar.addActionListener(ev -> popupMenu.show(btnAvatar, 0, btnAvatar.getHeight()));

        rightPanel.add(btnAvatar);

        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        add(rightPanel, gbc);
    }

    /**
     * Costruisce e configura il menu a tendina contestuale per l'avatar dell'utente.
     * Inserisce un'intestazione informativa non cliccabile seguita dall'azione di disconnessione (Logout),
     * gestendo il ciclo di vita della sessione e il reindirizzamento alla schermata di accesso.
     *
     * @param username Il nome dell'utente attualmente autenticato da mostrare come intestazione del menu.
     * @return L'oggetto {@link JPopupMenu} configurato e pronto per essere visualizzato.
     */
    private JPopupMenu createProfilePopupMenu(String username) {
        JPopupMenu popupMenu = new JPopupMenu();

        // Voce informativa che mostra il nome utente, disabilitata per fungere da etichetta
        JMenuItem itemUser = new JMenuItem("Profilo (" + username + ")");
        itemUser.setEnabled(false);

        // Voce per il logout con gestione della conferma e ritorno alla schermata di accesso
        JMenuItem itemLogout = new JMenuItem("Logout");
        itemLogout.addActionListener(e -> {
            if (GuiUtils.showConfirmDialog(parentFrame, "Vuoi uscire dall'applicazione?", "Logout", JOptionPane.QUESTION_MESSAGE)) {
                controller.logout();
                parentFrame.dispose();
                new LogInFrame(controller).setVisible(true);
            }
        });

        popupMenu.add(itemUser);
        popupMenu.addSeparator();
        popupMenu.add(itemLogout);
        return popupMenu;
    }
}