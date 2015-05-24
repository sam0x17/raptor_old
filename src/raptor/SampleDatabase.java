package raptor;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

//TODO: use index at beginning of file for fast random access
//TODO: implement read and write and other functions

public class SampleDatabase
{
	private String path;
	private RandomAccessFile file = null;
	private boolean readonly;
	private static int static_bits = 0;
	private static boolean static_bits_init = false;
	
	public SampleDatabase(String path, boolean readonly) throws IOException
	{
		this.path = path;
		this.readonly = readonly;
		String access_mode;
		if(readonly) access_mode = "r";
		else access_mode = "rw";
		new File(path).createNewFile();
		file = new RandomAccessFile(path, access_mode);
		init_bits();
	}
	
	public Instance2 get(long i) throws IOException
	{
		Instance2 instance = new Instance2();
		file.seek(0);
		instance.rx = file.readDouble();
		instance.ry = file.readDouble();
		instance.rz = file.readDouble();
		instance.true_dist = file.readDouble();
		
		return instance;
	}
	
	public static void init_bits() {
		if(static_bits_init) return;
		static_bits = 0;
		static_bits += 8 * 2; // id string (8 sequential chars
		static_bits += 4 * Double.SIZE; // rx, ry, rz, true_dist
		static_bits += 4 * Double.SIZE; // cov2d
		static_bits += 27 * Double.SIZE; // reference distances
		static_bits_init = true;
	}
	
	public void add(Instance instance)
	{
		if(readonly) throw new RuntimeException("can only add new training instances when in write mode!");

	}
}
