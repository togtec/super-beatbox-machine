package com.headfirstjava;

import javax.swing.text.PlainDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;

public class CharacterNumberLimiter extends PlainDocument {
	private static final long serialVersionUID = 1L;

    private int iMaxLength;
	
	//método construtor
    public CharacterNumberLimiter(int maxlen) {
        super();
        iMaxLength = maxlen;
    }
	
    @Override
    public void insertString(int offset, String str, AttributeSet attr) throws BadLocationException {
		
		//O parâmetro str pode ter qualquer tamanho se iMaxLength <= 0
        if (iMaxLength <= 0) {  
			super.insertString(offset, str.toString(), attr);
            return;
        }

		
        int newDocumentSize = (getLength() + str.length());
        if (newDocumentSize <= iMaxLength) //o novo tamanho do documento não pode ultrapassar iMaxLength
			super.insertString(offset, str.toString(), attr);
	}
	
}