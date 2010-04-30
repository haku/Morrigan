package net.sparktank.morrigan.model.library;

import java.util.HashMap;
import java.util.Map;

public class DbConFactory {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private static Map<String,SqliteLayer> sqliteLayer = new HashMap<String, SqliteLayer>();
	
	public static SqliteLayer getDbLayer (String dbFilePath) throws DbException {
		
		if (!sqliteLayer.containsKey(dbFilePath)) {
			sqliteLayer.put(dbFilePath, new SqliteLayer(dbFilePath));
		}
		
		return sqliteLayer.get(dbFilePath);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
