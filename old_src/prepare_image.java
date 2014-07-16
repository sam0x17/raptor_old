import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
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
public class prepare_image
{
	private static final double rad360 = 6.28318531;
	private static double rot_max = 1.0;
	public static void main(String[] args) throws IOException
	{
		BufferedImage img = null;
		PrintWriter writer = null;
		double true_distance, rx, ry, rz;
		int dest_width, dest_height;

		try // process inputs
		{
			if(args.length != 8) throw new Exception("invalid number of inputs (was " + args.length + ", should be 8)");

			if(args[2].contains(".")) true_distance = Double.parseDouble(args[2]);
			else true_distance = Integer.parseInt(args[2]);
			if(true_distance < 0) throw new Exception("true_distance cannot be less than 0");

			if(args[3].contains(".")) rx = Double.parseDouble(args[3]);
			else rx = Integer.parseInt(args[3]);
			if(rx > rot_max || rx < -1.0 * rot_max)
				throw new Exception("rx must be bounded between 0 and 2*PI");

			if(args[4].contains(".")) ry = Double.parseDouble(args[4]);
			else ry = Integer.parseInt(args[4]);
			if(ry > rot_max || ry < -1.0 * rot_max)
				throw new Exception("ry must be bounded between 0 and 2*PI");

			if(args[5].contains(".")) rz = Double.parseDouble(args[5]);
			else rz = Integer.parseInt(args[5]);
			if(rz > rot_max || rz < -1.0 * rot_max)
				throw new Exception("rz must be bounded between 0 and 2*PI");

			dest_width = Integer.parseInt(args[6]);
			dest_height = Integer.parseInt(args[7]);

			img = ImageIO.read(new File(args[0]));
			writer = new PrintWriter(new File(args[1]));
			System.out.println("RAPTOR Image Preparer Loaded");
			System.out.println();
			System.out.println("==================== INPUT PROCESSED SUCCESSFULLY ====================");
			System.out.println("     image_path: " + args[0]);
			System.out.println("annotation_path: " + args[1]);
			System.out.println("    orientation: (" + rx + ", " + ry + ", " + rz + ")");
			System.out.println("      true_dist: " + true_distance);
			System.out.println("     dest_width: " + dest_width);
			System.out.println("    dest_height: " + dest_height);
			System.out.println("======================================================================");
			System.out.println();
		} catch (Exception e) {
			System.out.println("usage: java annotate_image /path/to/input/image /path/to/output true_distance rx ry rz");
			System.out.println("error that occured: " + e.getMessage());
			if(writer != null) writer.close();
			return;
		}

		// crop image
		System.out.println("calculating crop bounds...");
		int backdrop_color = img.getRGB(0, 0);
		int left_crop = scanLeftCrop(backdrop_color, img);
		int right_crop = scanRightCrop(backdrop_color, img);
		int top_crop = scanTopCrop(backdrop_color, img);
		int bottom_crop = scanBottomCrop(backdrop_color, img);
		Rectangle crop_bounds = new Rectangle(left_crop, top_crop,
				Math.abs(right_crop - left_crop), Math.abs(bottom_crop - top_crop));
		System.out.println("LCROP: " + left_crop);
		System.out.println("TCROP: " + top_crop);
		System.out.println("RCROP: " + (img.getWidth() - right_crop));
		System.out.println("BCROP: " + (img.getHeight() - bottom_crop));
		System.out.println();
		System.out.println("cropping image...");
		System.out.println();
		img = cropImage(img, crop_bounds);

		// resize image to desired size
		int orig_width = img.getWidth();
		int orig_height = img.getHeight();
		System.out.println("calculating resize bounds...");
		Rectangle scale_bounds = getSmartResizeBounds(orig_width, orig_height, dest_width, dest_height);
		System.out.println("INNER_W: " + scale_bounds.width);
		System.out.println("INNER_H: " + scale_bounds.height);
		double scale_factor = (scale_bounds.width / (double)orig_width + scale_bounds.height / (double)orig_height) / 2.0;
		System.out.println("SCALE_FACTOR: " + scale_factor);
		System.out.println();
		System.out.println("resizing image...");
		img = resize(img, scale_bounds);

		// write image file
		System.out.println("writing image file...");
		ImageIO.write(img, "png", new File(args[0]));
		
		// write annotation file
		writer.println(true_distance);
		writer.println(rx);
		writer.println(ry);
		writer.println(rz);
		writer.println(scale_factor);
		writer.close();
		
		System.out.println();
		System.out.println("done.");
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

}

