/*
 * This file is part of lanterna (http://code.google.com/p/lanterna/).
 * 
 * lanterna is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Copyright (C) 2010-2016 Martin
 */
package com.googlecode.lanterna.gui2;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TerminalTextUtils;
import com.googlecode.lanterna.graphics.ThemeDefinition;
import com.googlecode.lanterna.graphics.ThemeStyle;
import com.googlecode.lanterna.input.KeyStroke;

/**
 * This component keeps a text content that is editable by the user. A TextBox can be single line or multiline and lets
 * the user navigate the cursor in the text area by using the arrow keys, page up, page down, home and end. For
 * multi-line {@code TextBox}:es, scrollbars will be automatically displayed if needed.
 * <p>
 * Size-wise, a {@code TextBox} should be hard-coded to a particular size, it's not good at guessing how large it should
 * be. You can do this through the constructor.
 */
public class TextBox extends AbstractInteractableComponent<TextBox> {

    /**
     * Enum value to force a {@code TextBox} to be either single line or multi line. This is usually auto-detected if
     * the text box has some initial content by scanning that content for \n characters.
     */
    public enum Style {
        /**
         * The {@code TextBox} contains a single line of text and is typically drawn on one row
         */
        SINGLE_LINE,
        /**
         * The {@code TextBox} contains a none, one or many lines of text and is normally drawn over multiple lines
         */
        MULTI_LINE,
        ;
    }
    public class SelectionModel
    {
    	/**
    	 * First line of selection (including)
    	 */
    	public int fromLine;
    	/**
    	 * First column of selection (including)
    	 */
    	public int fromCol;
    	/**
    	 * Last line of selection (including)
    	 */
    	public int toLine;
    	/**
    	 * Last column of selection (excluding)
    	 */
    	public int toCol;
		public boolean active;
		@Override
		public String toString() {
			return "["+fromLine+", "+fromCol+" -> "+toLine+", "+toCol+"]";
		}
    }
    public interface IDataModelListener
    {

		void linesDeleted(int index, int size);

		void linesAdded(int index, int size);
    	
    }
    /**
     * Stores the text edited in the editor as lines.
     * Also provides event that can be used to maintain models that are dependent on this datamodel.
     * (For example a markers model).
     */
    class DataModel
    {
        private final List<String> lines=new ArrayList<>();

		public int size() {
			return lines.size();
		}

		public Iterable<String> getLines() {
			return lines;
		}

		public void clear() {
			List<String> copy=new ArrayList<>(lines);
			addUndoPiece(new TextEditorUndoPiece() {
				@Override
				void doUndo() {
					lines.addAll(copy);
					linesAdded(0, copy.size());
				}
				@Override
				void doRedo() {
					lines.clear();
					linesDeleted(0, copy.size());
				}
			});
			lines.clear();
			linesDeleted(0, copy.size());
		}

		private void linesDeleted(int index, int size) {
			for(IDataModelListener l: datamodelListeners)
			{
				l.linesDeleted(index, size);
			}
		}

		private void linesAdded(int index, int size) {
			for(IDataModelListener l: datamodelListeners)
			{
				l.linesAdded(index, size);
			}
		}
		public String get(int row) {
			return lines.get(row);
		}

		public void add(String string) {
			addUndoPiece(new TextEditorUndoPiece() {
				@Override
				void doUndo() {
					lines.remove(lines.size()-1);
					linesDeleted(lines.size()-1, 1);
				}
				@Override
				void doRedo() {
					lines.add(string);
					linesAdded(lines.size()-1, 1);
				}
			});
			lines.add(string);
			linesAdded(lines.size()-1, 1);
		}

		public void set(int row, String line) {
			String prev=lines.set(row, line);
			addUndoPiece(new TextEditorUndoPiece() {
				@Override
				void doUndo() {
					lines.set(row, prev);
				}
				@Override
				void doRedo() {
					lines.set(row, line);
				}
			});
		}

		public void remove(int index, int nLine) {
			if(nLine>0)
			{
				List<String> subl=lines.subList(index, index+nLine);
				List<String> old=new ArrayList<>(subl);
				subl.clear();
				addUndoPiece(new TextEditorUndoPiece() {
					@Override
					void doUndo() {
						lines.addAll(index, old);
						linesAdded(index,nLine);
					}
					@Override
					void doRedo() {
						lines.subList(index, index+nLine).clear();
						linesDeleted(index,nLine);
					}
				});
				linesDeleted(index, nLine);
			}
		}

		public void add(int i, String newLine) {
			addUndoPiece(new TextEditorUndoPiece() {
				@Override
				void doUndo() {
					lines.remove(i);
					linesDeleted(i,1);
				}
				@Override
				void doRedo() {
					lines.add(i, newLine);
					linesAdded(i,1);
				}
			});
			lines.add(i, newLine);
			linesAdded(i,1);
		}

		public void append(int i, String string) {
			String newVal=lines.get(i)+string;
			String prev=lines.set(i, newVal);
			addUndoPiece(new TextEditorUndoPiece() {
				@Override
				void doUndo() {
					lines.set(i, prev);
				}
				@Override
				void doRedo() {
					lines.set(i,newVal);
				}
			});
		}

		public void addAll(int i, List<String> subList) {
			lines.addAll(i, subList);
			linesAdded(i, subList.size());
			addUndoPiece(new TextEditorUndoPiece() {
				@Override
				void doUndo() {
					lines.subList(i, i+subList.size()).clear();
					linesDeleted(i, subList.size());
				}
				@Override
				void doRedo() {
					lines.addAll(i, subList);
					linesAdded(i, subList.size());
				}
			});
		}
    }
    class LinePart
    {
    	boolean selected;
    	String characters;
    	int offset;
    	int length;
		public LinePart(boolean selected, String characters, int offset,
				int length) {
			super();
			this.selected = selected;
			this.characters = characters;
			this.offset = offset;
			this.length = length;
		}
		@Override
		public String toString() {
			return "part: "+selected+" "+characters;
		}
    }
    abstract class TextEditorUndoPiece extends UndoPiece
    {
       	abstract void doUndo();
       	abstract void doRedo();
       	@Override
       	public void undo() {
       		doUndo();
       	}
       	@Override
       	public void redo() {
       		doRedo();
       	}
       	@Override
       	public void undoCommit() {
       		fixCaretPosition();
       	}
       	@Override
       	public void redoCommit() {
      		fixCaretPosition();
       	}
    }
    private boolean markOn=false;
    private DataModel lines=new DataModel();
    private SelectionModel selection=new SelectionModel();
    private TerminalPosition selectionStartAt=null;
    private final Style style;

