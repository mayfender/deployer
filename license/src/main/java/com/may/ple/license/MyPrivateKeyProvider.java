package com.may.ple.license;

import java.io.File;
import java.io.FileInputStream;
import java.security.CodeSource;

import net.nicholaswilliams.java.licensing.encryption.PrivateKeyDataProvider;
import net.nicholaswilliams.java.licensing.exception.KeyNotFoundException;

import org.apache.commons.io.IOUtils;

public class MyPrivateKeyProvider implements PrivateKeyDataProvider {

	@Override
	public byte[] getEncryptedPrivateKeyData() throws KeyNotFoundException {
		try {
			CodeSource codeSource = MyPrivateKeyProvider.class.getProtectionDomain().getCodeSource();
			File currentPath = new File(codeSource.getLocation().toURI().getPath());
			String keyDir = currentPath.getParentFile().getParentFile().getPath();
			
			return IOUtils.toByteArray(new FileInputStream(new File(keyDir + File.separator + "private.key")));			
		} catch (Exception e) {
			return null;
		}
	}

}
