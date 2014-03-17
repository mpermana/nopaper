package controller;

import java.io.StringWriter;
import java.util.List;

import nopaper.Server.Route;

import org.bson.types.ObjectId;

import spark.Request;
import spark.Response;
import spark.Spark;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;
import com.mongodb.util.JSON;

public class DB {

	@SuppressWarnings("unchecked")
	public static <T> T getParam(Request request, String queryParam,
			T defaultValue) {
		String param = request.queryParams(queryParam);
		if (null == param) {
			return (T)defaultValue;
		}

		if (defaultValue instanceof Integer) {
			return (T) Integer.valueOf(param);
		} else if (defaultValue instanceof DBObject) {
			return (T) JSON.parse(param);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static <T> T getParam(Request request, String queryParam,
			Class<T> clazz) {
		String param = request.queryParams(queryParam);
		if (null == param) {
			try {
				return clazz.newInstance();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		if (Integer.class.equals(clazz)) {
			return (T) Integer.valueOf(param);
		} else if (DBObject.class.equals(clazz)) {
			return (T) JSON.parse(param);
		}
		return null;
	}

	// ffxiv useOid = true
	boolean useOid = true;

	void replaceIdWithString(DBObject o) {
		String id = o.get("_id").toString();
		o.put("_id", id);
	}

	BasicDBObject queryId(Request request) {
		return new BasicDBObject("_id", getId(request));
	}

	Object getId(Request request) {
		String id = request.params(":id");
		if (useOid) {
			if (id != null && id.length() == 24) {
				return new ObjectId(id);
			}
		} else {
			return id == null ? String.valueOf(System.currentTimeMillis()) : id;
		}

		return id;
	}

	public void addRoutes() {
		Spark.get(new Route("/db/:collection") {
			@Override
			public Object myHandle(final Request request,
					final Response response, DBCollection collection) {
				//String query = request.queryParams("query");
				DBObject filter = getParam(request, "query", DBObject.class);
				//if (query != null) {
				//	filter = (DBObject) JSON.parse(query);
				//}
				DBCursor cursor = collection.find(filter)
						.skip(getParam(request, "skip", 0))
						.limit(getParam(request, "limit", 0));
				try {
					List<DBObject> array = cursor.toArray();
					if (!useOid) {
						for (DBObject o : array) {
							replaceIdWithString(o);
						}
					}
					StringWriter writer = new StringWriter();
					writer.write(array.toString());
					return writer;
				} finally {
					cursor.close();
				}
			}
		});

		Spark.post(new Route("/db/:collection") {
			@Override
			public Object myHandle(final Request request,
					final Response response, DBCollection collection) {
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
		// id : if the length is 24, it'll try to convert the id to Mongo's
		// ObjectId
		Spark.put(new Route("/db/:collection/:id") {
			@Override
			public Object myHandle(final Request request,
					final Response response, DBCollection collection) {

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
					final Response response, DBCollection collection) {
				DBObject one = collection.findOne(queryId(request));
				if (one == null)
					return "null";

				return one.toString();
			}
		});

		Spark.delete(new Route("/db/:collection/:id") {
			@Override
			public Object myHandle(final Request request,
					final Response response, DBCollection collection) {
				WriteResult result = collection.remove(queryId(request));
				return result;
			}
		});

	}
}
