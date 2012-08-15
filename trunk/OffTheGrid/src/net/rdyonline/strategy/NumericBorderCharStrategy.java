package net.rdyonline.strategy;

/**
 * Extracts the digit in the same row or column as the cursor.  Whether to
 * search the current row or the current column is determined by the
 * direction of travel.  The cursor is not moved.
 */
public final class NumericBorderCharStrategy extends TypedBorderCharStrategy
{
	/** Creates a new <code>NumericBorderCharStrategy</code>. */
	public NumericBorderCharStrategy()
	{
		super(true);
	}
}