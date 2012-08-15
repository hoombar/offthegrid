package net.rdyonline;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import net.rdyonline.strategy.PasswordStrategy;
import android.content.Context;
import android.graphics.Point;

import com.flurry.android.FlurryAgent;

/***
 * This is arguably the most important object in the program. Every operation revolves around a grid.
 * The grid is created as a singleton to make sure it can be updated across activities without worrying about "stale" grids
 * reporting incorrect passwords
 * @author rdy
 *
 */
public class Grid {

	// singleton
	private static Grid instance = null;
	
	// the grid is 28x28. Each letter of the alphabet and two padding characters (one either side)
	private char[][] gridArray = new char[28][28];
	private String gridString = null;
	
	private directions currentDirection = directions.right;
	private Point currentPosition = new Point(1,1);
	
	// used during the algorithm for grid generation 
	private final int ROW_ITERATION_THRESHOLD = 200;
	private int getPossibleValuesCallCount = 0;
	
	// Random number generator used for generating grids.
	private final SecureRandom randomiser = new SecureRandom();
	
	// the directions we can seek through the grid
	private enum directions {
		up,
		down,
		left,
		right
	}
	
	public String getGridString() {
		return gridString;
	}

	public void setGridString(String gridString, Context context) {
		this.gridString = gridString;
		
		char[] gridStringAs1dArray = gridString.toCharArray();
		
		if (gridString != null)
		{
			for (int row = 0; row < 28; row++)
			{
				for (int col = 0; col < 28; col++)
				{
					gridArray[col][row] = gridStringAs1dArray[ (int) ((row)*28) + col ];
				}
			}
		}
		
		new ApplicationSettings(context).setGrid(gridString);
	}
	
	protected Grid()
	{
		// make this class a singleton - we always work with the same grid
	}
	
	public static Grid getInstance(Context context)
	{
		if (instance == null)
		{
			instance = new Grid();
		}
		
		return instance;
	}
	
	/***
	 * If there is a number, a dash or a dot in the domain name, it's replaced with the character in the grid directly below
	 * the corresponding number/dash/dot in the top padding section
	 * @param text domain name or password string
	 * @return text with non alphabetic characters replaced
	 */
	private String replaceNumberDashDot(String text) {
		String result = text;
		
		text = text.replace( this.getCharacterAt(new Point(8,0)), this.getCharacterAt(new Point(8,1)) ); // 0
		text = text.replace( this.getCharacterAt(new Point(9,0)), this.getCharacterAt(new Point(9,1)) ); // 1
		text = text.replace( this.getCharacterAt(new Point(10,0)), this.getCharacterAt(new Point(10,1)) ); // 2
		text = text.replace( this.getCharacterAt(new Point(11,0)), this.getCharacterAt(new Point(11,1)) ); // 3
		text = text.replace( this.getCharacterAt(new Point(12,0)), this.getCharacterAt(new Point(12,1)) ); // 4
		text = text.replace( this.getCharacterAt(new Point(13,0)), this.getCharacterAt(new Point(13,1)) ); // 5
		text = text.replace( this.getCharacterAt(new Point(14,0)), this.getCharacterAt(new Point(14,1)) ); // 6
		text = text.replace( this.getCharacterAt(new Point(15,0)), this.getCharacterAt(new Point(15,1)) ); // 7
		text = text.replace( this.getCharacterAt(new Point(16,0)), this.getCharacterAt(new Point(16,1)) ); // 8
		text = text.replace( this.getCharacterAt(new Point(17,0)), this.getCharacterAt(new Point(17,1)) ); // 9
		text = text.replace( this.getCharacterAt(new Point(18,0)), this.getCharacterAt(new Point(18,1)) ); // -
		text = text.replace( this.getCharacterAt(new Point(19,0)), this.getCharacterAt(new Point(19,1)) ); // .		
		
		text = text.replaceAll("[^A-Za-z]", "");
		
		result = text;
		
		return result;
	}
	
