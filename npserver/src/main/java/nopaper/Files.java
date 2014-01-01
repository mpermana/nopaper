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

	public static String list(String path) {
		StringBuilder builder = new StringBuilder();
		File[] files = new File(path).listFiles();
		builder.append("[\n");
		String prefix = "";
		for (File file : files) {
			builder.append(prefix);
			builder.append("{\n");
			builder.append("\t\"filename\":\""+ file.getName().replaceAll("\\\\", "\\\\\\\\") +"\",\n");
			builder.append("\t\"lastModified\":"+ file.lastModified() +"\n");
			builder.append("}");
			prefix = ",\n";
		}
		builder.append("]\n");
		return builder.toString();
	}

}
