import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;

import org.imgscalr.Scalr;

import javax.imageio.ImageIO;

/**
 * 
 * @author Sam Kelly
 * @version June 2014
 * 
 * The prepare_image class is used to generate ground-truth annotations for already-rendered
 * input/training/testing data for use with RAPTOR and to normalize images for use with RAPTOR.
 * The original image specified will be replaced with a new image that is cropped such that
 * only the foreground image is shown (i.e. translucent PNGS will be cropped such that the crop
 * area becomes a bounding box around the non-transparent content of the image, and the same
 * effect will be produced for images that have a solid color background). After cropping, the
 * new image will be scaled up or down to match the desired size. This scaling amount will be
 * recorded in the final annotation.
 * 
 * a text-based annotation file will be generated at the specified path containing the desired
 * annotation data
 * 
 * annotation files consist of 5 lines.
 * line 1: true_dist
 * line 2: rx
 * line 3: ry
 * line 4: rz
 * line 5: scale_factor
 * where scale factor is the degree to which the image was resized from the original
 * 
 * usage: java prepare_image /path/to/input/image /path/to/output true_distance rx ry rz dest_width dest_height
 * 
 * where (rx, ry, rz) represents the 3D orientation of the model in radians
 *
 */
public class prepare
{
	private static final double rad360 = 6.28318531;
	private static double rot_max = rad360;
	public static void main(String[] args) throws IOException
	{
		BufferedImage img = null;
		PrintWriter writer = null;
		double true_distance, rx, ry, rz;
		int dest_width, dest_height;
		
		final int num_valid_args = 17;
		
		double cov3d[][] = new double[3][3];

		try // process inputs
		{
			if(args.length != num_valid_args) throw new Exception("invalid number of inputs (was " + args.length + ", should be " + num_valid_args + ")");

			int arg_num = 2;
			//System.out.println(Arrays.toString(args));
			
			dest_width = Integer.parseInt(args[arg_num++]);
			dest_height = Integer.parseInt(args[arg_num++]);
			true_distance = doubleFromPython(args[arg_num++]);
			rx = doubleFromPython(args[arg_num++]);
			ry = doubleFromPython(args[arg_num++]);
			rz = doubleFromPython(args[arg_num++]);
			for(int r = 0; r < 3; r++)
				for(int c = 0; c < 3; c++)
					cov3d[r][c] = doubleFromPython(args[arg_num++]);
			
			img = ImageIO.read(new File(args[0]));
			writer = new PrintWriter(new File(args[1]));
			/*System.out.println("RAPTOR Image Preparer Loaded");
			System.out.println();
			System.out.println("==================== INPUT PROCESSED SUCCESSFULLY ====================");
			System.out.println("     image_path: " + args[0]);
			System.out.println("annotation_path: " + args[1]);
			System.out.println("    orientation: (" + rx + ", " + ry + ", " + rz + ")");
			System.out.println("      true_dist: " + true_distance);
			System.out.println("     dest_width: " + dest_width);
			System.out.println("    dest_height: " + dest_height);
			System.out.println("======================================================================");
			System.out.println();*/
		} catch (Exception e) {
			System.out.println("usage: java annotate_image /path/to/input/image /path/to/output dest_width dest_height true_distance rx ry rz cov00 cov01 cov02 cov03 cov04 cov05 cov06 cov07 cov08");
			System.out.println("error that occured: " + e.getClass().getName() + " " + e.getMessage());
			e.printStackTrace();
			if(writer != null) writer.close();
			return;
		}

		// crop image
		//System.out.println("calculating crop bounds...");
		int backdrop_color = img.getRGB(0, 0);
		int left_crop = scanLeftCrop(backdrop_color, img);
		int right_crop = scanRightCrop(backdrop_color, img);
		int top_crop = scanTopCrop(backdrop_color, img);
		int bottom_crop = scanBottomCrop(backdrop_color, img);
		Rectangle crop_bounds = new Rectangle(left_crop, top_crop,
				Math.abs(right_crop - left_crop), Math.abs(bottom_crop - top_crop));
		/*System.out.println("LCROP: " + left_crop);
		System.out.println("TCROP: " + top_crop);
		System.out.println("RCROP: " + (img.getWidth() - right_crop));
		System.out.println("BCROP: " + (img.getHeight() - bottom_crop));
		System.out.println();
		System.out.println("cropping image...");
		System.out.println();*/
		img = cropImage(img, crop_bounds);

		// resize image to desired size
		int orig_width = img.getWidth();
		int orig_height = img.getHeight();
		//System.out.println("calculating resize bounds...");
		Rectangle scale_bounds = getSmartResizeBounds(orig_width, orig_height, dest_width, dest_height);
		//System.out.println("INNER_W: " + scale_bounds.width);
		//System.out.println("INNER_H: " + scale_bounds.height);
		double scale_factor = (scale_bounds.width / (double)orig_width + scale_bounds.height / (double)orig_height) / 2.0;
		//System.out.println("SCALE_FACTOR: " + scale_factor);
		//System.out.println();
		//System.out.println("resizing image...");
		img = resize(img, scale_bounds);
		//System.out.println();
		
		//System.out.println("generating 2D covariance matrix...");
		//System.out.println();
		
		// load image data and threshold
		double image_data[][] = new double[dest_height][dest_width];
		for(int y = 0; y < dest_height; y++)
		{
			for(int x = 0; x < dest_width; x++)
			{
				Color c = new Color(img.getRGB(x, y));
				image_data[y][x] = (double)c.getBlue() / 255.0;
				if(image_data[y][x] < 0.03) image_data[y][x] = 0.0;
				else image_data[y][x] = 1.0;
			}
		}
		
		//print_binary_matrix(image_data, "image_data");
		
		double image_matrix[][] = matrixify_image(image_data);
		
		//print_matrix(image_matrix, "image matrix");
		
		double cov2d[][] = get_2d_covariance_matrix(image_matrix);
		
		//print_matrix(cov2d, "2D covariance matrix");
		//print_matrix(cov3d, "3D covariance matrix");

		// write image file
		//System.out.println("writing image file...");
		ImageIO.write(img, "png", new File(args[0]));
		
		// write annotation file
		writer.println(true_distance);
		writer.println(rx);
		writer.println(ry);
		writer.println(rz);
		writer.println(scale_factor);
		for(int r = 0; r < cov3d.length; r++)
			for(int c = 0; c < cov3d[0].length; c++)
				writer.println(cov3d[r][c]);
		for(int r = 0; r < cov2d.length; r++)
			for(int c = 0; c < cov2d[0].length; c++)
				writer.println(cov2d[r][c]);
		
		writer.close();
	}
	
