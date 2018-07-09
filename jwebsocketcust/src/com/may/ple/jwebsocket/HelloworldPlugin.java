package com.may.ple.jwebsocket;

import org.jwebsocket.api.PluginConfiguration;
import org.jwebsocket.plugins.TokenPlugIn;

public class HelloworldPlugin extends TokenPlugIn {

	public HelloworldPlugin(PluginConfiguration aConfiguration) {
		super(aConfiguration);
		System.out.println("################## HelloworldPlugin");
	}

}
