package controller;

import java.io.File;

import nopaper.Files;
import nopaper.Server.Route;

import org.apache.commons.io.FileUtils;

import spark.Request;
import spark.Response;
import spark.Spark;

import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class Convert extends Base {

	public void addRoutes() {
		Spark.get(new Route("/convert/:id") {
			@Override
			public Object myHandle(final Request request,
					final Response response, DBCollection collection) {
				response.header("Content-Type", "image/png");

				DBObject dbObject = findOne("convert", request.params("id"));
				Object svg = dbObject.get("svg");

				File svgFile = Files.createTempFile("snap", ".svg");
				File pngFile = Files.createTempFile("snap", ".png");

				String command = "/usr/bin/convert "
						+ svgFile.getAbsolutePath() + " "
						+ pngFile.getAbsolutePath();
				System.out.println(command);

				try {
					FileUtils.writeStringToFile(svgFile, svg.toString());

					Process p = Runtime.getRuntime().exec(command);
					p.getInputStream().close();
					p.getOutputStream().close();
					p.getErrorStream().close();
					p.waitFor();

					FileUtils.copyFile(pngFile, response.raw()
							.getOutputStream());

				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				return "ya";
			}
		});
	}

}
