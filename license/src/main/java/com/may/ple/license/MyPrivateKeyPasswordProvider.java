package com.may.ple.license;

import net.nicholaswilliams.java.licensing.encryption.PasswordProvider;

public class MyPrivateKeyPasswordProvider implements PasswordProvider {

	@Override
	public char[] getPassword() {
		return "w,j[vd8iy[".toCharArray();
	}

}
