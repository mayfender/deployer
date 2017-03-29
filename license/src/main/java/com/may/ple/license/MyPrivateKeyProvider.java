package com.may.ple.license;

import java.io.File;
import java.io.FileInputStream;

import org.apache.commons.io.IOUtils;

import net.nicholaswilliams.java.licensing.encryption.PrivateKeyDataProvider;
import net.nicholaswilliams.java.licensing.exception.KeyNotFoundException;

public class MyPrivateKeyProvider implements PrivateKeyDataProvider {

	@Override
	public byte[] getEncryptedPrivateKeyData() throws KeyNotFoundException {
		try {
			return IOUtils.toByteArray(new FileInputStream(new File("D:\\workspace\\64-bit\\mars\\exam1\\web\\private.key")));			
		} catch (Exception e) {
			return null;
		}
	}

}
