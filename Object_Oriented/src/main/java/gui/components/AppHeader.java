package gui.components;

import controller.Controller;
import gui.style.GuiUtils;
import gui.frames.LogInFrame;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Componente grafico che rappresenta l'intestazione (Header) dell'applicazione.
 * Contiene il titolo, il pulsante indietro, il link per i dettagli (se applicabile),
 * lo switch del tema e il menu utente.
 */
public class AppHeader extends JPanel {

    private final String title;
    private final transient Runnable onBack;
    private final transient Runnable onReloadTheme;
    private final transient Controller controller;
    private final JFrame parentFrame;
    private final transient Runnable onOpenDetails; // Callback per aprire i dettagli

    /**
     * Costruttore completo per le schermate che richiedono funzionalità di dettaglio (es. BoardFrame).
     *
     * @param title         Il titolo iniziale.
     * @param onBack        Azione da eseguire al click del tasto indietro.
     * @param onReloadTheme Azione per ricaricare il tema.
     * @param controller    Riferimento al controller applicativo.
     * @param parentFrame   Il frame genitore (per i dialoghi).
     * @param onOpenDetails Azione da eseguire quando si clicca su "Dettagli".
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
     * Costruttore semplificato per le schermate senza dettagli (es. HomeFrame).
     *
     * @param title         Il titolo iniziale.
     * @param onBack        Azione da eseguire al click del tasto indietro.
     * @param onReloadTheme Azione per ricaricare il tema.
     * @param controller    Riferimento al controller applicativo.
     * @param parentFrame   Il frame genitore.
     */
    public AppHeader(String title, Runnable onBack, Runnable onReloadTheme, Controller controller, JFrame parentFrame) {
        this(title, onBack, onReloadTheme, controller, parentFrame, null);
    }

    /**
     * Inizializza e dispone tutti i componenti grafici dell'header.
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
     * Crea e configura il menu popup relativo al profilo dell'utente.
     * Inserisce un'intestazione non cliccabile con il nome utente e un'opzione
     * per eseguire il logout.
     *
     * @param username Il nome dell'utente attualmente autenticato da visualizzare nel menu.
     * @return L'oggetto {@code JPopupMenu} configurato con le voci di profilo e logout.
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