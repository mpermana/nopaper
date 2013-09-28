package nopaper;

import java.io.File;
import java.io.IOException;

public class Files {

	public static File createTempFile(String prefix, String suffix) {
		String output = Config.fileData.getPath() + "/output";
		File fileOutputDirectory = new File(output);
		fileOutputDirectory.mkdirs();
		try {
			return File.createTempFile(prefix, suffix, fileOutputDirectory);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
