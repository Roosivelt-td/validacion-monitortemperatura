package com.sigcpa.monitortemperatura;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface LecturaDao {
    @Insert
    void insertar(LecturaEntity lectura);

    @Query("SELECT * FROM lecturas ORDER BY timestamp DESC")
    LiveData<List<LecturaEntity>> obtenerTodas();

    @Query("SELECT * FROM lecturas ORDER BY timestamp DESC LIMIT 10")
    LiveData<List<LecturaEntity>> obtenerUltimas10();

    @Query("SELECT COUNT(*) FROM lecturas")
    int contar();

    @Query("DELETE FROM lecturas")
    void borrarTodo();
}
