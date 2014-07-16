/**
 * Contains some basic string manipulation and file system functions
 * that are used throughout the RAPTOR system
 */
package raptor.engine;

/**
 * @author Sam Kelly
 */
public final class IO
{
	public static double doubleFromPython(String n) throws NumberFormatException
	{
		if(n.contains(".")) return Double.parseDouble(n);
		else return Integer.parseInt(n);
	}
}
