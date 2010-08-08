package net.sparktank.morrigan.model.pictures.gallery;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import net.sparktank.morrigan.helpers.RecyclingFactory;
import net.sparktank.morrigan.model.DbColumn;
import net.sparktank.morrigan.model.MediaSqliteLayer2;
import net.sparktank.morrigan.model.pictures.MediaPicture;
import net.sparktank.sqlitewrapper.DbException;

public class GallerySqliteLayer extends MediaSqliteLayer2<MediaPicture> {
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Factory.
	
	public static final DbConFactory FACTORY = new DbConFactory();
	
	public static class DbConFactory extends RecyclingFactory<GallerySqliteLayer, String, Void, DbException> {
		
		DbConFactory() {
			super(true);
		}
		
		@Override
		protected boolean isValidProduct(GallerySqliteLayer product) {
			return true;
		}
		
		@Override
		protected GallerySqliteLayer makeNewProduct(String material) throws DbException {
			return new GallerySqliteLayer(material);
		}
		
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Constructors.
	
	protected GallerySqliteLayer (String dbFilePath) throws DbException {
		super(dbFilePath);
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Public methods.
	
	public void setDimensions (String sfile, int width, int height) throws DbException {
		try {
			local_setDimensions(sfile, width, height);
		} catch (Exception e) {
			throw new DbException(e);
		}
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
	
	public static final DbColumn SQL_TBL_MEDIAFILES_COL_WIDTH =  new DbColumn("lwidth",  "width",  "INT(6)",   "0");
	public static final DbColumn SQL_TBL_MEDIAFILES_COL_HEIGHT = new DbColumn("lheight", "height", "INT(6)",   "0");
	
	public static final DbColumn[] SQL_TBL_MEDIAFILES_COLS2 = new DbColumn[] {
		SQL_TBL_MEDIAFILES_COL_WIDTH,
		SQL_TBL_MEDIAFILES_COL_HEIGHT,
		};
	
	static public DbColumn parseColumnFromName (String name) {
		for (DbColumn c : SQL_TBL_MEDIAFILES_COLS) {
			if (c.getName().equals(name)) {
				return c;
			}
		}
		for (DbColumn c : SQL_TBL_MEDIAFILES_COLS2) {
			if (c.getName().equals(name)) {
				return c;
			}
		}
		throw new IllegalArgumentException("Can't parse '"+name+"'.");
	}
	
	@Override
	protected List<DbColumn> generateSqlTblMediaFilesColumns() {
		List<DbColumn> l = super.generateSqlTblMediaFilesColumns();
		for (DbColumn c : SQL_TBL_MEDIAFILES_COLS2) {
			l.add(c);
		}
		return l;
	}
	
	private static final String SQL_TBL_MEDIAFILES_SETDIMENSIONS =
		"UPDATE tbl_mediafiles SET lwidth=?,lheight=?" +
		" WHERE sfile=?;";
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Types.
	
	@Override
	protected MediaPicture getNewT() {
		return new MediaPicture();
	}
	
	@Override
	protected void setTFromRs(MediaPicture item, ResultSet rs) throws SQLException {
		item.setWidth(rs.getInt(SQL_TBL_MEDIAFILES_COL_WIDTH.getName()));
		item.setHeight(rs.getInt(SQL_TBL_MEDIAFILES_COL_HEIGHT.getName()));
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//	Private methods for tbl_mediafiles.
	
	private void local_setDimensions(String sfile, int width, int height) throws SQLException, ClassNotFoundException, DbException {
		PreparedStatement ps;
		ps = getDbCon().prepareStatement(SQL_TBL_MEDIAFILES_SETDIMENSIONS);
		int n;
		try {
			ps.setInt(1, width);
			ps.setInt(2, height);
			ps.setString(3, sfile);
			n = ps.executeUpdate();
		} finally {
			ps.close();
		}
		if (n<1) throw new DbException("No update occured for setRemoteLocation('"+sfile+"','"+width+"','"+height+"').");
	}
	
//	- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
}
