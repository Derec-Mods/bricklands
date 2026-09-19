package com.alcatrazescapee.bricklands.util;

import net.minecraft.core.BlockPos;

/**
 * Derived from HexLands by AlcatrazEscapee (MIT License), itself based on the
 * original Hex Lands (2019) by superfluke, anonlinux777, and TehNut.
 * Forked and adapted here for Bricklands.
 *
 * A running-bond rectangle: odd rows are shifted by half a brick width.
 */
public record Brick(int col, int row, double width, double height)
{
    /**
     * Calculates the containing brick of block coordinates {@code (x, z)}.
     */
    public static Brick blockToBrick(double x, double z, double width, double height)
    {
        final int row = (int) Math.floor(z / height);
        final double xOffset = rowOffset(row, width);
        final int col = (int) Math.floor((x - xOffset) / width);
        return new Brick(col, row, width, height);
    }

    private static double rowOffset(int row, double width)
    {
        return Math.floorMod(row, 2) != 0 ? width / 2d : 0d;
    }

    /**
     * Calculates the center of this brick in block coordinates {@code (x, z)}.
     */
    public BlockPos center()
    {
        return new BlockPos((int) Math.round(centerX()), 0, (int) Math.round(centerZ()));
    }

    public double centerX()
    {
        return col * width + rowOffset(row, width) + width / 2d;
    }

    public double centerZ()
    {
        return row * height + height / 2d;
    }

    /**
     * Distance to the nearest brick edge. Zero on the boundary, larger toward the center.
     */
    public double distanceToEdge(double x, double z)
    {
        final double dx = width / 2d - Math.abs(x - centerX());
        final double dz = height / 2d - Math.abs(z - centerZ());
        return Math.max(0d, Math.min(dx, dz));
    }

    /**
     * The neighboring brick across the nearest edge to {@code (x, z)}.
     */
    public Brick adjacent(double x, double z)
    {
        final double dx = width / 2d - Math.abs(x - centerX());
        final double dz = height / 2d - Math.abs(z - centerZ());
        if (dx <= dz)
        {
            final double dir = x >= centerX() ? 1d : -1d;
            return blockToBrick(x + dir * (dx + 1e-4), z, width, height);
        }
        final double dir = z >= centerZ() ? 1d : -1d;
        return blockToBrick(x, z + dir * (dz + 1e-4), width, height);
    }

    @Override
    public int hashCode()
    {
        return col * 31 + row;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Brick brick = (Brick) o;
        return col == brick.col && row == brick.row;
    }

    @Override
    public String toString()
    {
        return "Brick{" + "col=" + col + ", row=" + row + '}';
    }
}
