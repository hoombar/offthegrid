package net.rdyonline;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import com.flurry.android.FlurryAgent;


import android.content.Context;
import android.graphics.Point;

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
	
	public String generateGrid(Context context)	{
		FlurryAgent.logEvent(Config.FLURRY_EVENT_GRID_GENERATED);
		
		String result = "";
		
		// first, clear the grid
		this.gridArray = new char[28][28];
		this.gridString = null;
		
		methodD();
		
		// now worry about the padding bit
		for (int i = 0; i < 28; i++)
		{
			if (i == 0 || i == 27)
			{
				// first or last row, fill every char except the first and last
				for (int j = 0; j < 28; j++)
				{
					if (j == 0 || j == 27)
					{
						this.gridArray[i][j] = Character.valueOf(' ');
					}
					else
					{
						// get random padding char
						this.gridArray[i][j] = getRandomBorderCharacter();
					}
				}
			}
			else
			{
				// first and last place on the y axes gets filled
				this.gridArray[i][0] = getRandomBorderCharacter();
				this.gridArray[i][27] = getRandomBorderCharacter();
			}
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
	
	private char getRandomBorderCharacter()
	{
		String paddingCharacters = "!\"#$%&'()*+,/:;<=>?@[\\]^{|}~0123456789";	// removed dot, dash, underscore, and back apostrophe
		
		SecureRandom randomiser = new SecureRandom();
		
		return paddingCharacters.toCharArray()[randomiser.nextInt(paddingCharacters.length())];
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
		return Character.toString(this.gridArray[position.x][position.y]);
	}
}
