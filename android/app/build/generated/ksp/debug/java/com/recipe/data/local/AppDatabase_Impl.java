package com.recipe.data.local;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile LocalRecipeDao _localRecipeDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `local_recipes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `serverId` INTEGER, `userId` INTEGER NOT NULL, `title` TEXT NOT NULL, `description` TEXT, `coverImage` TEXT, `ingredients` TEXT NOT NULL, `steps` TEXT NOT NULL, `cookingTime` INTEGER, `difficulty` TEXT NOT NULL, `cuisine` TEXT, `tags` TEXT, `syncStatus` TEXT NOT NULL, `originalAuthor` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '420db686d42bfaf4b15667cb43acbeeb')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `local_recipes`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsLocalRecipes = new HashMap<String, TableInfo.Column>(16);
        _columnsLocalRecipes.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocalRecipes.put("serverId", new TableInfo.Column("serverId", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocalRecipes.put("userId", new TableInfo.Column("userId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocalRecipes.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocalRecipes.put("description", new TableInfo.Column("description", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocalRecipes.put("coverImage", new TableInfo.Column("coverImage", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocalRecipes.put("ingredients", new TableInfo.Column("ingredients", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocalRecipes.put("steps", new TableInfo.Column("steps", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocalRecipes.put("cookingTime", new TableInfo.Column("cookingTime", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocalRecipes.put("difficulty", new TableInfo.Column("difficulty", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocalRecipes.put("cuisine", new TableInfo.Column("cuisine", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocalRecipes.put("tags", new TableInfo.Column("tags", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocalRecipes.put("syncStatus", new TableInfo.Column("syncStatus", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocalRecipes.put("originalAuthor", new TableInfo.Column("originalAuthor", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocalRecipes.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLocalRecipes.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysLocalRecipes = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesLocalRecipes = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoLocalRecipes = new TableInfo("local_recipes", _columnsLocalRecipes, _foreignKeysLocalRecipes, _indicesLocalRecipes);
        final TableInfo _existingLocalRecipes = TableInfo.read(db, "local_recipes");
        if (!_infoLocalRecipes.equals(_existingLocalRecipes)) {
          return new RoomOpenHelper.ValidationResult(false, "local_recipes(com.recipe.data.local.LocalRecipeEntity).\n"
                  + " Expected:\n" + _infoLocalRecipes + "\n"
                  + " Found:\n" + _existingLocalRecipes);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "420db686d42bfaf4b15667cb43acbeeb", "b94978972c78a27b3ea268c14456c340");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "local_recipes");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `local_recipes`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(LocalRecipeDao.class, LocalRecipeDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public LocalRecipeDao localRecipeDao() {
    if (_localRecipeDao != null) {
      return _localRecipeDao;
    } else {
      synchronized(this) {
        if(_localRecipeDao == null) {
          _localRecipeDao = new LocalRecipeDao_Impl(this);
        }
        return _localRecipeDao;
      }
    }
  }
}
