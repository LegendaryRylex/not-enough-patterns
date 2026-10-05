package dev.rylex.nep.machine;

public interface MatrixRenderHost {

    void advanceRender(float partialTick);

    MatrixRenderState renderState();
}
