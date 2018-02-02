package com.may.ple.kyschkpay;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.util.LoadLibs;

import org.apache.commons.lang3.StringUtils;

public class Tess4jCaptcha {
	public enum DENOISE {JAVA, CV};
	private final int WHITE = 0x00FFFFF5, BLACK = 0x0000000;
	private ITesseract tess;
	private DENOISE denoise;
	
	public static void main(String[] args) {
		System.out.println("Start " + String.format("%1$tH:%1$tM:%1$tS.%1$tL", Calendar.getInstance().getTime()));
		try {
			ThreadPoolExecutor executor = (ThreadPoolExecutor)Executors.newFixedThreadPool(10);
			final String INPUT = "D:\\captcha\\Captcha.jpg";
			
			for (int i = 0; i < 500; i++) {
				executor.execute(new Runnable() {
					@Override
					public void run() {
						try {						
							String txt = new Tess4jCaptcha(DENOISE.CV).solve(Files.readAllBytes(Paths.get(INPUT)));			
							System.out.println(Thread.currentThread() + " - " + txt);					
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
			}

			executor.shutdown();
			
			executor.awaitTermination(1, TimeUnit.DAYS);
			
			System.out.println("End " + String.format("%1$tH:%1$tM:%1$tS.%1$tL", Calendar.getInstance().getTime()));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public Tess4jCaptcha(DENOISE denoise){
		this.denoise = denoise;
		tess = new Tesseract();  
		tess.setTessVariable("tessedit_char_whitelist", "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ");
		
		File tessDataFolder = LoadLibs.extractTessResources("tessdata");
		tess.setDatapath(tessDataFolder.getAbsolutePath());
	}
	
	public String solve(byte[] in) throws Exception {	    
		try {
			return crackImage(this.denoise == DENOISE.CV ? DenoiseCV.denoise(in) : denoise(in));
		} catch (Exception e) {
			throw e;
		}
	}
	
	private String crackImage(BufferedImage image) throws Exception {
	    try {  
	    	return StringUtils.trimToEmpty(tess.doOCR(image)).replaceAll("\\s","");
	    } catch (Exception e) {  
	        throw e;
	    }  
	}

	private BufferedImage denoise(byte[] imageBytes) throws Exception {
		InputStream arryIn = null, in = null;
		
		try (
				ByteArrayOutputStream out = new ByteArrayOutputStream();
			) {
			
			in = new ByteArrayInputStream(imageBytes);
			BufferedImage image = ImageIO.read(in);
			int average = 0;
	
			image = createGrayscalePic(image);
			
			for( int row = 0; ++row < image.getHeight(); )
				for ( int column = 0; ++column < image.getWidth(); )
					average += image.getRGB(column, row) & 0x000000FF;
			average /= image.getWidth() * image.getHeight();
			
			for( int row = 0; ++row < image.getHeight(); )
				for ( int column = 0; ++column < image.getWidth(); )
					if ((image.getRGB(column, row) & 0x000000FF) <= average * .80)
						image.setRGB(column, row, BLACK);
					else
						image.setRGB(column, row, WHITE);
			
	                int consecutiveWhite = 0;
			
			for( int row = 0; ++row < image.getHeight(); )
				for ( int column = 0; ++column < image.getWidth(); )
					if ( (image.getRGB(column,row) & 0x000000FF) == 255 )
						consecutiveWhite++;
					else {
						if (consecutiveWhite < 3 && column > consecutiveWhite)
							for (int col = column - consecutiveWhite; col < column; col++)
								image.setRGB(col, row, BLACK);
						consecutiveWhite = 0;
					}
			consecutiveWhite = 0;
			
			for ( int column = 0; ++column < image.getWidth(); )
				for( int row = 0; ++row < image.getHeight(); )
					if ( (image.getRGB(column, row) & 0x000000FF) == 255 )
						consecutiveWhite++;
					else {
						if (consecutiveWhite < 2 && row > consecutiveWhite) 
							for (int r = row - consecutiveWhite; r < row; r++)
								image.setRGB(column, r, BLACK);
						consecutiveWhite = 0;
					}
	
			for( int row = 0; ++row < image.getHeight(); )
				for ( int column = 0; ++column < image.getWidth(); )
					if ((image.getRGB(column, row) & WHITE) == WHITE) {
						int height = countVerticalWhite(image, column, row);
						int width = countHorizontalWhite(image, column, row);
						if ((width * height <= 10) || (width == 1) || (height == 1))
							image.setRGB(column, row, BLACK);
					}
	
	                for( int row = 0; ++row < image.getHeight(); )
				for ( int column = 0; ++column < image.getWidth(); )
					if ((image.getRGB(column, row) & WHITE) == WHITE) {
						int height = countVerticalWhite(image, column, row);
						int width = countHorizontalWhite(image, column, row);
						if ((width * height <= 10) || (width == 1) || (height == 1))
							image.setRGB(column, row, BLACK);
					}
	
			for( int row = 0; ++row < image.getHeight(); )
				for ( int column = 0; ++column < image.getWidth(); )
					if ((image.getRGB(column, row) & WHITE) != WHITE)
						if (countBlackNeighbors(image, column, row) <= 3)
							image.setRGB(column, row, WHITE);
			
			ImageIO.write(image, "png", out);
			
			arryIn = new ByteArrayInputStream(out.toByteArray());
			
			return ImageIO.read(arryIn);
		} catch (Exception e) {
			throw e;
		} finally {
			if(in != null) in.close();
			if(arryIn != null) arryIn.close();
		}
	}

    private int countVerticalWhite(BufferedImage image, int x, int y) {
		return (countAboveWhite(image, x, y) + countBelowWhite(image, x, y)) + 1;
	}
        
	private int countAboveWhite(BufferedImage image, int x, int y) {
		int aboveWhite = 0;
		y--;
		while (y-- > 0)
			if ((image.getRGB(x, y) & WHITE) == WHITE)
				aboveWhite++;
			else
				break;
		return aboveWhite;
	}
	private int countBelowWhite(BufferedImage image, int x, int y) {
		int belowWhite = 0;
		y++;
		while (y < image.getHeight())
			if ((image.getRGB(x, y++) & WHITE) == WHITE)
				belowWhite++;
			else
				break;
		return belowWhite;
	}
	private int countHorizontalWhite(BufferedImage image, int x, int y) {
		return (countLeftWhite(image, x, y) + countRightWhite(image, x, y)) + 1;
	}
	private int countLeftWhite(BufferedImage image, int x, int y) {
		int leftWhite = 0;
		x--;
		while (x-- > 0)
			if ((image.getRGB(x, y) & WHITE) == WHITE)
				leftWhite++;
			else
				break;
		return leftWhite;
	}
	private int countRightWhite(BufferedImage image, int x, int y) {
		int rightWhite = 0;
		x++;
		while (x < image.getWidth())
			if ((image.getRGB(x++, y) & WHITE) == WHITE)
				rightWhite++;
			else
				break;
		return rightWhite;
	}
	private int countBlackNeighbors(BufferedImage image, int x, int y) {
		int numBlacks = 0;
		if (pixelColor(image, x - 1, y) != WHITE)
			numBlacks++;
		if (pixelColor(image, x - 1, y + 1) != WHITE)
			numBlacks++;
		if (pixelColor(image, x - 1, y - 1) != WHITE)
			numBlacks++;
		if (pixelColor(image, x, y + 1) != WHITE)
			numBlacks++;
		if (pixelColor(image, x, y - 1) != WHITE)
			numBlacks++;
		if (pixelColor(image, x + 1, y) != WHITE)
			numBlacks++;
		if (pixelColor(image, x + 1, y + 1) != WHITE)
			numBlacks++;
		if (pixelColor(image, x + 1, y - 1) != WHITE)
			numBlacks++;
		return numBlacks;
	}

	private int pixelColor(BufferedImage image, int x, int y) {
		if (x >= image.getWidth() || x < 0 || y < 0 || y >= image.getHeight())
			return WHITE;
		return image.getRGB(x, y) & WHITE;
	}
	
	private BufferedImage createGrayscalePic(BufferedImage raw) {
        BufferedImage temp = new BufferedImage(raw.getWidth(), raw.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics g = temp.getGraphics();
        g.drawImage(raw, 0, 0, null);
        g.dispose();
        return temp;
    }

}
