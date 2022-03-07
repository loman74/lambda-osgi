package org.codewhiteboard.lambda.plugins.s3;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Downloader implements BundleActivator  {
	
	private static final Logger logger = LoggerFactory.getLogger(Downloader.class);
	ServiceRegistration<Downloader> registration ;

	public Downloader() {
	}
	
	/* Download fileName from s3 bucket and key. Save it to "tmp" folder 
	 * 
	 */
	public static File downLoad(String jarPath, String destFolder) {
	
		String bucket = jarPath.split("/", 4)[2];
		String key = jarPath.split("/", 4)[3];
		String[] keyParts = jarPath.split("/");
		String fileName = keyParts[keyParts.length - 1];
		
		logger.info(" Initiating download of " + fileName + " from bucket " + bucket);
		final AmazonS3 s3 = AmazonS3ClientBuilder.standard().withRegion(Regions.US_EAST_1).build();
		
        String tmpDir = System.getProperty("java.io.tmpdir");
        File downloadedFile = new File(tmpDir + File.separator + destFolder + File.separator + fileName);
        
		try {
			S3Object o = s3.getObject(bucket, key);
			S3ObjectInputStream s3is = o.getObjectContent();
			
			FileOutputStream fos = new FileOutputStream(downloadedFile);
			byte[] read_buf = new byte[1024];
			int read_len = 0;
			while ((read_len = s3is.read(read_buf)) > 0) {
				fos.write(read_buf, 0, read_len);
			}
			s3is.close();
			fos.close();
		} catch (AmazonServiceException e) {
			System.out.println("Error " + e);
			throw new RuntimeException(e);
		} catch (FileNotFoundException e) {
			System.out.println("Error " + e);
			throw new RuntimeException(e);
		} catch (IOException e) {
			System.out.println("Error " + e);
			throw new RuntimeException(e);
		}

		logger.info("download completed");
		return downloadedFile;
		
	}


    @Override public void start(BundleContext context) throws Exception {
        logger.info("Registering " + Downloader.class.getName());
        registration = (ServiceRegistration<Downloader>)context.registerService(Downloader.class.getName(), this, null);

    }

    @Override public void stop(BundleContext context) throws Exception {
        logger.info("Unregistering " + Downloader.class.getName());
        registration.unregister();
    }



}
