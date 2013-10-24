package controller;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class Base {

	DB database;

	public void setDatabase(DB database) {
		this.database = database;
	}

	protected DBObject findOne(String collectionName, String id) {
		DBCollection collection = database.getCollection(collectionName);
		return collection.findOne(id);
	}

}
