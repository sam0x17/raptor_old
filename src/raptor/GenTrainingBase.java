/**
 * Base class for post-processing process that is called by render.py after rendering a series
 * of training frames
 */
package raptor;

import static raptor.engine.Image.*;
import static raptor.engine.IO.*;
import static raptor.engine.Math.*;

import org.jblas.*;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.PrintWriter;

import javax.imageio.ImageIO;

/**
 * @author Sam Kelly
 *
 */
public abstract class GenTrainingBase
{
	public BufferedImage orig_img = null;
	public String orig_img_path = null;
	public String annotation_path = null;
	public int dest_width, dest_height;
	public double rx, ry, rz;
	public double true_dist;
	public DoubleMatrix cov3d = null;
	public PrintWriter writer = null;
	public boolean verbose = true;
	
	public boolean cov3d_on = true;
	
	public Rectangle crop_bounds = null;
	public double crop_factor[] = new double[]{0.0, 0.0, 0.0, 0.0};
	private boolean auto_crop_on = false;
	
	public Rectangle resize_bounds = null;
	public double resize_factor_x = 0.0;
	public double resize_factor_y = 0.0;
	private boolean auto_resize_on = false;
	
	PixelGrid orig_img_grid = null;
	public DoubleMatrix cov2d = null;
	private boolean cov2d_on = false;
	
	public void println_verbose(String msg)
	{
		if(verbose) System.out.println(msg);
	}
	
	public void println_verbose()
	{
		if(verbose) System.out.println();
	}
	
	public void print_verbose(String msg)
	{
		if(verbose) System.out.print(msg);
	}
	
	public int getNumValidArgs()
	{
		return 17;
	}
	
	public final void run(String[] args)
	{
		if(!processArgs(args)) return;
		postProcessing();
		saveAnnotations();
		if(writer != null) writer.close();
	}
	
	public boolean processArgs(String args[])
	{
		try
		{
			if(args.length != getNumValidArgs())
				throw new IllegalArgumentException("invalid number of inputs (was "
			+ args.length + ", should be " + getNumValidArgs() + ")");
			int arg_num = 2;
			
			// load destination image width and height
			dest_width = Integer.parseInt(args[arg_num++]);
			dest_height = Integer.parseInt(args[arg_num++]);
			
			// load true distance of model from camera
			true_dist = doubleFromPython(args[arg_num++]);
			
			// load euler rotation angle of model
			// from default resting position
			rx = doubleFromPython(args[arg_num++]);
			ry = doubleFromPython(args[arg_num++]);
			rz = doubleFromPython(args[arg_num++]);
			
			if(cov3d_on)
			{
				// load 3D covariance matrix of model pose
				double cov3d_tmp[][] = new double[3][3];
				for(int r = 0; r < 3; r++)
					for(int c = 0; c < 3; c++)
						cov3d_tmp[r][c] = doubleFromPython(args[arg_num++]);
				cov3d = new DoubleMatrix(cov3d_tmp);
			}
			
			// load file paths
			orig_img_path = args[0];
			annotation_path = args[1];
			
			// load original image
			orig_img = ImageIO.read(new File(orig_img_path));
			
			// set up print writer for writing annotation file
			writer = new PrintWriter(new File(annotation_path));
			
			println_verbose("RAPTOR Training Generator Loaded");
			println_verbose();
			println_verbose("============================ INPUT PROCESSED SUCCESSFULLY ============================");
			println_verbose("     image_path: " + args[0]);
			println_verbose("annotation_path: " + args[1]);
			println_verbose("    orientation: (" + rx + ", " + ry + ", " + rz + ")");
			println_verbose("      true_dist: " + true_dist);
			println_verbose("     dest_width: " + dest_width);
			println_verbose("    dest_height: " + dest_height);
			println_verbose("          cov3d: " + indentString(matrix2String(cov3d), 17, " ").substring(17));
			println_verbose("======================================================================================");
			println_verbose();
			
		} catch (Exception e) {
			System.out.println("usage: java annotate_image /path/to/input/image /path/to/output dest_width dest_height true_distance rx ry rz cov00 cov01 cov02 cov03 cov04 cov05 cov06 cov07 cov08");
			System.out.println("error that occured: " + e.getClass().getName() + " " + e.getMessage());
			e.printStackTrace();
			if(writer != null) writer.close();
			return false;
		}
		return true;
	}
	
	public void autoCrop()
	{
		auto_crop_on = true;
		println_verbose();
		println_verbose("auto_crop: [ON]");
		println_verbose("original dimensions: " + orig_img.getWidth() + "\t" + orig_img.getHeight());
		crop_bounds = getSmartCropBounds(orig_img, orig_img.getRGB(0, 0));
		crop_factor[0] = (double)crop_bounds.x / (double)orig_img.getWidth();
		crop_factor[1] = (double)crop_bounds.y / (double)orig_img.getHeight();
		crop_factor[2] = (double)(orig_img.getWidth() - (crop_bounds.x + crop_bounds.width)) / (double)orig_img.getWidth();
		crop_factor[3] = (double)(orig_img.getHeight() - (crop_bounds.y + crop_bounds.height)) / (double)orig_img.getHeight();
		orig_img = cropImage(orig_img, crop_bounds);
	}
	
	public void autoResize()
	{
		auto_resize_on = true;
		resize_bounds = getSmartResizeBounds(orig_img.getWidth(), orig_img.getHeight(), dest_width, dest_height);
		resize_factor_x = resize_bounds.width / (double)orig_img.getWidth();
		resize_factor_y = resize_bounds.height / (double)orig_img.getHeight();
		orig_img = resize(orig_img, resize_bounds);
	}
	
	public void genImageCovarianceMatrix(double threshold)
	{
		cov2d_on = true;
		PixelGrid orig_img_grid = normalizeImage(orig_img);
		DoubleMatrix image_data = imageAs2DPointCloud(orig_img_grid, threshold);
		cov2d = getCovarianceMatrix(image_data);
		
	}
	
	public abstract void postProcessing();
	
	public void saveAnnotations()
	{
		writer.println(true_dist);
		writer.println(rx + "\t" + ry + "\t" + rz);
		
		if(auto_crop_on)
			writer.println(crop_factor[0] + "\t" + crop_factor[1] + "\t" + crop_factor[2] + "\t" + crop_factor[3]);
		
		if(auto_resize_on)
			writer.println(resize_factor_x + "\t" + resize_factor_y);
		
		if(cov3d_on)
		{
			double cov3d_tmp[][] = cov3d.toArray2();
			for(int r = 0; r < cov3d_tmp.length; r++)
			{
				for(int c = 0; c < cov3d_tmp[0].length; c++)
				{
					writer.print(cov3d_tmp[r][c]);
					if(c + 1 < cov3d_tmp[0].length) writer.print("\t");
				}
				writer.println();
			}
		}
		
		if(cov2d_on)
		{
			double cov2d_tmp[][] = cov2d.toArray2();
			for(int r = 0; r < cov2d_tmp.length; r++)
			{
				for(int c = 0; c < cov2d_tmp[0].length; c++)
				{
					writer.print(cov2d_tmp[r][c]);
					if(c + 1 < cov2d_tmp[0].length) writer.print("\t");
				}
				writer.println();
			}
		}
	}
	
	
}
