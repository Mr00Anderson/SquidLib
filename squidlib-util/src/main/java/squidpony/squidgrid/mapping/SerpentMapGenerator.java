package squidpony.squidgrid.mapping;

import squidpony.squidmath.Coord;
import squidpony.squidmath.CoordPacker;
import squidpony.squidmath.RNG;

import java.util.ArrayList;
import java.util.List;

/**
 * Generate dungeons based on a random, winding, looping path through 2D space. Uses techniques from MixedGenerator.
 * Uses a Moore Curve, which is related to Hilbert Curves but loops back to its starting point, and stretches and
 * distorts the grid to make sure a visual correlation isn't obvious.
 * <br>
 * The name comes from a vivid dream I had about gigantic, multi-colored snakes that completely occupied a roguelike
 * dungeon. Shortly after, I made the connection to the Australian mythology I'd heard about the Rainbow Serpent, which
 * in some stories dug water-holes and was similarly gigantic.
 * Created by Tommy Ettinger on 10/24/2015.
 */
public class SerpentMapGenerator {
    private MixedGenerator mix;
    private int[] columns, rows;
    private RNG random;

    /**
     * This prepares a map generator that will generate a map with the given width and height, using the given RNG.
     * The intended purpose is to carve a long path that loops through the whole dungeon, while hopefully maximizing
     * the amount of rooms the player encounters. You call the different carver-adding methods to affect what the
     * dungeon will look like, putCaveCarvers(), putBoxRoomCarvers(), and putRoundRoomCarvers(), defaulting to only
     * caves if none are called. You call generate() after adding carvers, which returns a char[][] for a map.
     * @param width the width of the final map in cells
     * @param height the height of the final map in cells
     * @param rng an RNG object to use for random choices; this make a lot of random choices.
     * @see MixedGenerator
     */
    public SerpentMapGenerator(int width, int height, RNG rng)
    {
        if(width <= 2 || height <= 2)
            throw new ExceptionInInitializerError("width and height must be greater than 2");
        random = rng;
        long columnAlterations = random.nextLong(0x10000);
        float columnBase = width / (Long.bitCount(columnAlterations) + 48.0f);
        long rowAlterations = random.nextLong(0x10000);
        float rowBase = height / (Long.bitCount(rowAlterations) + 48.0f);

        columns = new int[16];
        rows = new int[16];
        int csum = 0, rsum = 0;
        for (int i = 0, b = 7; i < 16; i++, b <<= 3) {
            columns[i] = csum + (int)(columnBase * 0.5f * (3 + Long.bitCount(columnAlterations & b)));
            csum += (int)(columnBase * (3 + Long.bitCount(columnAlterations & b)));
            rows[i] = rsum + (int)(rowBase * 0.5f * (3 + Long.bitCount(rowAlterations & b)));
            rsum += (int)(rowBase * (3 + Long.bitCount(rowAlterations & b)));
        }
        int cs2 = (int)Math.floor((width - csum) * 0.5);
        int rs2 = (int)Math.floor((height - rsum) * 0.5);
        int cs3 = (width == csum) ? 0 :  (int)Math.ceil((width - csum) * 0.5);
        int rs3 = (height == rsum) ? 0 : (int)Math.ceil((height - rsum) * 0.5);
        columns[7] += cs2;
        rows[7] += rs2;
        columns[8] += cs3;
        rows[8] += rs3;

        List<Coord> points = new ArrayList<Coord>(80);
        Coord temp;
        for (int i = 0, m = random.nextInt(256), r; i < 256; r = random.between(4, 12), i += r, m += r) {
            temp = CoordPacker.mooreToCoord(m);
            points.add(Coord.get(columns[temp.x], rows[temp.y]));
        }
        points.add(points.get(0));
        mix = new MixedGenerator(width, height, random, points);

    }

    /**
     * Changes the number of "carvers" that will create caves from one room to the next. If count is 0 or less, no caves
     * will be made. If count is at least 1, caves are possible, and higher numbers relative to the other carvers make
     * caves more likely. Carvers are shuffled when used, then repeat if exhausted during generation. Since typically
     * about 30-40 rooms are carved, large totals for carver count aren't really needed; aiming for a total of 10
     * between the count of putCaveCarvers(), putBoxRoomCarvers(), and putRoundRoomCarvers() is reasonable.
     * @see MixedGenerator
     * @param count the number of carvers making caves between rooms; only matters in relation to other carvers
     */
    public void putCaveCarvers(int count)
    {
        mix.putCaveCarvers(count);
    }
    /**
     * Changes the number of "carvers" that will create right-angle corridors from one room to the next, create rooms
     * with a random size in a box shape at the start and end, and a small room at the corner if there is one. If count
     * is 0 or less, no box-shaped rooms will be made. If count is at least 1, box-shaped rooms are possible, and higher
     * numbers relative to the other carvers make box-shaped rooms more likely. Carvers are shuffled when used, then
     * repeat if exhausted during generation. Since typically about 30-40 rooms are carved, large totals for carver
     * count aren't really needed; aiming for a total of 10 between the count of putCaveCarvers(), putBoxRoomCarvers(),
     * and putRoundRoomCarvers() is reasonable.
     * @see MixedGenerator
     * @param count the number of carvers making box-shaped rooms and corridors between them; only matters in relation
     *              to other carvers
     */
    public void putBoxRoomCarvers(int count)
    {
        mix.putBoxRoomCarvers(count);
    }
    /**
     * Changes the number of "carvers" that will create right-angle corridors from one room to the next, create rooms
     * with a random size in a circle shape at the start and end, and a small circular room at the corner if there is
     * one. If count is 0 or less, no circular rooms will be made. If count is at least 1, circular rooms are possible,
     * and higher numbers relative to the other carvers make circular rooms more likely. Carvers are shuffled when used,
     * then repeat if exhausted during generation. Since typically about 30-40 rooms are carved, large totals for carver
     * count aren't really needed; aiming for a total of 10 between the count of putCaveCarvers(), putBoxRoomCarvers(),
     * and putRoundRoomCarvers() is reasonable.
     * @see MixedGenerator
     * @param count the number of carvers making circular rooms and corridors between them; only matters in relation
     *              to other carvers
     */
    public void putRoundRoomCarvers(int count)
    {
        mix.putRoundRoomCarvers(count);
    }

    /**
     * This generates a new map by stretching a 16x16 grid of potential rooms to fit the width and height passed to the
     * constructor, randomly expanding columns and rows before contracting the whole to fit perfectly. This uses the
     * Moore Curve, a space-filling curve that loops around on itself, to guarantee that the rooms will always have a
     * long path through the dungeon that, if followed completely, will take you back to your starting room. Some small
     * branches are possible, and large rooms may merge with other rooms nearby. This uses MixedGenerator.
     * @see MixedGenerator
     * @return a char[][] where '#' is a wall and '.' is a floor or corridor; x first y second
     */
    public char[][] generate()
    {
        return mix.generate();
    }
}