	/***
	 * The start position is determined by moving right, up and down through the grid using the domain
	 * @param settings
	 * @param text
	 * @param Y
	 */
	private void determineStartPosition(String text, int Y) {

		// always start on the first column - row is passed in (assumed from settings)
		this.currentPosition = new Point(1, Y);
		// always start by moving right
		this.currentDirection = directions.right;
		char[] characters = text.toCharArray();
		
		for (int i = 0; i < characters.length; i++) {

			char character = characters[i];
			
			// update the starting position of (x or y) based on the previous direction
			directions direction = getDirectionSeed(i);
			this.currentDirection = direction;
			
			switch (direction)
			{
				case right:
					this.currentPosition.x = ((26+(this.currentPosition.x)) % 26)+1;
					break;
				case down:
					this.currentPosition.y = ((26+(this.currentPosition.y)) % 26)+1;
					break;
				case left:
					this.currentPosition.x = ((26+(this.currentPosition.x - 2)) % 26)+1;
					break;
				case up:
					this.currentPosition.y = ((26+(this.currentPosition.y - 2)) % 26)+1;
					break;
			}
			
			// never account for top row, bottom row, left most or right most
			findChar(character, this.currentDirection, true);
			
		}
	}
	
	public String getPasswordFromText(Context context, String text, int numChars, int Y)
	{		
		ApplicationSettings settings = new ApplicationSettings(context);
		PasswordStrategy strategy = settings.getPasswordStrategy();

		if (strategy != null)
		{
			
			char[] key = getKeyFromString(text, numChars / 2);
			return getPassword(key, Y, strategy);
			
		}
		else
		{
					
			String result = "";
			
			// swap out the numbers dots and dashes
			text = replaceNumberDashDot(text);
			
			// work out the starting position and the direction
			determineStartPosition(text, Y);
			
			this.currentDirection = this.getDirection(0); // getNextDirection();
			
			char[] characters = text.toCharArray();
			for (int i = 0; i < characters.length; i++) {
				
				char character = characters[i];
	
				if (result.toLowerCase().endsWith(Character.toString(character).toLowerCase()))
				{
					// update the starting position of (x or y) based on the previous direction
					directions direction = getDirection(i-1); // getPreviousDirection();
					
					switch (direction)
					{
						case right:
							this.currentPosition.x = ((26+(this.currentPosition.x)) % 26)+1;
							break;
						case down:
							this.currentPosition.y = ((26+(this.currentPosition.y)) % 26)+1;
							break;
						case left:
							this.currentPosition.x = ((26+(this.currentPosition.x - 2)) % 26)+1;
							break;
						case up:
							this.currentPosition.y = ((26+(this.currentPosition.y - 2)) % 26)+1;
							break;
					}
				}
				
				// never account for top row, bottom row, left most or right most
				result += findChar(character, this.currentDirection, false);
				this.currentDirection = this.getDirection(i+1); // getNextDirection();
			}
			
			// padding chars
			String paddChars = result.replaceAll("[a-zA-Z]", "");
			result = result.replaceAll("[^a-zA-Z]","");
			
			// only the first {x} characters are used
			int end = numChars;
			if (result.length() < numChars)
			{
				end = result.length();
			}
			
			result = result.substring(0, end);
			
			if (settings.getPaddingOption())
			{
				result = result + paddChars;
			}
			
			return result;			
		}
		
	}
	
