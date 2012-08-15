package net.rdyonline.strategy;

import net.rdyonline.Grid;
import android.graphics.Point;

/**
 * Extracts the digit or symbol (as specified) in the same row or column
 * as the cursor.  Whether to search the current row or the current column
 * is determined by the direction of travel.  The cursor is not moved.
 */
public abstract class TypedBorderCharStrategy implements PasswordStrategy
{
	
	/** A value indicating whether to extract a digit. */
	private final boolean digit;
	
	/**
	 * Creates a new <code>TypedBorderCharStrategy</code>.
	 * @param digit A value indicating whether to extract a digit.
	 */
	protected TypedBorderCharStrategy(boolean digit)
	{
		this.digit = digit;
	}

	/* (non-Javadoc)
	 * @see net.rdyonline.Grid.PasswordStrategy#apply(android.graphics.Point, android.graphics.Point, java.lang.StringBuilder)
	 */
	public final void apply(Grid grid, Point pos, Point step, StringBuilder password)
	{
		password.append(grid.getTypedBorderChar(pos, step, digit));
	}
	
}