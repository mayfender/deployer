package com.may.ple.kyschkpay;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.commons.lang3.StringUtils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class DenoiseCV {
	static {
		//-- First copy opencv_java340.dll to C:\Program Files\Java\jdk1.8.0_102\bin
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);		
	}
	
	public static void main(String[] args) throws IOException {
		try {
			String INPUT = "D:\\AdvCaptcha2.jpg";
			test2(Files.readAllBytes(Paths.get(INPUT)));
			System.out.println("Finished..");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static BufferedImage test2(byte[] imageData) throws Exception {
		try {
			System.out.println("test 999");
			BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
			BufferedImage imageCopy = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
			imageCopy.getGraphics().drawImage(image, 0, 0, null);
	
			byte[] data = ((DataBufferByte) imageCopy.getRaster().getDataBuffer()).getData();  
			Mat img = new Mat(image.getHeight(),image.getWidth(), CvType.CV_8UC3);
			img.put(0, 0, data);           
			
			Mat captcha_bw = new Mat();
//			Imgproc.resize(img, captcha_bw, new Size(1.5, 1.5));
			Imgproc.cvtColor(img, captcha_bw, Imgproc.COLOR_BGR2GRAY);
			Imgproc.GaussianBlur(captcha_bw, captcha_bw, new Size(3, 3), 2);
			Imgproc.threshold(captcha_bw, captcha_bw, 70, 255, Imgproc.THRESH_BINARY);
			
			Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(4, 4));
			Imgproc.erode(captcha_bw, captcha_bw, element);
			Imgproc.GaussianBlur(captcha_bw, captcha_bw, new Size(3, 3), 2);
			Imgproc.threshold(captcha_bw, captcha_bw, 220, 255, Imgproc.THRESH_BINARY);
			Imgproc.GaussianBlur(captcha_bw, captcha_bw, new Size(3, 3), 2);
			Imgproc.threshold(captcha_bw, captcha_bw, 220, 255, Imgproc.THRESH_BINARY);
			Imgproc.GaussianBlur(captcha_bw, captcha_bw, new Size(3, 3), 2);
			Imgproc.threshold(captcha_bw, captcha_bw, 220, 255, Imgproc.THRESH_BINARY);
//			element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));
//			Imgproc.dilate(captcha_bw, captcha_bw, element);
			
			
//			element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(10, 10));
//			Imgproc.dilate(captcha_bw, captcha_bw, element);
//			Imgproc.GaussianBlur(captcha_bw, captcha_bw, new Size(3, 3), 2);
			
//			Mat captcha_erode = new Mat();
//			Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(1, 1));
//			Imgproc.erode(captcha_bw, captcha_erode, element);
			
			Imgcodecs.imwrite("D:\\test.jpg", captcha_bw);
			return matToBufferedImage(captcha_bw);
		} catch (Exception e) {
			throw e;
		}
	}
	
	
	public static BufferedImage test(byte[] imageData) throws Exception {
		try {
		BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
		BufferedImage imageCopy = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
		imageCopy.getGraphics().drawImage(image, 0, 0, null);

		byte[] data = ((DataBufferByte) imageCopy.getRaster().getDataBuffer()).getData();  
		Mat src_img = new Mat(image.getHeight(),image.getWidth(), CvType.CV_8UC3);
		src_img.put(0, 0, data);   

		Mat dest = new Mat();

        Imgproc.cvtColor(src_img, dest, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(dest, dest, new Size(3, 3), 2);
        Imgproc.threshold(dest, dest, 80, 255, Imgproc.THRESH_BINARY_INV);
        Imgproc.dilate(dest, dest, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3)));
        Imgproc.threshold(dest, dest, 10, 255, Imgproc.THRESH_BINARY_INV);
        
        Imgcodecs.imwrite("D:\\temp.jpg", dest);
        return matToBufferedImage(dest);


        

         /* List<MatOfPoint> contours = new ArrayList<MatOfPoint>();  
          Mat heirarchy= new Mat();
          Point shift=new Point(150,0);
          Imgproc.findContours(dest, contours,heirarchy, Imgproc.RETR_TREE,Imgproc.CHAIN_APPROX_SIMPLE,shift);
          double[] cont_area =new double[contours.size()]; 

             for(int i=0; i< contours.size();i++)
             { 
                Rect rect = Imgproc.boundingRect(contours.get(i));
                cont_area[i]=Imgproc.contourArea(contours.get(i));

                System.out.println("Hight: "+rect.height);
                System.out.println("WIDTH: "+rect.width);
                System.out.println("AREA: "+cont_area[i]);
              //System.out.println(rect.x +","+rect.y+","+rect.height+","+rect.width);

                  Core.rectangle(src_img, new Point(rect.x,rect.y), new Point(rect.x+rect.width,rect.y+rect.height),new Scalar(0,0,255));
                  Imgproc.drawContours(dest_img, contours, i, new Scalar(0,0,0),-1,8,heirarchy,2,shift);
                  Core.rectangle(dest_img, new Point(rect.x,rect.y), new Point(rect.x+rect.width,rect.y+rect.height),new Scalar(0,255,0));
         }

             Highgui.imwrite("Final.jpg", dest_img);
             Highgui.imwrite("Original.jpg", src_img);*/
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}
	
	public static BufferedImage denoiseTest(byte[] imageData) throws Exception {
		try {
			BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
			BufferedImage imageCopy = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
			imageCopy.getGraphics().drawImage(image, 0, 0, null);
	
			byte[] data = ((DataBufferByte) imageCopy.getRaster().getDataBuffer()).getData();  
			Mat img = new Mat(image.getHeight(),image.getWidth(), CvType.CV_8UC3);
			img.put(0, 0, data);           
			
			Mat captcha_bw = new Mat();
//			Imgproc.threshold(img, captcha_bw, 50, 255, Imgproc.THRESH_BINARY);
//			Imgproc.threshold(captcha_bw, captcha_bw, 70, 255, Imgproc.THRESH_BINARY);
//			Imgproc.getStructuringElement(Imgproc.MORPH_CROSS, new Size(10, 10));
//			Imgproc.cvtColor(img, captcha_bw, Imgproc.COLOR_RGB2GRAY);
			
//			Imgproc.medianBlur(img, captcha_bw, 3);
//			Imgproc.Canny(img, captcha_bw, 100, 200);
			
//			Imgproc.dilate(img, captcha_bw, captcha_bw);
//			Mat test = new Mat();
//			Imgproc.erode(captcha_bw, captcha_bw, test);
//			Imgproc.cvtColor(captcha_bw, captcha_bw, Imgproc.COLOR_RGB2GRAY);
			
//			Imgproc.threshold(img, captcha_bw, 50, 255, Imgproc.THRESH_BINARY_INV);
//			Imgproc.cvtColor(captcha_bw, captcha_bw, Imgproc.COLOR_RGB2GRAY);
//			Imgproc.threshold(captcha_bw, captcha_bw, 50, 255, Imgproc.THRESH_BINARY);
//			Imgproc.threshold(img, captcha_bw, 200, 255, Imgproc.THRESH_BINARY_INV);
			
//			Photo.inpaint(img, captcha_bw, captcha_bw, 7, Photo.INPAINT_NS);
			
			
//			Mat captcha_erode = new Mat();
//			Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2, 2));
//			Imgproc.erode(captcha_bw, captcha_erode, element);
//			Imgproc.erode(captcha_bw, captcha_erode, element);
//			element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(4, 4));
//			Imgproc.erode(captcha_erode, captcha_erode, element);
			
			
			Imgproc.threshold(img, captcha_bw, 0, 255, Imgproc.THRESH_OTSU);
			
			Imgcodecs.imwrite("D:\\temp.jpg", captcha_bw);
			return matToBufferedImage(captcha_bw);
		} catch (Exception e) {
			throw e;
		}
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