	public String generateGrid(Context context)	{
		FlurryAgent.logEvent(Config.FLURRY_EVENT_GRID_GENERATED);
		
		String result = "";
		
		// first, clear the grid
		this.gridArray = new char[28][28];
		this.gridString = null;
		
		methodD();
		
		// Populate corners with placeholder characters.
		this.gridArray[0][0] = ' ';
		this.gridArray[0][27] = ' ';
		this.gridArray[27][0] = ' ';
		this.gridArray[27][27] = ' ';
		
		// now worry about the padding bit
		for (int i = 1; i < 27; i++)
		{
			char symbol, digit;
			boolean swap;
			
			// first and last place on the y axes gets filled
			if (i < 8 || i > 19) { // pick random chars for top and bottom.
				symbol = getRandomSymbol();
				digit = getRandomDigit();
				swap = randomiser.nextBoolean();
				this.gridArray[i][0] = swap ? symbol : digit;
				this.gridArray[i][27] = swap ? digit : symbol;
			} else if (i < 18) { // top is fixed, bottom is symbol
				this.gridArray[i][27] = getRandomSymbol();
			} else { // i == 18 or i == 19 -- top is fixed, bottom is digit.
				this.gridArray[i][27] = getRandomDigit();
			}
			
			// first and last place on the x axes gets filled
			symbol = getRandomSymbol();
			digit = getRandomDigit();
			swap = randomiser.nextBoolean();
			this.gridArray[0][i] = swap ? symbol : digit;
			this.gridArray[27][i] = swap ? digit : symbol;				
		}
		
		// on the top row, the following are "hard coded"
		this.gridArray[8][0] 	= '0';
		this.gridArray[9][0] 	= '1';
		this.gridArray[10][0] 	= '2';
		this.gridArray[11][0] 	= '3';
		this.gridArray[12][0] 	= '4';
		this.gridArray[13][0] 	= '5';
		this.gridArray[14][0] 	= '6';
		this.gridArray[15][0] 	= '7';
		this.gridArray[16][0] 	= '8';
		this.gridArray[17][0] 	= '9';
		this.gridArray[18][0] 	= '-';
		this.gridArray[19][0] 	= '.';
		
		for (int y = 0; y < 28; y++)
		{
			for (int x = 0; x < 28; x++)
			{
				result += Character.toString(this.gridArray[x][y]);
			}
		}
		
		this.gridString = result;
		
		// grid has been updated, save this to the settings
		new ApplicationSettings(context).setGrid(result);
		
		return result;
	}
	
	private char getRandomSymbol()
	{
		String symbols = "!\"#$%&'()*+,/:;<=>?@[\\]^{|}~";
		
		return symbols.toCharArray()[randomiser.nextInt(symbols.length())];
	}
	
	private char getRandomDigit()
	{
		String digits = "123456890";
		
		return digits.toCharArray()[randomiser.nextInt(digits.length())];
	}
	
	private void methodD()
	{
		for (int y = 1; y <= 26; y++)
		{
			char[] rowValues = getFittingRow().toCharArray();
			
			// cycle through rows
			for (int i = 0; i < rowValues.length; i++)
			{
				this.gridArray[i+1][y] = rowValues[i];
			}
		}
	}
	
	private String getFittingRow()
	{
		String result = "";
		
		while (result == "")
		{
			this.getPossibleValuesCallCount = 0;
			result = getCumulativeCellValues("");
		}
		
		return result;
	}
	
	/***
	 * Build up a row by assessing each character and then moving to the next
	 * @param currentValues the letters that have already been decided
	 * @return the current values as well as the new one to be added
	 */
	private String getCumulativeCellValues(String currentValues)
	{
		if (currentValues.length() == 26) return currentValues;
		
		String result = "";
		this.getPossibleValuesCallCount++;
		
		if (this.getPossibleValuesCallCount > this.ROW_ITERATION_THRESHOLD)
		{
			// which results in the row just being restarted
			return result;
		}
		
		
		char[] possibleValues = getPossibleValues(currentValues);
		
		for (int i = 0; i < possibleValues.length; i++)
		{
			result = getCumulativeCellValues(currentValues + Character.toString(possibleValues[i]));
			
			if (result != null && result.length() > 0) break;
		}
		
		return result;
	}
	
