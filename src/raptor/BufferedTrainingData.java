package raptor;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

//TODO: use index at beginning of file for fast random access
//TODO: implement read and write and other functions

public class BufferedTrainingData
{
	private String path;
	private RandomAccessFile file = null;
	private boolean readonly;
	
	public BufferedTrainingData(String path, boolean readonly) throws IOException
	{
		this.path = path;
		this.readonly = readonly;
		String access_mode;
		if(readonly) access_mode = "r";
		else access_mode = "rw";
		new File(path).createNewFile();
		file = new RandomAccessFile(path, access_mode);
	}
	
	public Instance get(long i) throws IOException
	{
		Instance instance = new Instance();
		file.seek(0);
	
		return instance;
	}
	
	public void add(Instance instance)
	{
		if(readonly) throw new RuntimeException("can only add new training instances when in write mode!");
		long static_bits = 0;
		static_bits += 5; // autocrop_on autoresize_on cov2d_on cov3d_on altimg_on
		static_bits += Double.SIZE * 10;
		static_bits += Integer.SIZE * 4;
		if(instance.cov2d != null) static_bits += instance.cov2d.length * Double.SIZE;
		if(instance.cov3d != null) static_bits += instance.cov2d.length * Double.SIZE;
	}
}
