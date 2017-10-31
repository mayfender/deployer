package com.may.ple.license;

import java.util.Calendar;

import org.apache.commons.codec.binary.Base64;

import net.nicholaswilliams.java.licensing.License;
import net.nicholaswilliams.java.licensing.licensor.LicenseCreator;
import net.nicholaswilliams.java.licensing.licensor.LicenseCreatorProperties;

public class GenerateLicense {
	
	public static void main(String[] args) {
		LicenseCreatorProperties.setPrivateKeyDataProvider(new MyPrivateKeyProvider());
        LicenseCreatorProperties.setPrivateKeyPasswordProvider(new MyPrivateKeyPasswordProvider());
        
        Calendar ca = Calendar.getInstance();
        
        License license = new License.Builder().
                withProductKey("BKK-0003-00003").
                withHolder("NNYY").
                withIssueDate(ca.getTimeInMillis()).
                build();
        
        byte[] licenseData = LicenseCreator.getInstance().signAndSerializeLicense(license, "w,j[vd8iy[".toCharArray());
        
        String trns = Base64.encodeBase64String(licenseData);
        
        System.out.println(trns);
        
	}

}