    private TerminalPosition _caretPosition;
    /**
     * Most text editors support the feature that when we move the cursor with up/down arrows
     * and we are at the column 15 but the next column only has 10 columns then we remember internally that we want to be
     * at the column 15. And next time when we move to a line that has 15 columns the position is reset to that position.
     * (Editing, left/right arrows, etc clears this state. up, down pgup, pgdown preserves this state.)
     * -1 means that this feature is not active currently.
     */
    private int wantToBeAtColumn=-1;
    private boolean wantoToBeAtColumnShouldBePreserved=false;
    private boolean caretWarp;
    private boolean readOnly;
    private boolean horizontalFocusSwitching;
    private boolean verticalFocusSwitching;
    private final int maxLineLength;
    private int longestRow;
    private Character mask;
    private ICommandStackRecorder recorder;
    private List<IDataModelListener> datamodelListeners=new ArrayList<>();

	/**
     * Default constructor, this creates a single-line {@code TextBox} of size 10 which is initially empty
     */
    public TextBox() {
        this(new TerminalSize(10, 1), "", Style.SINGLE_LINE);
    }

    /**
     * Constructor that creates a {@code TextBox} with an initial content and attempting to be big enough to display
     * the whole text at once without scrollbars
     * @param initialContent Initial content of the {@code TextBox}
     */
    public TextBox(String initialContent) {
        this(null, initialContent, initialContent.contains("\n") ? Style.MULTI_LINE : Style.SINGLE_LINE);
    }

    /**
     * Creates a {@code TextBox} that has an initial content and attempting to be big enough to display the whole text
     * at once without scrollbars.
     *
     * @param initialContent Initial content of the {@code TextBox}
     * @param style Forced style instead of auto-detecting
     */
    public TextBox(String initialContent, Style style) {
        this(null, initialContent, style);
    }

    /**
     * Creates a new empty {@code TextBox} with a specific size
     * @param preferredSize Size of the {@code TextBox}
     */
    public TextBox(TerminalSize preferredSize) {
        this(preferredSize, (preferredSize != null && preferredSize.getRows() > 1) ? Style.MULTI_LINE : Style.SINGLE_LINE);
    }

    /**
     * Creates a new empty {@code TextBox} with a specific size and style
     * @param preferredSize Size of the {@code TextBox}
     * @param style Style to use
     */
    public TextBox(TerminalSize preferredSize, Style style) {
        this(preferredSize, "", style);
    }

    /**
     * Creates a new empty {@code TextBox} with a specific size and initial content
     * @param preferredSize Size of the {@code TextBox}
     * @param initialContent Initial content of the {@code TextBox}
     */
    public TextBox(TerminalSize preferredSize, String initialContent) {
        this(preferredSize, initialContent, (preferredSize != null && preferredSize.getRows() > 1) || initialContent.contains("\n") ? Style.MULTI_LINE : Style.SINGLE_LINE);
    }

    /**
     * Main constructor of the {@code TextBox} which decides size, initial content and style
     * @param preferredSize Size of the {@code TextBox}
     * @param initialContent Initial content of the {@code TextBox}
     * @param style Style to use for this {@code TextBox}, instead of auto-detecting
     */
    public TextBox(TerminalSize preferredSize, String initialContent, Style style) {
        this.style = style;
        this.readOnly = false;
        this.caretWarp = false;
        this.verticalFocusSwitching = true;
        this.horizontalFocusSwitching = (style == Style.SINGLE_LINE);
        this._caretPosition = TerminalPosition.TOP_LEFT_CORNER;
        this.maxLineLength = -1;
        this.longestRow = 1;    //To fit the cursor
        this.mask = null;
        setText(initialContent);

        // Re-adjust caret position
        this._caretPosition = TerminalPosition.TOP_LEFT_CORNER.withColumn(getLine(0).length());

        if (preferredSize == null) {
            preferredSize = new TerminalSize(Math.max(10, longestRow), lines.size());
        }
        setPreferredSize(preferredSize);
    }

    /**
     * Updates the text content of the {@code TextBox} to the supplied string.
     * @param text New text to assign to the {@code TextBox}
     * @return Itself
     */
    public synchronized TextBox setText(String text) {
        List<String> split = cleverSplit(text);
        lines.clear();
        longestRow = 1;
        for(String line : split) {
            addLine(line);
        }
        if(_caretPosition.getRow() > lines.size() - 1) {
            _caretPosition = _caretPosition.withRow(lines.size() - 1);
        }
        if(_caretPosition.getColumn() > lines.get(_caretPosition.getRow()).length()) {
            _caretPosition = _caretPosition.withColumn(lines.get(_caretPosition.getRow()).length());
        }
        if(recorder!=null)
        {
        	recorder.commandFinished();
        }
        invalidate();
        dataChanged();
        return this;
    }
    /**
     * 
     * @param pos
     * @param text
     * @return the position of the last character inserted
     */
	public synchronized TerminalPosition insertText(TerminalPosition pos, String text) {
        List<String> split = cleverSplit(text);
        longestRow = 1;
        String pre=getLine(pos.getRow()).substring(0, pos.getColumn());
        String post=getLine(pos.getRow()).substring(pos.getColumn());
        TerminalPosition ret;
        if(split.size()==1)
        {
        	String newLine=pre+split.get(0)+post;
        	lines.set(pos.getRow(), newLine);
        	ret=pos.withRelativeColumn(split.get(0).length());
        }else if(split.size()>1)
        {
        	String newLine=pre+split.get(0);
        	lines.set(pos.getRow(), newLine);
        	lines.addAll(pos.getRow()+1, split.subList(1, split.size()-1));
        	newLine=split.get(split.size()-1)+post;
        	lines.add(pos.getRow()+split.size()-1, newLine);
        	ret=new TerminalPosition(split.get(split.size()-1).length(), pos.getRow()+split.size()-1);
        }
        else
        {
        	ret=pos;
        }
        invalidate();
        dataChanged();
        return ret;
	}

