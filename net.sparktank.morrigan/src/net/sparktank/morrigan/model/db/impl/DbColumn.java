package net.sparktank.morrigan.model.db.impl;

import net.sparktank.morrigan.model.db.interfaces.IDbColumn;

public class DbColumn implements IDbColumn {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	private String name;
	private String defaultValue;
	private String sqlType;
	private final String humanName;
	private final String sortOpts;
	
	public DbColumn (String name, String humanName, String sqlType, String defaultValue, String sortOpts) {
		this.setName(name);
		this.humanName = humanName;
		this.setSqlType(sqlType);
		this.setDefaultValue(defaultValue);
		this.sortOpts = sortOpts;
	}
	
	public DbColumn (String name, String humanName, String sqlType, String defaultValue) {
		this.setName(name);
		this.humanName = humanName;
		this.setSqlType(sqlType);
		this.setDefaultValue(defaultValue);
		this.sortOpts = null;
	}
	
	/* (non-Javadoc)
	 * @see net.sparktank.morrigan.model.db.IDbColumn#setName(java.lang.String)
	 */
	@Override
	public void setName(String name) {
		this.name = name;
	}
	/* (non-Javadoc)
	 * @see net.sparktank.morrigan.model.db.IDbColumn#getName()
	 */
	@Override
	public String getName() {
		return this.name;
	}
	
	/* (non-Javadoc)
	 * @see net.sparktank.morrigan.model.db.IDbColumn#setDefaultValue(java.lang.String)
	 */
	@Override
	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}
	/* (non-Javadoc)
	 * @see net.sparktank.morrigan.model.db.IDbColumn#getDefaultValue()
	 */
	@Override
	public String getDefaultValue() {
		return this.defaultValue;
	}
	
	/* (non-Javadoc)
	 * @see net.sparktank.morrigan.model.db.IDbColumn#setSqlType(java.lang.String)
	 */
	@Override
	public void setSqlType(String sqlType) {
		this.sqlType = sqlType;
	}
	/* (non-Javadoc)
	 * @see net.sparktank.morrigan.model.db.IDbColumn#getSqlType()
	 */
	@Override
	public String getSqlType() {
		return this.sqlType;
	}
	
	/* (non-Javadoc)
	 * @see net.sparktank.morrigan.model.db.IDbColumn#getHumanName()
	 */
	@Override
	public String getHumanName() {
		return this.humanName;
	}
	
	/* (non-Javadoc)
	 * @see net.sparktank.morrigan.model.db.IDbColumn#getSortOpts()
	 */
	@Override
	public String getSortOpts() {
		return this.sortOpts;
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
