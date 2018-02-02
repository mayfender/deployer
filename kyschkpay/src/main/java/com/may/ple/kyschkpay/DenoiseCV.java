package com.may.ple.kyschkpay;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class DenoiseCV {
	static {
		//-- First copy opencv_java340.dll to C:\Program Files\Java\jdk1.8.0_102\bin
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);		
	}
	
	public static void main(String[] args) throws IOException {
		
	}
	
	public static BufferedImage denoise(byte[] imageData) throws Exception {
		try {
			BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
			BufferedImage imageCopy = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
			imageCopy.getGraphics().drawImage(image, 0, 0, null);
	
			byte[] data = ((DataBufferByte) imageCopy.getRaster().getDataBuffer()).getData();  
			Mat img = new Mat(image.getHeight(),image.getWidth(), CvType.CV_8UC3);
			img.put(0, 0, data);           
			
			Mat captcha_bw = new Mat();
			Imgproc.threshold(img, captcha_bw, 50, 255, Imgproc.THRESH_BINARY);
			
			Mat captcha_erode = new Mat();
			Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(1, 1));
			Imgproc.erode(captcha_bw, captcha_erode, element);
			
//			Imgcodecs.imwrite("D:\\captcha\\temp.jpg", captcha_erode);
			return matToBufferedImage(captcha_erode);
		} catch (Exception e) {
			throw e;
		}
	}
	
	public static BufferedImage matToBufferedImage(Mat mat) {
        if (mat.height() > 0 && mat.width() > 0) {
            BufferedImage image = new BufferedImage(mat.width(), mat.height(), BufferedImage.TYPE_3BYTE_BGR);
            WritableRaster raster = image.getRaster();
            DataBufferByte dataBuffer = (DataBufferByte) raster.getDataBuffer();
            byte[] data = dataBuffer.getData();
            mat.get(0, 0, data);
            return image;
        }
        return null;
    }

}
