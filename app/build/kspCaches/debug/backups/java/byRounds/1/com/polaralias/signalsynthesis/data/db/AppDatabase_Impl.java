package com.polaralias.signalsynthesis.data.db;

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
import com.polaralias.signalsynthesis.data.db.dao.AiSummaryDao;
import com.polaralias.signalsynthesis.data.db.dao.AiSummaryDao_Impl;
import com.polaralias.signalsynthesis.data.db.dao.HistoryDao;
import com.polaralias.signalsynthesis.data.db.dao.HistoryDao_Impl;
import com.polaralias.signalsynthesis.data.db.dao.WatchlistDao;
import com.polaralias.signalsynthesis.data.db.dao.WatchlistDao_Impl;
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
  private volatile WatchlistDao _watchlistDao;

  private volatile HistoryDao _historyDao;

  private volatile AiSummaryDao _aiSummaryDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `watchlist` (`symbol` TEXT NOT NULL, `addedAt` INTEGER NOT NULL, PRIMARY KEY(`symbol`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `analysis_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `generatedAt` INTEGER NOT NULL, `intent` TEXT NOT NULL, `setupsJson` TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `ai_summaries` (`symbol` TEXT NOT NULL, `summary` TEXT NOT NULL, `risksJson` TEXT NOT NULL, `verdict` TEXT NOT NULL, `generatedAt` INTEGER NOT NULL, PRIMARY KEY(`symbol`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '6565e7fb43afa70c74e8a73212648631')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `watchlist`");
        db.execSQL("DROP TABLE IF EXISTS `analysis_history`");
        db.execSQL("DROP TABLE IF EXISTS `ai_summaries`");
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
        final HashMap<String, TableInfo.Column> _columnsWatchlist = new HashMap<String, TableInfo.Column>(2);
        _columnsWatchlist.put("symbol", new TableInfo.Column("symbol", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWatchlist.put("addedAt", new TableInfo.Column("addedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysWatchlist = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesWatchlist = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoWatchlist = new TableInfo("watchlist", _columnsWatchlist, _foreignKeysWatchlist, _indicesWatchlist);
        final TableInfo _existingWatchlist = TableInfo.read(db, "watchlist");
        if (!_infoWatchlist.equals(_existingWatchlist)) {
          return new RoomOpenHelper.ValidationResult(false, "watchlist(com.polaralias.signalsynthesis.data.db.entity.WatchlistEntity).\n"
                  + " Expected:\n" + _infoWatchlist + "\n"
                  + " Found:\n" + _existingWatchlist);
        }
        final HashMap<String, TableInfo.Column> _columnsAnalysisHistory = new HashMap<String, TableInfo.Column>(4);
        _columnsAnalysisHistory.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAnalysisHistory.put("generatedAt", new TableInfo.Column("generatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAnalysisHistory.put("intent", new TableInfo.Column("intent", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAnalysisHistory.put("setupsJson", new TableInfo.Column("setupsJson", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysAnalysisHistory = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesAnalysisHistory = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoAnalysisHistory = new TableInfo("analysis_history", _columnsAnalysisHistory, _foreignKeysAnalysisHistory, _indicesAnalysisHistory);
        final TableInfo _existingAnalysisHistory = TableInfo.read(db, "analysis_history");
        if (!_infoAnalysisHistory.equals(_existingAnalysisHistory)) {
          return new RoomOpenHelper.ValidationResult(false, "analysis_history(com.polaralias.signalsynthesis.data.db.entity.HistoryEntity).\n"
                  + " Expected:\n" + _infoAnalysisHistory + "\n"
                  + " Found:\n" + _existingAnalysisHistory);
        }
        final HashMap<String, TableInfo.Column> _columnsAiSummaries = new HashMap<String, TableInfo.Column>(5);
        _columnsAiSummaries.put("symbol", new TableInfo.Column("symbol", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAiSummaries.put("summary", new TableInfo.Column("summary", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAiSummaries.put("risksJson", new TableInfo.Column("risksJson", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAiSummaries.put("verdict", new TableInfo.Column("verdict", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAiSummaries.put("generatedAt", new TableInfo.Column("generatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysAiSummaries = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesAiSummaries = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoAiSummaries = new TableInfo("ai_summaries", _columnsAiSummaries, _foreignKeysAiSummaries, _indicesAiSummaries);
        final TableInfo _existingAiSummaries = TableInfo.read(db, "ai_summaries");
        if (!_infoAiSummaries.equals(_existingAiSummaries)) {
          return new RoomOpenHelper.ValidationResult(false, "ai_summaries(com.polaralias.signalsynthesis.data.db.entity.AiSummaryEntity).\n"
                  + " Expected:\n" + _infoAiSummaries + "\n"
                  + " Found:\n" + _existingAiSummaries);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "6565e7fb43afa70c74e8a73212648631", "72267f21d857de9785594ab3c118d240");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "watchlist","analysis_history","ai_summaries");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `watchlist`");
      _db.execSQL("DELETE FROM `analysis_history`");
      _db.execSQL("DELETE FROM `ai_summaries`");
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
    _typeConvertersMap.put(WatchlistDao.class, WatchlistDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(HistoryDao.class, HistoryDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(AiSummaryDao.class, AiSummaryDao_Impl.getRequiredConverters());
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
  public WatchlistDao watchlistDao() {
    if (_watchlistDao != null) {
      return _watchlistDao;
    } else {
      synchronized(this) {
        if(_watchlistDao == null) {
          _watchlistDao = new WatchlistDao_Impl(this);
        }
        return _watchlistDao;
      }
    }
  }

  @Override
  public HistoryDao historyDao() {
    if (_historyDao != null) {
      return _historyDao;
    } else {
      synchronized(this) {
        if(_historyDao == null) {
          _historyDao = new HistoryDao_Impl(this);
        }
        return _historyDao;
      }
    }
  }

  @Override
  public AiSummaryDao aiSummaryDao() {
    if (_aiSummaryDao != null) {
      return _aiSummaryDao;
    } else {
      synchronized(this) {
        if(_aiSummaryDao == null) {
          _aiSummaryDao = new AiSummaryDao_Impl(this);
        }
        return _aiSummaryDao;
      }
    }
  }
}