	/***
	 * Work out what the possible values that can be placed
	 * @param currentValues values already in the row
	 * @return characters that can be placed in the cell
	 */
	private char[] getPossibleValues(String currentValues)
	{
		List<Character> possibleValues = new ArrayList<Character>();
		char[] currentChars = currentValues.toCharArray();
		
		String returnString = "";
		
		for (int alphabet = 97; alphabet <= 122; alphabet++)
		{
			possibleValues.add((char)alphabet);
		}
		
		// remove the current values from the possible values list
		for (int current = 0; current < currentChars.length; current++)
		{
			if (possibleValues.contains(Character.toLowerCase(currentChars[current])))
			{
				possibleValues.remove(possibleValues.indexOf(Character.toLowerCase(currentChars[current])));
			}
		}
		
		// remove any characters that appear in the column
		for (int y = 1; y < this.gridArray.length-1; y++)
		{
			if (possibleValues.contains(Character.toLowerCase(this.gridArray[currentChars.length+1][y])))
			{
				possibleValues.remove(possibleValues.indexOf(Character.toLowerCase(this.gridArray[currentChars.length+1][y])));
			}
		}
		

		// shuffle possible values
		SecureRandom randomiser = new SecureRandom();
		SecureRandom upperCase = new SecureRandom();
		
		for (int i = 0; i < possibleValues.size(); i++) // give it a good ol' shuffle
		{
			int randomPosition = randomiser.nextInt(possibleValues.size());
			// get the character at position i - randomise whether it's upper or lower case
			char tmp = possibleValues.get(i);
			// set the letter at position i to the random chosen letter upper/lower case it also randomised
			possibleValues.set(i, possibleValues.get(randomPosition));
			// finally put the letter formerly at position i in to the random position
			possibleValues.set(randomPosition, tmp);
			
			if (upperCase.nextInt() % 2 == 0)
			{
				possibleValues.set(i, Character.toUpperCase(possibleValues.get(i)));
			}
		}
		
		
		for (Character c : possibleValues)
		{
			returnString += c.toString();
		}
		
		return returnString.toCharArray();
	}
	
	/***
	 * The pattern it moves is left up left down right down...repeat
	 * @param charCount which character in the sequence we're assessing
	 * @return direction
	 */
	private directions getDirection(int charCount) {
		
		int directionMod = charCount % 6;
		
		switch (directionMod)
		{
			default:
			case 0:
				return directions.left;
			case 1:
				return directions.up;
			case 2:
				return directions.left;
			case 3:
				return directions.down;
			case 4:
				return directions.right;
			case 5: 
				return directions.down;
		}
	}
	
	/***
	 * When starting off, the direction only moves right down and up
	 * @param charCount
	 * @return
	 */
	private directions getDirectionSeed(int charCount)
	{
		int directionMod = charCount % 4;
		// 0,1,2,3
		
		switch (directionMod)
		{
			default:
			case 0:
				return directions.right;
			case 1:
				return directions.down;
			case 2:
				return directions.right;
			case 3:
				return directions.up;
		}
	}
	
	/***
	 * Scan through the grid looking for the characters passed in.
	 * @param character
	 * @param direction
	 * @param seed
	 * @return
	 */
	private String findChar(char character, directions direction, boolean seed)
	{
		String result = "";
		
		if (!Character.toString(character).trim().equals(""))
		{
			// although each of these methods could be re-factored, they have been kept separate to avoid additional code complexity
			switch (this.currentDirection)
			{
				case up:
					result = this.findUp(character, seed);
					break;
				case down:
					result = this.findDown(character, seed);
					break;
				case left:
					result = this.findLeft(character, seed);
					break;
				default:
				case right:
					result = this.findRight(character, seed);
					break;
			}
		}
		
		return result;
	}
	
	
	/***
	 * Scan in an upward direction. If the top is reached, wrap around
	 * @param character
	 * @param seed
	 * @return
	 */
	private String findUp(char character, boolean seed)
	{
		boolean found = false;
		
		while (this.currentPosition.y >= 1) // 0 based
		{
			
			if (Character.toLowerCase(this.gridArray[this.currentPosition.x][this.currentPosition.y]) == Character.toLowerCase(character))
			{
				found = true;
				break;
			}
			
			this.currentPosition.y--;
		}
		if (!found)
		{
			this.currentPosition.y = 26; // 0 based
			return findUp(character, seed);
		}
		
		String result = 
			this.getCharacterAt(new Point(this.currentPosition.x, ((26+(this.currentPosition.y - 2)) % 26)+1)) +
			this.getCharacterAt(new Point(this.currentPosition.x, ((26+(this.currentPosition.y - 3)) % 26)+1)) +
			this.getCharacterAt(new Point(this.currentPosition.x, 0));
		
		// when being seeded, the start position uses the character we're seeking. When not being seeded, the next
		// two characters in sequence are taken, so the y position is offset by 2
		if (!seed) {
			this.currentPosition.y = ((26+(this.currentPosition.y - 3)) % 26)+1;	
		}
		
		return result;
	}
	
