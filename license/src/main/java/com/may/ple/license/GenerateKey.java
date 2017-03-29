package com.may.ple.license;

import java.io.IOException;
import java.security.KeyPair;

import net.nicholaswilliams.java.licensing.encryption.RSAKeyPairGenerator;
import net.nicholaswilliams.java.licensing.exception.AlgorithmNotSupportedException;
import net.nicholaswilliams.java.licensing.exception.InappropriateKeyException;
import net.nicholaswilliams.java.licensing.exception.InappropriateKeySpecificationException;
import net.nicholaswilliams.java.licensing.exception.RSA2048NotSupportedException;

public class GenerateKey {

	public static void main(String[] arguments) {
		RSAKeyPairGenerator generator = new RSAKeyPairGenerator();

		KeyPair keyPair;
		try {
			keyPair = generator.generateKeyPair();
		} catch (RSA2048NotSupportedException e) {
			return;
		}

		try {
			generator.saveKeyPairToFiles(keyPair, "private.key", "public.key", "w,j[vd8iy[".toCharArray());
		} catch (IOException | AlgorithmNotSupportedException | InappropriateKeyException | InappropriateKeySpecificationException e) {
			return;
		}
		
		System.out.println("Finished");
	}

}
