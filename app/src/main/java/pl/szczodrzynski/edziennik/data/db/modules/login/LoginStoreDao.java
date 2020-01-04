package pl.szczodrzynski.edziennik.data.db.modules.login;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public abstract class LoginStoreDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void add(LoginStore loginStore);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void addAll(List<LoginStore> loginStoreList);

    @Query("DELETE FROM loginStores WHERE loginStoreId = :loginStoreId")
    public abstract void remove(int loginStoreId);

    @Query("SELECT * FROM loginStores WHERE loginStoreId = :loginStoreId")
    public abstract LiveData<LoginStore> getById(int loginStoreId);

    @Query("SELECT * FROM loginStores WHERE loginStoreId = :loginStoreId")
    public abstract LoginStore getByIdNow(int loginStoreId);

    @Query("UPDATE loginStores SET loginStoreId = :targetId WHERE loginStoreId = :sourceId")
    public abstract void changeId(int sourceId, int targetId);

    @Query("SELECT * FROM loginStores ORDER BY loginStoreId")
    public abstract List<LoginStore> getAllNow();
}

