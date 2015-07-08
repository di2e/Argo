package net.dharwin.common.tools.cli.api.utils;

import java.util.ArrayList;
import java.util.List;

import com.impetus.annovention.listener.ClassAnnotationDiscoveryListener;

public class CLIAnnotationDiscovereryListener implements ClassAnnotationDiscoveryListener {
	
	private List<String> _discoveredClasses;
	
	public String[] _supportedAnnotations;
	
	public CLIAnnotationDiscovereryListener(String[] supportedAnnotations) {
		_discoveredClasses = new ArrayList<String>();
		_supportedAnnotations = supportedAnnotations;
	}
	
	public List<String> getDiscoveredClasses() {
		return _discoveredClasses;
	}
	
	@Override
	public String[] supportedAnnotations() {
		return _supportedAnnotations;
	}

	@Override
	public void discovered(String clazz, String annotation) {
		_discoveredClasses.add(clazz);
	}

}
