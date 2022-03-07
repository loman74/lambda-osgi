package org.codewhiteboard.lambda.handlers;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicHandler implements RequestHandler<Map<String, String>, String> {

	private static final Logger logger = LoggerFactory.getLogger(BasicHandler.class);
	Gson gson = new GsonBuilder().setPrettyPrinting().create();
	
	private static final String CACHELOC = System.getProperty("java.io.tmpdir") + File.separator + "OSGI";

	@Override
	public String handleRequest(Map<String, String> event, Context context) {


		String requestId = context.getAwsRequestId();
		String logGroupName = context.getLogGroupName();
		String logStreamName = context.getLogStreamName();
		
		// reset logging to baseline. Logging configuration persists after
		// invocation.
		Configurator.reconfigure();
		String dbgClassName = null;
		if (event.containsKey("log_debug") && event.get("log_debug") != null) {
			dbgClassName = event.get("log_debug").trim();
			configureLogging(dbgClassName,null);
		}

		File tmpFolder = new File(System.getProperty("java.io.tmpdir"));


		logger.info("Used storage space of " + tmpFolder.toString() + " folder is "
				+ folderSize(tmpFolder.toPath()) / (1024 * 1024) + " MB");

		try {
			PlugIns.initialize();
		} catch (Exception e) {
			logger.error("Failed to initialize PlugIns " + e);
			reclaimStorage(CACHELOC);
			throw new RuntimeException(e);
		}

		logger.debug("Used storage space of " + tmpFolder.toString() + " folder is "
				+ folderSize(tmpFolder.toPath()) / (1024 * 1024) + " MB");
		// log execution details
		logger.debug("ENVIRONMENT VARIABLES: " + gson.toJson(System.getenv()));
		logger.debug("CONTEXT: " + gson.toJson(context));
		

		// process event
		logger.info("EVENT: " + gson.toJson(event));

		String appJarPath = null;
		if (event.containsKey("jar") && event.get("jar") != null) {
			appJarPath = event.get("jar");
		} else {
			logger.error("Mandatory jar field is missing or null, exiting...");
			reclaimStorage(CACHELOC);
			throw new RuntimeException("Mandatory jar field is missing or null, exiting...");
		}



		File file = null;
		if (appJarPath.startsWith("s3://") || appJarPath.startsWith("S3://")) {
			//download all files in a separate folder called osgi, for easy clean up later
			String destFolderName = "osgi";
			try {
				file = (File)PlugIns.downLoad(appJarPath, destFolderName, requestId);
			} catch (Exception e) {
				logger.error("App jar download failed, exiting...{}", e.getMessage());
				reclaimStorage(CACHELOC);
				throw new RuntimeException(e);
			}

		} else {
			//app jar may be sourced within the container
			file = new File(appJarPath);
		}

		logger.info("App jar: " + file.getAbsolutePath());
		logger.debug("App jar file is readable : " + file.canRead());
		logger.info("App jar size: " + file.length() / (1024 * 1024) + " MB");
		logger.info("App jar download time: " + new Date(file.lastModified()).toString());

		logger.debug("Used storage space of " + tmpFolder.toString() + " folder is "
				+ folderSize(tmpFolder.toPath()) / (1024 * 1024) + " MB");

		if (!file.exists() || (file.exists() && file.length() == 0)) {
			logger.error("Unable to download App Jar from s3 or find it in container");
			reclaimStorage(CACHELOC);
			throw new RuntimeException("Unable to download App Jar from s3 or find it in container");
		}



		logger.info("Executing App jar..");
		
		String className = null;
		if (event.containsKey("class") && event.get("class") != null)
			className = event.get("class").trim();
		String methodName = "main"; // convention over configuration. But for dm client, it must be mainProxy
		if (event.containsKey("method") && event.get("method") != null)
			methodName = event.get("method").trim();
		String[] args = null;
		if (event.containsKey("command_line") && event.get("command_line") != null)
			args = translateCommandline(event.get("command_line").trim());
		logger.info("Translated arguments: " + Arrays.toString(args));


		int exitCode = 999;
		try {
			exitCode = runAppJar(file, className, methodName, args, dbgClassName);
		} catch (Exception e) {
			reclaimStorage(CACHELOC);
			throw new RuntimeException(e);
		}

		logger.info("App Jar execution complete");
		logger.debug("Used storage space of " + tmpFolder.toString() + " folder is "
				+ folderSize(tmpFolder.toPath()) / (1024 * 1024) + " MB");

		String response = "Exit code: " + exitCode + ", Log Group Name: " + logGroupName + ", Log Stream Name: "
				+ logStreamName + ", RequestId: " + requestId;
		if (exitCode != 0) {
			reclaimStorage(CACHELOC);
			throw new RuntimeException("Error running Jams job: " + response);
		}

		

		logger.debug("Used storage space of " + tmpFolder.toString() + " folder is "
				+ folderSize(tmpFolder.toPath()) / (1024 * 1024) + " MB");

		return response;
	}
	
	private Integer runAppJar(File jar, String className, String methodName, String[] args, String dbgClassName) throws Exception  {
		
		
		Integer exitCode = 999;
		URL url = jar.toURI().toURL();
		
		URLClassLoader appLoader = new URLClassLoader(new URL[] { url }, this.getClass().getClassLoader());

		Class classToLoad;
		Object result;

		try {
			classToLoad = Class.forName(className, true, appLoader);
		} catch (ClassNotFoundException e) {
			if (appLoader != null)
				try {
					appLoader.close();
				} catch (IOException e1) {
					logger.error("Error closing app class loader during clean up, silencing exception");
				}
			throw e;
		}
			// set log level to debug for given Application class
		if (dbgClassName != null) {
			configureLogging(dbgClassName,appLoader);
		}
		Method method;
		try {
			method = classToLoad.getMethod(methodName, String[].class);
		} catch (NoSuchMethodException | SecurityException e) {
			if (appLoader != null)
				try {
					appLoader.close();
				} catch (IOException e1) {
					logger.error("Error closing app class loader during clean up, silencing exception");
				}
			throw e;
		}
		// relay command line arguments to main or a custom main and capture exit code
		try {
			result = method.invoke(null, (Object) args);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			if (appLoader != null)
				try {
					appLoader.close();
				} catch (IOException e1) {
					logger.error("Error closing app class loader during clean up, silencing exception");
				}
			throw e;
		}
		
		
		return (Integer) result;

	}
	
	private static void configureLogging(String className, ClassLoader cl) {
		
			if ("root".equals(className)) {
				logger.info("setting log level to debug for root");
				Configurator.setAllLevels(LogManager.getRootLogger().getName(), Level.DEBUG);
			} else {
				logger.info("setting log level to debug for class: " + className);
				Class targetClass = null;
				try {
					targetClass = Class.forName(className, true, cl);
					Configurator.setLevel(LogManager.getLogger(targetClass).getName(), Level.DEBUG);
				} catch (ClassNotFoundException e) {
					logger.info("class " + className + " does not exist");

				}

			}
	}

	private static void reclaimStorage(String cacheLocation) {

		PlugIns.stop();
		try {
			logger.info("reclaiming storage for subsequent invocations");
			FileUtils.cleanDirectory(new File(cacheLocation));
		} catch (Exception e) {
			logger.error("exception while reclaiming storage, silencing...");
		}
	}

	/**
	 * [https://stackoverflow.com/questions/3259143/split-a-string-containing-command-line-parameters-into-a-string-in-java]
	 * [code borrowed from ant.jar]
	 * 
	 * @param toProcess the command line to process.
	 * @return the command line broken into strings. An empty or null toProcess
	 *         parameter results in a zero sized array.
	 */
	private static String[] translateCommandline(String toProcess) {
		if (toProcess == null || toProcess.length() == 0) {
			// no command? no string
			return new String[0];
		}
		// parse with a simple finite state machine

		final int normal = 0;
		final int inQuote = 1;
		final int inDoubleQuote = 2;
		int state = normal;
		final StringTokenizer tok = new StringTokenizer(toProcess, "\"\' ", true);
		final ArrayList<String> result = new ArrayList<String>();
		final StringBuilder current = new StringBuilder();
		boolean lastTokenHasBeenQuoted = false;

		while (tok.hasMoreTokens()) {
			String nextTok = tok.nextToken();
			switch (state) {
			case inQuote:
				if ("\'".equals(nextTok)) {
					lastTokenHasBeenQuoted = true;
					state = normal;
				} else {
					current.append(nextTok);
				}
				break;
			case inDoubleQuote:
				if ("\"".equals(nextTok)) {
					lastTokenHasBeenQuoted = true;
					state = normal;
				} else {
					current.append(nextTok);
				}
				break;
			default:
				if ("\'".equals(nextTok)) {
					state = inQuote;
				} else if ("\"".equals(nextTok)) {
					state = inDoubleQuote;
				} else if (" ".equals(nextTok)) {
					if (lastTokenHasBeenQuoted || current.length() != 0) {
						result.add(current.toString());
						current.setLength(0);
					}
				} else {
					current.append(nextTok);
				}
				lastTokenHasBeenQuoted = false;
				break;
			}
		}
		if (lastTokenHasBeenQuoted || current.length() != 0) {
			result.add(current.toString());
		}
		if (state == inQuote || state == inDoubleQuote) {
			throw new RuntimeException("unbalanced quotes in " + toProcess);
		}
		return result.toArray(new String[result.size()]);
	}

	/*
	 * helper method to keep track of /tmp folder size
	 * 
	 */
	private static long folderSize(Path path) {

		final AtomicLong size = new AtomicLong(0);

		try {
			Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {

					size.addAndGet(attrs.size());
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file, IOException exc) {

					logger.info("skipped: " + file + " (" + exc + ")");
					// Skip folders that can't be traversed
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) {

					if (exc != null)
						logger.info("had trouble traversing: " + dir + " (" + exc + ")");
					// Ignore errors traversing a folder
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			throw new AssertionError("walkFileTree will not throw IOException if the FileVisitor does not");
		}

		return size.get();
	}

}
