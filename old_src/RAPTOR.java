
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import java.util.Scanner;

import javax.imageio.ImageIO;

import org.encog.engine.network.activation.ActivationSigmoid;
import org.encog.engine.network.activation.ActivationTANH;
import org.encog.engine.network.activation.ActivationFunction;
import org.encog.ml.data.MLData;
import org.encog.ml.data.MLDataPair;
import org.encog.ml.data.MLDataSet;
import org.encog.ml.data.basic.BasicMLDataSet;
import org.encog.ml.train.MLTrain;
import org.encog.neural.flat.FlatLayer;
import org.encog.neural.flat.FlatNetwork;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.ContainsFlat;
import org.encog.neural.networks.layers.BasicLayer;
import org.encog.neural.networks.training.propagation.back.Backpropagation;
import org.encog.neural.networks.training.propagation.resilient.ResilientPropagation;
import org.encog.ml.data.MLData;
import org.imgscalr.Scalr;

public class RAPTOR
{

	public static double xor_input[][] = {{0.0, 0.0},
										  {1.0, 0.0},
										  {0.0, 1.0},
										  {1.0, 1.0}};
	
	public static double xor_output[][] = {{0.0}, {1.0}, {1.0}, {0.0}};
	public static int input_size, output_size;
	
	public static Random rand = new Random();
	
	private static double rot_max = 1.0;
	
	public static void main(String[] args) throws IOException
	{
		
		RaptorData raw_data = loadData("/home/skelly/RAPTOR/DATA", 0.80);
		
		TrainingData train_data = new TrainingData(raw_data.train_input, raw_data.train_output);
		BasicMLDataSet validation_set = new BasicMLDataSet(raw_data.validation_input, raw_data.validation_output);
		BasicMLDataSet training_set = new BasicMLDataSet(train_data.input, train_data.output);
		BasicNetwork network = new BasicNetwork();
		network.addLayer(new BasicLayer(null, false, train_data.input[0].length));
		network.addLayer(new BasicLayer(new ActivationTANH(), true, 65));
		//network.addLayer(new BasicLayer(new ActivationTANH(), true, 25));
		//network.addLayer(new BasicLayer(new ActivationTANH(), true, 25));
		network.addLayer(new BasicLayer(new ActivationTANH(), true, train_data.output[0].length));
		network.getStructure().finalizeStructure();
		network.reset();

		MLTrain train = new ResilientPropagation(network, training_set);
		

		
	
		int epoch = 0;
		do {
			train.iteration();
			epoch++;
			double avg_correct_percent = 0.0;
			double avg_diff = 0.0;
			double max_diff = Double.MIN_VALUE;
			double min_correct_percent = Double.MAX_VALUE;
			
			for(MLDataPair vdata : validation_set)
			{
				MLData output = network.compute(vdata.getInput());
				double dist_ideal = vdata.getIdealArray()[0] * 70.0;
				double dist_actual = output.getData()[0] * 70.0;
				double correct_percent = Math.max(0.0, (Math.min(dist_ideal, dist_actual) / Math.max(dist_ideal, dist_actual)));
				
				avg_correct_percent += correct_percent;
				if(correct_percent < min_correct_percent) min_correct_percent = correct_percent;
				double diff = Math.abs(dist_ideal - dist_actual);
				avg_diff += diff;
				if(diff > max_diff) max_diff = diff;
			}
			avg_correct_percent = avg_correct_percent / (double)validation_set.size();
			avg_diff = avg_diff / (double)validation_set.size();
			System.out.println("Epoch #"
					+ epoch
					+ " Error:"
					+ train.getError());
			System.out.println("AVG % ACCURACY: " + avg_correct_percent);
			System.out.println("MIN % ACCURACY: " + min_correct_percent);
			System.out.println("      AVG DIFF: " + avg_diff);
			System.out.println("      MAX DIFF: " + max_diff);
			System.out.println("==========================================");
			
			/*System.out.println("Epoch #"
					+ epoch
					+ " Error:"
					+ train.getError());*/
			
		} while(true);
		
		
	}
	
