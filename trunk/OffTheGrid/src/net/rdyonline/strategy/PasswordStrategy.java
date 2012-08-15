/**
 * 
 */
package net.rdyonline.strategy;

import net.rdyonline.Grid;
import android.graphics.Point;

/**
 * Represents a technique for extracting passwords from the grid and
 * potentially moving the grid cursor after finding the next key character.
 */
public interface PasswordStrategy
{
	
	/**
	 * Extracts the next set of password characters and potentially moves
	 * the provided grid cursor.
	 * @param grid The <code>Grid</code> to extract password characters from.
	 * @param pos The <code>Point</code> at which the next key character was
	 *   found.  This <code>Point</code> may be modified.
	 * @param step The <code>Point</code> indicating the direction that was
	 *   traveled (up, down, left, right) in order to find the key
	 *   character.  This <code>Point</code> must have exactly one non-zero
	 *   component with absolute value <code>1</code>.
	 * @param password The <code>StringBuilder</code> used to assemble the
	 *   password.
	 */
	void apply(Grid grid, Point pos, Point step, StringBuilder password);
	
}

