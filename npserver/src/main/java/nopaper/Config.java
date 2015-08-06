package nopaper;

import java.io.File;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.EnvironmentConfiguration;
import org.apache.commons.configuration.SystemConfiguration;

public class Config {

	static CompositeConfiguration config = new CompositeConfiguration();
	static String dataDir = "";
	static {
		config.addConfiguration(new SystemConfiguration());
		config.addConfiguration(new EnvironmentConfiguration());
		dataDir = config.getString("NPDATA");
		if (dataDir == null) {
			dataDir = "/Users/mpermana/projects/nopaper/data";
		}
		Server.logger.info(dataDir);
	}
	
	public static File fileData = new File(dataDir);
	

}
