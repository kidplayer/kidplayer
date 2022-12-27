package com.github.kidplayer.data;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.github.kidplayer.R;


import java.sql.SQLException;

/**
 * Database helper class used to manage the creation and upgrading of your database. This class also usually provides
 * the DAOs used by the other classes.
 */
public class DatabaseHelper extends OrmLiteSqliteOpenHelper {

    // name of the database file for your application -- change to something appropriate for your app
    private static final String DATABASE_NAME = "db.db";
    // any time you make changes to your database objects, you may have to increase the database version
    private static final int DATABASE_VERSION = 62;

    // the DAO object we use to access the SimpleData table

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION, R.raw.ormlite_config);
    }

    /**
     * This is called when the database is first created. Usually you should call createTable statements here to create
     * the tables that will store your data.
     */
    @Override
    public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource) {
        try {
            Log.i(DatabaseHelper.class.getName(), "onCreate");
            TableUtils.createTable(connectionSource, Drive.class);
            TableUtils.createTable(connectionSource, Folder.class);
            TableUtils.createTable(connectionSource, VFile.class);
            TableUtils.createTable(connectionSource, Cache.class);
            TableUtils.createTable(connectionSource, Video.class);
            TableUtils.createTable(connectionSource, ChannelCheck.class);
            TableUtils.createTable(connectionSource, CatType.class);

        } catch (SQLException e) {
            Log.e(DatabaseHelper.class.getName(), "Can't create database", e);
            throw new RuntimeException(e);
        }

        // here we try inserting data in the on-create as a test
       /* RuntimeExceptionDao<ResItem, Integer> dao = getSimpleDataDao();
        long millis = System.currentTimeMillis();
        // create some entries in the onCreate
        ResItem simple = new ResItem(millis);
        dao.create(simple);
        simple = new ResItem(millis + 1);
        dao.create(simple);
        Log.i(DatabaseHelper.class.getName(), "created new entries in onCreate: " + millis);*/
    }

    /**
     * This is called when your application is upgraded and it has a higher version number. This allows you to adjust
     * the various data to match the new version number.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, ConnectionSource connectionSource, int oldVersion, int newVersion) {
        try {
            Log.i(DatabaseHelper.class.getName(), "onUpgrade");
            TableUtils.dropTable(connectionSource, VFile.class, true);
            TableUtils.dropTable(connectionSource, Folder.class, true);
            TableUtils.dropTable(connectionSource, Drive.class, true);
            TableUtils.dropTable(connectionSource, Cache.class, true);
            TableUtils.dropTable(connectionSource, Video.class, true);
            TableUtils.dropTable(connectionSource, ChannelCheck.class, true);
            TableUtils.dropTable(connectionSource, CatType.class, true);

               //getDao(ResItem.class).executeRaw("ALTER TABLE `Folder` ADD COLUMN typeId NUMBER default 0;");

            //getDao(His.class).executeRaw("ALTER TABLE `His` ADD COLUMN orderN NUMBER default 0;");
            //TableUtils.dropTable(connectionSource, ResItem.class, true);

            // after we drop the old databases, we create the new ones
            onCreate(db, connectionSource);
        } catch (SQLException e) {
            Log.e(DatabaseHelper.class.getName(), "Can't drop databases", e);
            throw new RuntimeException(e);
        }
    }




    /**
     * Close the database connections and clear any cached DAOs.
     */
    @Override
    public void close() {
        super.close();
    }
}