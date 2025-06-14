package com.example.mima.data

import androidx.paging.PagingSource
import androidx.room.*
import kotlinx.coroutines.flow.Flow



@Dao
interface LoginDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(dataList: List<LoginData>)
    @Insert suspend fun insert(data: LoginData): Long
    @Update suspend fun update(data: LoginData)
    @Delete suspend fun delete(data: LoginData)
    @Query("SELECT * FROM login_data WHERE id = :id") suspend fun getById(id: Long): LoginData?
    @Query("SELECT * FROM login_data ORDER BY lastModified DESC") suspend fun getAll(): List<LoginData>
    @Query("DELETE FROM login_data WHERE id = :id") suspend fun deleteById(id: Long)
    @Query("SELECT * FROM login_data ORDER BY lastModified DESC") fun getAllFlow(): Flow<List<LoginData>>
    @Query("SELECT id FROM login_data WHERE projectname = :projectName LIMIT 1")
    suspend fun getProjectIdByName(projectName: String): Long?
    // 加上分页支持
    @Query("SELECT * FROM login_data ORDER BY lastModified DESC")
    fun getAllPagingSource(): PagingSource<Int, LoginData>
    @Query("SELECT * FROM login_data WHERE projectname LIKE :query OR username LIKE :query ORDER BY lastModified DESC")
    fun searchPagingSource(query: String): PagingSource<Int, LoginData>
}


// 回收站 DAO
@Dao
interface RecycleBinDao {
    @Insert suspend fun insert(data: RecycleBinData): Long
    @Query("SELECT * FROM recycle_bin_data ORDER BY deletedTime DESC") suspend fun getAll(): List<RecycleBinData>
    @Query("SELECT * FROM recycle_bin_data WHERE originalId = :originalId") suspend fun getByOriginalId(originalId: Long): RecycleBinData?
    @Query("DELETE FROM recycle_bin_data WHERE id = :id") suspend fun permanentDelete(id: Long)
    @Query("DELETE FROM recycle_bin_data WHERE originalId = :originalId") suspend fun permanentDeleteByOriginalId(originalId: Long)
}

// 密码历史记录 DAO
@Dao
interface HistoryDao {
    @Insert suspend fun insert(history: PasswordHistory): Long
    @Query("SELECT * FROM password_history ORDER BY timestamp DESC") suspend fun getAll(): List<PasswordHistory>
    @Query("SELECT * FROM password_history WHERE projectName = :projectName ORDER BY timestamp DESC") suspend fun getByProject(projectName: String): List<PasswordHistory>
}

//云同步辅助判断
@Dao
interface SyncMetadataDao {
    @Query("SELECT * FROM sync_metadata WHERE id = :id")
    suspend fun getMetadata(id: String = "singleton"): SyncMetadata?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(metadata: SyncMetadata)
}



