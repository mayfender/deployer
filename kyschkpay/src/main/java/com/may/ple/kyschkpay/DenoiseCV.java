package com.may.ple.kyschkpay;

/*import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;*/

public class DenoiseCV {
	
	/*public static void main(String[] args) throws IOException {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		File input = new File("D:\\captcha\\Captcha.jpg");

		BufferedImage image = ImageIO.read(input);
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
		
		Imgcodecs.imwrite("D:\\captcha\\temp.jpg", captcha_erode);
	}*/

}