	private static double doubleFromPython(String n)
	{
		if(n.contains("."))
			return Double.parseDouble(n);
		else return Integer.parseInt(n);
	}

	private static int scanLeftCrop(int backdrop_color, BufferedImage img)
	{
		for(int x = 0; x < img.getWidth(); x++)
			for(int y = 0; y < img.getHeight(); y++)
			{
				int pixel = img.getRGB(x, y);
				if(pixel == backdrop_color) continue;
				return x - 1;
			}
		return -1;
	}

	private static int scanRightCrop(int backdrop_color, BufferedImage img)
	{
		for(int x = img.getWidth() - 1; x >= 0; x--)
			for(int y = 0; y < img.getHeight(); y++)
			{
				int pixel = img.getRGB(x, y);
				if(pixel == backdrop_color) continue;
				return x + 1;
			}
		return -1;
	}

	private static int scanTopCrop(int backdrop_color, BufferedImage img)
	{
		for(int y = 0; y < img.getHeight(); y++)
			for(int x = 0; x < img.getWidth(); x++)
			{
				int pixel = img.getRGB(x, y);
				if(pixel == backdrop_color) continue;
				return y - 1;
			}
		return -1;
	}

	private static int scanBottomCrop(int backdrop_color, BufferedImage img)
	{
		for(int y = img.getHeight() - 1; y >= 0; y--)
			for(int x = 0; x < img.getWidth(); x++)
			{
				int pixel = img.getRGB(x, y);
				if(pixel == backdrop_color) continue;
				return y + 1;
			}
		return -1;
	}

	private static BufferedImage cropImage(BufferedImage src, Rectangle rect)
	{
		BufferedImage dest = src.getSubimage(rect.x, rect.y, rect.width, rect.height);
		return dest; 
	}

