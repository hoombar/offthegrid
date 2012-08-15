package net.rdyonline.strategy;

import net.rdyonline.Grid;
import android.graphics.Point;

/**
 * Extracts a given number of password characters by stepping in the
 * last direction of travel.  The grid cursor is moved along.
 */
public class ScanForwardStrategy implements PasswordStrategy
{
	
	/** Number of characters to extract. */
	private final int charsPerKey;
	
	/**
	 * Creates a new <code>ScanForwardStrategy</code>.
	 * @param charsPerKey The number of characters to extract.
	 */
	public ScanForwardStrategy(int charsPerKey)
	{
		this.charsPerKey = charsPerKey;
	}
	
	/* (non-Javadoc)
	 * @see net.rdyonline.Grid.PasswordStrategy#apply(android.graphics.Point, android.graphics.Point, java.lang.StringBuilder)
	 */
	public void apply(Grid grid, Point pos, Point step, StringBuilder password)
	{
		for (int i = 0; i < charsPerKey; i++)
		{
			grid.offsetWrapped(pos, step);
			password.append(grid.charAt(pos));
		}
	}
	
}