	/***
	 * Scan in a downward direction. If the bottom is reached, wrap around
	 * @param character
	 * @param seed
	 * @return
	 */
	private String findDown(char character, boolean seed)
	{
		boolean found = false;
		
		while (this.currentPosition.y <= 26 ) // 0 based
		{
			
			if (Character.toLowerCase(this.gridArray[this.currentPosition.x][this.currentPosition.y]) == Character.toLowerCase(character))
			{
				found = true;
				break;
			}
			
			this.currentPosition.y++;
		}
		if (!found)
		{
			this.currentPosition.y = 1; // 0 based
			return findDown(character, seed);
		}
		
		String result = 
			this.getCharacterAt(new Point(this.currentPosition.x, ((26+(this.currentPosition.y)) % 26)+1)) +
			this.getCharacterAt(new Point(this.currentPosition.x, ((26+(this.currentPosition.y + 1)) % 26)+1)) +
			this.getCharacterAt(new Point(this.currentPosition.x, 27));
		
		// when being seeded, the start position uses the character we're seeking. When not being seeded, the next
		// two characters in sequence are taken, so the y position is offset by 2
		if (!seed) {
			this.currentPosition.y = ((26+(this.currentPosition.y + 1)) % 26)+1;	
		}
		
		return result;
	}
	
	/***
	 * Scan left. If the very left is reached, wrap around
	 * @param character
	 * @param seed
	 * @return
	 */
	private String findLeft(char character, boolean seed)
	{
		boolean found = false;
		
		while (this.currentPosition.x >= 1) // 0 based
		{
			
			if (Character.toLowerCase(this.gridArray[this.currentPosition.x][this.currentPosition.y]) == Character.toLowerCase(character))
			{
				found = true;
				break;
			}
			
			this.currentPosition.x--;
		}
		if (!found)
		{
			this.currentPosition.x = 26; // 0 based
			return findLeft(character, seed);
		}
		
		String result = 
			this.getCharacterAt(new Point(((26+(this.currentPosition.x - 2)) % 26)+1,this.currentPosition.y)) +
			this.getCharacterAt(new Point(((26+(this.currentPosition.x - 3)) % 26)+1, this.currentPosition.y)) +
			this.getCharacterAt(new Point(0, this.currentPosition.y));
		
		// when being seeded, the start position uses the character we're seeking. When not being seeded, the next
		// two characters in sequence are taken, so the y position is offset by 2
		if (!seed) {
			this.currentPosition.x = ((26+(this.currentPosition.x - 3)) % 26)+1;	
		}
		
		return result;
	}
	
	/***
	 * Scan right. If the very right is reached, wrap around
	 * @param character
	 * @param seed
	 * @return
	 */
	private String findRight(char character, boolean seed)
	{
		boolean found = false;
		
		while (this.currentPosition.x <= 26 ) // 0 based
		{
			
			if (Character.toLowerCase(this.gridArray[this.currentPosition.x][this.currentPosition.y]) == Character.toLowerCase(character))
			{
				found = true;
				break;
			}
			
			this.currentPosition.x++;
		}
		if (!found)
		{
			this.currentPosition.x = 1; // 0 based
			return findRight(character, seed);
		}
		
		String result = 
			this.getCharacterAt(new Point(((26+(this.currentPosition.x)) % 26)+1, this.currentPosition.y)) +
			this.getCharacterAt(new Point(((26+(this.currentPosition.x + 1)) % 26)+1, this.currentPosition.y)) +
			this.getCharacterAt(new Point(27, this.currentPosition.y));
		
		// when being seeded, the start position uses the character we're seeking. When not being seeded, the next
		// two characters in sequence are taken, so the y position is offset by 2
		if (!seed) {
			this.currentPosition.x = ((26+(this.currentPosition.x + 1)) % 26)+1;	
		}
		
		return result;
	}
	
	/***
	 * Return the characters in the position in the grid array
	 * @param position
	 * @return
	 */
	private String getCharacterAt(Point position)
	{
		return Character.toString(charAt(position));
	}
	
