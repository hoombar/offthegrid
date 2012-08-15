package net.rdyonline;

/**
 * Static utility methods.
 */
public final class Util {
	
	/**
	 * Compares two characters case insensitively.
	 * @param a The first character
	 * @param b The second character
	 * @return A value indicating whether the characters represent the same
	 *   letter or are identical.
	 */
	public static boolean equalsCaseInsensitive(char a, char b)
	{
		return Character.toLowerCase(a) == Character.toLowerCase(b);
	}

	/**
	 * Gets a value representing the sign of an <code>int</code>.
	 * @param x The <code>int</code> to get the sign of.
	 * @return <code>1</code> if <code>x &gt; 0</code>, <code>-1</code> if
	 *   <code>x &lt; 0</code>, or <code>0</code> if <code>x == 0</code>.
	 */
	public static int sign(int x)
	{
		return x > 0 ? 1 : (x < 0 ? -1 : 0);
	}

	/** Constructor is private so that instances may not be created. */
	private Util() {}
	
}
