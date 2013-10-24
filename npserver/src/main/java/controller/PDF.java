package controller;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

import nopaper.Config;
import nopaper.Files;
import nopaper.ReadWritePDF;
import nopaper.Server.Route;

import org.apache.pdfbox.io.IOUtils;
import org.bson.BSONObject;

import spark.Request;
import spark.Response;
import spark.Spark;

import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class PDF extends Base {

	DBObject getMe(String userId) {
		DBCollection collection = database.getCollection("me");
		return collection.findOne(userId);
	}

	public void addRoutes() {
		Spark.get(new Route("/pdf/:user_id/:filename") {
			@Override
			public Object myHandle(final Request request,
					final Response response, DBCollection collection) {
				response.header("Content-Type", "application/pdf");

				try {
					String userId = request.params("user_id");
					String filename = request.params("filename");
					String input = Config.fileData.getPath() + "/pdf/"
							+ filename;

					File tempFile = Files.createTempFile(userId, ".pdf");

					DBObject me = getMe(userId);
					BSONObject fb = (BSONObject) me.get("fb");

					Map<String, String> fieldValues = new HashMap<String, String>();
					fieldValues.put(
							"topmostSubform[0].Page1[0].Entity[0].p1-t4[0]", fb
									.get("first_name").toString());
					fieldValues.put(
							"topmostSubform[0].Page1[0].Entity[0].p1-t5[0]", fb
									.get("last_name").toString());
					System.out.println(tempFile.getAbsolutePath());
					ReadWritePDF.createFilledPDF(input,
							tempFile.getAbsolutePath(), fieldValues);

					try (FileInputStream fis = new FileInputStream(tempFile)) {
						IOUtils.copy(fis, response.raw().getOutputStream());
					}
					tempFile.delete();
					return null;
				} catch (Exception e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			}
		});

	}


}
