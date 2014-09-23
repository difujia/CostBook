package costbook.data.cost;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

/**
 * Singleton
 * Main database managing cost records. Broadcasts data changes.
 */
public class CostDatabase {
	private static final String		TAG				= "CurrencyDatabase";
	
	/*
	 * broadcast keys
	 */
	public static final String		DATA_DID_CHANGE	= "costs data changed";
	public static final String		CHANGED_ON_DATE	= "data changed on date";

	private static CostDatabase	mInstance;
	private static Context			mContext;
	private CostsDbOpenHelper		helper;
	private SQLiteDatabase			db;
	private DateFormat				formatter;

	/*
	 * database configuration
	 */
	private static final String		databaseName	= "costs";
	private static final int		version			= 1;
	private static final String		TABLE			= "costs";
	
	/*
	 * schema
	 */
	private static final String		ID				= "_id";
	private static final String		DATE			= "date";
	private static final String		CATEGORY		= "category";
	private static final String		REMARK			= "remark";
	private static final String		CURRENCY		= "currency";
	private static final String		AMOUNT			= "amount";

	private String					setupSQL		= "create table " + TABLE + " (" + ID
													+ " INTEGER PRIMARY KEY AUTOINCREMENT, "
													+ DATE + " TEXT, " + CATEGORY + " TEXT, "
													+ REMARK + " TEXT, " + CURRENCY + " TEXT, "
													+ AMOUNT + " REAL)";

	/*
	 * constructor
	 */
	private CostDatabase(Context context) {
		helper = new CostsDbOpenHelper(context.getApplicationContext());
		mContext = context;
		db = helper.getWritableDatabase();
		formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
	}

	public static CostDatabase getInstance(Context context) {
		if (mInstance == null) {
			mInstance = new CostDatabase(context);
		}
		return mInstance;
	}

	/*
	 * CRUD
	 */
	public void putCost(Cost cost) {
		if (cost.getId() > 0) {
			// update
			int result = db.update(TABLE, 
									toContentValues(cost), 
									ID + " = ?", 
									new String[] { String.valueOf(cost.getId()) });
			if (result != 1) Log.e(TAG, "unknown incoming cost id");
		} else {
			// insert new item
			long result = db.insert(TABLE, null, toContentValues(cost));
			if (result == -1) Log.e(TAG, "database error, insert failed");
		}
		notifyDataDidChangeOnDate(cost.getDate());
	}

	/**
	 * Delete the given cost record in the database.
	 * 
	 * @param cost
	 *            target record to delete
	 */
	public void deleteCost(Cost cost) {
		db.delete(TABLE, ID + " = ?", new String[] { String.valueOf(cost.getId()) });
		notifyDataDidChangeOnDate(cost.getDate());
	}

	/**
	 * Query for all the cost records of a single day.
	 * 
	 * @param date
	 * @return List of cost records matching the given date or empty list if no
	 *         records are found.
	 */
	public List<Cost> getCostsByDate(Date date) {
		String dateString = formatter.format(date);
		Cursor c = db.rawQuery("select * from " + TABLE + " where " + DATE + " = ?" + " ORDER BY " + ID + " DESC",
				new String[] { dateString });
		ArrayList<Cost> costs = new ArrayList<Cost>(c.getCount());
		while (c.moveToNext()) {
			costs.add(toCost(c));
		}
		c.close();
		return costs;
	}

	/**
	 * Query for all the cost records in a range of days.
	 * 
	 * @param start
	 *            the earliest day in the range
	 * @param end
	 *            the last day in the range
	 * @return List of cost records in the range of days or empty list if no
	 *         records are found.
	 */
	public List<Cost> getCostsBetween(Date start, Date end) {
		String startString = formatter.format(start);
		String endString = formatter.format(end);
		Cursor c = db.rawQuery("select * from " + TABLE + " where " + DATE + " between ? and ?", new String[] {
				startString, endString });
		ArrayList<Cost> costs = new ArrayList<Cost>(c.getCount());
		while (c.moveToNext()) {
			costs.add(toCost(c));
		}
		c.close();
		return costs;
	}

	/*
	 *  helpers
	 */
	private ContentValues toContentValues(Cost cost) {
		String dateString = formatter.format(cost.getDate());
		ContentValues values = new ContentValues();
		values.put(DATE, dateString);
		values.put(CATEGORY, cost.getCategory());
		values.put(REMARK, cost.getRemark());
		values.put(CURRENCY, cost.getCurrency());
		values.put(AMOUNT, cost.getAmount());
		return values;
	}

	private Cost toCost(Cursor c) {
		Cost item = new Cost();
		item.setId(c.getInt(c.getColumnIndex(ID)));
		item.setAmount(c.getDouble(c.getColumnIndex(AMOUNT)));
		item.setCategory(c.getString(c.getColumnIndex(CATEGORY)));
		item.setCurrency(c.getString(c.getColumnIndex(CURRENCY)));
		item.setRemark(c.getString(c.getColumnIndex(REMARK)));
		Date date;
		try {
			date = formatter.parse(c.getString(c.getColumnIndex(DATE)));
			item.setDate(date);
		} catch (ParseException e) {
			Log.e(TAG, e.toString());
		}
		return item;
	}

	private void notifyDataDidChangeOnDate(Date date) {
		Intent intent = new Intent(DATA_DID_CHANGE);
		intent.putExtra(CHANGED_ON_DATE, date);
		LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
	}

	/*
	 *  inner SQLiteOpenHelper class
	 */
	private class CostsDbOpenHelper extends SQLiteOpenHelper {

		public CostsDbOpenHelper(Context context) {
			super(context, databaseName, null, version);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(setupSQL);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

		}
	}
}
