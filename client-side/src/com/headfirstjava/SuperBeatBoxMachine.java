package com.headfirstjava;

import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Label;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JCheckBox;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.JScrollPane;
import javax.swing.Box;
import javax.swing.text.Document;
import javax.swing.DefaultListModel;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.List;
import java.util.Vector;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class SuperBeatBoxMachine {
    public static final Border OUTER_BORDER = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
    public static final Border INNER_BORDER = BorderFactory.createEmptyBorder(10,10,10,10);
    public static final Border COMPOUND_BORDER = BorderFactory.createCompoundBorder(OUTER_BORDER, INNER_BORDER);
    
    private JFrame frame;
    
    private CustomDrawPanel dynamicInstrumentDrawPanel;	

    private JPanel beatSelectionPanel;
    
    private JTextField speedTextField;	//utilizado para modificar o tempoFactor do beatSelectionPanelSequencer
    private float tempoFactor = 1.0f;	//por padrão, o tempoFactor do sequenciador é 1.0
    
    private ArrayList<JCheckBox> checkboxList;	//armazena as referências dos 256 JCheckBox do beatSelectionPanel
    
    private Sequencer labelSequencer;
    private Sequencer beatSelectionPanelSequencer;
        
    private String[] instrumentNames = {"1 - Whistle", "2 - Hand Clap", "3 - Vibraslap", "4 - Cowbell", "5 - Maracas", "6 - Crash Cymbal", "7 - Low-mid Tom", "8 - High Tom", "9 - Bass Drum", "10 - Acoustic Snare", "11 - Open Hi-Hat", "12 - Closed Hi-Hat", "13 - Low Conga", "14 - Hi Bongo", "15 - High Agogo", "16 - Open Hi Conga"};
    private String[] instrumentImages = {"000_Whistle.jpg", "001_Hand_Clap.jpg", "002_Vibraslap.jpg", "003_Cowbell.jpg", "004_Maracas.jpg", "005_Crash_Cymbal.jpg", "006_Low_mid_Tom.jpg", "007_High_Tom.jpg", "008_Bass_Drum.jpg", "009_Acoustic_Snare.jpg", "010_Open_Hi_Hat.jpg", "011_Closed_Hi_Hat.jpg", "012_Low_Conga.jpg", "013_Hi_Bongo.jpg", "014_High_Agogo.jpg", "015_Open_Hi_Conga.jpg", "016_todos_juntos_grande.jpg"};

    /* lista de códigos dos instrumentos musicais
        - em um canal MIDI normal, canal 1 por exemplo, cada número corresponde a uma nota do instrumento 
        - no canal 9, canal da percusão, cada número corresponde a um instrumento */
    private int[] instrumentCodes = {72, 39, 58, 56, 70, 49, 47, 50, 35, 38, 46, 42, 64, 60, 67, 63};
    
    private File currentDirectory;
    
    private String userName;
    
    private ObjectOutputStream out;		//fluxo de cadeia de saída
    private ObjectInputStream in;		//fluxo de cadeia de entrada
    
    private JTextField outputMessageTextField;
    
    private JList<String> incomingJList;	//exibe as mensagens recebidas via chat	
    
    private DefaultListModel<String> listModel = new DefaultListModel<String>(); //utilizado como parâmetro do construtor da JList.
    
    /* armazena o índice da célula selecionada do JList
        será utilizada pelas classes internas:
            JlistMouseListener: para saber se o usuário clicou em um item já selecionado. Se sim, a seleção será removida. 
            DeleteListener: ao excluir a célula selecionada, jListCurrentIndex volta a ser -1.
        obs: a Jlist tem duas classes ouvintes: JlistMouseListener e JListSelectionListener */
    private int jListCurrentIndex = -1;
        
    //armazena a combinação "mensagem" + "padrão de batidas" recebidas via chat 
    private HashMap<String, boolean[]> otherSeqMap = new HashMap<String, boolean[]>();
    
    
    //método construtor
    public SuperBeatBoxMachine() {		
        File dir = new File("saved-beats"); //cria uma pasta para o usuário salvar as batidas
        dir.mkdir();
        currentDirectory = dir; //atualiza o diretório corrente
    }
    
    public void startUp(String userName, String ipAddress) {
        this.userName = userName; //será utilizado pela classse interna SendListener
        
        String connectionStatus = null;

        if (isValidIPAddress(ipAddress)) {
            //abre uma conexão com o servidor
            try {
                //cria uma conexão com o que estiver sendo executado na porta 4242
                Socket sock = new Socket(ipAddress, 4242);
                
                //criando os fluxo de saída e entrada 
                out = new ObjectOutputStream(sock.getOutputStream());
                in = new ObjectInputStream(sock.getInputStream());
                
                //grava o nome do usuário no fluxo de saída do servidor
                out.writeObject(userName);
                
                //cria um novo segmento para monitorar o fluxo de entrada "in" 
                Thread remote = new Thread(new RemoteReader());
                remote.start();
                
                connectionStatus = "Connected to Server " + ipAddress;
            } catch (Exception ex) {
                System.out.println("\n\tCouldn't connect - you'll have to play alone!");
                connectionStatus = "Playing Alone";
            }
        } else { //executa caso o ip informado seja inválido
            System.out.println("\n\tCouldn't connect - you'll have to play alone!");
            connectionStatus = "Playing Alone";
        }
                    
        setUpMidi(); //configurando os objetos MIDIs		
        buildGUI(userName, connectionStatus); //construindo a GUI
    }
    
    public static boolean isValidIPAddress(String ipAddress) {
        try {
            InetAddress inetAddress = InetAddress.getByName(ipAddress);
            return inetAddress.getHostAddress().equals(ipAddress);
        } catch (UnknownHostException ex) {
            return false;
        }
    }
        
    private void buildGUI(String userName, String connectionStatus) {
        frame = new JFrame("Super BeatBox Machine | Music Client | " + connectionStatus + " | " + userName);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH); //maximiza a janela
        frame.setSize(1358, 638); //define o tamanho da janela restaurada
        frame.setLocationRelativeTo(null); //centraliza a janela restaurada
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                
        JPanel backgroundPanel = new JPanel(new GridBagLayout());
        //backgroundPanel.setBackground(Color.BLACK);
        backgroundPanel.setBorder(INNER_BORDER);	
        frame.setContentPane(backgroundPanel);
                
        addStaticInstrumentDrawPanel(backgroundPanel);
        addDynamicInstrumentDrawPanel(backgroundPanel);
        addButtonPanel(backgroundPanel);
        addInstrumentNamePanel(backgroundPanel);
        addChatPanel(backgroundPanel);
        addBeatSelectionPanel(backgroundPanel);
        
        //frame.pack(); //comentado para permitir que a janela inicie maximizada
        frame.setVisible(true);
    }
    
    private void addStaticInstrumentDrawPanel(JPanel backgroundPanel) {
        CustomDrawPanel staticInstrumentDrawPanel = new CustomDrawPanel();
        staticInstrumentDrawPanel.setBorder(OUTER_BORDER);		
        staticInstrumentDrawPanel.setShowTitle(false); //desabilita o título da imagem	 
        staticInstrumentDrawPanel.setImageFolder("images");
        staticInstrumentDrawPanel.setImageName(instrumentImages[16]); //imagem do conjunto de instrumentos numerados
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.ipadx = 378;
        gbc.ipady = 561;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridheight = 2;
        backgroundPanel.add(staticInstrumentDrawPanel, gbc);
    }
    
    private void addDynamicInstrumentDrawPanel(JPanel backgroundPanel) {
        dynamicInstrumentDrawPanel = new CustomDrawPanel();
        dynamicInstrumentDrawPanel.setBorder(OUTER_BORDER);		
        dynamicInstrumentDrawPanel.setShowTitle(true); //habilita o título  da imagem
        dynamicInstrumentDrawPanel.setStringTitle("Escolha um Instrumento!"); //define o título da imagem
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.ipadx = 378;
        gbc.ipady = 378;
        gbc.gridx = 1;
        gbc.gridy = 0;
        backgroundPanel.add(dynamicInstrumentDrawPanel, gbc);
    }
    
    private void addButtonPanel(JPanel backgroundPanel) {
        GridLayout grid = new GridLayout(0,3);
        grid.setHgap(2);
        grid.setVgap(2);	
        JPanel buttonPanel = new JPanel(grid);
        //buttonPanel.setBackground(Color.GRAY);
        buttonPanel.setBorder(COMPOUND_BORDER);		
        GridBagConstraints gbc = new GridBagConstraints();		
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        backgroundPanel.add(buttonPanel, gbc);
        
        /* botões linha 1 */
        JButton btPlay = new JButton("Play");
        btPlay.addActionListener(new PlayListener());
        buttonPanel.add(btPlay);
        
        JButton btTimeUp = new JButton("Time Up");
        btTimeUp.addActionListener(new TimeUpListener());
        buttonPanel.add(btTimeUp);
        
        JButton btSave = new JButton("Save");
        btSave.addActionListener(new SaveListener());
        buttonPanel.add(btSave);
        
        /* linha 2 */
        buttonPanel.add(Box.createRigidArea(new Dimension(0,1)));	//filler
                 
        speedTextField = new JTextField(); //controla o tempoFactor do sequenciador do painel de batidas
        speedTextField.setHorizontalAlignment(JTextField.CENTER);
        speedTextField.setText(String.format("%.2f", tempoFactor));
        speedTextField.addActionListener(new SpeedListener());
        buttonPanel.add(speedTextField);
        
        buttonPanel.add(Box.createRigidArea(new Dimension(0,1)));	//filler
        
        /* botões linha 3 */					
        JButton btStop = new JButton("Stop");
        btStop.addActionListener(new StopListener());
        buttonPanel.add(btStop);

        JButton btTimeDown = new JButton("Time Down");
        btTimeDown.addActionListener(new TimeDownListener());
        buttonPanel.add(btTimeDown);

        JButton btRestore = new JButton("Restore");
        btRestore.addActionListener(new RestoreListener());
        buttonPanel.add(btRestore);
        
        /* botão linha 4 */
        buttonPanel.add(Box.createRigidArea(new Dimension(0,1)));	//filler
        
        JButton btClear = new JButton("Clear");
        btClear.addActionListener(new ClearListener());
        buttonPanel.add(btClear);
                    
        buttonPanel.add(Box.createRigidArea(new Dimension(0,1)));	//filler
    }
    
    private void addInstrumentNamePanel(JPanel backgroundPanel) {		
        JPanel instrumentNamePanel = new JPanel();
        //instrumentNamePanel.setBackground(Color.GRAY);
        instrumentNamePanel.setLayout(new BoxLayout(instrumentNamePanel, BoxLayout.Y_AXIS)); //Y_AXIS = eixo vertical
        instrumentNamePanel.setBorder(COMPOUND_BORDER);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        gbc.fill = GridBagConstraints.VERTICAL;
        backgroundPanel.add(instrumentNamePanel, gbc);
                
        InstrumentNameListener instrumentNameListener = new InstrumentNameListener(); //cria um ouvinte MouseEvent
        Label label; //optou-se por Label pois JLabel afeta o alinhamento vertical centralizado dos elementos dentro do painel
        
        for (int i = 0; i < 16; i++) {			
            if (i < 9) 
                label = new Label("  " + instrumentNames[i]); //adiciona 2 espaços em branco antes do nome do instrumento para alinhá-lo à direita
             else 
                label = new Label(instrumentNames[i]);
            
            label.setFont(new Font("Courier New", Font.PLAIN, 12));				
            label.addMouseListener(instrumentNameListener);			
            instrumentNamePanel.add(label);
        }
    }
    
    private void addChatPanel(JPanel backgroundPanel) {
        JPanel chatPanel = new JPanel(new GridBagLayout());
        //chatPanel.setBackground(Color.GREEN);
        chatPanel.setBorder(COMPOUND_BORDER);
        GridBagConstraints gbc1 = new GridBagConstraints();
        gbc1.gridx = 2;
        gbc1.gridy = 1;
        gbc1.gridwidth = 2;
        backgroundPanel.add(chatPanel, gbc1);

        incomingJList = new JList<String>(listModel);
        //adiciona dois ouvintes
            //ouvinte de seleção - manipula eventos ListSelectionEvent (gerado apenas quando o usuário seleciona uma nova célula)
            incomingJList.addListSelectionListener(new JListSelectionListener());
            //ouvinte de mouse - manipula eventos MouseEvent (gerado inclusive quando o usuário clica em célula já selecionada) 
            incomingJList.addMouseListener(new JlistMouseListener());
        incomingJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); //restringe a seleção à um item por vez
        incomingJList.setFixedCellHeight(25); //altura da célula
        incomingJList.setVisibleRowCount(4); //quantidade de itens visíveis sem rolagem (evita erros quando a janela é minimizada)
        incomingJList.setFont(new Font("Courier New", Font.BOLD, 12));

        JScrollPane jListScrollPane = new JScrollPane(incomingJList);
        jListScrollPane.setPreferredSize(new Dimension(513, 112));
        jListScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS); //insere barra de rolagem vertical
        jListScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER); //remove barra de rolagem horizontal
        GridBagConstraints gbc2 = new GridBagConstraints();
        gbc2.gridx = 0;
        gbc2.gridy = 0;
        chatPanel.add(jListScrollPane, gbc2);

        /* outputMessagePanel */
        JPanel outputMessagePanel = new JPanel(); //painel com gerenciador de conteúdo FlowLayout
        //outputMessagePanel.setBackground(Color.ORANGE);
        GridBagConstraints gbc3 = new GridBagConstraints();
        gbc3.gridx = 0;
        gbc3.gridy = 1;
        gbc3.insets = new Insets(10, 0, 0, 0); //margem superior de 10
        chatPanel.add(outputMessagePanel, gbc3);
        
        JButton btDelete = new JButton("Delete");
        btDelete.addActionListener(new DeleteListener());
        outputMessagePanel.add(btDelete);
        
        SendListener sendListener = new SendListener(); //ouvinte do btSend e do outputMessageTextField
                
        JButton btSend = new JButton("Send");	
        btSend.addActionListener(sendListener);
        outputMessagePanel.add(btSend);
        
        /* outputMessageTextField
            - possui um limitador do número de caracteres que podem ser digitados 
            - a quantiade de caracteres permitida depende do tamanho do userName */
        int limit = 49 - userName.length();
        Document limiter = new CharacterNumberLimiter(limit);
        String text = null;
        int columns = 50 - userName.length();		
        outputMessageTextField = new JTextField(limiter, text, columns);
        outputMessageTextField.addActionListener(sendListener); //sendListener é ouvinte do btSend e do outputMessageTextField
        outputMessageTextField.setFont(new Font("Courier New", Font.PLAIN, 12));
        outputMessagePanel.add(outputMessageTextField);
    }
    
    private void addBeatSelectionPanel(JPanel backgroundPanel) {		
        GridLayout grid = new GridLayout(16,16); //16 linhas por 16 colunas
        grid.setHgap(2);
        grid.setVgap(2);
        beatSelectionPanel = new JPanel(grid);
        //beatSelectionPanel.setBackground(Color.ORANGE);
        beatSelectionPanel.setBorder(COMPOUND_BORDER);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 3;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        gbc.fill = GridBagConstraints.VERTICAL;
        backgroundPanel.add(beatSelectionPanel, gbc);
        
        //cria uma lista com as referências dos 256 JCheckBox
        checkboxList = new ArrayList<JCheckBox>();		
        
        Color cinza = new Color(206, 214, 223); //define a cor das colunas guia
        List<Integer> guideColumns = Arrays.asList(2, 3, 6, 7, 10, 11, 14, 15); //define as colunas guia
        
        for (int i = 0; i < 16; i++) { // 16 linhas
            for (int j = 0; j < 16; j++) { //16 colunas				
                JCheckBox c = new JCheckBox();			
                c.setSelected(false);
                checkboxList.add(c);
                beatSelectionPanel.add(c);				
                if (guideColumns.contains(j)) //se é coluna guia
                    c.setBackground(cinza); //troca a cor de fundo
            }
        }
    }
    
    /*configura labelSequencer e beatSelectionPanelSequencer
        - labelSequencer: reproduz uma batida quando o usuário posiciona o ponteiro do mouse sobre o nome de um instrumento
        - beatSelectionPanelSequencer: reproduz as batidas do beatSelectionPanel */
    private void setUpMidi() {
        try {			
            /* configurando labelSequencer */			
            labelSequencer = MidiSystem.getSequencer();
            labelSequencer.open();			
            labelSequencer.setTempoInBPM(120); //define o número de batidas tocadas por minuto
            
            /* configurando beatSelectionPanelSequencer */
            beatSelectionPanelSequencer = MidiSystem.getSequencer();
            beatSelectionPanelSequencer.open();			
            beatSelectionPanelSequencer.setTempoInBPM(120); //define o número de batidas tocadas por minuto
            
        }catch (Exception ex) {ex.printStackTrace();}
    }
    
    //converte o estado de cada JCheckBox do beatSelectionPanel em eventos MIDI e aperta o play
    private void buildBSPtrackAndStart() {
        try {
            Sequence sequence = new Sequence(Sequence.PPQ, 4);	//cria a sequência
            
            Track track = sequence.createTrack();				//cria a trilha
            
            /* cria os MIDI events (NOTE ON e NOTE OFF) para as JCheckBox selecionadas */
            for (int i = 0; i < 16; i++) {	//para cada um dos 16 instrumentos
                int instrumentCode = instrumentCodes[i];
                for (int contBeat = 0; contBeat < 16; contBeat++) {	//para cada uma das 16 batidas do instrumento atual (instrumentCode)
                    JCheckBox jc = checkboxList.get(contBeat + (i * 16));
                    //se a JCheckBox estiver selecionada, cria um evento NOTE ON e um evento NOTE OFF no número da batida (contBeat)
                    if (jc.isSelected()) {
                        //144 (NOTE ON); 9 (canal 10 reservado para ritmo), 100 (velocidade)  
                        track.add(makeEvent(144, 9, instrumentCode, 100, contBeat));
                        //128 (NOTE OFF), 9 (canal 10 reservado para ritmo), 100 (velocidade)
                        track.add(makeEvent(128, 9, instrumentCode, 100, contBeat+1));
                    }
                }//fecha o loop interno
            }//fecha o loop externo
            
            /* é necessário um evento na batida 16 para garantir que a BeatBox percorra as 16 batidas antes de iniciar novamente */
            //192 (código da troca de instrumento); 9 (canal reservado para ritmo); 1 (número do instrumento), 0 (velocidade), 15 (batida)
            track.add(makeEvent(192, 9, 1, 0, 15));
            
            beatSelectionPanelSequencer.setSequence(sequence);	//fornece a sequencia ao sequenciador
            beatSelectionPanelSequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);	//estabelece um loop contínuo
            beatSelectionPanelSequencer.start();	//aciona o play
        } catch (Exception ex) {ex.printStackTrace();}
    }
    
    /* método utilitário estático que cria uma mensagem e retorna um MidiEvent	
        - no canal 9, cada nota corresponde a um instrumento diferente de percusão */
    public static MidiEvent makeEvent( int cmd, int canal, int nota, int velocidade, int batida) {
        MidiEvent event = null;
        try {
            ShortMessage a = new ShortMessage();
            a.setMessage(cmd, canal, nota, velocidade);
            event = new MidiEvent(a, batida);
        } catch (Exception ex) {ex.printStackTrace();}
        return event;
    }	

    /* método changeSequence - utilizado pela classe interna JListSelectionListener (ouvinte do JList)
        - carrega no painel de seleção de batidas o padrão recebido via parâmetro */
    private void changeSequence(boolean[] checkBoxState) {
        for (int i = 0; i < 256; i++) {
            JCheckBox check = checkboxList.get(i);
            if (checkBoxState[i]) {
                check.setSelected(true);
            } else {
                check.setSelected(false);
            }
        }
    }

    /* classe interna - ouvinte dos eventos do mouse */
    private class InstrumentNameListener extends MouseAdapter{
        private final Font negrito = new Font("Courier New", Font.BOLD, 12); //BOLD = negrito
        private	final Font normal = new Font("Courier New", Font.PLAIN, 12); //PLAIN = normal (sem negrito)
        private Label labelSelected;	//variável de referência do Label selecionado
        
        /* método mouseEntered - é chamado todas as vezes que o usuário posicionar o ponteiro do mouse sobre o nome de um instrumento:
            1 - O rótulo selecionado fica azul e negrito
            2 - A imagem do instrumento correspondente ao rótulo selecionado é desenhada no dynamicInstrumentDrawPanel
            3 - Uma batida do instrumento correspondente é reproduzida para que o usuário possa ouvi-la */
        public void mouseEntered(MouseEvent e) {
            int position = -1;		//contador para percorrer os arrays instrumentNames e instrumentImages
            
            /* 1 - localiza o objeto que gerou o evento e o torna azul negrito */
            labelSelected = (Label) e.getSource();
            labelSelected.setForeground(Color.BLUE);
            labelSelected.setFont(negrito);

            /* 2 - imprime no dynamicInstrumentDrawPanel a imagem correspondente ao labelSelected */
            String instrumentName = labelSelected.getText().trim(); //trim para retirar os espaços em branco no início dos rótulos de 1 a 9
            dynamicInstrumentDrawPanel.setStringTitle(instrumentName); //para imprimir o nome do instrumento
            //percorre o array instrumentNames para descobrir a posição do nome do instrumento
            for (position = 0; position < instrumentNames.length; position++) {
                if (instrumentNames[position].equals(instrumentName)) {
                    dynamicInstrumentDrawPanel.setImageFolder("images"); //pasta que armazena as imagens
                    dynamicInstrumentDrawPanel.setImageName(instrumentImages[position]); //nome da imagem à ser impressa
                    dynamicInstrumentDrawPanel.repaint(); //chama repaint para executar paintComponent()					
                    break; //aborta a execução do for
                }
            }
                
            /* 3 - emite uma batida do instrumento para que o usuário possa ouvi-la */
            try {
                //cria uma sequencia (como se fosse o CD a ser reproduzido)
                Sequence sequence = new Sequence(Sequence.PPQ, 4);
            
                //cria uma faixa
                Track track = sequence.createTrack();
            
                /* cria o evento NOTE ON */
                //144 (NOTE ON), 9 (canal 10 reservado para ritmo), instrumentCodes[position] (equivale a nota), 100 (Velocidade), 1 (Batida)
                track.add(makeEvent(144, 9, instrumentCodes[position], 100, 1));
                
                /* cria o evento NOTE OFF */
                //128 (NOTE OFF), 9 (canal 10 reservado para ritmo), instrumentCodes[position] (equivale a nota), 100 (Velocidade), 2 (Batida)
                track.add(makeEvent(128, 9, instrumentCodes[position], 100, 2));

                labelSequencer.setSequence(sequence);	//fornece a sequencia ao sequenciador - como inserir o CD no player
                labelSequencer.start();					//aciona o play
            } catch (Exception ex) {ex.printStackTrace();}
        }//fim do método mouseEntered
        
        /* método mouseExited - executado todas as vezes que o usuário retirar o ponteiro do mouse de cima do rótulo ativo 
            1 - configura a cor da letra para preto
            2 - retira o negrito */
        public void mouseExited(MouseEvent e) {
            labelSelected.setForeground(Color.BLACK); //troca a cor da fonte para preto			
            labelSelected.setFont(normal); //retira o negrito
        }		
    }//fim da classe interna InstrumentNameListener

    //classe interna - ouvinte do btClear
    private class ClearListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            for (int i = 0; i < 256; i++) {
                JCheckBox jc = checkboxList.get(i);
                jc.setSelected(false);
            }
            beatSelectionPanelSequencer.stop();
        }
    }
    
    //classe interna - ouvinte do btPlay
    private class PlayListener implements ActionListener {
        public void actionPerformed (ActionEvent e) {
            buildBSPtrackAndStart();
        }
    }
    
    //classe interna - ouvinte do btStop
    private class StopListener implements ActionListener {
        public void actionPerformed (ActionEvent e) {
            beatSelectionPanelSequencer.stop();
        }
    }
    
    //classe interna - ouvinte do btTimeUp
    private class TimeUpListener implements ActionListener {
        public void actionPerformed (ActionEvent e) {
            //aumenta o valor da variável de instância tempoFactor em 5%
            tempoFactor = (float) (tempoFactor * 1.05);
    
            //exibe o novo valor de tempoFactor no JTextField
            speedTextField.setText(String.format("%.2f", tempoFactor));
            
            //setTempoFactor permite modificar o ritmo de uma sequência em reprodução 
            beatSelectionPanelSequencer.setTempoFactor(tempoFactor);
        }
    }
    
    //classe interna - ouvinte do speedTextField
    private class SpeedListener implements ActionListener {
        public void actionPerformed (ActionEvent e) {
            //captura o valor digitado pelo usuário substituindo qualquer vírgula por ponto
            String newSpeed = speedTextField.getText().replace(",", ".");
                        
            try {
                //se o valor digitado pelo usuário puder ser convertido em float
                tempoFactor = Float.parseFloat(newSpeed);
                
                //modifica o ritmo da sequência em reprodução 
                beatSelectionPanelSequencer.setTempoFactor(tempoFactor);
            
                //atualiza o valor da caixa de texto speedTextField
                speedTextField.setText(String.format("%.2f", tempoFactor));
                
                //retira o foco da caixa de texto
                speedTextField.transferFocusBackward();
                
            } catch (Exception ex) {
                //caso contrário, imprime na caixa de texto uma mensagem de erro
                speedTextField.setText("Invalid Value");
            }
        }
    }	
    
    //classe interna - ouvinte do btTimeDown
    private class TimeDownListener implements ActionListener {
        public void actionPerformed (ActionEvent e) {
            //diminui o valor da variável de instância tempoFactor em 5%
            tempoFactor = (float) (tempoFactor * .95);
    
            //exibe o novo valor de tempoFactor no JTextField
            speedTextField.setText(String.format("%.2f", tempoFactor));
            
            //setTempoFactor permite modificar o ritmo de uma sequência em execução 
            beatSelectionPanelSequencer.setTempoFactor(tempoFactor);
        }
    }
    
    /* classe interna - ouvinte do btSave */
    private class SaveListener implements ActionListener {
        public void actionPerformed (ActionEvent e) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(currentDirectory);
            
            int i = fileChooser.showSaveDialog(frame); //exibe para o usuário a caixa de diálogo "Save"
            
            if (i == 0) { //significa que o usuário clicou no botão Save
                File file = fileChooser.getSelectedFile(); //endereço e nome do arquivo digitado pelo usuário
                currentDirectory = file.getParentFile(); //atualiza o diretório corrente

                //cria uma matriz booleana para armazenar o estado de cada caixa de seleção
                boolean[] checkBoxState = new boolean[256];	
            
                /* percorre o checkboxList; captura o estado de cada JCheckBox; e adiciona à matriz booleana */
                for (int cont = 0; cont < 256; cont++) {
                    JCheckBox check = checkboxList.get(cont);
                    if (check.isSelected()) {
                        checkBoxState[cont] = true;
                    }
                }
                
                //salvando em disco
                try {
                    FileOutputStream fileStream = new FileOutputStream(file);
                    ObjectOutputStream objectStream = new ObjectOutputStream(fileStream);
                    objectStream.writeObject(checkBoxState);	
                } catch(IOException ex) {
                    ex.printStackTrace();					
                }
            }			
        }
    } //fecha a classe interna SaveListener

    /* classe interna - ouvinte do btRestore */
    private class RestoreListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(currentDirectory);			
            
            int i = fileChooser.showOpenDialog(frame); //exibe para o usuário a caixa de diálogo "Open"
            
            if( i == 0) { //significa que o usuário clicou no botão Open				 
                File file = fileChooser.getSelectedFile(); //endereço e nome do arquivo escolhido pelo usuário				
                currentDirectory = file.getParentFile(); //atualiza o diretório corrente	
                
                boolean[] checkBoxStatesReadFromFile = null;		
                
                
                try { //desserializando o arquivo					
                    FileInputStream fileInput = new FileInputStream(file);
                    ObjectInputStream inputStream = new ObjectInputStream(fileInput);
                    checkBoxStatesReadFromFile = (boolean[]) inputStream.readObject(); //lê o arquivo
                    
                } catch(IOException ioEx) {
                    ioEx.printStackTrace();
                } catch(ClassNotFoundException cnfEx) {
                    cnfEx.printStackTrace();
                }
                
                //restaura o estado de cada caixa de seleção no painel de seleção de batidas
                for (int j = 0; j < 256; j++) {
                    JCheckBox check = checkboxList.get(j);
                    if (checkBoxStatesReadFromFile[j]) {
                        check.setSelected(true);
                    } else {
                        check.setSelected(false);
                    }
                }
                
                beatSelectionPanelSequencer.stop(); //interrompe a reprodução atual				
                buildBSPtrackAndStart(); //reproduz o padrão carregado do arquivo
            }	
        }
    } //fim da classe interna RestoreListener

    /* classe interna - ouvinte do btSend e do outputMessageTextField 
        - grava dois objetos no fuxo de saída (para enviá-os ao servidor): 
            objeto "mensagem" 
            objeto "padrão da batidas" */	
    private class SendListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {			
            //cria uma matriz booleana com o estado de cada um dos 256 JCheckBox do painel de seleção de batidas
            boolean[] checkBoxState = new boolean[256];
              
            boolean padraoMusicalIncorporado = false;
            for (int i = 0; i < 256; i++) {
                JCheckBox check = checkboxList.get(i);
                if (check.isSelected()) {
                    checkBoxState[i] = true;
                    padraoMusicalIncorporado = true;
                }
            } //fecha o for
            
            //prepara a mensagem 				
            String beatStatus = "";	
            if (padraoMusicalIncorporado) { //adiciona [btd] em rosa				
                beatStatus = "<font style=\"color:#FF1493;\"> [btd] </font>";
                
            } else { //adiciona [btd] em cinza claro
                beatStatus = "<font style=\"color:#808080;\"> [btd] </font>";
            }
            
            //constrói a mensagem
            String[] message = new String[3];
            message[0] = "<font style=\"color:#0000FF;\">" + userName + ": </font>";
            message[1] = beatStatus;
            message[2] = outputMessageTextField.getText();
            
            //grava os objetos "mensagem" e "padrão de batidas" no fluxo de saída (envia-os ao servidor)
            try {					
                out.writeObject(message);				
                out.writeObject(checkBoxState);
                
            } catch(Exception ex) {
                System.out.println("\n\tSorry dude. Could not send it to the server!");
            }
            
            outputMessageTextField.setText(""); //limpa a caixa de texto
        }
    }
    
    /* classe interna - ouvinte do btDelete 
        - exclui a mensagem selecionada do Jlist e do HashMap */
    private class DeleteListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            //imprime a quantidade de intens do HashMap (somente em caso teste)
            //System.out.println(otherSeqMap.size());

            //recupera a mensagem selecionada
            String selected = incomingJList.getSelectedValue();
                        
            if (selected != null) { //verifica se existe uma célula selecionada				
                otherSeqMap.remove(selected); //exclui a mensagem selecionada do HashMap				
                incomingJList.clearSelection(); //cancela a seleção do Jlist				
                jListCurrentIndex = -1; //atualiza a célula atualmente selecionda				
                listModel.removeElement(selected); //apaga a mensagem selecionada do JList				
                beatSelectionPanelSequencer.stop(); //encerra a reprodução
            }
            //imprime a quantidade de intens do HashMap (somente em caso teste)
            //System.out.println(otherSeqMap.size());
        }
    }
        
    /* classe interna - tarefa de segmento
        - monitora o segmento de entrada "in" e atualiza o JList com as mensagens que chegam do servidor */
    private class RemoteReader implements Runnable {
        public void run() {
            String message = null;          //armazena o primeiro objeto lido
            boolean[] checkBoxState = null; //armazena o segundo objeto lido
            Object aux = null;              //objeto auxiliar
            
            try {
                
                //lê dados vindos do servidor 
                while ( (aux = in.readObject() ) != null) { //lê o primeiro objeto vindo do servidor				
                    String[] messageReceived = (String[]) aux; //converte objeto lido em array de Strings
                    
                    /* constrói a string que será impressa na Jlist e utilizada como chave do HashMap 
                        - indispensável acrescentar hora pois o HashMap não aceita chaves duplicadas */	
                    //cria e formata a string hora
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
                    String hora = sdf.format(new Date());
                    //montando a string message
                    message = "<html>" + messageReceived[0] + messageReceived[1] + hora + " - " + messageReceived[2] + "</html>";
                    
                    //lê o segundo objeto vindo do servidor
                    checkBoxState = (boolean[]) in.readObject();
                    
                    //adiciona ambos ao HashMap
                    otherSeqMap.put(message, checkBoxState);
                    
                    //adiciona o objeto message no listModel para ser exibido na JList
                    listModel.addElement(message);
                                        
                    //emite um aviso sonoro advertindo que uma nova mensagem chegou
                    //Toolkit.getDefaultToolkit().beep();
                    
                } //fecha while
                
            } catch(Exception ex) {ex.printStackTrace(); }
        }
    }
    
    /* classe interna - 1º ouvinte do incomingJList
        - manipula eventos ListSelectionEvent (gerado apenas quando o usuário seleciona uma nova célula)
        - carrega e reproduz o padrão de batidas incorporado à mensagem selecionada pelo usuário */
    private class JListSelectionListener implements ListSelectionListener {		
        public void valueChanged(ListSelectionEvent e) {			
            /* Durante o processo de mudança de valor, os ouvintes recebem eventos com a propriedade 
                valueIsAdjusting = true. Quando a mudança é concluída, os ouvintes recebem um evento
                com a propriedade valueIsAdjusting = false */
            if (e.getValueIsAdjusting() == false) {
                //recupera o valor selecionado pelo usuário
                String selected = incomingJList.getSelectedValue();
                //se a mensagem selecionada pelo usuário foi recuperada com sucesso
                if (selected != null) {
                    //utiliza a mensagem recuperada como chave para extrair do HashMap o padrão de batidas
                    boolean[] selectedState = otherSeqMap.get(selected);
                    
                    changeSequence(selectedState); //carrega o padrão no painel de seleção de batidas
                    
                    //interrompe o que estiver sendo reproduzido no momento
                    beatSelectionPanelSequencer.stop();
                    //reproduz o padrão recém carregado no painel de batidas
                    buildBSPtrackAndStart();
                }
            }
        }		
    }
    
    /* classe interna - 2º ouvinte do incomingJList
        - manipula eventos MouseEvent (gerado inclusive quando o usuário clica em uma célula já selecionada)
        - verifica se a célula clicada pelo usuário já estava selecionada no momento do clique 
            - se sim: 
                - cancela a seleção e para a reproduação.

        - observações:
            - vale tanto para o botão esquerdo do mouse quanto para o direito
            - essa classe é acionada todas as vezes em que o usuário clica em uma das opções do JList - sem exceção */
    private class JlistMouseListener extends MouseAdapter {
        public void mouseClicked(MouseEvent e) {
            /* verifica se ocorreu um duplo click (apenas para efeito de teste e registro de código) */
            //if (e.getClickCount() == 2) {
                //int index = incomingJList.locationToIndex(e.getPoint());
                //System.out.println("Double clicked on Item " + index);
            //}
            
            //recupera o índice da célula clicada através de MouseEvent
            int indexOpClicked = incomingJList.locationToIndex(e.getPoint());
            
            //verifica se a opção clicada já estava selecionada
            if (indexOpClicked == jListCurrentIndex) {
                incomingJList.clearSelection(); //remove a seleção
                //passa o foco para o JFrame (se não, o foco vai para o botão clear e fica esquisito)
                frame.requestFocusInWindow();				
                
                jListCurrentIndex = -1; //atualiza o índice corrente (-1 pois nada está selecionado)
                
                beatSelectionPanelSequencer.stop(); //encerra a reprodução em execução
                
            } else { //o usuário clicou em um item não selecionado			
                // atualiza o índice corrente (célula atualmente selecionada)
                if (incomingJList.isSelectedIndex(indexOpClicked)) { //verifica se a célula clicada foi devidadmente selecionada
                    jListCurrentIndex = indexOpClicked;
                }		
            }
        }
    }
    
}