	/**
	 * Returns the character at a given position in the array.
	 * @param position The <code>Point</code> indicating the position within
	 *   the grid.
	 * @return The character at the specified position.
	 */
	public char charAt(Point position)
	{
		return this.gridArray[position.x][position.y];
	}
	
	/**
	 * Determines if the specified point is within the Latin Square portion of
	 * the grid (i.e., on the grid, but not on the border).
	 * @param p The <code>Point</code> to test.
	 * @return A value indicating whether the <code>position</code> is on the
	 *   Latin Square.
	 */
	private boolean isInLatinSquare(Point p)
	{
		return 1 <= p.x && p.x <= 26 &&
			   1 <= p.y && p.y <= 26;
	}

	/**
	 * Finds the position of the specified character within the Latin Square
	 * portion of the grid, scanning from the specified start position and
	 * jumping by the specified offset until the edge of the grid is reached.
	 * The start point itself is considered in the search.
	 * @param c The character to look for.
	 * @param start The <code>Point</code> at which to begin searching.
	 * @param dx The step-size in the horizontal direction.
	 * @param dy The step-size in the vertical direction.
	 * @return The <code>Point</code> at which the specified character is found,
	 *   or <code>null</code> if the edge of the grid is reached before the
	 *   specified character could be found.
	 */
	private Point find(char c, Point start, int dx, int dy)
	{
		Point pos = new Point(start.x, start.y);
		for (; isInLatinSquare(pos); pos.offset(dx, dy))
		{
			if (Util.equalsCaseInsensitive(charAt(pos), c))
			{
				return pos;
			}
		}
		return null;
	}
	
	/**
	 * Finds the position of the specified character within a row.
	 * @param c The character to look for.
	 * @param row The index of the row to search.
	 * @return The <code>Point</code> at which the specified character is found.
	 */
	private Point findInRow(char c, int row)
	{
		return find(c, new Point(1, row), 1, 0);
	}
	
	/**
	 * Finds the position of the specified character within a column.
	 * @param c The character to look for.
	 * @param col The index of the column in which to search.
	 * @return The <code>Point</code> at which the specified character is found.
	 */
	private Point findInCol(char c, int col)
	{
		return find(c, new Point(col, 1), 0, 1);
	}
	
	/**
	 * Finds the position of the specified character within the row or column
	 * containing the specified start position.
	 * @param c The character to look for.
	 * @param start The <code>Point</code> indicating the row or column to
	 *   search.
	 * @param dir A value indicating whether to search the row (true) or column
	 *   (false) containing <code>start</code>. 
	 * @return The <code>Point</code> in the same row or column as
	 *   <code>start</code> at which the character <code>c</code> may be found.
	 */
	private Point findInRowOrCol(char c, Point start, boolean dir)
	{
		return dir ? findInRow(c, start.y) : findInCol(c, start.x);
	}
	
	/**
	 * Gets the direction required to travel from one <code>Point</code> to
	 * another.
	 * @param start The <code>Point</code> to start from.
	 * @param target The <code>Point</code> to travel to.
	 * @return The <code>Point</code> indicating the direction of travel
	 *   required to find <code>target</code> starting from <code>start</code>.
	 *   This will have the form <code>Point(x, y)</code> where <code>x</code>
	 *   and <code>y</code> are both in <code>{-1, 0, 1}</code>.
	 */
	private Point getStepDirection(Point start, Point target)
	{
		return new Point(
				Util.sign(target.x - start.x),
				Util.sign(target.y - start.y));
	}
	
	/**
	 * Offset a given <code>Point</code> by a specified amount, wrapping within
	 * the Latin Square portion of the grid as necessary.
	 * @param p The <code>Point</code> to move.
	 * @param offset The <code>Point</code> indicating how much to move by.
	 */
	public void offsetWrapped(Point p, Point offset)
	{
		int x = p.x + offset.x;
		int y = p.y + offset.y;
		while (x < 1) x += 26;
		while (y < 1) y += 26;
		while (x > 26) x -= 26;
		while (y > 26) y -= 26;
		p.set(x, y);
	}
	
