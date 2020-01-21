package squidpony.squidmath;

/**
 * An unusual continuous noise generator that tends to produce organic-looking forms, currently supporting 2D.
 * Produces noise values from -1.0 inclusive to 1.0 exclusive.
 */
public class FoamNoise implements Noise.Noise2D, Noise.Noise3D {
    public static final FoamNoise instance = new FoamNoise();
    
    public int seed = 0xF0F0F0F0;
    public FoamNoise() {
    }

    public FoamNoise(int seed) {
        this.seed = seed;
    }

    public FoamNoise(long seed) {
        this.seed = (int) (seed ^ seed >>> 32);
    }

    public static double foamNoise(final double x, final double y, int seed) {
        double xin = x * 0.540302 + y * 0.841471; // sin and cos of 1
        double yin = x * -0.841471 + y * 0.540302;
        final double a = valueNoise(seed, xin + NumberTools.swayRandomized(~seed, yin) * 0.5f, yin);
        seed = (seed ^ 0x9E3779BD) * 0xDAB;
        seed ^= seed >>> 14;
        xin = x * -0.989992 + y * 0.141120; // sin and cos of 3
        yin = x * -0.141120 + y * -0.989992;
        final double b = valueNoise(seed, xin + NumberTools.swayRandomized(~seed, yin - a) * 0.5f, yin + a);
        seed = (seed ^ 0x9E3779BD) * 0xDAB;
        seed ^= seed >>> 14;
        xin = x * 0.283662 + y * -0.958924; // sin and cos of 5
        yin = x * 0.958924 + y * 0.283662;
        final double c = valueNoise(seed, xin + NumberTools.swayRandomized(~seed, yin + b) * 0.5f, yin - b);
        final double result = (a + b) * 0.3125 + c * 0.375;
        return result * result * (6.0 - 4.0 * result) - 1.0;
    }
    
    /*
x * -0.185127 + y * -0.791704 + z * -0.582180;
x * -0.776796 + y * 0.628752 + z * -0.035464;
x * 0.822283 + y * 0.467437 + z * -0.324582;

x * 0.139640 + y * -0.304485 + z * 0.942226;
x * -0.776796 + y * 0.628752 + z * -0.035464;
x * 0.822283 + y * 0.467437 + z * -0.324582;

x * 0.139640 + y * -0.304485 + z * 0.942226;
x * -0.185127 + y * -0.791704 + z * -0.582180;
x * 0.822283 + y * 0.467437 + z * -0.324582;

x * 0.139640 + y * -0.304485 + z * 0.942226;
x * -0.185127 + y * -0.791704 + z * -0.582180;
x * -0.776796 + y * 0.628752 + z * -0.035464;
     */
    
    
    public static double foamNoise(final double x, final double y, final double z, int seed) {
        double xin = x * -0.185127 + y * -0.791704 + z * -0.582180;
        double yin = x * -0.776796 + y * 0.628752 + z * -0.035464;
        double zin = x * 0.822283 + y * 0.467437 + z * -0.324582;
        final double a = valueNoise(seed, xin + NumberTools.swayRandomized(~seed, yin) * 0.5f, yin, zin);
        seed = (seed ^ 0x9E3779BD) * 0xDAB;
        seed ^= seed >>> 14;
        xin = x * 0.139640 + y * -0.304485 + z * 0.942226;
        yin = x * -0.776796 + y * 0.628752 + z * -0.035464;
        zin = x * 0.822283 + y * 0.467437 + z * -0.324582;
        final double b = valueNoise(seed, xin - a, yin + NumberTools.swayRandomized(~seed, zin - a) * 0.5f, zin + a);
        seed = (seed ^ 0x9E3779BD) * 0xDAB;
        seed ^= seed >>> 14;
        xin = x * 0.139640 + y * -0.304485 + z * 0.942226;
        yin = x * -0.185127 + y * -0.791704 + z * -0.582180;
        zin = x * 0.822283 + y * 0.467437 + z * -0.324582;
        final double c = valueNoise(seed, xin + b, yin - b, zin  + NumberTools.swayRandomized(~seed, xin - b) * 0.5f);
        seed = (seed ^ 0x9E3779BD) * 0xDAB;
        seed ^= seed >>> 14;
        xin = x * 0.139640 + y * -0.304485 + z * 0.942226;
        yin = x * -0.185127 + y * -0.791704 + z * -0.582180;
        zin = x * -0.776796 + y * 0.628752 + z * -0.035464;
        final double d = valueNoise(seed, xin + NumberTools.swayRandomized(~seed, zin - c) * 0.5f, yin - c, zin + c);

        final double result = (a + b + c + d) * 0.25;
        return  (result * result * (6.0 - 4.0 * result) - 1.0);
    }

