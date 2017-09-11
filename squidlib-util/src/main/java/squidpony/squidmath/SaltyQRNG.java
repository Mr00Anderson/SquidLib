package squidpony.squidmath;

import squidpony.annotation.Beta;

/**
 * A different kind of quasi-random number generator (also called a sub-random sequence) that can be "salted" like some
 * hashing functions can, to produce many distinct sub-random sequences without changing its performance qualities.
 * This generally will be used to produce doubles or floats, using {@link #nextDouble()} or {@link #nextFloat()}, and
 * the other generator methods use the same update implementation internally (just without any conversion to floating
 * point). This tends to have fairly good distribution regardless of salt, with the first 16384 doubles produced
 * (between 0.0 and 1.0, for any salt tested) staying separated enough that {@code (int)(result * 32768)} will be
 * unique, meaning no two values were closer to each other than they were to their optimally-separated position on a
 * subdivided range of values. That test allows getting "n" unique sub-random values from an integer range with size
 * "n * 2", but if the range is smaller, like if it is just "n" or "n * 3 / 2", this will probably not produce fully
 * unique values. The maximum number of values this can produce without overlapping constantly is 16384, or 2 to the 14.
 * There are 4 different groups of non-overlapping sequences this can produce, with 8192 individual sequences in each
 * group (determined by salt) and each sequence with a period of 16384.
 * <br>
 * This changed from an earlier version that used exponents of the golden ratio phi, which worked well until it got past
 * 256 values, and then it ceased to be adequately sub-random. The earlier approach also may have had issues with very
 * high exponents being treated as infinite and thus losing any information that could be obtained from them.
 * <br>
 * Created by Tommy Ettinger on 9/9/2017.
 */
@Beta
public class SaltyQRNG implements StatefulRandomness {

    /**
     * Creates a SaltyQRNG with a random salt and a random starting state. The random source used here is
     * {@link Math#random()}, which produces rather few particularly-random bits, but enough for this step.
     */
    public SaltyQRNG()
    {
        salt = (int)((Math.random() - 0.5) * 4.294967296E9) & 0xFFF8 | 4;
        current = (int)((Math.random() - 0.5) * 4.294967296E9) >>> 16;

    }

    /**
     * Creates a SaltyQRNG with a specific salt (this should usually be a non-negative int less than 8192). The salt
     * determines the precise sequence that will be produced over the whole lifetime of the QRNG, and two SaltyQRNG
     * objects with different salt values should produce different sequences, at least at some points in generation.
     * The starting state will be 0, which this tolerates well. The salt is allowed to be 0, since some changes are made
     * to the salt before use.
     * @param salt an int; only the bottom 13 bits will be used, so different values range from 0 to 8191
     */
    public SaltyQRNG(int salt)
    {
        current = 0;
        setSalt(salt);
    }

    /**
     * Creates a SaltyQRNG with a specific salt (this should usually be a non-negative int less than 8192) and a point
     * it has already advanced to in the sequence this generates. The salt determines the precise sequence that will be
     * produced over the whole lifetime of the QRNG, and two SaltyQRNG objects with different salt values should produce
     * different sequences, at least at some points in generation. The advance will only have its least-significant 16
     * bits used, so an int can be safely passed as advance without issue (even a negative int). The salt is allowed to
     * be 0, since some changes are made to the salt before use.
     * @param salt an int; only the bottom 13 bits will be used, so different values range from 0 to 8191
     * @param advance a long to use as the state; only the bottom 32 bits are used, so any int can also be used
     */
    public SaltyQRNG(int salt, long advance)
    {
        setState(advance);
        current = 0;
        setSalt(salt);
    }
    private int salt;

    public int getSalt()
    {
        return salt >>> 3;
    }

    /**
     * Sets the salt, which should usually be a non-negative int less than 8192, though it can be any int (only the
     * bottom 13 bits are used). The salt determines the precise sequence that will be produced over the whole lifetime
     * of the QRNG, and two SaltyQRNG objects with different salt values should produce different sequences, at least at
     * some points in generation. The salt is allowed to be 0, since some changes are made to the salt before use.
     * @param newSalt an int; only the bottom 13 bits will be used, so different values range from 0 to 8191
     */
    public void setSalt(int newSalt)
    {
        salt = newSalt << 3 | 4;
    }

    private int current;
    @Override
    public long getState() {
        return current;
    }

    /**
     * Sets the current "state" of the QRNG (which number in the sequence it will produce), using the least-significant
     * 16 bits of a given long.
     * @param state a long (0 is tolerated); this only uses the bottom 16 bits, so you could pass a short or an int
     */
    @Override
    public void setState(long state) {
        current = (int) state & 0xFFFF;
    }
    /**
     * Advances the state twice, causing the same state change as a call to {@link #next(int)} or two calls to
     * {@link #nextFloat()} or {@link #nextDouble()}.
     * @return a quasi-random int in the full range for ints, which can be negative or positive
     */
    public int nextInt()
    {
        return ((current + salt) * 0xDE45) ^ ((current += salt << 1) * 0xDE450000);
    }
    @Override
    public int next(int bits) {
        return nextInt() >>> (32 - bits);
    }

    /**
     * Advances the state four times, causing the same state change as two calls to {@link #nextInt()} or
     * {@link #next(int)}, or four calls to {@link #nextFloat()} or {@link #nextDouble()}.
     * @return a quasi-random long in the full range for longs, which can be negative or positive
     */
    @Override
    public long nextLong() {
        return ((current + salt) * 0xDE45L) ^ ((current + (salt << 1)) * 0xDE450000L)
                ^ ((current + salt * 3) * 0xDE4500000000L) ^ ((current += salt << 2) * 0xDE45000000000000L);
    }

    /**
     * Gets the next double in the sequence, between 0.0 (inclusive) and 1.0 (exclusive)
     * @return a double between 0.0 (inclusive) and 1.0 (exclusive)
     */
    public double nextDouble()
    {
        return (((current += salt) * 0xDE45) & 0xFFFF) * 0x1p-16;
    }

    /**
     * Gets the next float in the sequence, between 0.0f (inclusive) and 1.0f (exclusive)
     * @return a double between 0.0f (inclusive) and 1.0f (exclusive)
     */
    public float nextFloat()
    {
        return (((current += salt) * 0xDE45) & 0xFFFF) * 0x1p-16f;
    }

    @Override
    public RandomnessSource copy() {
        return new SaltyQRNG(salt, current);
    }
}
