package com.googlecode.lanterna.gui2;

public class ClipboardSupport {
	private String s;
	public void copy(String s) {
//		Toolkit.getDefaultToolkit().getSystemClipboard();
		this.s=s;
	}

	public String paste() {
		return s;
	}

}
