package net.rdyonline.strategy;

import net.rdyonline.Grid;
import android.graphics.Point;

/**
 * Extracts the border character found by traveling to the edge of the grid
 * in the last direction of travel.  The cursor is not moved.
 */
public class BorderCharStrategy implements PasswordStrategy
{
	/* (non-Javadoc)
	 * @see net.rdyonline.Grid.PasswordStrategy#apply(android.graphics.Point, android.graphics.Point, java.lang.StringBuilder)
	 */
	public void apply(Grid grid, Point pos, Point step, StringBuilder password)
	{
		password.append(grid.getBorderChar(pos, step));
	}
}