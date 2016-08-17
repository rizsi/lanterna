package com.googlecode.lanterna.gui2;

abstract public class UndoPiece {
   	public abstract void undo();
   	public abstract void redo();
	abstract public void undoCommit();
	abstract public void redoCommit();
}
