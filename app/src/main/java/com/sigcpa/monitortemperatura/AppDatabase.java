package com.sigcpa.monitortemperatura;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {LecturaEntity.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract LecturaDao lecturaDao();

    private static volatile AppDatabase INSTANCIA;

    public static AppDatabase obtenerInstancia(final Context contexto) {
        if (INSTANCIA == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCIA == null) {
                    INSTANCIA = Room.databaseBuilder(contexto.getApplicationContext(),
                            AppDatabase.class, "monitor_temperatura.db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCIA;
    }
}
