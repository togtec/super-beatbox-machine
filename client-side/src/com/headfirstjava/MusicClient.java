package com.headfirstjava;

import java.awt.Container;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.BorderFactory;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.text.Document;


public class MusicClient {
    private JFrame frame;
    private JTextField usernameTextField;
    private JTextField ipAddressTextField;

    public static void main (String[] args) {
        new MusicClient().startUp();
    }

    private void startUp() {
        buildGUI();
    }
	
    private void buildGUI() {
        frame = new JFrame("Super BeatBox Machine | Music Client");
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH); //maximiza a janela		
        frame.setSize(1358, 638); //define o tamanho da janela restaurada
        frame.setLocationRelativeTo(null); //centraliza a janela restaurada
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        Border outerBorder = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
        Border innerBorder = BorderFactory.createEmptyBorder(10,10,10,10);
        Border compoundBorder = BorderFactory.createCompoundBorder(outerBorder, innerBorder);
        
        /* backgroundPanel (novo painel de conteúdo da janela) */
        JPanel backgroundPanel = new JPanel(new GridBagLayout());
        //backgroundPanel.setBackground(Color.BLACK);
        frame.setContentPane(backgroundPanel);

        /* formPanel */
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(outerBorder);
        //formPanel.setBackground(Color.YELLOW);
        Dimension dimension = new Dimension(1322, 570);
        formPanel.setPreferredSize(dimension);
        GridBagConstraints gbcFormPanel = new GridBagConstraints();
        gbcFormPanel.gridx = 0;
        gbcFormPanel.gridy = 0;
        backgroundPanel.add(formPanel, gbcFormPanel);

        JLabel usernameLabel = new JLabel("Username:");
        GridBagConstraints gbc1 = new GridBagConstraints();
        gbc1.gridx = 0;
        gbc1.gridy = 0;
        gbc1.anchor = GridBagConstraints.LINE_START;
        formPanel.add(usernameLabel, gbc1);

        int limit = 25;
        Document limiter = new CharacterNumberLimiter(limit);
        String text = null;
        int columns = 20; //the number of columns to use to calculate the preferred width
        usernameTextField = new JTextField(limiter, text, columns);
        GridBagConstraints gbc2 = new GridBagConstraints();
        gbc2.gridx = 0;
        gbc2.gridy = 1;
        gbc2.insets = new Insets(5, 0, 0, 0);
        formPanel.add(usernameTextField, gbc2);

        JLabel ipAddressLabel = new JLabel("Server IP Address:");
        GridBagConstraints gbc3 = new GridBagConstraints();
        gbc3.gridx = 0;
        gbc3.gridy = 2;
        gbc3.anchor = GridBagConstraints.LINE_START;
        gbc3.insets = new Insets(10, 0, 0, 0);
        formPanel.add(ipAddressLabel, gbc3);

        ipAddressTextField = new JTextField("127.0.0.1", 20);
        GridBagConstraints gbc4 = new GridBagConstraints();
        gbc4.gridx = 0;
        gbc4.gridy = 3;
        gbc4.insets = new Insets(5, 0, 0, 0);
        formPanel.add(ipAddressTextField, gbc4);

        JButton connectButton = new JButton("Connect");
        connectButton.addActionListener(new ConnectListener());
        GridBagConstraints gbc5 = new GridBagConstraints();
        gbc5.gridx = 0;
        gbc5.gridy = 4;
        gbc5.insets = new Insets(20, 0, 0, 0);
        formPanel.add(connectButton, gbc5);

        /* mapear a tecla Enter para o evento do botão */
        KeyStroke enterKey = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0); //tecla ENTER
        //O valor 0 indica que nenhum modificador de tecla será utilizado (Ctrl, Shift, Alt, etc)

        InputMap inputMap = connectButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        //JComponent.WHEN_IN_FOCUSED_WINDOW -> o foco pode estar em qualquer componente da Janela
        //JComponent.WHEN_FOCUSED -> o usuário terá que utilizar o tab para colocar o foco no botão
        inputMap.put(enterKey, "pressEnterForButtonAction");

        ActionMap actionMap = connectButton.getActionMap();
        actionMap.put("pressEnterForButtonAction", new ButtonClickListener(connectButton));
        /* fim mapear a tecla Enter para o evento do botão */

        //frame.pack(); //comentado para permitir que a janela inicie maximizada
        frame.setVisible(true);	//torna a janela visível
    }

    //classe interna - ouvinte do connectButton
    private class ConnectListener implements ActionListener {
        public void actionPerformed (ActionEvent e) {
            String userName = usernameTextField.getText();
            if (userName == null || userName.equals("")) {
                userName = "User Unknown";
            }

            new SuperBeatBoxMachine().startUp(userName, ipAddressTextField.getText());
            frame.dispose(); //fecha a janela atua			
        }
    }

    /**
    * Classe interna - estende AbstractAction para simular um clique no botão.
    * Utilizada para mapear a tecla "Enter" ao evento de clique do botão.
    */
    private class ButtonClickListener extends AbstractAction {
        private static final long serialVersionUID = 1L;

        private JButton buttonToClick;

        //construtor
        public ButtonClickListener(JButton button) {
            this.buttonToClick = button;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            buttonToClick.doClick();
        }
    }

}