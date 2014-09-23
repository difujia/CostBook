package costbook.data.currency;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.PreferenceManager;
import android.util.Log;

import com.costs.R;

import costbook.data.cost.Cost;

/**
 * Singleton
 * Currency database, maintained by CurrencyUpdateService
 * Inner Converter class facilitates conversion of currencies
 */
public class CurrencyManager {
	private static final String		TAG				= "CurrencyManager";

	private static CurrencyManager	instance;
	private Context					mContext;
	private CurrencyDbOpenHelper	helper;
	private SQLiteDatabase			db;

	/*
	 * database configuration
	 */
	private static final String		databaseName	= "currency";
	private static final int		version			= 1;
	private static final String		TABLE			= "currency";
	
	/*
	 * schema
	 */
	private static final String		ID				= "_id";
	private static final String		CODE			= "code";
	private static final String		RATE			= "rate";

	private String					setupSQL		= "create table " + TABLE + " (" 
													+ ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " 
													+ CODE + " TEXT, "
													+ RATE + " REAL)";

	/*
	 * constructor
	 */
	private CurrencyManager(Context context) {
		this.helper = new CurrencyDbOpenHelper(context.getApplicationContext());
		this.mContext = context;
		this.db = helper.getWritableDatabase();
	}
	
	
	public static CurrencyManager getInstance(Context context) {
		if (instance == null) {
			instance = new CurrencyManager(context);
		}
		return instance;
	}

	/*
	 *  factory for Converter class
	 */
	public Converter getConverter() {
		return new Converter();
	}

	/**
	 * @param code
	 *            currency code
	 * @return exchange rate based on USD (USD = 1)
	 */
	public double getRateByCode(String code) {
		Cursor c = db.query(TABLE, 
							new String[] { RATE }, 
							CODE + " = ?", 
							new String[] { code }, 
							null, null, null);
		c.moveToFirst();
		return c.getDouble(c.getColumnIndex(RATE));
	}

	/**
	 * Update exchange rate by currency code. New item will be created if the code does not exist in database.
	 * @param code
	 * @param rate
	 * @return
	 */
	public int updateRateByCode(String code, double rate) {
		ContentValues values = new ContentValues();
		values.put(RATE, rate);
		int affected = db.update(TABLE, values, CODE + " = ?", new String[] { code });
		if (affected == 0) {
			helper.addCurrency(db, code, rate);
		}
		return affected;
	}

	/*
	 *  inner SQLiteOpenHelper class
	 */
	private class CurrencyDbOpenHelper extends SQLiteOpenHelper {

		public CurrencyDbOpenHelper(Context context) {
			super(context, databaseName, null, version);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(setupSQL);
			/*
			 * add some initial currencies from string-array
			 */
			String[] codes = mContext.getResources().getStringArray(R.array.currency_codes);
			for (int i = 0; i < codes.length; i++) {
				addCurrency(db, codes[i], 1);
			}
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.e(TAG, "unexpected database upgrade");
		}

		/*
		 * convenience methods for addition
		 */
		private void addCurrency(SQLiteDatabase db, String code, double rate) {
			ContentValues cv = new ContentValues();
			cv.put(CODE, code);
			cv.put(RATE, rate);
			db.insert(TABLE, null, cv);
		}
	}

	/**
	 * Facility for converting currency to primary currency
	 */
	public class Converter {
		
		private String	primary;

		private Converter() {
			SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
			primary = sp.getString(mContext.getResources().getString(R.string.pref_key_primary_currency), null);
		}

		/**
		 * Convert given cost record to primary currency, original object is not
		 * modified
		 * 
		 * @param cost
		 * @return amount in primary currency
		 */
		public double toPrimaryCurrency(Cost cost) {
			String used = cost.getCurrency();
			if (primary.equals(used)) {
				return cost.getAmount();
			} else {
				double primaryRate = getRateByCode(primary);
				double usedRate = getRateByCode(used);
				return cost.getAmount() * primaryRate / usedRate;
			}
		}
	}
}
