package com.headfirstjava;

import java.awt.Graphics;
import java.awt.Font;
import java.awt.Color;
import java.awt.Image;
import java.awt.FontMetrics;
import javax.swing.JPanel;
import javax.swing.ImageIcon;

public class CustomDrawPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	
	//variáveis de instância referentes ao título da imagem
	private boolean showTitle = false;
	private final Font fontTitle = new Font("Dialog", Font.BOLD, 16);		
	private String stringTitle = "";
		
	//variáveis de instância referentes à imagem impressa
	private String imageFolder;
	private String imageName = "";


	//habilita/desabilita a exibição do título da figura
	public void setShowTitle(boolean b) {
		showTitle = b;
	}
	//define o título a ser exibido
	public void setStringTitle(String s){
		stringTitle = s;
	}
	//define o local da imagem
	public void setImageFolder(String folder) {
		imageFolder = folder;
	}
	//define o nome da imagem
	public void setImageName(String name) {
		imageName = name;
	}		
	
	@Override
	public void paintComponent(Graphics g) {
		//imprime as dimensões do painel de desenho 
		//System.out.println("this.getWidth(): " + this.getWidth());
		//System.out.println("this.getHeight(): " + this.getHeight());			
			
		//seleciona a cor de fundo do painel
		//g.setColor(Color.YELLOW); // amarelo - para visualizar as dimensões do painel durante testes
		g.setColor(Color.WHITE);    // branco - para combinar com a cor de fundo das imagens exibidas
		g.fillRect(0, 0, this.getWidth(), this.getHeight());
			
		/* desenha a imagem centralizada no painel */		
		if (imageName != "") { //se o nome da imagem não for vazio
			//cria um ImageIcon da imagem
			ImageIcon imageIcon = createImageIcon(imageFolder + "/" + imageName);
				
			//calcula a largura e altura da imagem a ser impressa;
			int largura = imageIcon.getIconWidth();
			int altura = imageIcon.getIconHeight();
		
			//calcula a posição do ponto x e y do canto superior esquerdo para centralizar a imagem no painel
			int x = (this.getWidth() - largura) / 2;
			int y = (this.getHeight() - altura) / 2;		

			//transforma o ImageIcon em Image para ser impresso
			Image image = imageIcon.getImage();

			//desenha a imagem
			g.drawImage(image, x, y, this);
		}
			
		/* imprime o título da imagem centralizando horizontalmente*/
		if (showTitle) { //se a exibição do título estiver ativa
			g.setColor(Color.BLACK);	//configurando a cor da fonte
			g.setFont(fontTitle);		//configurando a fonte
	
			//criando um objeto para medir o tamanho do título impresso
			FontMetrics fm = g.getFontMetrics();
			//calculando a largura do título
			int largura = fm.stringWidth(stringTitle);
			
			//imprimindo o título centralizando horizontalmente
			int margemSuperior = 35;
			g.drawString(stringTitle, ((this.getWidth() - largura) / 2), margemSuperior);
		}
	}
	
	protected static ImageIcon createImageIcon(String path) {
        java.net.URL imgURL = CustomDrawPanel.class.getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL);
        } else {
            System.err.println("Não foi possível carregar a imagem: " + path);
            return null;
        }
    }
	
}