
package com.fyp.resilience.database;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;

import android.content.Context;
import android.util.Log;

import com.fyp.resilience.Flags;
import com.fyp.resilience.ResilienceApplication;
import com.fyp.resilience.database.model.DataPiece;
import com.fyp.resilience.database.model.DataWhole;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;

public class ResilienceDbManager {

    /**
     * A helper method used by functions within this class.
     * 
     * @param {@link Context}
     * @return {@link ResilienceDbHelper}
     */
    private static ResilienceDbHelper getDbHelper(final Context context) {
        return OpenHelperManager.getHelper(context, ResilienceDbHelper.class);
    }

    /**
     * Returns all the DataWholes currently within the application's database.
     * 
     * @param {@link Context}
     * @return a {@link List} of the {@link DataWhole}s within the database.
     */
    public static List<DataWhole> getDataWholes(final Context context) {
        final ResilienceDbHelper dBHelper = getDbHelper(context);
        List<DataWhole> wholes = null;
        List<DataPiece> pieces = null;

        try {
            final Dao<DataWhole, String> wholeDao = dBHelper.getDataWholeDao();
            final Dao<DataPiece, String> pieceDao = dBHelper.getDataPieceDao();

            wholes = wholeDao.queryForAll();

            for (final DataWhole dataWhole : wholes) {
                pieces = pieceDao.queryForEq(DataPiece.COL_WHOLE_ID, dataWhole);

                if (pieces.size() > 0) {
                    dataWhole.setPieces(pieces);
                }
            }

        } catch (SQLException e) {
            if (Flags.DEBUG) {
                Log.e(ResilienceDbManager.class.getSimpleName(), e.getMessage());
            }
        } finally {
            OpenHelperManager.releaseHelper();
        }
        return wholes;
    }

    /**
     * Returns all the DataPieces currently within the application's database.
     * 
     * @param {@link Context}
     * @return a {@link List} of the {@link DataPiece}s within the database.
     */
    public static List<DataPiece> getDataPieces(final Context context) {
        final ResilienceDbHelper dBHelper = getDbHelper(context);
        List<DataPiece> pieces = null;

        try {
            final Dao<DataPiece, String> dao = dBHelper.getDataPieceDao();
            pieces = dao.queryForAll();
        } catch (SQLException e) {
            if (Flags.DEBUG) {
                Log.e(ResilienceDbManager.class.getSimpleName(), e.getMessage());
            }
        } finally {
            OpenHelperManager.releaseHelper();
        }
        return pieces;
    }

    /**
     * Persists a DataWhole, along with its attached DataPieces, to the
     * database. Uses a batch creation upon DataPieces.
     * 
     * @param {@link Context}
     * @param The {@link DataWhole} to be persisted
     */
    public static void persistDataWhole(final Context context, final DataWhole dataWhole) {
        final ResilienceApplication resilApp = (ResilienceApplication) context.getApplicationContext();

        resilApp.getDatabaseThreadExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                final ResilienceDbHelper dbHelper = getDbHelper(context);
                try {
                    final Dao<DataWhole, String> wholeDao = dbHelper.getDataWholeDao();
                    wholeDao.createOrUpdate(dataWhole);

                    final Dao<DataPiece, String> pieceDao = dbHelper.getDataPieceDao();
                    final List<DataPiece> pieceList = dataWhole.getPieces();

                    if (pieceList != null) {

                        pieceDao.callBatchTasks(new Callable<Object>() {
                            @Override
                            public Object call() throws Exception {
                                try {
                                    for (final DataPiece dataPiece : pieceList) {
                                        pieceDao.createOrUpdate(dataPiece);
                                        if (Flags.DEBUG) {
                                            Log.i(ResilienceDbManager.class.getSimpleName(),
                                                    dataPiece.getKey() + " has been persisted!");
                                        }
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                return true;
                            }
                        });

                    }

                    if (Flags.DEBUG) {
                        Log.d(ResilienceDbManager.class.getSimpleName(), dataWhole.getKey()
                                + " has been persisted!");
                    }
                } catch (SQLException e) {
                    if (Flags.DEBUG) {
                        Log.e(ResilienceDbManager.class.getSimpleName(), e.getMessage());
                    }
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } finally {
                    OpenHelperManager.releaseHelper();
                }
            }
        });

    }

    /**
     * @param context
     * @param dataPiece
     */
    public static void persistDataPiece(final Context context, final DataPiece dataPiece) {
        final ResilienceApplication resilApp = (ResilienceApplication) context.getApplicationContext();
        resilApp.getDatabaseThreadExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                final ResilienceDbHelper dbHelper = getDbHelper(context);
                try {
                    final Dao<DataPiece, String> pieceDao = dbHelper.getDataPieceDao();
                    pieceDao.createOrUpdate(dataPiece);
                } catch (Exception e) {

                } finally {
                    OpenHelperManager.releaseHelper();
                }
            }
        });
    }

    public static void removeDataPieces(final Context context, final List<DataPiece> pieces) {
        final ResilienceApplication resilApp = (ResilienceApplication) context.getApplicationContext();

        resilApp.getDatabaseThreadExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                final ResilienceDbHelper dbHelper = getDbHelper(context);
                try {
                    final Dao<DataPiece, String> pieceDao = dbHelper.getDataPieceDao();
                    pieceDao.delete(pieces);

                } catch (SQLException e) {
                    if (Flags.DEBUG) {
                        Log.e(ResilienceDbManager.class.getSimpleName(), e.getMessage());
                    }
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } finally {
                    OpenHelperManager.releaseHelper();
                }
            }
        });
    }

    /**
     * Removes the specified DataWhole, along with its associated DataPieces,
     * from the database.
     * 
     * @param {@link Context}
     * @param The {@link DataWhole} to be removed
     */
    public static void removeDataWhole(final Context context, final DataWhole dataWhole) {
        final ResilienceApplication resilApp = (ResilienceApplication) context.getApplicationContext();

        resilApp.getDatabaseThreadExecutorService().execute(new Runnable() {
            @Override
            public void run() {
                final ResilienceDbHelper dbHelper = getDbHelper(context);
                try {
                    /* Delete the DataPieces */
                    final Dao<DataPiece, String> pieceDao = dbHelper.getDataPieceDao();
                    pieceDao.delete(dataWhole.getPieces());

                    /* Delete the DataWholes */
                    final Dao<DataWhole, String> wholeDao = dbHelper.getDataWholeDao();
                    wholeDao.delete(dataWhole);

                    if (Flags.DEBUG) {
                        Log.d(ResilienceDbManager.class.getSimpleName(), dataWhole.getKey()
                                + " has been deleted!");
                    }

                } catch (SQLException e) {
                    if (Flags.DEBUG) {
                        Log.e(ResilienceDbManager.class.getSimpleName(), e.getMessage());
                    }
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } finally {
                    OpenHelperManager.releaseHelper();
                }
            }
        });
    }

}
