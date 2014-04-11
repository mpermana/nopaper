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

import static m.T.dict;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spark.Request;
import spark.Response;
import spark.Spark;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;

import controller.PDF;
import controller.TextHTML;

public class Server {
	public static final Logger logger = LoggerFactory.getLogger("logger");

	private static MongoClient client;

	public static abstract class Route extends spark.Route {

		static String prefix = "/npserver";

		protected Route(String path) {
			super(prefix + path);
			logger.info(prefix + path);
		}

		public static String getConfigValue(Request request, String name,
				String defaultValue) {
			String value;
			value = request.queryParams(name);
			if (value != null) {
				return value;
			}
			value = request.headers(name);
			if (value != null) {
				return value;
			}
			value = request.cookie(name);
			if (value != null) {
				return value;
			}
			value = System.getProperties().getProperty(name);
			if (value != null) {
				return value;
			}
			return defaultValue;
		}

		private DBCollection getCollection(Request request) {
			DB database = client.getDB(getConfigValue(request, "database", "test"));
			String collectionName = request.params(":collection");
			if (null != collectionName) {
				return database.getCollection(collectionName);
			}
			return null;
		}

		@Override
		public Object handle(Request request, Response response) {

			response.header("Content-Type", "application/json");
			setCORSResponseHeader(response);
			DBCollection collection = getCollection(request);
			try {
				logger.info(request.toString());
				return myHandle(request, response, collection);
			} catch (Exception e) {
				BasicDBObject x = new BasicDBObject(dict("error", e.toString(),
						"stacktrace", printStackTrace(e)));
				response.status(400);
				return x;
			}
		}

		private String printStackTrace(Exception e) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			PrintStream ps = new PrintStream(baos);
			e.printStackTrace(ps);
			return baos.toString();
		}

		abstract public Object myHandle(Request request, Response response,
				DBCollection collection);

	}

	public static void setCORSResponseHeader(final Response response) {
		response.header("Access-Control-Allow-Origin", "*");
		String snaplogicFuckedUpHeaders = ", x-date, authorization";
		response.header("Access-Control-Allow-Headers",
				"Origin, X-Requested-With, Content-Type, Accept"
						+ snaplogicFuckedUpHeaders);
		response.header("Access-Control-Allow-Methods", "POST, PUT, DELETE");
	}

	public static void main(String[] args) throws UnknownHostException {
		String databaseHostname = System.getProperty("database.hostname");
		if (null == databaseHostname)
			databaseHostname = "duren.dyndns.org";
		client = new MongoClient(databaseHostname, 27017);

		Spark.options(new Route("/*") {
			@Override
			public Object handle(final Request request, final Response response) {
				setCORSResponseHeader(response);
				response.status(200);
				return new StringWriter();
			}

			@Override
			public Object myHandle(Request request, Response response,
					DBCollection collection) {
				return null;
			}
		});

		Spark.get(new Route("/system/:command") {
			@Override
			public Object myHandle(final Request request,
					final Response response, DBCollection collection) {
				String command = request.params(":command");
				System.out.println("system command:" + command);
				if (command.equals("shutdown")) {
					System.exit(0);
				}
				if (command.equals("ls")) {
					String path = request.queryParams("path");
					if (null == path) {
						path = "/";
					}
					return Files.list(path);
				}
				if (command.equals("sleep")) {
					String seconds = request.queryParams("seconds");
					if (null == seconds) {
						seconds = "1";
					}
					int i = Integer.parseInt(seconds);
					try {
						Thread.sleep(i * 1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					return System.currentTimeMillis();
				}
				return null;
			}
		});

		PDF pdf = new PDF();
		pdf.setDatabase(client.getDB("test"));
		pdf.addRoutes();

		controller.DB db = new controller.DB();
		db.addRoutes();

		Spark.get(new Route("/") {
			@Override
			public Object myHandle(final Request request,
					final Response response, DBCollection collection) {
				try {
					return new BasicDBObject(dict("status", "ok", "hostname",
							InetAddress.getLocalHost().getHostName()));
				} catch (UnknownHostException e) {
					throw new RuntimeException(e);
				}
			}
		});

		controller.Convert convert = new controller.Convert();
		convert.setDatabase(client.getDB("test"));
		convert.addRoutes();

		controller.Map map = new controller.Map();
		map.addRoutes();

		TextHTML textHtml = new TextHTML();
		textHtml.addRoutes();
	}

}
