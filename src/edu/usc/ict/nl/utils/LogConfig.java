package edu.usc.ict.nl.utils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;

import edu.usc.ict.nl.util.FileUtils;

public class LogConfig {
	public static URL findLogConfig(String filename) {
		return findLogConfig(null,filename, false);
	}
	public static URL findLogConfig(String inDir,String filename) {
		return findLogConfig(inDir,filename, false);
	}
	public static URL findLogConfig(String inDir,String filename,boolean preferJar) {
		URL log4Jresource=getLogConfig(inDir,filename, preferJar);
		if (log4Jresource == null) log4Jresource=getLogConfig(inDir,filename, !preferJar);
		return log4Jresource;
	}
	
	private static URL getLogConfig(String inDir,String filename,boolean preferJar) {
		URL log4Jresource=null;
		if (preferJar) log4Jresource = ClassLoader.getSystemResource(filename);
		else {
			try {
				File file=new File(inDir,filename);
				if (file.exists()) log4Jresource=file.toURI().toURL();
				else {
					Set<File> dirs = FileUtils.findAllDirsContainingFileMatching(new File(inDir), Pattern.quote(filename));
					if (dirs!=null && !dirs.isEmpty()) {
						Iterator<File> it=dirs.iterator();
						while(it.hasNext()) {
							file=it.next();
							if (file.exists()) {
								log4Jresource=file.toURI().toURL();
								break;
							}
						}
					}
				}
			} catch (MalformedURLException e) {}
		}
		return log4Jresource;
	}
}
