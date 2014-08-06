package controller;

import java.io.IOException;
import java.io.StringWriter;

import javax.servlet.ServletOutputStream;

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
import com.mongodb.util.JSONSerializers;

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
		} else if (defaultValue instanceof Boolean) {
			return (T) Boolean.valueOf(param);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static <T> T getParam(Request request, String queryParam,
			Class<T> clazz) {
		String param = request.queryParams(queryParam);
		if (null == param) {
			return null;
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
		Spark.get(new Route("/db") {
			public Object myHandle(final Request request,
					final Response response, DBCollection collection) {
				com.mongodb.DB database = client.getDB(getConfigValue(request,
						"database", "test"));
				return JSONSerializers.getStrict().serialize(
						database.getCollectionNames());
			}
		});

		Spark.get(new Route("/db/:collection") {
			@Override
			/**
			 * http://192.168.1.80/npserver/db/note?query={title:%22Note%208%22}
			 * http://192.168.1.80/npserver/db/note?orderBy={_id:-1}&query={_id:{$gte:{$oid:%22534a36ace4b092ccb66a3e02%22}}}&limit=1
			 */
			public Object myHandle(final Request request,
					final Response response, DBCollection collection) {
				DBObject filter = getParam(request, "query", DBObject.class);
				DBCursor cursor = collection.find(filter)
						.skip(getParam(request, "skip", 0))
						.limit(getParam(request, "limit", 0));
				DBObject sort = getParam(request, "sort", DBObject.class);
				cursor.sort(sort);
				boolean oid = getParam(request, "oid", useOid);
				try {
					ServletOutputStream os = response.raw().getOutputStream();
					os.write('[');
					byte[] separator = "".getBytes();
					byte[] separatorLines = ",\n".getBytes();
					for (DBObject dbObject : cursor) {
						os.write(separator);
						if (!oid) replaceIdWithString(dbObject);
						// TODO mapper
						// result = mapper(dbObject)
						os.write(dbObject.toString().getBytes());
						separator = separatorLines;
					}
					os.write(']');
					return "";
				} catch (IOException e) {
					return e;
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

		Spark.get(new Route("/db/:collection/distinct/:key") {
			@Override
			public Object myHandle(final Request request,
					final Response response, DBCollection collection) {
				return collection.distinct(request.params("key"));
			}
		});

	}
}
