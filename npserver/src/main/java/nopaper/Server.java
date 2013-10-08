/*
 * Copyright (c) 2008 - 2013 10gen, Inc. <http://10gen.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package nopaper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.io.IOUtils;
import org.bson.BSONObject;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spark.Request;
import spark.Response;
import spark.Spark;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.WriteResult;
import com.mongodb.util.JSON;

public class Server {
	public static final Logger logger = LoggerFactory.getLogger("logger");

	private static DB database;

	// ffxiv useOid = true
	private static boolean useOid = true;

	static abstract class Route extends spark.Route {

		protected DBCollection collection;

		static String prefix = "/npserver";

		protected Route(String path) {
			super(prefix + path);
			logger.info(prefix + path);
		}

		@Override
		public Object handle(Request request, Response response) {
			response.header("Content-Type", "application/json");
			setCORSResponseHeader(response);
			String collectionName = request.params(":collection");
			if (null != collectionName)
				collection = database.getCollection(collectionName);
			else
				collection = null;
			try {
				logger.info(request.toString());
				return myHandle(request, response);
			} catch (Exception e) {
				BasicDBObject x = new BasicDBObject(m.T.dict("error",e.toString(),"stacktrace",printStackTrace(e)));
				response.status(400);
				return x;
			}
		}

		private String printStackTrace(Exception e) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			PrintStream ps =new PrintStream(baos);
			e.printStackTrace(ps);
			return baos.toString();
		}

		public BasicDBObject queryId(Request request) {
			return new BasicDBObject("_id", getId(request));
		}

		public Object getId(Request request) {
			String id = request.params(":id");
			if (useOid) {
				if (id != null && id.length() == 24) {
					return new ObjectId(id);
				}
			} else {
				return id == null ? String.valueOf(System.currentTimeMillis())
						: id;
			}

			return id;
		}

		abstract public Object myHandle(Request request, Response response);

	}

	public static void setCORSResponseHeader(final Response response) {
		response.header("Access-Control-Allow-Origin", "*");
		String snaplogicFuckedUpHeaders = ", x-date, authorization";
		response.header("Access-Control-Allow-Headers",
				"Origin, X-Requested-With, Content-Type, Accept"+
		snaplogicFuckedUpHeaders);
		response.header("Access-Control-Allow-Methods", "POST, PUT, DELETE");
	}

	public static void main(String[] args) throws UnknownHostException {

		MongoClient client = new MongoClient(new ServerAddress(
				"taru.zeeses.com", 27017));
		database = client.getDB("test");

		Spark.options(new Route("/*") {
			@Override
			public Object handle(final Request request, final Response response) {
				setCORSResponseHeader(response);
				response.status(200);
				return new StringWriter();
			}

			@Override
			public Object myHandle(Request request, Response response) {
				// TODO Auto-generated method stub
				return null;
			}
		});

		Spark.get(new Route("/system/:command") {
			@Override
			public Object myHandle(final Request request,
					final Response response) {
				String command = request.params(":command");
				System.out.println("system command:" + command);
				if (command.equals("shutdown")) {
					System.exit(0);
				}
				return null;
			}
		});

		Spark.get(new Route("/db/:collection") {
			@Override
			public Object myHandle(final Request request,
					final Response response) {
				DBCursor cursor = collection.find();
				List<DBObject> array = cursor.toArray();
				if (!useOid) {
					for (DBObject o : array) {
						replaceIdWithString(o);
					}
				}
				StringWriter writer = new StringWriter();
				writer.write(array.toString());
				return writer;
			}
		});

		Spark.post(new Route("/db/:collection") {
			@Override
			public Object myHandle(final Request request,
					final Response response) {
				DBObject object = (DBObject) JSON.parse(request.body());

				collection.save(object);
				StringWriter writer = new StringWriter();
				if (request.queryParams("fetch") != null) {
					DBCursor cursor = collection.find();
					writer.write(cursor.toArray().toString());
				} else {
					if (!useOid) {
						replaceIdWithString(object);
					}
					writer.write(object.toString());
				}
				return writer;
			}
		});

		// perform upsert on 1 object and set only the fields specified
		// id : if the length is 24, it'll try to convert the id to Mongo's ObjectId
		Spark.put(new Route("/db/:collection/:id") {
			@Override
			public Object myHandle(final Request request,
					final Response response) {

				DBObject o = (DBObject) JSON.parse(request.body());
				o.removeField("_id");
				// collection.save(object);

				DBObject query = queryId(request);
				DBObject update = new BasicDBObject("$set", o);
				// collection.update(query, update, true, false);
				DBObject newObject = collection.findAndModify(query, null,
						null, false, update, true, true);

				StringWriter writer = new StringWriter();
				writer.write(newObject.toString());
				return writer;
			}
		});

		Spark.get(new Route("/db/:collection/:id") {
			@Override
			public Object myHandle(final Request request,
					final Response response) {
				DBObject one = collection.findOne(queryId(request));
				if (one == null)
					return "null";

				return one.toString();
			}
		});

		Spark.delete(new Route("/db/:collection/:id") {
			@Override
			public Object myHandle(final Request request,
					final Response response) {
				WriteResult result = collection.remove(queryId(request));
				return result;
			}
		});

		Spark.get(new Route("/") {
			@Override
			public Object myHandle(final Request request,
					final Response response) {
				return "{'status':'ok'}";
			}
		});

		Spark.get(new Route("/convert/:id") {
			@Override
			public Object myHandle(final Request request,
					final Response response) {
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
		Spark.get(new Route("/pdf/:user_id/:filename") {
			@Override
			public Object myHandle(final Request request,
					final Response response) {
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

		Spark.post(new Route("/map/fb_feed/:user_id") {
			@Override
			public Object myHandle(final Request request,
					final Response response) {
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

	public static Object getValue(Object o, String json_path) {
		Object currentObject = o;
		String[] paths = json_path.split("\\.");
		for (String path : paths) {
			currentObject = ((BSONObject) currentObject).get(path);
		}
		return currentObject;
	}

	protected static void replaceIdWithString(DBObject o) {
		String id = o.get("_id").toString();
		o.put("_id", id);
	}

	protected static DBObject getMe(String userId) {
		DBCollection collection = database.getCollection("me");
		return collection.findOne(userId);
	}

	protected static DBObject trackFindOne(String userId) {
		DBCollection collection = database.getCollection("track");
		return collection.findOne(userId);
	}

	protected static DBObject findOne(String collectionName, String id) {
		DBCollection collection = database.getCollection(collectionName);
		return collection.findOne(id);
	}

	protected static String getNoPaperValues(String string) {
		// TODO Auto-generated method stub
		return "michael";
	}

}