    private static double valueNoise(int seed, double x, double y)
    {
        int xFloor = x >= 0.0 ? (int) x : (int) x - 1;
        x -= xFloor;
        x *= x * (3.0 - 2.0 * x);
        int yFloor = y >= 0.0 ? (int) y : (int) y - 1;
        y -= yFloor;
        y *= y * (3.0 - 2.0 * y);
        xFloor *= 0xD1B55;
        yFloor *= 0xABC99;
        return ((1.0 - y) * ((1.0 - x) * hashPart1024(xFloor, yFloor, seed) + x * hashPart1024(xFloor + 0xD1B55, yFloor, seed))
                + y * ((1.0 - x) * hashPart1024(xFloor, yFloor + 0xABC99, seed) + x * hashPart1024(xFloor + 0xD1B55, yFloor + 0xABC99, seed))) * (0x1.010101010101p-10);
    }

    //x should be premultiplied by 0xD1B55
    //y should be premultiplied by 0xABC99
    private static int hashPart1024(final int x, final int y, int s) {
        s += x ^ y;
        s ^= s << 8;
        return s >>> 10 & 0x3FF;
    }

    private static double valueNoise(int seed, double x, double y, double z)
    {
        int xFloor = x >= 0.0 ? (int) x : (int) x - 1;
        x -= xFloor;
        x *= x * (3.0 - 2.0 * x);
        int yFloor = y >= 0.0 ? (int) y : (int) y - 1;
        y -= yFloor;
        y *= y * (3.0 - 2.0 * y);
        int zFloor = z >= 0.0 ? (int) z : (int) z - 1;
        z -= zFloor;
        z *= z * (3.0 - 2.0 * z);
        //0xDB4F1, 0xBBE05, 0xA0F2F
        xFloor *= 0xDB4F1;
        yFloor *= 0xBBE05;
        zFloor *= 0xA0F2F;
        return ((1.0 - z) *
                ((1.0 - y) * ((1.0 - x) * hashPart1024(xFloor, yFloor, zFloor, seed) + x * hashPart1024(xFloor + 0xDB4F1, yFloor, zFloor, seed))
                        + y * ((1.0 - x) * hashPart1024(xFloor, yFloor + 0xBBE05, zFloor, seed) + x * hashPart1024(xFloor + 0xDB4F1, yFloor + 0xBBE05, zFloor, seed)))
                + z * 
                ((1.0 - y) * ((1.0 - x) * hashPart1024(xFloor, yFloor, zFloor + 0xA0F2F, seed) + x * hashPart1024(xFloor + 0xDB4F1, yFloor, zFloor + 0xA0F2F, seed)) 
                        + y * ((1.0 - x) * hashPart1024(xFloor, yFloor + 0xBBE05, zFloor + 0xA0F2F, seed) + x * hashPart1024(xFloor + 0xDB4F1, yFloor + 0xBBE05, zFloor + 0xA0F2F, seed)))
                ) * (0x1.010101010101p-10);
    }

    //x should be premultiplied by 0xDB4F1
    //y should be premultiplied by 0xBBE05
    //z should be premultiplied by 0xA0F2F
    private static int hashPart1024(final int x, final int y, final int z, int s) {
        s += x ^ y ^ z;
        s ^= s << 8;
        return s >>> 10 & 0x3FF;
    }

    @Override
    public double getNoise(double x, double y) {
        return foamNoise(x, y, seed);
    }
    @Override
    public double getNoiseWithSeed(double x, double y, long seed) {
        return foamNoise(x, y, (int) (seed ^ seed >>> 32));
    }
    @Override
    public double getNoise(double x, double y, double z) {
        return foamNoise(x, y, z, seed);
    }
    @Override
    public double getNoiseWithSeed(double x, double y, double z, long seed) {
        return foamNoise(x, y, z, (int) (seed ^ seed >>> 32));
    }
}