	/**
	 * Gets the position of the border character found by traveling from a
	 * given <code>Point</code> in a given direction.
	 * @param p The <code>Point</code> to start from.
	 * @param step The <code>Point</code> indicating the direction in which to
	 *   travel.  This <code>Point</code> must have exactly one non-zero
	 *   component.
	 * @return The <code>Point</code> on the border found by traveling from
	 *   <code>p</code> in the direction indicated by <code>step</code>.
	 * @throws IllegalArgumentException if <code>step == Point(0, 0)</code> or
	 *   <code>step</code> has two non-zero components.
	 */
	private Point findBorderPosition(Point p, Point step)
	{
		if ((step.x == 0 && step.y == 0) || (step.x != 0 && step.y != 0))
		{
			throw new IllegalArgumentException("step must have exactly one non-zero component.");
		}
		
		if (step.x < 0)
			return new Point(0, p.y);
		else if (step.x > 0)
			return new Point(27, p.y);
		else if (step.y < 0)
			return new Point(p.x, 0);
		else // step.y > 0
			return new Point(p.x, 27);
	}
	
	/**
	 * Gets the border character found by traveling from a given
	 * <code>Point</code> in a given direction.
	 * @param p The <code>Point</code> to start from.
	 * @param step The <code>Point</code> indicating the direction in which to
	 *   travel.  This <code>Point</code> must have exactly one non-zero
	 *   component.
	 * @return The character on the border found by traveling from
	 *   <code>p</code> in the direction indicated by <code>step</code>.
	 * @throws IllegalArgumentException if <code>step == Point(0, 0)</code> or
	 *   <code>step</code> has two non-zero components.
	 */
	public char getBorderChar(Point p, Point step)
	{
		return charAt(findBorderPosition(p, step));
	}
	
	/**
	 * Gets the border character in the same row or column (as indicated by the
	 * specified direction of travel) that is either a digit or symbol (as
	 * specified).
	 * @param p The <code>Point</code> indicating the row or column in which to
	 *   search.
	 * @param step The <code>Point</code> indicating the direction of travel.
	 *   This <code>Point</code> must have exactly one non-zero component.
	 * @param digit A value indicating whether to get the digit (true) or
	 *   symbolic (false) border character.
	 * @return
	 *   <ul>
	 *     <li>If <code>step.x != 0 && digit == true</code>, the digit in the
	 *       same row as <code>p</code> is returned.</li>
	 *     <li>If <code>step.y != 0 && digit == true</code>, the digit in the
	 *       same column as <code>p</code> is returned.</li>
	 *     <li>If <code>step.x != 0 && digit == false</code>, the symbol in the
	 *       same row as <code>p</code> is returned.</li>
	 *     <li>If <code>step.y != 0 && digit == false</code>, the symbol in the
	 *       same column as <code>p</code> is returned.</li>
	 *   </ul>
	 * @throws IllegalArgumentException if <code>step == Point(0, 0)</code> or
	 *   <code>step</code> has two non-zero components.
	 */
	public char getTypedBorderChar(Point p, Point step, boolean digit)
	{
		char c = getBorderChar(p, step);
		if (Character.isDigit(c) != digit)
		{
			c = getBorderChar(p, new Point(-step.x, -step.y));
		}
		return c;
	}
	
	/**
	 * Finds the position at which to start extracting the password.  This is
	 * determined by alternately scanning horizontally then vertically for each
	 * character in the provided key, using the algorithm described at
	 * <a href="https://www.grc.com/otg/operation.htm">grc.com</a>.
	 * @param key The array of characters used to obtain the password.  The
	 *   array must consist of letters only (any processing to convert other
	 *   characters to letters must be done prior to calling this method).
	 * @param row The index of the row from which to begin. 
	 * @return The <code>Point</code> at which to begin scanning for password
	 *   characters.
	 */
	private Point findSeedPosition(char[] key, int row)
	{
		Point pos = new Point(0, row);
		Point step = new Point(0, 1);
		boolean dir = true;
		for (char c : key)
		{
			// If the character is at the current position, take one more step
			// the direction we had been traveling previously.
			if (Util.equalsCaseInsensitive(charAt(pos), c))
			{
				offsetWrapped(pos, step);
			}
			
			Point next = findInRowOrCol(c, pos, dir);
			step = getStepDirection(pos, next);
			pos = next;
			dir = !dir;
		}
		
		// If we end up on a square that is the first character of the key, then
		// take one more step so that we don't start on that square for the
		// password phase.
		if (Util.equalsCaseInsensitive(charAt(pos), key[0]))
		{
			offsetWrapped(pos, step);
		}
		return pos;
	}

