package com.fundynamic.d2tm.game.map;

import com.fundynamic.d2tm.game.entities.Entity;
import com.fundynamic.d2tm.game.entities.Player;
import com.fundynamic.d2tm.game.terrain.impl.DuneTerrain;
import com.fundynamic.d2tm.graphics.Shroud;
import com.fundynamic.d2tm.math.Coordinate;
import com.fundynamic.d2tm.math.MapCoordinate;
import com.fundynamic.d2tm.math.Rectangle;
import com.fundynamic.d2tm.math.Vector2D;
import org.newdawn.slick.SlickException;

import java.util.ArrayList;
import java.util.List;

import static com.fundynamic.d2tm.game.map.Cell.TILE_SIZE;

/**
 *
 * This class represents the game map data.
 *
 */
public class Map {

    private Shroud shroud;
    private final int height, width;
    private final int heightWithInvisibleBorder, widthWithInvisibleBorder;

    private Cell[][] cells;

    public Map(Shroud shroud, int width, int height) throws SlickException {
        this.shroud = shroud;
        this.height = height;
        this.width = width;
        this.heightWithInvisibleBorder = height + 2;
        this.widthWithInvisibleBorder = width + 2;

        this.cells = new Cell[widthWithInvisibleBorder][heightWithInvisibleBorder];
        for (int x = 0; x < widthWithInvisibleBorder; x++) {
            for (int y = 0; y < heightWithInvisibleBorder; y++) {
                Cell cell = Cell.emptyTerrainCell(this, x, y);
                // This is quirky, but I think Cell will become part of a list and Map will no longer be an array then.
                cells[x][y] = cell;
            }
        }
    }

    /**
     * The playable width of the map
     * @return
     */
    public int getWidth() {
        return width;
    }

    /**
     * The playable height of the map
     * @return
     */
    public int getHeight() {
        return height;
    }

    /**
     * Get the exact cell on x,y directly from the internals. Only should be used for drawing (not for game logic).
     * The map class has an 'invisible border'. So a map of 64x64 is actually 66x66.
     *
     * @param mapX
     * @param mapY
     * @return
     */
    public Cell getCell(int mapX, int mapY) {
        try {
            return cells[mapX][mapY];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new ArrayIndexOutOfBoundsException("You're going out of bounds!\n" +
                    "Parameters given: screenX = " + mapX + ", screenY = " + mapY + ".\n" +
                    "You must keep within the dimensions:\n" +
                    "Width: 0 to (not on or over!) " + widthWithInvisibleBorder + "\n" +
                    "Height: 0 to (not on or over!) " + heightWithInvisibleBorder);
        }
    }

    /**
     * Get cell, like {@link #getCell(int, int)}
     * @param mapCoordinate
     * @return
     */
    public Cell getCell(MapCoordinate mapCoordinate) {
        return getCell(mapCoordinate.getXAsInt(), mapCoordinate.getYAsInt());
    }

    public MapEditor.TerrainFacing getTerrainFacing(int x, int y) {
        return getCell(x, y).getTerrain().getTerrainFacing();
    }

    /**
     * Possibly corrects given coordinates and returns always a cell.
     *
     * @param x
     * @param y
     * @return
     */
    public Cell getCellProtected(int x, int y) {
        int correctedX = x;
        if (correctedX < 0) correctedX = 0; // make sure we never get out of bounds ( < 0)
        if (correctedX >= widthWithInvisibleBorder) correctedX = widthWithInvisibleBorder - 1;

        int correctedY = y;
        if (correctedY < 0) correctedY = 0;
        if (correctedY >= heightWithInvisibleBorder) correctedY = heightWithInvisibleBorder - 1;

        return getCell(correctedX, correctedY);
    }

    /**
     * Same as {@link #getCellProtected(int, int)}
     * @param mapCoordinate
     * @return
     */
    public Cell getCellProtected(MapCoordinate mapCoordinate) {
        return getCellProtected(mapCoordinate.getXAsInt(), mapCoordinate.getYAsInt());
    }

    public Cell getCellWithinBoundariesOrNullObject(int x, int y) {
        if (x < 0) return null; // make sure we never get out of bounds ( < 0)
        if (x >= widthWithInvisibleBorder) return null;

        if (y < 0) return null;
        if (y >= heightWithInvisibleBorder) return null;

        return getCell(x, y);
    }

    public int getWidthInPixels() {
        return this.width * TILE_SIZE;
    }

    public int getHeightInPixels() {
        return this.height * TILE_SIZE;
    }

    public Rectangle createViewablePerimeter(Vector2D viewportDimensions) {
        Vector2D dimensions = Vector2D.create(getWidthInPixels(), getHeightInPixels()).min(viewportDimensions);
        return new Rectangle(TILE_SIZE, TILE_SIZE, dimensions);
    }

    public Shroud getShroud() {
        return shroud;
    }