	@Override
    public TextBoxRenderer getRenderer() {
        return (TextBoxRenderer)super.getRenderer();
    }

    /**
     * Adds a single line to the {@code TextBox} at the end, this only works when in multi-line mode
     * @param line Line to add at the end of the content in this {@code TextBox}
     * @return Itself
     */
    public synchronized TextBox addLine(String line) {
        StringBuilder bob = new StringBuilder();
        for(int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if(c == '\n' && style == Style.MULTI_LINE) {
                String string = bob.toString();
                int lineWidth = TerminalTextUtils.getColumnWidth(string);
                lines.add(string);
                if(longestRow < lineWidth + 1) {
                    longestRow = lineWidth + 1;
                }
                addLine(line.substring(i + 1));
                return this;
            }
            else if(Character.isISOControl(c)) {
                continue;
            }

            bob.append(c);
        }
        String string = bob.toString();
        int lineWidth = TerminalTextUtils.getColumnWidth(string);
        lines.add(string);
        if(longestRow < lineWidth + 1) {
            longestRow = lineWidth + 1;
        }
        invalidate();
        dataChanged();
        return this;
    }
    /**
     * Append string to an existing line
     * @param i index of the line to append string to.
     * @param string
     */
	public void appendLine(int i, String string) {
		if(string.contains("\n"))
		{
			throw new IllegalArgumentException("String to append must not contain newline character.");
		}
		lines.append(i, string);
		if(recorder!=null)
		{
			recorder.commandFinished();
		}
	}
    /**
     * Sets if the caret should jump to the beginning of the next line if right arrow is pressed while at the end of a
     * line. Similarly, pressing left arrow at the beginning of a line will make the caret jump to the end of the
     * previous line. This only makes sense for multi-line TextBox:es; for single-line ones it has no effect. By default
     * this is {@code false}.
     * @param caretWarp Whether the caret will warp at the beginning/end of lines
     * @return Itself
     */
    public TextBox setCaretWarp(boolean caretWarp) {
        this.caretWarp = caretWarp;
        return this;
    }

    /**
     * Checks whether caret warp mode is enabled or not. See {@code setCaretWarp} for more details.
     * @return {@code true} if caret warp mode is enabled
     */
    public boolean isCaretWarp() {
        return caretWarp;
    }

    /**
     * Returns the position of the caret, as a {@code TerminalPosition} where the row and columns equals the coordinates
     * in a multi-line {@code TextBox} and for single-line {@code TextBox} you can ignore the {@code row} component.
     * @return Position of the text input caret
     */
    public TerminalPosition getCaretPosition() {
        return _caretPosition;
    }

    /**
     * Moves the text caret position horizontally to a new position in the {@link TextBox}. For multi-line
     * {@link TextBox}:es, this will move the cursor within the current line. If the position is out of bounds, it is
     * automatically set back into range.
     * @param column Position, in characters, within the {@link TextBox} (on the current line for multi-line
     * {@link TextBox}:es) to where the text cursor should be moved
     * @return Itself
     */
    public synchronized TextBox setCaretPosition(int column) {
        return setCaretPosition(getCaretPosition().getRow(), column);
    }

    /**
     * Moves the text caret position to a new position in the {@link TextBox}. For single-line {@link TextBox}:es, the
     * line component is not used. If one of the positions are out of bounds, it is automatically set back into range.
     * @param line Which line inside the {@link TextBox} to move the caret to (0 being the first line), ignored if the
     *             {@link TextBox} is single-line
     * @param column  What column on the specified line to move the text caret to (0 being the first column)
     * @return Itself
     */
    public synchronized TextBox setCaretPosition(int line, int column) {
        if(line < 0) {
            line = 0;
        }
        else if(line >= lines.size()) {
            line = lines.size() - 1;
        }
        if(column < 0) {
            column = 0;
        }
        else if(column > lines.get(line).length()) {
            column = lines.get(line).length();
        }
        _caretPosition = _caretPosition.withRow(line).withColumn(column);
        return this;
    }

    /**
     * Returns the text in this {@code TextBox}, for multi-line mode all lines will be concatenated together with \n as
     * separator.
     * @return The text inside this {@code TextBox}
     */
    public synchronized String getText() {
        StringBuilder bob = new StringBuilder(lines.get(0));
        for(int i = 1; i < lines.size(); i++) {
            bob.append("\n").append(lines.get(i));
        }
        return bob.toString();
    }

    /**
     * Helper method, it will return the content of the {@code TextBox} unless it's empty in which case it will return
     * the supplied default value
     * @param defaultValueIfEmpty Value to return if the {@code TextBox} is empty
     * @return Text in the {@code TextBox} or {@code defaultValueIfEmpty} is the {@code TextBox} is empty
     */
    public String getTextOrDefault(String defaultValueIfEmpty) {
        String text = getText();
        if(text.isEmpty()) {
            return defaultValueIfEmpty;
        }
        return text;
    }

    /**
     * Returns the current text mask, meaning the substitute to draw instead of the text inside the {@code TextBox}.
     * This is normally used for password input fields so the password isn't shown
     * @return Current text mask or {@code null} if there is no mask
     */
    public Character getMask() {
        return mask;
    }

    /**
     * Sets the current text mask, meaning the substitute to draw instead of the text inside the {@code TextBox}.
     * This is normally used for password input fields so the password isn't shown
     * @param mask New text mask or {@code null} if there is no mask
     * @return Itself
     */
    public TextBox setMask(Character mask) {
        if(mask != null && TerminalTextUtils.isCharCJK(mask)) {
            throw new IllegalArgumentException("Cannot use a CJK character as a mask");
        }
        this.mask = mask;
        invalidate();
        return this;
    }

    /**
     * Returns {@code true} if this {@code TextBox} is in read-only mode, meaning text input from the user through the
     * keyboard is prevented
     * @return {@code true} if this {@code TextBox} is in read-only mode
     */
    public boolean isReadOnly() {
        return readOnly;
    }

