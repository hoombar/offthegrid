package net.rdyonline.strategy;

import java.util.ArrayList;
import java.util.List;

import net.rdyonline.Grid;
import android.graphics.Point;

/**
 * Extracts password characters and updates the cursor using multiple
 * strategies in sequence.
 */
public class CompositePasswordStrategy implements PasswordStrategy
{
	
	/** The <code>List</code> of <code>PasswordStrategies</code> to use. */
	private final List<PasswordStrategy> inner = new ArrayList<PasswordStrategy>();
	
	/**
	 * Adds a <code>PasswordStrategy</code> to the end of the sequence.
	 * @param strategy The <code>PasswordStrategy</code> to use.
	 * @return This <code>CompositePasswordStrategy</code>.
	 */
	public CompositePasswordStrategy add(PasswordStrategy strategy)
	{
		inner.add(strategy);
		return this;
	}
	
	/* (non-Javadoc)
	 * @see net.rdyonline.Grid.PasswordStrategy#apply(android.graphics.Point, android.graphics.Point, java.lang.StringBuilder)
	 */
	public void apply(Grid grid, Point pos, Point step, StringBuilder password)
	{
		for (PasswordStrategy strategy : inner)
		{
			strategy.apply(grid, pos, step, password);
		}
	}
	
}