	private static Rectangle getSmartResizeBounds(int orig_width, int orig_height, Integer dest_width, Integer dest_height)
	{
		double vertical_offset = 0;
		double horizontal_offset = 0;
		double test_width = dest_width;
		double test_height = dest_height;
		double test_orig_width = orig_width;
		double test_orig_height = orig_height;
		if(dest_width == null)
		{
			if(dest_height == null)
				throw new IllegalArgumentException("dest_width and dest_height cannot both be null");
			test_width = test_height * test_orig_width / test_orig_height;
		} else if(dest_height == null) test_height = test_width * test_orig_height / test_orig_width;
		else {
			vertical_offset = test_height - (test_orig_height * test_width) / test_orig_width;
			horizontal_offset = test_width - (test_height * test_orig_width) / test_orig_height;
			if(vertical_offset < 0) vertical_offset = 0;
			if(horizontal_offset < 0) horizontal_offset = 0;
		}
		int x = (int)Math.floor(horizontal_offset / 2.0);
		int y = (int)Math.floor(vertical_offset / 2.0);
		int w = (int)Math.floor(test_width - horizontal_offset);
		int h = (int)Math.floor(test_height - vertical_offset);
		
		while(true)
		{
			if(w + x * 2 < dest_width) { w++; continue; }
			if(w + x * 2 > dest_height) { w--; continue; }
			if(h + y * 2 < dest_width) { h++; continue; }
			if(h + y * 2 > dest_height) { h--; continue; }
			break;
		}


		return new Rectangle(x, y, w, h);
	}

	private static BufferedImage resize(BufferedImage img, Rectangle bounds)
	{
		// smoother if you do it this way 
		BufferedImage resize = Scalr.resize(img, bounds.width, bounds.height, Scalr.OP_ANTIALIAS);
		BufferedImage dimg = new BufferedImage(bounds.width + bounds.x * 2, bounds.height + bounds.y * 2, img.getType());
		Graphics2D g = dimg.createGraphics();
		g.setComposite(AlphaComposite.Src);
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
		g.drawImage(resize, bounds.x, bounds.y, bounds.width + bounds.x, bounds.height + bounds.y, 0, 0, resize.getWidth(), resize.getHeight(), null);
		g.setComposite(AlphaComposite.SrcAtop);
		g.dispose();
		return dimg;  
	}
	
	
	public static enum OP { ADD, SUB, MUL, DIV, CROSS, DOT }
	private static double[][] mop(double[][] m1, OP op, double[][] m2)
	{
		double ret[][];
		switch(op)
		{
		case ADD:
			ret = new double[m1.length][m1[0].length];
			for(int r = 0; r < m1.length; r++)
				for(int c = 0; c < m1[0].length; c++)
					ret[r][c] = m1[r][c] + m2[r][c];
			return ret;
		case SUB:
			ret = new double[m1.length][m1[0].length];
			for(int r = 0; r < m1.length; r++)
				for(int c = 0; c < m1[0].length; c++)
					ret[r][c] = m1[r][c] - m2[r][c];
			return ret;
		}
		return null;
	}
	private static double[][] smop(double[][] M, OP op, double s)
	{
		double ret[][] = new double[M.length][M[0].length];
		switch(op)
		{
		case ADD:
			for(int r = 0; r < M.length; r++)
				for(int c = 0; c < M[0].length; c++)
					ret[r][c] = M[r][c] + s;
			break;
		case SUB:
			for(int r = 0; r < M.length; r++)
				for(int c = 0; c < M[0].length; c++)
					ret[r][c] = M[r][c] - s;
			break;
		case MUL:
			for(int r = 0; r < M.length; r++)
				for(int c = 0; c < M[0].length; c++)
					ret[r][c] = M[r][c] * s;
			break;
		case DIV:
			for(int r = 0; r < M.length; r++)
				for(int c = 0; c < M[0].length; c++)
					ret[r][c] = M[r][c] / s;
			break;
		}
		return ret;
	}
	private static double[] vop(double[] v1, OP op, double[] v2)
	{
		double ret[] = new double[v1.length];
		switch(op)
		{
		case ADD:
			for(int i = 0; i < v1.length; i++)
				ret[i] = v1[i] + v2[i];
			break;
		case SUB:
			for(int i = 0; i < v1.length; i++)
				ret[i] = v1[i] - v2[i];
			break;
		case MUL:
			for(int i = 0; i < v1.length; i++)
				ret[i] = v1[i] * v2[i];
			break;
		case DIV:
			for(int i = 0; i < v1.length; i++)
				ret[i] = v1[i] / v2[i];
			break;
		}
		return ret;
	}
	
	private static double[] svop(double[] v, OP op, double s)
	{
		double ret[] = new double[v.length];
		switch(op)
		{
		case ADD:
			for(int i = 0; i < v.length; i++)
				ret[i] = v[i] + s;
			break;
		case SUB:
			for(int i = 0; i < v.length; i++)
				ret[i] = v[i] - s;
			break;
		case MUL:
			for(int i = 0; i < v.length; i++)
				ret[i] = v[i] * s;
			break;
		case DIV:
			for(int i = 0; i < v.length; i++)
				ret[i] = v[i] / s;
			break;
		}
		return ret;
	}
	
