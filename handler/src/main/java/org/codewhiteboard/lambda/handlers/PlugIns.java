package org.codewhiteboard.lambda.handlers;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlugIns {

	private static Framework framework;
	private static BundleContext bndlContext;
	private static Map<String,Bundle> bundlesMap = new HashMap<String,Bundle>();
	
	//handles to each plugin bundle
	private static Bundle s3DownloaderPlugin;
	
	private static final Logger logger = LoggerFactory.getLogger(PlugIns.class);
	
	public static void initialize() throws Exception {
		
		
		//initialize embedded OSGi 
		FrameworkFactory frameworkFactory = ServiceLoader.load(FrameworkFactory.class).iterator().next();
		Map<String, String> config = new HashMap<>();
		String tmpDir = System.getProperty("java.io.tmpdir");
		/*
		 * Expose the host's SLF4J API to the container. This ensures that
		 * any time a package requires SLF4J, the actual implementation will
		 * be resolved to the one on the host.
		 */

		final StringBuilder sb = new StringBuilder(128);
		sb.append("org.slf4j; version=1.8.0.beta4");
		sb.append(",");
		sb.append("org.slf4j.*; version=1.8.0.beta4");
		config.put("org.osgi.framework.system.packages.extra", sb.toString());
		config.put("org.osgi.framework.storage", tmpDir + File.separator + "osgi" + File.separator + "felix-cache");
		config.put(Constants.FRAMEWORK_STORAGE_CLEAN, "onFirstInit");

		framework = frameworkFactory.newFramework(config);
		framework.start();

		bndlContext = framework.getBundleContext();
		
		//collect all bundles
		List<String> filePaths = getAllBundles();
		
		//install plugin bundles
		installAllBundles(filePaths);
		
		//start all plugin bundles. This step resolves imports and exports among dependencies
		startAllBundles();


	}
	
	private static List<String> getAllBundles() throws IOException{
		
		Set<Path> bndFolders = new HashSet<Path>();
		
		String workingDir = System.getProperty("user.dir");
		logger.info("Working dir: " + workingDir);
		
		Properties props = new Properties();
		try {
			props.load(PlugIns.class.getClassLoader().getResourceAsStream("plugins.properties"));
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
		
		//collect all bundle containing folders into a Set
		String s3DownloaderBundlePath = props.getProperty("s3downloader.bundlepath","../.plugin");
		logger.info("relative path to s3downloader bundle: " + s3DownloaderBundlePath);
		bndFolders.add(new File(workingDir + File.separator + s3DownloaderBundlePath).toPath());
		
		String s3DownloaderDepPath = props.getProperty("s3downloader.dependency","../.plugin");
		logger.info("relative path to s3downloader dependency: " + s3DownloaderDepPath);		
		bndFolders.add(new File(workingDir + File.separator + s3DownloaderDepPath).toPath());
		
		//collect all bundle jar file paths from the folders
		List<String> fileNamesList = new ArrayList<String>();
		for (Path aFolder : bndFolders) {
			// reading the folder and getting Stream.
			try (Stream<Path> walk = Files.walk(aFolder)) {

				fileNamesList.addAll(
						walk.map(x -> x.toString()).filter(f -> f.endsWith(".jar")).collect(Collectors.toList()));

			} catch (IOException e) {
				logger.error("failed to traverse the bundles folder",e.getCause());
				throw e;
			}

		}
		
		return fileNamesList;
	}
	
	private static void installAllBundles(List<String> filePaths) throws MalformedURLException, BundleException {
		
		URL url = null;
		for(String fileName: filePaths) {
			File bundleFile = new File(fileName);
				url = bundleFile.toURI().toURL();
			try {
				Bundle bndl = bndlContext.installBundle(url.toString());
				bundlesMap.put(bndl.getSymbolicName(), bndl);
				
				logger.debug("Installed bundle " + url.toString());
			} catch (BundleException be) {
				//bundle set cannot have duplicates, but in case
				if (!be.getMessage().contains("Bundle symbolic name and version are not unique")) {
					throw be;
				}
				logger.info("Ignoring Bundle Install Exception " + be.getMessage() + " raised for " + url.toString());
			}
			
			
		}
		logger.info("Installed bundles count:" + bundlesMap.size());
	}
	
	private static void startAllBundles() throws BundleException {
		
		for (Bundle bundle : bundlesMap.values()) {
			if (bundle.getHeaders().get(Constants.FRAGMENT_HOST) == null) {
				bundle.start();
				logger.debug("Started bundle " + bundle.getSymbolicName() + ", state:" + bundle.getState());
			}
		}
	}
	
	public static Object downLoad(String artifactPath, String destFolder) throws Exception {
		
		//get the s3 downloader bundle		
		s3DownloaderPlugin = bundlesMap.get("org.codewhiteboard.osgi-plugin-s3downloader");
		if(s3DownloaderPlugin == null) throw new RuntimeException("S3 download plugin not found, exiting..");
		
		Class<?> mainclass = s3DownloaderPlugin.loadClass("org.codewhiteboard.lambda.plugins.s3.Downloader");
		Method method = mainclass.getMethod("downLoad", new Class [] {String.class,String.class});
		return method.invoke(null, artifactPath, destFolder);

	}
	
	public static void stop() {
		
		try {
			framework.stop();
			framework.waitForStop(5000);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
	}

}
