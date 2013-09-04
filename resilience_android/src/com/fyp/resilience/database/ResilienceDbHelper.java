
package com.fyp.resilience.database;

import java.sql.SQLException;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.fyp.resilience.Flags;
import com.fyp.resilience.database.model.DataPiece;
import com.fyp.resilience.database.model.DataWhole;
import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

public class ResilienceDbHelper extends OrmLiteSqliteOpenHelper {

    /* Database specific constants */
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "Resilience.db";

    /* List of table classes */
    private static final Class<?>[] TABLE_CLASSES = {
            DataWhole.class,
            DataPiece.class
    };

    /* List of DAO objects */
    private Dao<DataWhole, String> mDataWholeDao;
    private Dao<DataPiece, String> mDataPieceDao;

    public ResilienceDbHelper(final Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(final SQLiteDatabase arg0, final ConnectionSource arg1) {
        try {
            if (Flags.DEBUG) {
                // TODO: Add logging
            }
            /* Create each of the table classes */
            for (Class<?> tableClass : TABLE_CLASSES) {
                TableUtils.createTable(connectionSource, tableClass);
            }

        } catch (SQLException e) {
            // TODO: Add logging
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onUpgrade(final SQLiteDatabase arg0, final ConnectionSource arg1, final int arg2, final int arg3) {
        //TODO: Have a think about this!
    }

    public Dao<DataWhole, String> getDataWholeDao() throws SQLException {
        if (null == mDataWholeDao) {
            mDataWholeDao = getDao(DataWhole.class);
        }
        return mDataWholeDao;
    }

    public Dao<DataPiece, String> getDataPieceDao() throws SQLException {
        if (null == mDataPieceDao) {
            mDataPieceDao = getDao(DataPiece.class);
        }
        return mDataPieceDao;
    }

    @Override
    public void close() {
        mDataWholeDao = null;
        mDataPieceDao = null;
        super.close();
    }
}
