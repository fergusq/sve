package org.kaivos.sve.applet;

import javax.swing.JApplet;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.kaivos.sve.interpreter.SveInterpreter;
import org.kaivos.sve.interpreter.exception.SveRuntimeException;
import org.kaivos.sve.interpreter.exception.SveVariableNotFoundException;

public class SveApplet extends JApplet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	{
		try {
			@SuppressWarnings("unused")
			SveInterpreter inter = new SveInterpreter(true);
		} catch (SveVariableNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SveRuntimeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void init() {
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					
					JTextArea output = new JTextArea();
					add(output);
				}
			});
		} catch (Exception e) {
			System.err.println("createGUI didn't complete successfully");
		}
	}
	

}
