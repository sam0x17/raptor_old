package raptor;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;

import org.jblas.util.Random;

public class CombineData {

	public static void main(String[] args) throws IOException {
		File dest_folder = new File(args[0]);
		File source_folders[] = new File[args.length - 1];
		if(!dest_folder.isDirectory())
			throw new IllegalArgumentException("dest folder must be a directory!");
		for(int i = 1; i < args.length; i++) {
			File src = new File(args[i]);
			if(!src.isDirectory())
				throw new IllegalArgumentException("source folders must be directories!");
			source_folders[i - 1] = src;
		}
		System.out.println("getting list of unique files...");
		ArrayList<Sample> original_samples = new ArrayList<Sample>();
		for(File source_folder : source_folders) {
			File[] files = source_folder.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name)
				{
					String lowercaseName = name.toLowerCase();
					if(lowercaseName.endsWith(".txt")) return true;
					else return false;
				}
			});
			for(File annotation_file : files) {
				Sample samp = new Sample();
				samp.prefix = annotation_file.getName().substring(0, annotation_file.getName().length() - 4);
				samp.parent_directory = annotation_file.getParentFile().getAbsolutePath();
				original_samples.add(samp);
			}
		}
		System.out.println("Found " + original_samples.size() + " unique samples.");
		HashSet<String> taken_prefixes = new HashSet<String>();
		for(Sample sample : original_samples) {
			String new_prefix = null;
			while(new_prefix == null || taken_prefixes.contains(new_prefix))
				new_prefix = randomString(10);
			taken_prefixes.add(new_prefix);
			File original_image_file = new File(sample.parent_directory + "/" + sample.prefix + ".png");
			File original_annotation_file = new File(sample.parent_directory + "/" + sample.prefix + ".txt");
			if(!original_image_file.exists())
				throw new RuntimeException("MISSING IMAGE FILE: " + original_image_file.getPath());
			if(!original_annotation_file.exists())
				throw new RuntimeException("MISSING ANNOTATION FILE: " + original_annotation_file.getPath());
			File dest_image = new File(dest_folder.getAbsolutePath() + "/" + new_prefix + ".png");
			File dest_annotation = new File(dest_folder.getAbsolutePath() + "/" + new_prefix + ".txt");
			Files.copy(original_image_file.toPath(), dest_image.toPath());
			Files.copy(original_annotation_file.toPath(), dest_annotation.toPath());
		}
	}
	
	static final String AB = "0123456789abcdefghijklmnopqrstuvwxyz";
	static Random rnd = new Random();

	public static String randomString( int len ) 
	{
	   StringBuilder sb = new StringBuilder( len );
	   for( int i = 0; i < len; i++ ) 
	      sb.append( AB.charAt( rnd.nextInt(AB.length()) ) );
	   return sb.toString();
	}
	
	public static class Sample {
		public String prefix;
		public String parent_directory;
	}

}
