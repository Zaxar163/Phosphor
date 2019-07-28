package ru.zaxar163.phosphor.api;

public interface IChunkLightingData {
	short[] getNeighborLightChecks();

	boolean isLightInitialized();

	void setLightInitialized(boolean val);

	void setNeighborLightChecks(short[] data);

	void setSkylightUpdatedPublic();
}