    /**
     * Sets the read-only mode of the {@code TextBox}, meaning text input from the user through the keyboard is
     * prevented. The user can still focus and scroll through the text in this mode.
     * @param readOnly If {@code true} then the {@code TextBox} will switch to read-only mode
     * @return Itself
     */
    public TextBox setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        invalidate();
        return this;
    }

    /**
     * If {@code true}, the component will switch to the next available component above if the cursor is at the top of
     * the TextBox and the user presses the 'up' array key, or switch to the next available component below if the
     * cursor is at the bottom of the TextBox and the user presses the 'down' array key. The means that for single-line
     * TextBox:es, pressing up and down will always switch focus.
     * @return {@code true} if vertical focus switching is enabled
     */
    public boolean isVerticalFocusSwitching() {
        return verticalFocusSwitching;
    }

    /**
     * If set to {@code true}, the component will switch to the next available component above if the cursor is at the
     * top of the TextBox and the user presses the 'up' array key, or switch to the next available component below if
     * the cursor is at the bottom of the TextBox and the user presses the 'down' array key. The means that for
     * single-line TextBox:es, pressing up and down will always switch focus with this mode enabled.
     * @param verticalFocusSwitching If called with true, vertical focus switching will be enabled
     * @return Itself
     */
    public TextBox setVerticalFocusSwitching(boolean verticalFocusSwitching) {
        this.verticalFocusSwitching = verticalFocusSwitching;
        return this;
    }

    /**
     * If {@code true}, the TextBox will switch focus to the next available component to the left if the cursor in the
     * TextBox is at the left-most position (index 0) on the row and the user pressed the 'left' arrow key, or vice
     * versa for pressing the 'right' arrow key when the cursor in at the right-most position of the current row.
     * @return {@code true} if horizontal focus switching is enabled
     */
    public boolean isHorizontalFocusSwitching() {
        return horizontalFocusSwitching;
    }

    /**
     * If set to {@code true}, the TextBox will switch focus to the next available component to the left if the cursor
     * in the TextBox is at the left-most position (index 0) on the row and the user pressed the 'left' arrow key, or
     * vice versa for pressing the 'right' arrow key when the cursor in at the right-most position of the current row.
     * @param horizontalFocusSwitching If called with true, horizontal focus switching will be enabled
     * @return Itself
     */
    public TextBox setHorizontalFocusSwitching(boolean horizontalFocusSwitching) {
        this.horizontalFocusSwitching = horizontalFocusSwitching;
        return this;
    }

    /**
     * Returns the line on the specific row. For non-multiline TextBox:es, calling this with index set to 0 will return
     * the same as calling {@code getText()}. If the row index is invalid (less than zero or equals or larger than the
     * number of rows), this method will throw IndexOutOfBoundsException.
     * @param index Index of the row to return the contents from
     * @return The line at the specified index, as a String
     * @throws IndexOutOfBoundsException if the row index is less than zero or too large
     */
    public synchronized String getLine(int index) {
        return lines.get(index);
    }

    /**
     * Returns the number of lines currently in this TextBox. For single-line TextBox:es, this will always return 1.
     * @return Number of lines of text currently in this TextBox
     */
    public synchronized int getLineCount() {
        return lines.size();
    }

    @Override
    protected TextBoxRenderer createDefaultRenderer() {
        return new DefaultTextBoxRenderer();
    }

    @Override
    public synchronized Result handleKeyStroke(KeyStroke keyStroke) {
    	Result r;
		try {
			wantoToBeAtColumnShouldBePreserved=false;
			r = handleKeyStrokeWrapped(keyStroke);
		} catch (Exception e) {
			// TODO Exceptions in key handling are caught because they would cause the application to crash.
			// We return handled as the key processing was started to be handled.
			e.printStackTrace();
			return Result.HANDLED;
		}
		if(!wantoToBeAtColumnShouldBePreserved)
		{
			wantToBeAtColumn=-1;
		}
    	if(recorder!=null)
    	{
    		recorder.commandFinished();
    	}
    	return r;
    }
    public synchronized Result handleKeyStrokeWrapped(KeyStroke keyStroke) {
        switch(keyStroke.getKeyType()) {
            case Character:
            {
            	if(keyStroke.isCtrlDown())
            	{
            		switch (keyStroke.getCharacter()) {
					case 'c':
					{
						String s=getSelectedContent();
						if(s!=null)
						{
							getTextGUI().getClipboardSupport().copy(s);
						}
						break;
					}
					case 'x':
					{
						String s=getSelectedContent();
						if(s!=null)
						{
							getTextGUI().getClipboardSupport().copy(s);
							if(readOnly)
							{
								return Result.HANDLED;
							}
							deleteSelectedContent();
			                dataChanged();
						}
						break;
					}
					case 'v':
					{
						if(readOnly)
						{
							return Result.HANDLED;
						}
		            	if(selection.active)
		            	{
		            		deleteSelectedContent();
		            	}
						String s=getTextGUI().getClipboardSupport().paste();
						if(s!=null)
						{
							_caretPosition=insertText(_caretPosition, s);
							fixCaretPosition();
						}
		                dataChanged();
						break;
					}
					default:
						break;
					}
            		break;
            	}
            	if(keyStroke.isAltDown())
            	{
            		// ALT+char combinations are not handled.
            		break;
            	}
				if(readOnly)
				{
					return Result.HANDLED;
				}
            	if(selection.active)
            	{
            		deleteSelectedContent();
                    dataChanged();
            	}
                String line = lines.get(_caretPosition.getRow());
                if(maxLineLength == -1 || maxLineLength > line.length() + 1) {
                    line = line.substring(0, _caretPosition.getColumn()) + keyStroke.getCharacter() + line.substring(_caretPosition.getColumn());
                    lines.set(_caretPosition.getRow(), line);
                    _caretPosition = _caretPosition.withRelativeColumn(1);
                    dataChanged();
                }
                return Result.HANDLED;
            }
            case Backspace:
            {
				if(readOnly)
				{
					return Result.HANDLED;
				}
            	if(selection.active)
            	{
            		deleteSelectedContent();
                    dataChanged();
            		return Result.HANDLED;
            	}
                String line = lines.get(_caretPosition.getRow());
                if(_caretPosition.getColumn() > 0) {
                    line = line.substring(0, _caretPosition.getColumn() - 1) + line.substring(_caretPosition.getColumn());
                    lines.set(_caretPosition.getRow(), line);
                    _caretPosition = _caretPosition.withRelativeColumn(-1);
                }
                else if(style == Style.MULTI_LINE && _caretPosition.getRow() > 0) {
                    String concatenatedLines = lines.get(_caretPosition.getRow() - 1) + line;
                    lines.remove(_caretPosition.getRow(), 1);
                    _caretPosition = _caretPosition.withRelativeRow(-1);
                    _caretPosition = _caretPosition.withColumn(lines.get(_caretPosition.getRow()).length());
                    lines.set(_caretPosition.getRow(), concatenatedLines);
                }
                dataChanged();
                return Result.HANDLED;
            }
            case Delete:
            {
				if(readOnly)
				{
					return Result.HANDLED;
				}
            	if(selection.active)
            	{
            		deleteSelectedContent();
                    dataChanged();
                    return Result.HANDLED;
            	}
                String line = lines.get(_caretPosition.getRow());
                if(_caretPosition.getColumn() < line.length()) {
                    line = line.substring(0, _caretPosition.getColumn()) + line.substring(_caretPosition.getColumn() + 1);
                    lines.set(_caretPosition.getRow(), line);
                }
                else if(style == Style.MULTI_LINE && _caretPosition.getRow() < lines.size() - 1) {
                    String concatenatedLines = line + lines.get(_caretPosition.getRow() + 1);
                    lines.set(_caretPosition.getRow(), concatenatedLines);
                    lines.remove(_caretPosition.getRow() + 1, 1);
                }
                dataChanged();
                return Result.HANDLED;
            }
            case ArrowLeft:
            	checkSelectionStart(keyStroke);
                if(_caretPosition.getColumn() > 0) {
                    setCaretPositionCheckSelect(_caretPosition.withRelativeColumn(-1));
                }
                else if(style == Style.MULTI_LINE && caretWarp && getCaretPosition().getRow() > 0) {
                	setCaretPositionCheckSelect(_caretPosition.withRelativeRow(-1).withColumn(lines.get(_caretPosition.getRow()-1).length()));
                }
                else if(horizontalFocusSwitching) {
                    return Result.MOVE_FOCUS_LEFT;
                }
                return Result.HANDLED;
            case ArrowRight:
            	checkSelectionStart(keyStroke);
                if(_caretPosition.getColumn() < lines.get(_caretPosition.getRow()).length()) {
                	setCaretPositionCheckSelect(_caretPosition.withRelativeColumn(1));
                }
                else if(style == Style.MULTI_LINE && caretWarp && _caretPosition.getRow() < lines.size() - 1) {
                	setCaretPositionCheckSelect(_caretPosition.withRelativeRow(1).withColumn(0));
                }
                else if(horizontalFocusSwitching) {
                    return Result.MOVE_FOCUS_RIGHT;
                }
                return Result.HANDLED;
            case ArrowUp:
            {
            	checkSelectionStart(keyStroke);
                if(_caretPosition.getRow() > 0) {
                	jumpToLine(_caretPosition.getRow(), _caretPosition.getRow()-1);
                }
                else if(verticalFocusSwitching) {
                    return Result.MOVE_FOCUS_UP;
                }
                return Result.HANDLED;
            }
            case ArrowDown:
            {
            	checkSelectionStart(keyStroke);
                if(_caretPosition.getRow() < lines.size() - 1) {
                	jumpToLine(_caretPosition.getRow(), _caretPosition.getRow()+1);
                }
                else if(verticalFocusSwitching) {
                    return Result.MOVE_FOCUS_DOWN;
                }
                return Result.HANDLED;
            }
            case End:
            {
                String line = lines.get(_caretPosition.getRow());
            	checkSelectionStart(keyStroke);
            	setCaretPositionCheckSelect(_caretPosition.withColumn(line.length()));
                return Result.HANDLED;
            }
            case Enter:
            {
				if(readOnly)
				{
					return Result.HANDLED;
				}
            	if(selection.active)
            	{
            		deleteSelectedContent();
            	}
                String line = lines.get(_caretPosition.getRow());
                if(style == Style.SINGLE_LINE) {
                	return singleLineEntered();
                }
                String newLine = line.substring(_caretPosition.getColumn());
                String oldLine = line.substring(0, _caretPosition.getColumn());
                lines.set(_caretPosition.getRow(), oldLine);
                lines.add(_caretPosition.getRow() + 1, newLine);
                _caretPosition = _caretPosition.withColumn(0).withRelativeRow(1);
                dataChanged();
                return Result.HANDLED;
            }
            case Home:
            	checkSelectionStart(keyStroke);
            	setCaretPositionCheckSelect(_caretPosition.withColumn(0));
                return Result.HANDLED;
            case PageDown:
            {
            	checkSelectionStart(keyStroke);
            	int toRow=_caretPosition.getRow()+getSize().getRows();
                if(toRow > lines.size() - 1) {
                    toRow = lines.size() - 1;
                }
            	jumpToLine(_caretPosition.getRow(), toRow);
                return Result.HANDLED;
            }
            case PageUp:
            {
            	checkSelectionStart(keyStroke);
            	int toRow=_caretPosition.getRow()-getSize().getRows();
                if(toRow < 0) {
                    toRow = 0;
                }
            	jumpToLine(_caretPosition.getRow(), toRow);
                return Result.HANDLED;
            }
            case F3:
            {
            	markOn=!markOn;
            }
            default:
        }
        return super.handleKeyStroke(keyStroke);
    }
	private void jumpToLine(int from, int to) {
		wantoToBeAtColumnShouldBePreserved=true;
        String lineFrom = lines.get(from);
		if(wantToBeAtColumn==-1)
		{
			wantToBeAtColumn=TerminalTextUtils.getColumnIndex(lineFrom, _caretPosition.getColumn());
		}
        int column=wantToBeAtColumn;
        String lineTo = lines.get(to);
        if(column > TerminalTextUtils.getColumnWidth(lineTo)) {
            column=lineTo.length();
        }
        else {
            column=TerminalTextUtils.getStringCharacterIndex(lineTo, column);
        }
        setCaretPositionCheckSelect(new TerminalPosition(column, to));
	}

	/**
     * Get the top-left position of the current view.
     * @return
     */
    public TerminalPosition getViewTopLeft()
    {
    	return getRenderer().getViewTopLeft();
    }

	private String getSelectedContent() {
		if(selection.active)
		{
			StringBuilder ret=new StringBuilder();
			if(selection.fromLine==selection.toLine)
			{
				String line=getLine(selection.fromLine);
				ret.append(line.substring(selection.fromCol,selection.toCol));
			}else
			{
				String line=getLine(selection.fromLine);
				ret.append(line.substring(selection.fromCol));
				for(int i=selection.fromLine+1;i<selection.toLine; ++i)
				{
					ret.append("\n");
					ret.append(lines.get(i));
				}
				line=getLine(selection.toLine);
				ret.append("\n");
				ret.append(line.substring(0, selection.toCol));
			}
			return ret.toString();
		}else
		{
			return null;
		}
	}
	private void deleteSelectedContent() {
		if(selection.active)
		{
			TerminalPosition nc=new TerminalPosition(selection.fromCol, selection.fromLine);
			if(selection.fromLine==selection.toLine)
			{
				String line=getLine(selection.fromLine);
				lines.set(selection.fromLine, line.substring(0, selection.fromCol)+line.substring(selection.toCol, line.length()));
			}else
			{
				String line=getLine(selection.fromLine);
				lines.set(selection.fromLine, line.substring(0, selection.fromCol));
				lines.remove(selection.fromLine+1, selection.toLine-(selection.fromLine+1));
				line=getLine(selection.fromLine+1);
				line=line.substring(selection.toCol);
				lines.remove(selection.fromLine+1, 1);
				lines.append(selection.fromLine, line);
			}
			if(nc.getRow()>=lines.size())
			{
				nc.withRow(lines.size()-1);
			}
			String line=lines.get(nc.getRow());
			if(line.length()<=nc.getColumn())
			{
				nc.withColumn(line.length());
			}
			_caretPosition=nc;
			selection.active=false;
			selectionStartAt=null;
		}
	}

	private void setCaretPositionCheckSelect(TerminalPosition nc) {
		_caretPosition=nc;
		if(selectionStartAt!=null)
		{
			TerminalPosition nc2=selectionStartAt;
			int cmp=compare(nc, nc2);
			if(cmp!=0)
			{
				if(cmp<0)
				{
					TerminalPosition atm=nc2;
					nc2=nc;
					nc=atm;
				}
				selection.active=true;
				selection.fromLine=nc2.getRow();
				selection.fromCol=nc2.getColumn();
				selection.toLine=nc.getRow();
				selection.toCol=nc.getColumn();
			}else
			{
				selection.active=false;
			}
		}
	}

	private int compare(TerminalPosition nc, TerminalPosition selectionStartAt2) {
		if(nc.getRow()>selectionStartAt2.getRow())
		{
			return 1;
		}else if(nc.getRow()==selectionStartAt2.getRow())
		{
			return Integer.compare(nc.getColumn(), selectionStartAt2.getColumn());
		}else
		{
			return -1;
		}
	}
	private void deleteSelection()
	{
		selectionStartAt=null;
		selection.active=false;
	}
	private void checkSelectionStart(KeyStroke keyStroke) {
		if(keyStroke.isShiftDown()||markOn)
		{
			if(selectionStartAt==null)
			{
				selectionStartAt=_caretPosition;
			}
		}else
		{
			deleteSelection();
		}
	}

    /**
     * Helper interface that doesn't add any new methods but makes coding new text box renderers a little bit more clear
     */
    public interface TextBoxRenderer extends InteractableRenderer<TextBox> {
        TerminalPosition getViewTopLeft();
        void setViewTopLeft(TextBox component, TerminalPosition position);
    }

    /**
     * This is the default text box renderer that is used if you don't override anything. With this renderer, the text
     * box is filled with a solid background color and the text is drawn on top of it. Scrollbars are added for
     * multi-line text whenever the text inside the {@code TextBox} does not fit in the available area.
     */
    public static class DefaultTextBoxRenderer implements TextBoxRenderer {
        private TerminalPosition viewTopLeft;
        private final ScrollBar verticalScrollBar;
        private final ScrollBar horizontalScrollBar;
        private boolean hideScrollBars;
        private Character unusedSpaceCharacter;

        /**
         * Default constructor
         */
        public DefaultTextBoxRenderer() {
            viewTopLeft = TerminalPosition.TOP_LEFT_CORNER;
            verticalScrollBar = new ScrollBar(Direction.VERTICAL);
            horizontalScrollBar = new ScrollBar(Direction.HORIZONTAL);
            hideScrollBars = false;
            unusedSpaceCharacter = null;
        }

        /**
         * Sets the character to represent an empty untyped space in the text box. This will be an empty space by
         * default but you can override it to anything that isn't double-width.
         * @param unusedSpaceCharacter Character to draw in unused space of the {@link TextBox}
         * @throws IllegalArgumentException If unusedSpaceCharacter is a double-width character
         */
        public void setUnusedSpaceCharacter(char unusedSpaceCharacter) {
            if(TerminalTextUtils.isCharDoubleWidth(unusedSpaceCharacter)) {
                throw new IllegalArgumentException("Cannot use a double-width character as the unused space character in a TextBox");
            }
            this.unusedSpaceCharacter = unusedSpaceCharacter;
        }

        @Override
        public TerminalPosition getViewTopLeft() {
            return viewTopLeft;
        }

        @Override
        public void setViewTopLeft(TextBox component, TerminalPosition position) {
            if(position.getColumn() < 0) {
                position = position.withColumn(0);
            }
            if(position.getRow() < 0) {
                position = position.withRow(0);
            }
            if(viewTopLeft==null||!viewTopLeft.equals(position))
            {
            	viewTopLeft = position;
            	component.fireViewTopChanged();
            }
        }

        @Override
        public TerminalPosition getCursorLocation(TextBox component) {
            //Adjust caret position if necessary
            TerminalPosition caretPosition = component.getCaretPosition();
            String line = component.getLine(caretPosition.getRow());
            caretPosition = caretPosition.withColumn(Math.min(caretPosition.getColumn(), line.length()));

            return caretPosition
                    .withColumn(TerminalTextUtils.getColumnIndex(line, caretPosition.getColumn()))
                    .withRelativeColumn(-viewTopLeft.getColumn())
                    .withRelativeRow(-viewTopLeft.getRow());
        }

        @Override
        public TerminalSize getPreferredSize(TextBox component) {
            return new TerminalSize(component.longestRow, component.lines.size());
        }

        /**
         * Controls whether scrollbars should be visible or not when a multi-line {@code TextBox} has more content than
         * it can draw in the area it was assigned (default: false)
         * @param hideScrollBars If {@code true}, don't show scrollbars if the multi-line content is bigger than the
         *                       area
         */
        public void setHideScrollBars(boolean hideScrollBars) {
            this.hideScrollBars = hideScrollBars;
        }

        @Override
        public void drawComponent(TextGUIGraphics graphics, TextBox component) {
            TerminalSize realTextArea = graphics.getSize();
            if(realTextArea.getRows() == 0 || realTextArea.getColumns() == 0) {
                return;
            }
            boolean drawVerticalScrollBar = false;
            boolean drawHorizontalScrollBar = false;
            int textBoxLineCount = component.getLineCount();
            if(!hideScrollBars && textBoxLineCount > realTextArea.getRows() && realTextArea.getColumns() > 1) {
                realTextArea = realTextArea.withRelativeColumns(-1);
                drawVerticalScrollBar = true;
            }
            if(!hideScrollBars && component.longestRow > realTextArea.getColumns() && realTextArea.getRows() > 1) {
                realTextArea = realTextArea.withRelativeRows(-1);
                drawHorizontalScrollBar = true;
                if(textBoxLineCount > realTextArea.getRows() && realTextArea.getRows() == graphics.getSize().getRows()) {
                    realTextArea = realTextArea.withRelativeColumns(-1);
                    drawVerticalScrollBar = true;
                }
            }

            drawTextArea(graphics.newTextGraphics(TerminalPosition.TOP_LEFT_CORNER, realTextArea), component);

            //Draw scrollbars, if any
            if(drawVerticalScrollBar) {
                verticalScrollBar.onAdded(component.getParent());
                verticalScrollBar.setViewSize(realTextArea.getRows());
                verticalScrollBar.setScrollMaximum(textBoxLineCount);
                verticalScrollBar.setScrollPosition(viewTopLeft.getRow());
                verticalScrollBar.draw(graphics.newTextGraphics(
                        new TerminalPosition(graphics.getSize().getColumns() - 1, 0),
                        new TerminalSize(1, graphics.getSize().getRows() - (drawHorizontalScrollBar ? 1 : 0))));
            }
            if(drawHorizontalScrollBar) {
                horizontalScrollBar.onAdded(component.getParent());
                horizontalScrollBar.setViewSize(realTextArea.getColumns());
                horizontalScrollBar.setScrollMaximum(component.longestRow - 1);
                horizontalScrollBar.setScrollPosition(viewTopLeft.getColumn());
                horizontalScrollBar.draw(graphics.newTextGraphics(
                        new TerminalPosition(0, graphics.getSize().getRows() - 1),
                        new TerminalSize(graphics.getSize().getColumns() - (drawVerticalScrollBar ? 1 : 0), 1)));
            }
        }

        private void drawTextArea(TextGUIGraphics graphics, TextBox component) {

        	TerminalSize textAreaSize = graphics.getSize();
            if(viewTopLeft.getColumn() + textAreaSize.getColumns() > component.longestRow) {
                TerminalPosition newviewTopLeft = viewTopLeft.withColumn(component.longestRow - textAreaSize.getColumns());
                if(newviewTopLeft.getColumn() < 0) {
                    newviewTopLeft = newviewTopLeft.withColumn(0);
                    setViewTopLeft(component, newviewTopLeft);
                }
            }
            if(viewTopLeft.getRow() + textAreaSize.getRows() > component.getLineCount()) {
            	TerminalPosition newviewTopLeft = viewTopLeft.withRow(component.getLineCount() - textAreaSize.getRows());
                if(newviewTopLeft.getRow() < 0) {
                    newviewTopLeft = newviewTopLeft.withRow(0);
                    setViewTopLeft(component, newviewTopLeft);
                }
            }
            ThemeDefinition themeDefinition = component.getThemeDefinition();
            ThemeStyle style=getThemeStyle(component, themeDefinition);
            ThemeStyle styleSelected=getThemeStyleSelected(component, themeDefinition);
            graphics.applyThemeStyle(style);

            Character fillCharacter = unusedSpaceCharacter;
            if(fillCharacter == null) {
                fillCharacter = themeDefinition.getCharacter("FILL", ' ');
            }
            graphics.fill(fillCharacter);

            {
                //Adjust caret position if necessary
                TerminalPosition caretPosition = component.getCaretPosition();
                String caretLine = component.getLine(caretPosition.getRow());
                caretPosition = caretPosition.withColumn(Math.min(caretPosition.getColumn(), caretLine.length()));

                //Adjust the view if necessary
                int trueColumnPosition = TerminalTextUtils.getColumnIndex(caretLine, caretPosition.getColumn());
                TerminalPosition viewTopLeftTemporary=viewTopLeft;
                if (trueColumnPosition < viewTopLeftTemporary.getColumn()) {
                    viewTopLeftTemporary = viewTopLeftTemporary.withColumn(trueColumnPosition);
                }
                else if (trueColumnPosition >= textAreaSize.getColumns() + viewTopLeftTemporary.getColumn()) {
                    viewTopLeftTemporary = viewTopLeftTemporary.withColumn(trueColumnPosition - textAreaSize.getColumns() + 1);
                }
                if (caretPosition.getRow() < viewTopLeftTemporary.getRow()) {
                    viewTopLeftTemporary = viewTopLeftTemporary.withRow(caretPosition.getRow());
                }
                else if (caretPosition.getRow() >= textAreaSize.getRows() + viewTopLeftTemporary.getRow()) {
                    viewTopLeftTemporary = viewTopLeftTemporary.withRow(caretPosition.getRow() - textAreaSize.getRows() + 1);
                }

                //Additional corner-case for CJK characters
                if(trueColumnPosition - viewTopLeftTemporary.getColumn() == graphics.getSize().getColumns() - 1) {
                    if(caretLine.length() > caretPosition.getColumn() &&
                            TerminalTextUtils.isCharCJK(caretLine.charAt(caretPosition.getColumn()))) {
                        viewTopLeftTemporary = viewTopLeftTemporary.withRelativeColumn(1);
                    }
                }
                setViewTopLeft(component, viewTopLeftTemporary);
            }

            for (int row = 0; row < textAreaSize.getRows(); row++) {
                int rowIndex = row + viewTopLeft.getRow();
                if(rowIndex >= component.lines.size()) {
                    continue;
                }
                LinePart[] parts=component.getLinePartsToRender(rowIndex, viewTopLeft.getColumn(), textAreaSize.getColumns());
                for(LinePart p: parts)
                {
                	if(p.selected)
                	{
                        graphics.applyThemeStyle(styleSelected);
                	}
                	String s=TerminalTextUtils.fitString(p.characters, 0, p.length);
                    graphics.putString(p.offset, row, s); 
                	if(p.selected)
                	{
                        graphics.applyThemeStyle(style);
                	}
                }
            }
        }

		private ThemeStyle getThemeStyle(TextBox component, ThemeDefinition themeDefinition) {
            if (component.isFocused()) {
                if(component.isReadOnly()) {
                    return themeDefinition.getSelected();
                }
                else {
                	return themeDefinition.getActive();
                }
            }
            else {
                if(component.isReadOnly()) {
                	return themeDefinition.getInsensitive();
                }
                else {
                	return themeDefinition.getNormal();
                }
            }
		}
		private ThemeStyle getThemeStyleSelected(TextBox component, ThemeDefinition themeDefinition) {
			return themeDefinition.getCustom("selectedText", themeDefinition.getInsensitive());
		}
    }
    /**
     * Data model has changed. Fire listeners.
     * This method may be called from within synchronized block. Listeners are fired 
     * on an invokeLater call on the GUI thread.
     */
    private void dataChanged() {
    	if(getTextGUI()!=null)
    	{
	    	getTextGUI().getGUIThread().invokeLater(new Runnable() {
				@Override
				public void run() {
					fireDataChangeListeners();
				}
			});
    	}
	}

	protected void fireViewTopChanged() {
		
	}

	public LinePart[] getLinePartsToRender(int rowIndex, int column, int columns) {
    	List<LinePart> ret=new ArrayList<>();
    	String line=lines.get(rowIndex);
    	if(selection.active &&selection.fromLine<=rowIndex&&selection.toLine>=rowIndex)
    	{
    		int fromCol=selection.fromCol;
    		int toCol=selection.toCol;
    		if(selection.toLine>rowIndex)
    		{
    			toCol=line.length();
    		}
    		if(selection.fromLine<rowIndex)
    		{
    			fromCol=0;
    		}
    		if(fromCol>line.length())
    		{
    			fromCol=line.length();
    		}
    		if(toCol>line.length())
    		{
    			toCol=line.length();
    		}
    		if(fromCol>0)
    		{
    			ret.add(new LinePart(false, line.substring(0, fromCol), 0, fromCol));
    		}
			ret.add(new LinePart(true, line.substring(fromCol, toCol), fromCol, toCol-fromCol));
    		if(toCol<line.length())
    		{
    			ret.add(new LinePart(false, line.substring(toCol, line.length()), toCol, line.length()-toCol));
    		}
    	}else
    	{
    		ret.add(new LinePart(false, line, 0, line.length()));
    	}
    	
        if(getMask() != null) {
            StringBuilder builder = new StringBuilder();
            for(int i = 0; i < line.length(); i++) {
                builder.append(getMask());
            }
            line = builder.toString();
        }
		return ret.toArray(new LinePart[]{});
	}

	/**
     * Fire data changed listeners.
     * Default implementation does nothing.
     */
	protected void fireDataChangeListeners() {
		
	}
	/**
	 * Method is called when on a single line text box enter was pressed.
	 * Default implementation passes the focus to the next element.
	 */
    synchronized protected Result singleLineEntered() {
        return Result.MOVE_FOCUS_NEXT;
	}
    private void addUndoPiece(UndoPiece piece)
    {
    	if(recorder!=null)
    	{
    		recorder.recordPiece(piece);
    	}
    }
    public ICommandStackRecorder getRecorder() {
		return recorder;
	}

	public void setRecorder(ICommandStackRecorder recorder) {
		this.recorder = recorder;
	}
	private void fixCaretPosition() {
		if(_caretPosition.getRow()>=lines.size())
		{
			_caretPosition=_caretPosition.withRow(lines.size()-1);
		}
		String line=lines.get(_caretPosition.getRow());
		if(_caretPosition.getColumn()>=line.length())
		{
			_caretPosition=_caretPosition.withColumn(line.length());
		}
	}
	/**
	 * Split string with preserving empty lines (even at the end of the string)
	 * @param s
	 * @return
	 */
	public static List<String> cleverSplit(String s)
	{
		List<String> ret=new ArrayList<>();
		int at=0;
		int idx=s.indexOf('\n');
		while(idx>=0)
		{
			ret.add(s.substring(at, idx));
			at=idx+1;
			idx=s.indexOf('\n', at);
		}
		ret.add(s.substring(at));
		return ret;
	}
	public void selectAll() {
		selection.active=true;
		selection.fromLine=0;
		selection.fromCol=0;
		selection.toCol=lines.get(lines.size()-1).length();
		selection.toLine=lines.size()-1;
	}
	/**
	 * Add data model listener. Data model listener is called whenever a line is added or removed from the model.
	 * @param l
	 */
	public void addDataModelListener(IDataModelListener l)
	{
		datamodelListeners.add(l);
	}
	/**
	 * Remove data model listener (see addDataModelListener)
	 * @param l
	 */
	public void removeDataModelListener(IDataModelListener l)
	{
		datamodelListeners.add(l);
	}

	public TextBox setValidationPattern(Pattern numberPattern) {
		return this;
	}
	public SelectionModel getSelection() {
		return selection;
	}

	public void setSelection(int row1, int col1, int row2, int col2) {
		selection.active=true;
		selection.fromLine=row1;
		selection.fromCol=col1;
		selection.toLine=row2;
		selection.toCol=col2;
	}
}