	private static double[][] transpose(double[][] M)
	{
		double M2[][] = new double[M[0].length][M.length];
		for(int r = 0; r < M.length; r++)
			for(int c = 0; c < M[0].length; c++)
				M2[c][r] = M[r][c];
		return M2;
	}
	
	
	private static double[][] hvert(double[] v)
	{
		double mat[][] = new double[1][v.length];
		for(int i = 0; i < mat[0].length; i++)
			mat[0][i] = v[i];
		return mat;
	}
	
	private static double[][] vvert(double[] v)
	{
		double mat[][] = new double[v.length][1];
		for(int i = 0; i < mat.length; i++)
			mat[i][0] = v[i];
		return mat;
	}
	
	private static double[][] outer_product(double[][] v1, double[][] v2, boolean already_transposed)
	{
		if(!already_transposed)
			v2 = transpose(v2);
		double mat[][] = new double[v1.length][v1.length];
		for(int r = 0; r < mat.length; r++)
			for(int c = 0; c < mat[0].length; c++)
				mat[r][c] = v1[r][0] * v2[0][c];
		return mat;
	}
	
	private static double[][] outer_product(double[][] v1, double[][] v2)
	{
		return outer_product(v1, v2, false);
	}
	
	private static double[][] outer_product(double[] v1, double[] v2)
	{
		return outer_product(vvert(v1), hvert(v2), true);
	}
	
	private static double[][] get_2d_covariance_matrix(double[][] data)
	{
		return get_covariance_matrix_nd(data, 2, 2);
	}
	
	private static double[][] get_3d_covariance_matrix(double[][] point_cloud)
	{
		return get_covariance_matrix_nd(point_cloud, 3, 3);
	}
	
	private static double[][] matrixify_image(double[][] pixels)
	{
		int num_ones = 0;
		for(int y = 0; y < pixels.length; y++)
			for(int x = 0; x < pixels[0].length; x++)
				if(pixels[y][x] > 0.05) num_ones++;
		double image_data[][] = new double[num_ones][2];
		for(int y = 0, k = 0; y < pixels.length; y++)
		{
			for(int x = 0; x < pixels[0].length; x++)
			{
				if(pixels[y][x] <= 0.05) continue;
				//image_data[k][0] = pixels[y][x];
				image_data[k][0] = x;
				image_data[k][1] = y;
				k++;
			}
		}
		return image_data;
	}
	
	private static double[][] get_covariance_matrix_nd(double[][] points, int input_dimensions, int output_dimensions)
	{
		double means[] = new double[input_dimensions];
		double cov[][] = new double[output_dimensions][output_dimensions];
		for(int i = 0; i < points.length; i++)
			for(int j = 0; j < input_dimensions; j++)
				means[j] += points[i][j];
		for(int i = 0; i < input_dimensions; i++)
			means[i] /= points.length;
		for(int i = 0; i < output_dimensions; i++)
			for(int j = 0; j < output_dimensions; j++)
			{
				for(int k = 0; k < points.length; k++)
					cov[i][j] += (means[i] - points[k][i]) * (means[j] - points[k][j]);
				cov[i][j] /= points.length - 1;
			}
		return cov;
	}
	
	private static void print_matrix(double[][] M, String label)
	{
		if(label != null) System.out.println(label + ":");
		else System.out.println();
		boolean all_int = true;
		back:
		for(int r = 0; r < M.length; r++)
			for(int c = 0; c < M[0].length; c++)
				if(M[r][c] != Math.floor(M[r][c]))
				{
					all_int = false;
					break back;
				}
		if(all_int)
		{
			int MI[][] = new int[M.length][M[0].length];
			for(int r = 0; r < M.length; r++)
				for(int c = 0; c < M[0].length; c++)
					MI[r][c] = (int) M[r][c];
			for(int r = 0; r < M.length; r++)
				System.out.println(Arrays.toString(MI[r]));
		} else for(int r = 0; r < M.length; r++)
			System.out.println(Arrays.toString(M[r]));
		System.out.println();
	}
	
	private static void print_binary_matrix(double[][] M, String label)
	{
		if(label != null) System.out.println(label + ":");
		else System.out.println();
		for(int r = 0; r < M.length; r++)
		{
			for(int c = 0; c < M[0].length; c++)
			{
				if(M[r][c] > 0.001) System.out.print("1");
				else System.out.print("0");
			}
			System.out.println();
		}
	}

}
