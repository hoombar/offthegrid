package net.rdyonline.strategy;

/**
 * Extracts the symbol in the same row or column as the cursor.  Whether to
 * search the current row or the current column is determined by the
 * direction of travel.  The cursor is not moved.
 */
public final class SymbolicBorderCharStrategy extends TypedBorderCharStrategy
{
	/** Creates a new <code>SymbolicBorderCharStrategy</code>. */
	public SymbolicBorderCharStrategy()
	{
		super(false);
	}
}