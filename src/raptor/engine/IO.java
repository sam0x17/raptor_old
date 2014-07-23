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
	
	public static String indentString(String st, int num_indents, String indent_char)
	{
		String ret = "";
		for(String line : st.split("\n"))
		{
			for(int i = 0; i < num_indents; i++)
				ret += indent_char;
			ret += line + "\n";
		}
		return ret;
	}
}