    public List<Cell> getAllCellsOccupiedByEntity(Entity entity) {
        List<Cell> result = new ArrayList<>(entity.getWidthInCells() * entity.getWidthInCells());
        MapCoordinate mapCoordinate = entity.getCoordinate().toMapCoordinate();
        for (int x = 0; x < entity.getWidthInCells(); x++) {
            for (int y = 0; y < entity.getWidthInCells(); y++) {
                result.add(this.getCell(mapCoordinate.getXAsInt() + x, mapCoordinate.getYAsInt() + y));
            }
        }
        return result;
    }

    public Cell getCellByAbsoluteMapCoordinates(Coordinate coordinates) {
        return getCellProtected((int) (coordinates.getX() / TILE_SIZE), (int) (coordinates.getY() / TILE_SIZE));
    }

    public Cell getCellByMapCoordinates(MapCoordinate coordinates) {
        return getCellProtected(coordinates.getXAsInt(), coordinates.getYAsInt());
    }

    public Cell getCellFor(Entity entity) {
        return getCellByMapCoordinates(entity.getCoordinate().toMapCoordinate());
    }

    public Entity revealShroudFor(Entity entity) {
        List<MapCoordinate> allCoordinates = entity.getAllCellsAsMapCoordinates();
        for (MapCoordinate coordinate : allCoordinates) {
            revealShroudFor(coordinate, entity.getSight(), entity.getPlayer());
        }
        return entity;
    }

    public String getAsciiShroudMap(Player player) {
        String result = "";
        for (int y = 0; y < height; y++) {
            String line = "";
            for (int x = 0; x < width; x++) {
                if (player.isShrouded(getCell(x, y).getMapCoordinate())) {
                    line += "#";
                } else {
                    line += ".";
                }
            }
            result += line + "\n";
        }
        return result;
    }

    public String getTerrainMap() {
        String result = "";
        for (int y = 1; y < (height + 1); y++) {
            String line = "";
            for (int x = 1; x < (width + 1); x++) {
                switch (getCell(x, y).getTerrain().getTerrainType()) {
                    case DuneTerrain.TERRAIN_SAND:
                        line += "S";
                        break;
                    case DuneTerrain.TERRAIN_ROCK:
                        line += "R";
                        break;
                    case DuneTerrain.TERRAIN_MOUNTAIN:
                        line += "M";
                        break;
                    case DuneTerrain.TERRAIN_SPICE:
                        line += "#";
                        break;
                    case DuneTerrain.TERRAIN_SPICE_HILL:
                        line += "H";
                        break;
                    default:
                        line += "?";
                        break;
                }
            }
            result += line + "\n";
        }
        return result;
    }

    public void revealShroudFor(int x, int y, Player player) {
        player.revealShroudFor(getCell(x, y).getMapCoordinate());
    }

    public void revealShroudFor(int x, int y, int range, Player player) {
        if (range < 1) return;


        float halfATile = TILE_SIZE / 2;
        // convert to absolute pixel coordinates
        Vector2D asPixelsCentered =
//                getCellCoordinatesInAbsolutePixels(x, y).
                MapCoordinate.create(x, y).toCoordinate().
                add(Vector2D.create(halfATile, halfATile));

        double centerX = asPixelsCentered.getX();
        double centerY = asPixelsCentered.getY();

        for (int rangeStep=0; rangeStep < range; rangeStep++) { // range 'steps'

            for (int degrees=0; degrees < 360; degrees++) {

                // calculate as if we would draw a circle and remember the coordinates
                float rangeInPixels = (rangeStep * TILE_SIZE);
                double circleX = (centerX + (Trigonometry.cos[degrees] * rangeInPixels));
                double circleY = (centerY + (Trigonometry.sin[degrees] * rangeInPixels));

                // convert back the pixel coordinates back to a cell
                Cell cell = getCellByAbsoluteMapCoordinates(
                        Coordinate.create((int) Math.ceil(circleX), (int) Math.ceil(circleY))
                );

                player.revealShroudFor(cell.getMapCoordinate());
            }
        }
    }

    public void revealShroudFor(MapCoordinate mapCoordinate, int range, Player player) {
        revealShroudFor(mapCoordinate.getXAsInt(), mapCoordinate.getYAsInt(), range, player);
    }

    public void revealAllShroudFor(Player player) {
        for (Cell[] cellsRow : cells) {
            for (Cell cell : cellsRow) {
                player.revealShroudFor(cell.getMapCoordinate());
            }
        }
    }

    /**
     * Returns true if the given map coordinate is within the playable dimensions. These are 1 based.
     * Thus, a map of 64x64, starts at 1,1 (minimum) and ends on 64,64.
     * @param mapCoordinate
     * @return
     */
    public boolean isWithinPlayableMapBoundaries(MapCoordinate mapCoordinate) {
        int x = mapCoordinate.getXAsInt();
        int y = mapCoordinate.getYAsInt();
        return x >= 1 && x <= width && y >= 1 && y <= height;
    }

    public int getSurfaceAreaInTiles() {
        return width * height;
    }

    public float getDistanceThatCoversWholeMap() {
        return getSurfaceAreaInTiles() * TILE_SIZE;
    }
}