	/**
	 * Extracts the password from the grid starting from the specified point.
	 * @param key The array of characters used to obtain the password.  The
	 *   array must consist of letters only (any processing to convert other
	 *   characters to letters must be done prior to calling this method).
	 * @param start The <code>Point</code> at which to start extracting the
	 *   password.
	 * @param strategy The <code>PasswordStrategy</code> to use to extract
	 *   password characters and move the cursor after finding the next key
	 *   character.
	 * @return The extracted password.
	 * @throws IllegalArgumentException If the character at the start point is
	 *   equal to <code>key[0]</code> (because if this is the case, we don't
	 *   know whether we should go left or right to extract the first set of
	 *   password characters).
	 */
	private String getPasswordFrom(char[] key, Point start, PasswordStrategy strategy)
	{
		// Check that we are not starting at a position containing the first key
		// character.
		if (charAt(start) == key[0])
		{
			throw new IllegalArgumentException("Cannot start on the first character of the key.");
		}

		// Initialization
		Point pos = new Point(start.x, start.y);
		Point step = new Point(0, 0);
		StringBuilder password = new StringBuilder();
		boolean dir = true;
		
		for (char c : key)
		{
			// If the last step left us on a grid position containing the next
			// key character, then take one more step in the last direction of
			// travel.
			if (Util.equalsCaseInsensitive(charAt(pos), c))
			{
				offsetWrapped(pos, step);
			}
			
			// Find the next key character in the current row or column.
			Point next = findInRowOrCol(c, pos, dir);
			
			// Determine whether we are going up, down, left, or right to get to
			// that character.
			step = getStepDirection(pos, next);
			
			// Use the provided strategy to gather the password characters and
			// move the cursor to the starting position for the next round.
			strategy.apply(this, next, step, password);
			
			pos = next;		// Update current position.
			dir = !dir;		// Switch directions (row <-> col).
		}
		
		return password.toString();
	}
	
	/**
	 * Extracts the password from the grid starting from the specified row.
	 * @param key The array of characters used to obtain the password.  The
	 *   array must consist of letters only (any processing to convert other
	 *   characters to letters must be done prior to calling this method).
	 * @param row The index of the row from which to begin. 
	 * @param strategy The <code>PasswordStrategy</code> to use to extract
	 *   password characters and move the cursor after finding the next key
	 *   character.
	 * @return The extracted password.
	 */
	private String getPassword(char[] key, int row, PasswordStrategy strategy)
	{
		Point start = findSeedPosition(key, row);
		return getPasswordFrom(key, start, strategy);
	}
	
	/**
	 * Gets the key to use based on the provided string.
	 * @param s The string to use to generate the key.
	 * @param length The length of the key.
	 * @return An array of characters, with non-alphabetic characters mapped to
	 *   alphabetic characters as described at
	 *   <a href="https://www.grc.com/otg/operation.htm">grc.com</a>.  If
	 *   <code>s.length() &lt; length</code>, it will first be padded with
	 *   dashes.  This is not specified at grc.com, but is an arbitrary choice.
	 *   The official algorithm uses <code>length == 6</code> and expects that
	 *   the string (a URL) will always be at least this long.  If
	 *   <code>s.length() &gt; length</code>, the first <code>length</code>
	 *   characters are used to determine the key.
	 */
	private char[] getKeyFromString(String s, int length)
	{
		StringBuilder keyStr = new StringBuilder(s);
		for (int i = 0, n = length - s.length(); i < n; i++)
		{
			keyStr.append('-');
		}
		
		return replaceNumberDashDot(
				keyStr.substring(0, length)).toCharArray();
	}
	
}
