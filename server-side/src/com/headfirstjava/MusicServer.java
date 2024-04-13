package com.headfirstjava;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Font;
import java.awt.Color;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.Style;
import javax.swing.text.StyledDocument;
import javax.swing.text.StyleConstants;
import javax.swing.text.BadLocationException;


public class MusicServer {
	public static final Color PRETO = Color.decode("#333333");
    public static final Color ROSA = Color.decode("#C02374");
    public static final Color VERDE = Color.decode("#228B22");
    public static final Color CINZA = Color.decode("#8C8C8C");
	
	private JFrame frame;
	private JTextPane pane = new JTextPane();
	//armazena os fluxos de saída para cada cliente conectado
	private ArrayList<ObjectOutputStream> clientOutputStreams = new ArrayList<ObjectOutputStream>();
	private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
	
	
	public static void main(String[] args) {
		new MusicServer().startUp();
	}
	
	private void startUp() {
		buildGUI();
		
		try {
			//cria um fluxo de conexão de escuta da porta 4242
			ServerSocket serverSock = new ServerSocket(4242);
			addConsoleMessage(pane, "\n\t", "Super BeatBox Machine Music Server started successfully. \n", PRETO);
			addConsoleMessage(pane, "\t", "Listening on port 4242. \n", PRETO);
			addConsoleMessage(pane, "\t", "Waiting for connections. \n", PRETO);
			
			while(true) {				
				/* O método accept() permanecerá bloqueado até uma solicitação chegar. Assim que houver solicitação,
				um objeto Socket será retornado em alguma porta anônima para a comunicação com o cliente */
				Socket clientSocket = serverSock.accept();
				
				//cria um fluxo de cadeia de saída para gravar objetos
				ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
				//armazena o fluxo de saída no ArrayList
				clientOutputStreams.add(out);
				
				//cria um fluxo de entrada para este cliente; e um novo segmento para monitorá-lo constantemente
				Thread thread = new Thread(new ClientHandler(clientSocket, out));
				thread.start();
				
			}
		} catch(Exception ex) {
			ex.printStackTrace();
		}
	}

	private void buildGUI() {
		frame = new JFrame("Super BeatBox Machine | Music Server");
		frame.setExtendedState(JFrame.MAXIMIZED_BOTH); //maximiza a janela
		frame.setSize(1358, 638); //define o tamanho da janela restaurada
		frame.setLocationRelativeTo(null); //centraliza a janela restaurada
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	
		/* backgroundPanel (novo painel de conteúdo da janela) */
		JPanel backgroundPanel = new JPanel(new GridBagLayout());
		//backgroundPanel.setBackground(Color.BLACK);
		frame.setContentPane(backgroundPanel);

		pane = new JTextPane();
		pane.setEditable(false);
		Font font = pane.getFont(); //retorna a fonte atual
		pane.setFont(new Font(font.getName(), font.getStyle(), 18)); //aumenta o tamanho da fonte

		JScrollPane scrollPane = new JScrollPane(pane);
		scrollPane.setPreferredSize(new Dimension(1322, 570));
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
		gbc.gridy = 0;
		backgroundPanel.add(scrollPane, gbc);
						
		//frame.pack();			//comentado para permitir que a janela inicie maximizada
		frame.setVisible(true);	//torna a janela visível
	}
	
	private void addConsoleMessage(JTextPane pane, String position, String text, Color color) {
		text = position + sdf.format(new Date()) + " - " + text; //adiciona hora na mensagem
        StyledDocument doc = pane.getStyledDocument();

        Style style = pane.addStyle("Color Style", null); //null: o novo estilo será baseado no estilo padrão
		style.addAttribute(StyleConstants.Foreground, color); //define a cor do estilo
        try {
            doc.insertString(doc.getLength(), text, style);
        } 
        catch (BadLocationException e) {
            e.printStackTrace();
        }           
    }
		
	//envia os objetos recebidos via parâmetro para todos os clientes conectados
	private void tellEveryOne(Object one, Object two) {
		//imprime o ArrayList de fluxos de saída para verificar os clientes conecatdos (apenas para efeito de teste)
		//System.out.println("");
		//System.out.println(clientOutputStreams);
		
		Iterator<ObjectOutputStream> it = clientOutputStreams.iterator();
		
		while(it.hasNext()) {
			try {
				//obtém o fluxo de saída do cliente
				ObjectOutputStream out = it.next();
				//grava os dois objetos no fluxo de saída do cliente
				out.writeObject(one);
				out.writeObject(two);
			} catch(Exception ex) {ex.printStackTrace();}
		}
	}
		
	/* classe interna - tarefa de segmento (implementa Runnable)
		- para cada cliente conectado, cria um fluxo de entrada e monitora-o constantemente */	
	private class ClientHandler implements Runnable {
		private ObjectInputStream in;	//fluxo de cadeia de entrada

		private ObjectOutputStream out; //Utilizado para apagar o fluxo de saída do ArrayList quando o cliente for desconectado.
										//Não queremos que o método tellEveryOne mande mensagens para clientes desconectados.
		
		private String userName;
		
		//método construtor
		private ClientHandler(Socket socket, ObjectOutputStream out) {
			this.out = out;
			
			try {				
				//cria um fluxo de entrada
				in = new ObjectInputStream(socket.getInputStream());
				
				//lê o nome de usuário do fluxo de entrada do cliente
				userName = (String) in.readObject();
				
				//registra na console que a conexão foi criada
				addConsoleMessage(pane, "\n\t\t", userName + " got a connection! \n", ROSA);
				
			} catch(Exception ex) {
				ex.printStackTrace();
			}
		}
		
		//método de implementação obrigatória da interface Runnable
		public void run() {
			Object o1 = null;
			Object o2 = null;
			
			//cria um loop para monitorar constantemente o fluxo de entrada
			try {
				while ( (o1 = in.readObject() ) != null) {
					//recebe o segundo objeto (padrão de batidas)
					o2 = in.readObject();					
					//grava uma mensagem na console
					addConsoleMessage(pane, "\n\t\t", "Two objects received.", VERDE);
					
					//envia os objetos recebidos para todos os clientes conectados
					tellEveryOne(o1, o2);
					//grava uma mensagem na console
					addConsoleMessage(pane, "\n\t\t", "Two objects transmitted.\n", VERDE);
					
				} //fecha while
				
			} catch(IOException ioEx) {	//se o cliente foi desconectado			
				//registra uma mensagem na console
				addConsoleMessage(pane, "\n\t\t", userName +  " has disconnected.\n", CINZA);
				
				//apaga o fluxo de saída do cliente no ArryList					
				clientOutputStreams.remove(out);
				
			} catch(ClassNotFoundException cnfEx) {
				cnfEx.printStackTrace();
			}
			
		}
	}
	
}