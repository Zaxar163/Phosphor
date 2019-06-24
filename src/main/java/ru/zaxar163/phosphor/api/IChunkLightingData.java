package ru.zaxar163.phosphor.api;

public interface IChunkLightingData {
    short[] getNeighborLightChecks();

    void setNeighborLightChecks(short[] data);

    boolean isLightInitialized();

    void setLightInitialized(boolean val);

    void setSkylightUpdatedPublic();
}
