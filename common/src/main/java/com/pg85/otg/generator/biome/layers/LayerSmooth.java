package com.pg85.otg.generator.biome.layers;

import com.pg85.otg.common.LocalWorld;
import com.pg85.otg.generator.biome.ArraysCache;

public class LayerSmooth extends Layer
{

    LayerSmooth(long seed, int defaultOceanId, Layer childLayer)
    {
        super(seed, defaultOceanId);
        this.child = childLayer;
    }

    @Override
    public int[] getInts(LocalWorld world, ArraysCache cache, int x, int z, int xSize, int zSize)
    {
        int x0 = x - 1;
        int z0 = z - 1;
        int xSize0 = xSize + 2;
        int zSize0 = zSize + 2;

        int[] childInts = this.child.getInts(world, cache, x0, z0, xSize0, zSize0);
        int[] thisInts = cache.getArray(xSize * zSize);

        int northCheck;
        int southCheck;
        int eastCheck;
        int westCheck;
        int centerCheck;
        for (int zi = 0; zi < zSize; ++zi)
        {
            for (int xi = 0; xi < xSize; ++xi)
            {
                northCheck = childInts[xi + 1 + (zi + 0) * xSize0];
                southCheck = childInts[xi + 1 + (zi + 2) * xSize0];
                eastCheck = childInts[xi + 2 + (zi + 1) * xSize0];
                westCheck = childInts[xi + 0 + (zi + 1) * xSize0];
                centerCheck = childInts[xi + 1 + (zi + 1) * xSize0];

                if (westCheck == eastCheck && northCheck == southCheck)
                {
                    this.initChunkSeed((long) (xi + x), (long) (zi + z));

                    if (this.nextInt(2) == 0)
                    {
                        centerCheck = westCheck;
                    } else {
                        centerCheck = northCheck;
                    }
                } else {
                    if (westCheck == eastCheck)
                    {
                       centerCheck = westCheck;
                    }

                    if (northCheck == southCheck)
                    {
                        centerCheck = northCheck;
                    }
                }

                thisInts[xi + zi * xSize] = centerCheck;
            }
        }

        return thisInts;
    }
}