	public static RaptorData loadData(String path, double train_percent) throws IOException
	{
		File folder = new File(path);
		if(!folder.isDirectory())
			throw new IllegalArgumentException("path must be a directory!");
		
		System.out.println("LOADING DATA FROM: " + path);
		File[] files = folder.listFiles(new FilenameFilter()
		{
			public boolean accept(File dir, String name)
			{
				String lowercaseName = name.toLowerCase();
				if(lowercaseName.endsWith(".txt")) return true;
				else return false;
			}
		});
		System.out.println("Sorting...");
		Arrays.sort(files,new Comparator<File>() { public int compare(File a, File b) { return a.getName().compareTo(b.getName()); } });
		RaptorData data = new RaptorData();
		
		int img_size = 30;


		System.out.println("allocating memory...");
		int max_files = 100000;
		if(max_files > files.length) max_files = files.length;
		int num_train = (int)(max_files * train_percent);
		int num_valid = max_files - num_train;
		data.train_input = new double[num_train][img_size * img_size + 1];
		data.train_output = new double[num_train][1];
		data.validation_input = new double[num_valid][img_size * img_size + 1];
		data.validation_output = new double[num_valid][1];

		int file_num = 0;
		for(final File file : files)
		{
			String basename_print = file.getName().substring(0, file.getName().length() - 4);
			String basename = file.getAbsolutePath().substring(0, file.getAbsolutePath().length() - 4);
			BufferedImage img = null;
			try
			{
				img = ImageIO.read(new File(basename + ".png"));
				System.out.println("processing " + basename_print);
			} catch (Exception e) {
				//e.printStackTrace();
				file_num++;
				continue;
			}
			img = resize(img, img_size, img_size);
			img = as_grayscale(img);
			if(file_num % 1000 == 0) ImageIO.write(img, "png", new File("test_" + file_num + ".png"));
			for(int x = 0; x < img.getWidth(); x++)
			{
				for(int y = 0; y < img.getHeight(); y++)
				{
					int num = x * img.getWidth() + y;
					Color c = new Color(img.getRGB(x, y));
					double val = c.getBlue() / 255.0;

					if(file_num < num_train) data.train_input[file_num][num] = val;
					else data.validation_input[file_num - data.train_input.length][num] = val;
				}
			}
			img.flush();
			Scanner sc = new Scanner(new File(basename + ".txt"));
			double true_dist = Double.parseDouble(sc.nextLine());
			double rx = Double.parseDouble(sc.nextLine());
			double ry = Double.parseDouble(sc.nextLine());
			double rz = Double.parseDouble(sc.nextLine());
			double scale = Double.parseDouble(sc.nextLine());
			if(file_num < num_train)
			{
				data.train_input[file_num][data.train_input[0].length - 1] = scale / 7.0;
				//data.train_input[file_num][data.train_input[0].length - 1] = true_dist / 70.0;
				/*data.train_output[file_num][0] = rx / rot_max;
				data.train_output[file_num][1] = ry / rot_max;
				data.train_output[file_num][2] = rz / rot_max;*/
				data.train_output[file_num][0] = true_dist / 70.0;
			} else {
				data.validation_input[file_num - data.train_input.length][data.validation_input[0].length - 1] = scale / 7.0;
				//data.validation_input[file_num - data.train_output.length][data.validation_input[0].length - 1] = true_dist / 70.0;
				/*data.validation_output[file_num - data.train_output.length][0] = rx / rot_max;
				data.validation_output[file_num - data.train_output.length][1] = ry / rot_max;
				data.validation_output[file_num - data.train_output.length][2] = rz / rot_max;*/
				data.validation_output[file_num - data.train_output.length][0] = true_dist / 70.0;
			}

			sc.close();
			/*
			System.out.println("scale:\t" + data.input[file_num][data.input[0].length - 1]);
			System.out.println("rx:\t" + data.output[file_num][0]);
			System.out.println("ry:\t" + data.output[file_num][1]);
			System.out.println("rz:\t" + data.output[file_num][2]);
			System.out.println("tdist:\t" + data.output[file_num][3]);
			System.out.println();*/
			
			
			file_num++;
			if(file_num > max_files - 1) break;

		}
		return data;
	}
	
	private static BufferedImage as_grayscale(BufferedImage img)
	{
		BufferedImage ret = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
		Graphics2D g = ret.createGraphics();
		g.drawImage(img, 0, 0, img.getWidth(), img.getHeight(), 0, 0, img.getWidth(), img.getHeight(), null);
		g.dispose();
		return ret;
	}
	
	private static BufferedImage resize(BufferedImage img, int dest_width, int dest_height)
	{
		// smoother if you do it this way 
		BufferedImage resize = Scalr.resize(img, dest_width, dest_height, Scalr.OP_ANTIALIAS);
		BufferedImage dimg = new BufferedImage(dest_width, dest_height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = dimg.createGraphics();
		g.setComposite(AlphaComposite.SrcAtop);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
				RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING,
				RenderingHints.VALUE_COLOR_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_RENDERING,
				RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
				RenderingHints.VALUE_STROKE_PURE);
		//g.drawImage(img,0,0,dest_width,dest_height,0,0,img.getWidth(),img.getHeight(),null);
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, dest_width, dest_height);
		g.drawImage(resize, 0, 0, dest_width, dest_height, 0, 0, dest_width, dest_height, null);
		g.dispose();
		return dimg;  
		//return resize;
	} 
	
	public static class RaptorData
	{
		public double[][] train_input = null;
		public double[][] train_output = null;
		public double validation_input[][];
		public double validation_output[][];
	}
}