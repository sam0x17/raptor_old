/**
 * Contains image manipulation and processing functions used throughout RAPTOR
 */
package raptor.engine;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Arrays;

import org.imgscalr.Scalr;

import static java.lang.Math.*;

/**
 * @author Sam Kelly
 *
 */
public final class Image
{
	public static class PixelGrid
	{
		public PixelGrid(double[][] pixels)
		{
			this.pixels = pixels;
		}
		
		public double pixels[][];
		
		public String toString()
		{
			String st = "";
			for(int r = 0; r < pixels.length; r++)
				st += Arrays.toString(pixels[r]) + "\n";
			return st;
		}
	}
	
	public static PixelGrid normalizeImage(BufferedImage img)
	{
		int w = img.getWidth(); int h = img.getHeight();
		PixelGrid grid = new PixelGrid(new double[h][w]);
		for(int y = 0; y < h; y++)
			for(int x = 0; x < w; x++)
				grid.pixels[y][x] = new Color(img.getRGB(x, y)).getBlue() / 255.0;
		return grid;
	}
	
	public static void threshold(PixelGrid grid, double threshold)
	{
		for(int y = 0; y < grid.pixels.length; y++)
			for(int x = 0; x < grid.pixels[0].length; x++)
				grid.pixels[y][x] = grid.pixels[y][x] >= threshold ? 1.0 : 0.0;
	}
	
	/**
	 * Calculates resize bounds that would force an original image
	 * of size (sw, sh) to fit within the bounds (dw, dh) without
	 * breaking the aspect ratio of the original image. Either dw
	 * or dh can be left null, in which case the dimension that
	 * was left null will be maximized or minimized so that as
	 * much of the original image as possible can fit within
	 * the dimension that was specified.
	 * 
	 * @param sw the width of the source (original) image
	 * @param sh the height of the source (original) image
	 * @param dw the desired width (can be null)
	 * @param dh the desired height (can be null)
	 * @return a Rectangle representing the smart resize bounds
	 */
	public static Rectangle getSmartResizeBounds(int sw, int sh, Integer dw, Integer dh)
	{
		double voff = 0.0; double hoff = 0.0;
		double tdw = dw; double tdh = dh;
		double tsw = sw; double tsh = sh;
		if(dw == null)
		{
			if(dh == null)
				throw new IllegalArgumentException("dest_width and dest_height cannot both be null");
			tdw = tdh * tsw / tsh;
		} else if(dh == null)
			tdh = tdw * tsh / tsw;
		else {
			voff = tdh - (tsh * tdw) / tsw;
			hoff = tdw - (tdh * tsw) / tsh;
			if(voff < 0.0)voff = 0.0;
			if(hoff < 0.0) hoff = 0.0;
		}
		int x = (int)floor(hoff / 2.0);
		int y = (int)floor(voff / 2.0);
		int w = (int)floor(tdw - hoff);
		int h = (int)floor(tdh - voff);
		while(true)
		{ // gets rid of some nasty rounding errors
			if(w + x * 2 < dw) { w++; continue; }
			if(w + x * 2 > dh) { w--; continue; }
			if(h + y * 2 < dw) { h++; continue; }
			if(h + y * 2 > dh) { h--; continue; }
			break;
		}
		return new Rectangle(x, y, w, h);
	}
	
	/**
	 * Quickly resizes the specified input image to fit
	 * the specified resize bounds (with padding)
	 * @param img the input image
	 * @param bounds the resize bounds (with padding)
	 * @return img resized to the specified bounds
	 */
	public static BufferedImage resize(BufferedImage img, Rectangle bounds)
	{
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
	
	/**
	 * Quickly resizes the specified input image
	 * @param img the input image
	 * @param dw the desired width
	 * @param dh the desired height
	 * @return img resized to (dw, dh)
	 */
	public static BufferedImage resize(BufferedImage img, int dw, int dh)
	{
		return Scalr.resize(img, dw, dh, Scalr.OP_ANTIALIAS);
	}
	
	/**
	 * Adds a background color to a translucent ARGB image and returns the
	 * resulting RGB image
	 * @param img the translucent ARGB input image
	 * @param backcolor the desired background color
	 * @return img as an RGB image with backcolor as the background color
	 */
	public static BufferedImage flattenARGB(BufferedImage img, Color backcolor)
	{
		int w = img.getWidth(); int h = img.getHeight();
		BufferedImage dimg = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
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
		g.setColor(backcolor);
		g.fillRect(0, 0, w, h);
		g.drawImage(img, 0, 0, w, h, 0, 0, w, h, null);
		g.dispose();
		return dimg;
	}
	
	/**
	 * @param img the input image
	 * @return a grayscale version of the input image
	 */
	public static BufferedImage toGrayscale(BufferedImage img)
	{
		BufferedImage ret = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
		Graphics2D g = ret.createGraphics();
		g.drawImage(img, 0, 0, img.getWidth(), img.getHeight(), 0, 0, img.getWidth(), img.getHeight(), null);
		g.dispose();
		return ret;
	}
	
	/**
	 * Crops the specified image using the specified rectangular bounds
	 * 
	 * @param img the image to be cropped
	 * @param bounds the bounds specifying the degree to which img should be cropped
	 * @return the cropped image
	 */
	public static BufferedImage cropImage(BufferedImage img, Rectangle bounds)
	{
		BufferedImage dest = img.getSubimage(bounds.x, bounds.y, bounds.width, bounds.height);
		return dest; 
	}
	
	/**
	 * Scans img from the top, left, right, and bottom sides moving towards the center
	 * looking for the first pixel on each side that is NOT of color backdrop_color.
	 * The stopping points of these four scans are used to generate crop bounds.
	 * 
	 * @param img the image to be cropped
	 * @param backdrop_color the backdrop color that will be ignored during the scan
	 * @return a Rectangle representing the crop bounds that were found for img
	 */
	public static Rectangle getSmartCropBounds(BufferedImage img, Color backdrop_color)
	{
		return getSmartCropBounds(img, backdrop_color.getRGB());
	}
	
	/**
	 * Scans img from the top, left, right, and bottom sides moving towards the center
	 * looking for the first pixel on each side that is NOT of color backdrop_color.
	 * The stopping points of these four scans are used to generate crop bounds.
	 * 
	 * @param img the image to be cropped
	 * @param backdrop_color an RGB int corresponding with the color that will be ignored during the scan
	 * @return a Rectangle representing the crop bounds that were found for img
	 */
	public static Rectangle getSmartCropBounds(BufferedImage img, int backdrop_color)
	{
		int left_crop = scanLeftCrop(backdrop_color, img);
		int right_crop = scanRightCrop(backdrop_color, img);
		int top_crop = scanTopCrop(backdrop_color, img);
		int bottom_crop = scanBottomCrop(backdrop_color, img);
		return new Rectangle(left_crop, top_crop, abs(right_crop - left_crop), abs(bottom_crop - top_crop));
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
}
