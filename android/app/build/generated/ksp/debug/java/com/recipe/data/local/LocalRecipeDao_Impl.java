package com.recipe.data.local;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class LocalRecipeDao_Impl implements LocalRecipeDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<LocalRecipeEntity> __insertionAdapterOfLocalRecipeEntity;

  private final EntityDeletionOrUpdateAdapter<LocalRecipeEntity> __deletionAdapterOfLocalRecipeEntity;

  private final EntityDeletionOrUpdateAdapter<LocalRecipeEntity> __updateAdapterOfLocalRecipeEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  private final SharedSQLiteStatement __preparedStmtOfUpdateSyncStatus;

  public LocalRecipeDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfLocalRecipeEntity = new EntityInsertionAdapter<LocalRecipeEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `local_recipes` (`id`,`serverId`,`userId`,`title`,`description`,`coverImage`,`ingredients`,`steps`,`cookingTime`,`difficulty`,`cuisine`,`tags`,`syncStatus`,`originalAuthor`,`createdAt`,`updatedAt`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final LocalRecipeEntity entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getServerId() == null) {
          statement.bindNull(2);
        } else {
          statement.bindLong(2, entity.getServerId());
        }
        statement.bindLong(3, entity.getUserId());
        statement.bindString(4, entity.getTitle());
        if (entity.getDescription() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getDescription());
        }
        if (entity.getCoverImage() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getCoverImage());
        }
        statement.bindString(7, entity.getIngredients());
        statement.bindString(8, entity.getSteps());
        if (entity.getCookingTime() == null) {
          statement.bindNull(9);
        } else {
          statement.bindLong(9, entity.getCookingTime());
        }
        statement.bindString(10, entity.getDifficulty());
        if (entity.getCuisine() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getCuisine());
        }
        if (entity.getTags() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getTags());
        }
        statement.bindString(13, entity.getSyncStatus());
        if (entity.getOriginalAuthor() == null) {
          statement.bindNull(14);
        } else {
          statement.bindString(14, entity.getOriginalAuthor());
        }
        statement.bindLong(15, entity.getCreatedAt());
        statement.bindLong(16, entity.getUpdatedAt());
      }
    };
    this.__deletionAdapterOfLocalRecipeEntity = new EntityDeletionOrUpdateAdapter<LocalRecipeEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `local_recipes` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final LocalRecipeEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfLocalRecipeEntity = new EntityDeletionOrUpdateAdapter<LocalRecipeEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `local_recipes` SET `id` = ?,`serverId` = ?,`userId` = ?,`title` = ?,`description` = ?,`coverImage` = ?,`ingredients` = ?,`steps` = ?,`cookingTime` = ?,`difficulty` = ?,`cuisine` = ?,`tags` = ?,`syncStatus` = ?,`originalAuthor` = ?,`createdAt` = ?,`updatedAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final LocalRecipeEntity entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getServerId() == null) {
          statement.bindNull(2);
        } else {
          statement.bindLong(2, entity.getServerId());
        }
        statement.bindLong(3, entity.getUserId());
        statement.bindString(4, entity.getTitle());
        if (entity.getDescription() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getDescription());
        }
        if (entity.getCoverImage() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getCoverImage());
        }
        statement.bindString(7, entity.getIngredients());
        statement.bindString(8, entity.getSteps());
        if (entity.getCookingTime() == null) {
          statement.bindNull(9);
        } else {
          statement.bindLong(9, entity.getCookingTime());
        }
        statement.bindString(10, entity.getDifficulty());
        if (entity.getCuisine() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getCuisine());
        }
        if (entity.getTags() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getTags());
        }
        statement.bindString(13, entity.getSyncStatus());
        if (entity.getOriginalAuthor() == null) {
          statement.bindNull(14);
        } else {
          statement.bindString(14, entity.getOriginalAuthor());
        }
        statement.bindLong(15, entity.getCreatedAt());
        statement.bindLong(16, entity.getUpdatedAt());
        statement.bindLong(17, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM local_recipes WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateSyncStatus = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE local_recipes SET syncStatus = ?, serverId = ?, updatedAt = ? WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final LocalRecipeEntity recipe,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfLocalRecipeEntity.insertAndReturnId(recipe);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final LocalRecipeEntity recipe,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfLocalRecipeEntity.handle(recipe);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final LocalRecipeEntity recipe,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfLocalRecipeEntity.handle(recipe);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteById(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteById.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateSyncStatus(final long id, final String status, final Long serverId,
      final long updatedAt, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateSyncStatus.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, status);
        _argIndex = 2;
        if (serverId == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindLong(_argIndex, serverId);
        }
        _argIndex = 3;
        _stmt.bindLong(_argIndex, updatedAt);
        _argIndex = 4;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfUpdateSyncStatus.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<LocalRecipeEntity>> getRecipesByUser(final long userId) {
    final String _sql = "SELECT * FROM local_recipes WHERE userId = ? ORDER BY updatedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, userId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"local_recipes"}, new Callable<List<LocalRecipeEntity>>() {
      @Override
      @NonNull
      public List<LocalRecipeEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfServerId = CursorUtil.getColumnIndexOrThrow(_cursor, "serverId");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfCoverImage = CursorUtil.getColumnIndexOrThrow(_cursor, "coverImage");
          final int _cursorIndexOfIngredients = CursorUtil.getColumnIndexOrThrow(_cursor, "ingredients");
          final int _cursorIndexOfSteps = CursorUtil.getColumnIndexOrThrow(_cursor, "steps");
          final int _cursorIndexOfCookingTime = CursorUtil.getColumnIndexOrThrow(_cursor, "cookingTime");
          final int _cursorIndexOfDifficulty = CursorUtil.getColumnIndexOrThrow(_cursor, "difficulty");
          final int _cursorIndexOfCuisine = CursorUtil.getColumnIndexOrThrow(_cursor, "cuisine");
          final int _cursorIndexOfTags = CursorUtil.getColumnIndexOrThrow(_cursor, "tags");
          final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "syncStatus");
          final int _cursorIndexOfOriginalAuthor = CursorUtil.getColumnIndexOrThrow(_cursor, "originalAuthor");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<LocalRecipeEntity> _result = new ArrayList<LocalRecipeEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LocalRecipeEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final Long _tmpServerId;
            if (_cursor.isNull(_cursorIndexOfServerId)) {
              _tmpServerId = null;
            } else {
              _tmpServerId = _cursor.getLong(_cursorIndexOfServerId);
            }
            final long _tmpUserId;
            _tmpUserId = _cursor.getLong(_cursorIndexOfUserId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final String _tmpCoverImage;
            if (_cursor.isNull(_cursorIndexOfCoverImage)) {
              _tmpCoverImage = null;
            } else {
              _tmpCoverImage = _cursor.getString(_cursorIndexOfCoverImage);
            }
            final String _tmpIngredients;
            _tmpIngredients = _cursor.getString(_cursorIndexOfIngredients);
            final String _tmpSteps;
            _tmpSteps = _cursor.getString(_cursorIndexOfSteps);
            final Integer _tmpCookingTime;
            if (_cursor.isNull(_cursorIndexOfCookingTime)) {
              _tmpCookingTime = null;
            } else {
              _tmpCookingTime = _cursor.getInt(_cursorIndexOfCookingTime);
            }
            final String _tmpDifficulty;
            _tmpDifficulty = _cursor.getString(_cursorIndexOfDifficulty);
            final String _tmpCuisine;
            if (_cursor.isNull(_cursorIndexOfCuisine)) {
              _tmpCuisine = null;
            } else {
              _tmpCuisine = _cursor.getString(_cursorIndexOfCuisine);
            }
            final String _tmpTags;
            if (_cursor.isNull(_cursorIndexOfTags)) {
              _tmpTags = null;
            } else {
              _tmpTags = _cursor.getString(_cursorIndexOfTags);
            }
            final String _tmpSyncStatus;
            _tmpSyncStatus = _cursor.getString(_cursorIndexOfSyncStatus);
            final String _tmpOriginalAuthor;
            if (_cursor.isNull(_cursorIndexOfOriginalAuthor)) {
              _tmpOriginalAuthor = null;
            } else {
              _tmpOriginalAuthor = _cursor.getString(_cursorIndexOfOriginalAuthor);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new LocalRecipeEntity(_tmpId,_tmpServerId,_tmpUserId,_tmpTitle,_tmpDescription,_tmpCoverImage,_tmpIngredients,_tmpSteps,_tmpCookingTime,_tmpDifficulty,_tmpCuisine,_tmpTags,_tmpSyncStatus,_tmpOriginalAuthor,_tmpCreatedAt,_tmpUpdatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getRecipeById(final long id,
      final Continuation<? super LocalRecipeEntity> $completion) {
    final String _sql = "SELECT * FROM local_recipes WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<LocalRecipeEntity>() {
      @Override
      @Nullable
      public LocalRecipeEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfServerId = CursorUtil.getColumnIndexOrThrow(_cursor, "serverId");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfCoverImage = CursorUtil.getColumnIndexOrThrow(_cursor, "coverImage");
          final int _cursorIndexOfIngredients = CursorUtil.getColumnIndexOrThrow(_cursor, "ingredients");
          final int _cursorIndexOfSteps = CursorUtil.getColumnIndexOrThrow(_cursor, "steps");
          final int _cursorIndexOfCookingTime = CursorUtil.getColumnIndexOrThrow(_cursor, "cookingTime");
          final int _cursorIndexOfDifficulty = CursorUtil.getColumnIndexOrThrow(_cursor, "difficulty");
          final int _cursorIndexOfCuisine = CursorUtil.getColumnIndexOrThrow(_cursor, "cuisine");
          final int _cursorIndexOfTags = CursorUtil.getColumnIndexOrThrow(_cursor, "tags");
          final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "syncStatus");
          final int _cursorIndexOfOriginalAuthor = CursorUtil.getColumnIndexOrThrow(_cursor, "originalAuthor");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final LocalRecipeEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final Long _tmpServerId;
            if (_cursor.isNull(_cursorIndexOfServerId)) {
              _tmpServerId = null;
            } else {
              _tmpServerId = _cursor.getLong(_cursorIndexOfServerId);
            }
            final long _tmpUserId;
            _tmpUserId = _cursor.getLong(_cursorIndexOfUserId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final String _tmpCoverImage;
            if (_cursor.isNull(_cursorIndexOfCoverImage)) {
              _tmpCoverImage = null;
            } else {
              _tmpCoverImage = _cursor.getString(_cursorIndexOfCoverImage);
            }
            final String _tmpIngredients;
            _tmpIngredients = _cursor.getString(_cursorIndexOfIngredients);
            final String _tmpSteps;
            _tmpSteps = _cursor.getString(_cursorIndexOfSteps);
            final Integer _tmpCookingTime;
            if (_cursor.isNull(_cursorIndexOfCookingTime)) {
              _tmpCookingTime = null;
            } else {
              _tmpCookingTime = _cursor.getInt(_cursorIndexOfCookingTime);
            }
            final String _tmpDifficulty;
            _tmpDifficulty = _cursor.getString(_cursorIndexOfDifficulty);
            final String _tmpCuisine;
            if (_cursor.isNull(_cursorIndexOfCuisine)) {
              _tmpCuisine = null;
            } else {
              _tmpCuisine = _cursor.getString(_cursorIndexOfCuisine);
            }
            final String _tmpTags;
            if (_cursor.isNull(_cursorIndexOfTags)) {
              _tmpTags = null;
            } else {
              _tmpTags = _cursor.getString(_cursorIndexOfTags);
            }
            final String _tmpSyncStatus;
            _tmpSyncStatus = _cursor.getString(_cursorIndexOfSyncStatus);
            final String _tmpOriginalAuthor;
            if (_cursor.isNull(_cursorIndexOfOriginalAuthor)) {
              _tmpOriginalAuthor = null;
            } else {
              _tmpOriginalAuthor = _cursor.getString(_cursorIndexOfOriginalAuthor);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new LocalRecipeEntity(_tmpId,_tmpServerId,_tmpUserId,_tmpTitle,_tmpDescription,_tmpCoverImage,_tmpIngredients,_tmpSteps,_tmpCookingTime,_tmpDifficulty,_tmpCuisine,_tmpTags,_tmpSyncStatus,_tmpOriginalAuthor,_tmpCreatedAt,_tmpUpdatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getByServerId(final long serverId, final long userId,
      final Continuation<? super LocalRecipeEntity> $completion) {
    final String _sql = "SELECT * FROM local_recipes WHERE serverId = ? AND userId = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, serverId);
    _argIndex = 2;
    _statement.bindLong(_argIndex, userId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<LocalRecipeEntity>() {
      @Override
      @Nullable
      public LocalRecipeEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfServerId = CursorUtil.getColumnIndexOrThrow(_cursor, "serverId");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfCoverImage = CursorUtil.getColumnIndexOrThrow(_cursor, "coverImage");
          final int _cursorIndexOfIngredients = CursorUtil.getColumnIndexOrThrow(_cursor, "ingredients");
          final int _cursorIndexOfSteps = CursorUtil.getColumnIndexOrThrow(_cursor, "steps");
          final int _cursorIndexOfCookingTime = CursorUtil.getColumnIndexOrThrow(_cursor, "cookingTime");
          final int _cursorIndexOfDifficulty = CursorUtil.getColumnIndexOrThrow(_cursor, "difficulty");
          final int _cursorIndexOfCuisine = CursorUtil.getColumnIndexOrThrow(_cursor, "cuisine");
          final int _cursorIndexOfTags = CursorUtil.getColumnIndexOrThrow(_cursor, "tags");
          final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "syncStatus");
          final int _cursorIndexOfOriginalAuthor = CursorUtil.getColumnIndexOrThrow(_cursor, "originalAuthor");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final LocalRecipeEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final Long _tmpServerId;
            if (_cursor.isNull(_cursorIndexOfServerId)) {
              _tmpServerId = null;
            } else {
              _tmpServerId = _cursor.getLong(_cursorIndexOfServerId);
            }
            final long _tmpUserId;
            _tmpUserId = _cursor.getLong(_cursorIndexOfUserId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final String _tmpCoverImage;
            if (_cursor.isNull(_cursorIndexOfCoverImage)) {
              _tmpCoverImage = null;
            } else {
              _tmpCoverImage = _cursor.getString(_cursorIndexOfCoverImage);
            }
            final String _tmpIngredients;
            _tmpIngredients = _cursor.getString(_cursorIndexOfIngredients);
            final String _tmpSteps;
            _tmpSteps = _cursor.getString(_cursorIndexOfSteps);
            final Integer _tmpCookingTime;
            if (_cursor.isNull(_cursorIndexOfCookingTime)) {
              _tmpCookingTime = null;
            } else {
              _tmpCookingTime = _cursor.getInt(_cursorIndexOfCookingTime);
            }
            final String _tmpDifficulty;
            _tmpDifficulty = _cursor.getString(_cursorIndexOfDifficulty);
            final String _tmpCuisine;
            if (_cursor.isNull(_cursorIndexOfCuisine)) {
              _tmpCuisine = null;
            } else {
              _tmpCuisine = _cursor.getString(_cursorIndexOfCuisine);
            }
            final String _tmpTags;
            if (_cursor.isNull(_cursorIndexOfTags)) {
              _tmpTags = null;
            } else {
              _tmpTags = _cursor.getString(_cursorIndexOfTags);
            }
            final String _tmpSyncStatus;
            _tmpSyncStatus = _cursor.getString(_cursorIndexOfSyncStatus);
            final String _tmpOriginalAuthor;
            if (_cursor.isNull(_cursorIndexOfOriginalAuthor)) {
              _tmpOriginalAuthor = null;
            } else {
              _tmpOriginalAuthor = _cursor.getString(_cursorIndexOfOriginalAuthor);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new LocalRecipeEntity(_tmpId,_tmpServerId,_tmpUserId,_tmpTitle,_tmpDescription,_tmpCoverImage,_tmpIngredients,_tmpSteps,_tmpCookingTime,_tmpDifficulty,_tmpCuisine,_tmpTags,_tmpSyncStatus,_tmpOriginalAuthor,_tmpCreatedAt,_tmpUpdatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getByTitle(final long userId, final String title,
      final Continuation<? super LocalRecipeEntity> $completion) {
    final String _sql = "SELECT * FROM local_recipes WHERE userId = ? AND title = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, userId);
    _argIndex = 2;
    _statement.bindString(_argIndex, title);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<LocalRecipeEntity>() {
      @Override
      @Nullable
      public LocalRecipeEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfServerId = CursorUtil.getColumnIndexOrThrow(_cursor, "serverId");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfCoverImage = CursorUtil.getColumnIndexOrThrow(_cursor, "coverImage");
          final int _cursorIndexOfIngredients = CursorUtil.getColumnIndexOrThrow(_cursor, "ingredients");
          final int _cursorIndexOfSteps = CursorUtil.getColumnIndexOrThrow(_cursor, "steps");
          final int _cursorIndexOfCookingTime = CursorUtil.getColumnIndexOrThrow(_cursor, "cookingTime");
          final int _cursorIndexOfDifficulty = CursorUtil.getColumnIndexOrThrow(_cursor, "difficulty");
          final int _cursorIndexOfCuisine = CursorUtil.getColumnIndexOrThrow(_cursor, "cuisine");
          final int _cursorIndexOfTags = CursorUtil.getColumnIndexOrThrow(_cursor, "tags");
          final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "syncStatus");
          final int _cursorIndexOfOriginalAuthor = CursorUtil.getColumnIndexOrThrow(_cursor, "originalAuthor");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final LocalRecipeEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final Long _tmpServerId;
            if (_cursor.isNull(_cursorIndexOfServerId)) {
              _tmpServerId = null;
            } else {
              _tmpServerId = _cursor.getLong(_cursorIndexOfServerId);
            }
            final long _tmpUserId;
            _tmpUserId = _cursor.getLong(_cursorIndexOfUserId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final String _tmpCoverImage;
            if (_cursor.isNull(_cursorIndexOfCoverImage)) {
              _tmpCoverImage = null;
            } else {
              _tmpCoverImage = _cursor.getString(_cursorIndexOfCoverImage);
            }
            final String _tmpIngredients;
            _tmpIngredients = _cursor.getString(_cursorIndexOfIngredients);
            final String _tmpSteps;
            _tmpSteps = _cursor.getString(_cursorIndexOfSteps);
            final Integer _tmpCookingTime;
            if (_cursor.isNull(_cursorIndexOfCookingTime)) {
              _tmpCookingTime = null;
            } else {
              _tmpCookingTime = _cursor.getInt(_cursorIndexOfCookingTime);
            }
            final String _tmpDifficulty;
            _tmpDifficulty = _cursor.getString(_cursorIndexOfDifficulty);
            final String _tmpCuisine;
            if (_cursor.isNull(_cursorIndexOfCuisine)) {
              _tmpCuisine = null;
            } else {
              _tmpCuisine = _cursor.getString(_cursorIndexOfCuisine);
            }
            final String _tmpTags;
            if (_cursor.isNull(_cursorIndexOfTags)) {
              _tmpTags = null;
            } else {
              _tmpTags = _cursor.getString(_cursorIndexOfTags);
            }
            final String _tmpSyncStatus;
            _tmpSyncStatus = _cursor.getString(_cursorIndexOfSyncStatus);
            final String _tmpOriginalAuthor;
            if (_cursor.isNull(_cursorIndexOfOriginalAuthor)) {
              _tmpOriginalAuthor = null;
            } else {
              _tmpOriginalAuthor = _cursor.getString(_cursorIndexOfOriginalAuthor);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new LocalRecipeEntity(_tmpId,_tmpServerId,_tmpUserId,_tmpTitle,_tmpDescription,_tmpCoverImage,_tmpIngredients,_tmpSteps,_tmpCookingTime,_tmpDifficulty,_tmpCuisine,_tmpTags,_tmpSyncStatus,_tmpOriginalAuthor,_tmpCreatedAt,_tmpUpdatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getUploadedByTitle(final long userId, final String title,
      final Continuation<? super LocalRecipeEntity> $completion) {
    final String _sql = "SELECT * FROM local_recipes WHERE userId = ? AND title = ? AND syncStatus = 'UPLOADED' LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, userId);
    _argIndex = 2;
    _statement.bindString(_argIndex, title);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<LocalRecipeEntity>() {
      @Override
      @Nullable
      public LocalRecipeEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfServerId = CursorUtil.getColumnIndexOrThrow(_cursor, "serverId");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfCoverImage = CursorUtil.getColumnIndexOrThrow(_cursor, "coverImage");
          final int _cursorIndexOfIngredients = CursorUtil.getColumnIndexOrThrow(_cursor, "ingredients");
          final int _cursorIndexOfSteps = CursorUtil.getColumnIndexOrThrow(_cursor, "steps");
          final int _cursorIndexOfCookingTime = CursorUtil.getColumnIndexOrThrow(_cursor, "cookingTime");
          final int _cursorIndexOfDifficulty = CursorUtil.getColumnIndexOrThrow(_cursor, "difficulty");
          final int _cursorIndexOfCuisine = CursorUtil.getColumnIndexOrThrow(_cursor, "cuisine");
          final int _cursorIndexOfTags = CursorUtil.getColumnIndexOrThrow(_cursor, "tags");
          final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "syncStatus");
          final int _cursorIndexOfOriginalAuthor = CursorUtil.getColumnIndexOrThrow(_cursor, "originalAuthor");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final LocalRecipeEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final Long _tmpServerId;
            if (_cursor.isNull(_cursorIndexOfServerId)) {
              _tmpServerId = null;
            } else {
              _tmpServerId = _cursor.getLong(_cursorIndexOfServerId);
            }
            final long _tmpUserId;
            _tmpUserId = _cursor.getLong(_cursorIndexOfUserId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final String _tmpCoverImage;
            if (_cursor.isNull(_cursorIndexOfCoverImage)) {
              _tmpCoverImage = null;
            } else {
              _tmpCoverImage = _cursor.getString(_cursorIndexOfCoverImage);
            }
            final String _tmpIngredients;
            _tmpIngredients = _cursor.getString(_cursorIndexOfIngredients);
            final String _tmpSteps;
            _tmpSteps = _cursor.getString(_cursorIndexOfSteps);
            final Integer _tmpCookingTime;
            if (_cursor.isNull(_cursorIndexOfCookingTime)) {
              _tmpCookingTime = null;
            } else {
              _tmpCookingTime = _cursor.getInt(_cursorIndexOfCookingTime);
            }
            final String _tmpDifficulty;
            _tmpDifficulty = _cursor.getString(_cursorIndexOfDifficulty);
            final String _tmpCuisine;
            if (_cursor.isNull(_cursorIndexOfCuisine)) {
              _tmpCuisine = null;
            } else {
              _tmpCuisine = _cursor.getString(_cursorIndexOfCuisine);
            }
            final String _tmpTags;
            if (_cursor.isNull(_cursorIndexOfTags)) {
              _tmpTags = null;
            } else {
              _tmpTags = _cursor.getString(_cursorIndexOfTags);
            }
            final String _tmpSyncStatus;
            _tmpSyncStatus = _cursor.getString(_cursorIndexOfSyncStatus);
            final String _tmpOriginalAuthor;
            if (_cursor.isNull(_cursorIndexOfOriginalAuthor)) {
              _tmpOriginalAuthor = null;
            } else {
              _tmpOriginalAuthor = _cursor.getString(_cursorIndexOfOriginalAuthor);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new LocalRecipeEntity(_tmpId,_tmpServerId,_tmpUserId,_tmpTitle,_tmpDescription,_tmpCoverImage,_tmpIngredients,_tmpSteps,_tmpCookingTime,_tmpDifficulty,_tmpCuisine,_tmpTags,_tmpSyncStatus,_tmpOriginalAuthor,_tmpCreatedAt,_tmpUpdatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object searchRecipes(final long userId, final String keyword,
      final Continuation<? super List<LocalRecipeEntity>> $completion) {
    final String _sql = "SELECT * FROM local_recipes WHERE userId = ? AND (title LIKE '%' || ? || '%' OR tags LIKE '%' || ? || '%') ORDER BY updatedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 3);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, userId);
    _argIndex = 2;
    _statement.bindString(_argIndex, keyword);
    _argIndex = 3;
    _statement.bindString(_argIndex, keyword);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<LocalRecipeEntity>>() {
      @Override
      @NonNull
      public List<LocalRecipeEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfServerId = CursorUtil.getColumnIndexOrThrow(_cursor, "serverId");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfCoverImage = CursorUtil.getColumnIndexOrThrow(_cursor, "coverImage");
          final int _cursorIndexOfIngredients = CursorUtil.getColumnIndexOrThrow(_cursor, "ingredients");
          final int _cursorIndexOfSteps = CursorUtil.getColumnIndexOrThrow(_cursor, "steps");
          final int _cursorIndexOfCookingTime = CursorUtil.getColumnIndexOrThrow(_cursor, "cookingTime");
          final int _cursorIndexOfDifficulty = CursorUtil.getColumnIndexOrThrow(_cursor, "difficulty");
          final int _cursorIndexOfCuisine = CursorUtil.getColumnIndexOrThrow(_cursor, "cuisine");
          final int _cursorIndexOfTags = CursorUtil.getColumnIndexOrThrow(_cursor, "tags");
          final int _cursorIndexOfSyncStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "syncStatus");
          final int _cursorIndexOfOriginalAuthor = CursorUtil.getColumnIndexOrThrow(_cursor, "originalAuthor");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<LocalRecipeEntity> _result = new ArrayList<LocalRecipeEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LocalRecipeEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final Long _tmpServerId;
            if (_cursor.isNull(_cursorIndexOfServerId)) {
              _tmpServerId = null;
            } else {
              _tmpServerId = _cursor.getLong(_cursorIndexOfServerId);
            }
            final long _tmpUserId;
            _tmpUserId = _cursor.getLong(_cursorIndexOfUserId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final String _tmpCoverImage;
            if (_cursor.isNull(_cursorIndexOfCoverImage)) {
              _tmpCoverImage = null;
            } else {
              _tmpCoverImage = _cursor.getString(_cursorIndexOfCoverImage);
            }
            final String _tmpIngredients;
            _tmpIngredients = _cursor.getString(_cursorIndexOfIngredients);
            final String _tmpSteps;
            _tmpSteps = _cursor.getString(_cursorIndexOfSteps);
            final Integer _tmpCookingTime;
            if (_cursor.isNull(_cursorIndexOfCookingTime)) {
              _tmpCookingTime = null;
            } else {
              _tmpCookingTime = _cursor.getInt(_cursorIndexOfCookingTime);
            }
            final String _tmpDifficulty;
            _tmpDifficulty = _cursor.getString(_cursorIndexOfDifficulty);
            final String _tmpCuisine;
            if (_cursor.isNull(_cursorIndexOfCuisine)) {
              _tmpCuisine = null;
            } else {
              _tmpCuisine = _cursor.getString(_cursorIndexOfCuisine);
            }
            final String _tmpTags;
            if (_cursor.isNull(_cursorIndexOfTags)) {
              _tmpTags = null;
            } else {
              _tmpTags = _cursor.getString(_cursorIndexOfTags);
            }
            final String _tmpSyncStatus;
            _tmpSyncStatus = _cursor.getString(_cursorIndexOfSyncStatus);
            final String _tmpOriginalAuthor;
            if (_cursor.isNull(_cursorIndexOfOriginalAuthor)) {
              _tmpOriginalAuthor = null;
            } else {
              _tmpOriginalAuthor = _cursor.getString(_cursorIndexOfOriginalAuthor);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new LocalRecipeEntity(_tmpId,_tmpServerId,_tmpUserId,_tmpTitle,_tmpDescription,_tmpCoverImage,_tmpIngredients,_tmpSteps,_tmpCookingTime,_tmpDifficulty,_tmpCuisine,_tmpTags,_tmpSyncStatus,_tmpOriginalAuthor,_tmpCreatedAt,_tmpUpdatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getRecipeCount(final long userId, final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM local_recipes WHERE userId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, userId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
