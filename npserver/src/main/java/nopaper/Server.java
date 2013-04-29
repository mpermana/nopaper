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

import java.io.File;
import java.io.FileInputStream;
import java.io.StringWriter;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.bson.BSONObject;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spark.Request;
import spark.Response;
import spark.Spark;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import com.mongodb.util.JSON;

public class Server {
	private static final Logger logger = LoggerFactory.getLogger("logger");

	private static DB database;

	static abstract class Route extends spark.Route {

		protected DBCollection collection;

		static String prefix = "/npserver";

		protected Route(String path) {
			super(prefix + path);
			logger.info(prefix + path);
		}

		protected StringWriter writer;

		@Override
		public Object handle(Request request, Response response) {
			writer = new StringWriter();
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
				writer.write("{\"error\":\"" + e.getMessage() + "\"}");
				response.status(400);
				return writer;
			}

		}

		public Object getId(Request request) {
			String id = request.params(":id");
			if (id != null && id.length() == 24) {
				return new ObjectId(id);
			}
			return id;
		}

		abstract public Object myHandle(Request request, Response response);

	}

	public static void setCORSResponseHeader(final Response response) {
		response.header("Access-Control-Allow-Origin", "*");
		response.header("Access-Control-Allow-Headers",
				"Origin, X-Requested-With, Content-Type, Accept");
		response.header("Access-Control-Allow-Methods", "POST, PUT");
	}

	public static void main(String[] args) throws UnknownHostException {

		MongoClient client = new MongoClient(new ServerAddress(
				"taru.zeeses.com", 27017));
		database = client.getDB("test");

		Spark.options(new spark.Route("*") {
			@Override
			public Object handle(final Request request, final Response response) {
				setCORSResponseHeader(response);
				response.status(200);
				return new StringWriter();
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
				writer.write(cursor.toArray().toString());
				return writer;
			}
		});

		Spark.post(new Route("/db/:collection") {
			@Override
			public Object myHandle(final Request request,
					final Response response) {
				DBObject object = (DBObject) JSON.parse(request.body());

				collection.save(object);
				if (request.queryParams("fetch") != null) {
					DBCursor cursor = collection.find();
					writer.write(cursor.toArray().toString());
				} else {
					writer.write(object.toString());
				}
				return writer;
			}
		});

		Spark.put(new Route("/db/:collection/:id") {
			@Override
			public Object myHandle(final Request request,
					final Response response) {
				DBObject object = (DBObject) JSON.parse(request.body());
				collection.save(object);
				writer.write(object.toString());
				return writer;
			}
		});

		Spark.get(new Route("/db/:collection/:id") {
			@Override
			public Object myHandle(final Request request,
					final Response response) {
				DBObject one = collection.findOne(getId(request));
				if (one == null)
					writer.write("null");
				else
					writer.write(one.toString());
				return writer;
			}
		});

		Spark.get(new Route("/") {
			@Override
			public Object myHandle(final Request request,
					final Response response) {
				StringWriter writer = new StringWriter();
				writer.write("{'status':'ok'}");
				return writer;
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
					String input = Config.fileData.getPath() + "/pdf/" + filename;
					
					String output = Config.fileData.getPath() + "/output";
					File fileOutputDirectory = new File(output);
					fileOutputDirectory.mkdirs();
					File tempFile = File.createTempFile(userId, ".pdf", fileOutputDirectory);
					
					DBObject me = getMe(userId);
					BSONObject fb = (BSONObject) me.get("fb");

					Map<String, String> fieldValues = new HashMap<String, String>();
					fieldValues.put("topmostSubform[0].Page1[0].Entity[0].p1-t4[0]", fb.get("first_name").toString());
					fieldValues.put("topmostSubform[0].Page1[0].Entity[0].p1-t5[0]", fb.get("first_name").toString());
					System.out.println(tempFile.getAbsolutePath());
					ReadWritePDF.createFilledPDF(input, tempFile.getAbsolutePath(), fieldValues);

					byte[] b = new byte[(int) tempFile.length()];
					FileInputStream fis = new FileInputStream (tempFile);
					fis.read(b);
					fis.close();
					response.raw().getOutputStream().write(b);
					tempFile.delete();
					return null;
				} catch (Exception e) {
					e.printStackTrace();
					throw new RuntimeException(e);
				}
			}
		});
	}

	protected static DBObject getMe(String userId) {
		DBCollection collection = database.getCollection("me");
		return collection.findOne(userId);
	}
	protected static String getNoPaperValues(String string) {
		// TODO Auto-generated method stub
		return "michael";
	}
}
