package com.sigcpa.monitortemperatura;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "lecturas")
public class LecturaEntity {
    @PrimaryKey(autoGenerate = true)
    public long id = 0;

    public double temperatura;
    public double humedad;
    public long timestamp = System.currentTimeMillis();

    public LecturaEntity() {}

    @Ignore
    public LecturaEntity(double temperatura, double humedad) {
        this.temperatura = temperatura;
        this.humedad = humedad;
        this.timestamp = System.currentTimeMillis();
    }
}
