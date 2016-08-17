package com.googlecode.lanterna.gui2;

public interface ICommandStackRecorder {

	void recordPiece(UndoPiece piece);

	void commandFinished();

}
