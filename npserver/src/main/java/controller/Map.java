package controller;

import org.bson.BSONObject;

import nopaper.Server.Route;
import spark.Request;
import spark.Response;
import spark.Spark;

import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class Map extends Base {

	protected DBObject trackFindOne(String userId) {
		DBCollection collection = database.getCollection("track");
		return collection.findOne(userId);
	}

	public Object getValue(Object o, String json_path) {
		Object currentObject = o;
		String[] paths = json_path.split("\\.");
		for (String path : paths) {
			currentObject = ((BSONObject) currentObject).get(path);
		}
		return currentObject;
	}


	public void addRoutes() {
		Spark.post(new Route("/map/fb_feed/:user_id") {
			@Override
			public Object myHandle(final Request request,
					final Response response, DBCollection collection) {
				try {
					String userId = request.params("user_id");
					DBObject me = trackFindOne(userId);
					String accessToken = getValue(me,
							"fbLoginStatus.authResponse.accessToken")
							.toString();
					String latitude = getValue(me, "coords.latitude")
							.toString();
					String longitude = getValue(me, "coords.longitude")
							.toString();
					String fbUserID = getValue(me,
							"fbLoginStatus.authResponse.userID").toString();
					String command = "/usr/bin/curl -F access_token="
							+ accessToken;
					command += " -F message=https://maps.google.com/maps?q="
							+ latitude + "," + longitude + "+(I%20was%20here)";
					command += " -v https://graph.facebook.com/" + fbUserID
							+ "/feed";
					Process proc = Runtime.getRuntime().exec(command);
					StringBuilder sb = new StringBuilder();
					int b;
					while (true) {
						b = proc.getInputStream().read();
						if (-1 == b)
							break;
						sb.append((char) b);
					}
					proc.getInputStream().close();
					proc.getOutputStream().close();

					return sb.toString();
				} catch (Exception e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			}
		});
		
	}

}
