package squidpony.gdx.tests;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Files;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ImmediateModeRenderer20;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import squidpony.ArrayTools;
import squidpony.squidmath.BlueNoise;
import squidpony.squidmath.FastNoise;
import squidpony.squidmath.FoamNoise;
import squidpony.squidmath.Noise;

import static com.badlogic.gdx.Input.Keys.*;
import static com.badlogic.gdx.graphics.GL20.GL_POINTS;
import static squidpony.squidmath.Noise.IntPointHash.hash256;

/**
 */
public class FFTVisualizer extends ApplicationAdapter {

    private FastNoise noise = new FastNoise(1);
    private FoamNoise foam = new FoamNoise(1234567890L);
    private static final int MODE_LIMIT = 4;
    private int mode = 0;
    private int dim = 0; // this can be 0, 1, 2, or 3; add 2 to get the actual dimensions
    private int octaves = 3;
    private float freq = 0.125f;
    private boolean inverse = false;
    private ImmediateModeRenderer20 renderer;
    
    private static final int width = 256, height = 256;
    private final double[][] real = new double[width][height], imag = new double[width][height];
    private final float[][] colors = new float[width][height];
    private InputAdapter input;
    
    private Viewport view;
    private int ctr = -128;

    public static float basicPrepare(float n)
    {
        return n * 0.5f + 0.5f;
    }

    public static double basicPrepare(double n)
    {
        return n * 0.5 + 0.5;
    }

    @Override
    public void create() {
        renderer = new ImmediateModeRenderer20(width * height * 2, false, true, 0);
        view = new ScreenViewport();
        Gdx.graphics.setContinuousRendering(true);
        input = new InputAdapter(){
            @Override
            public boolean keyDown(int keycode) {
                switch (keycode) {
                    case MINUS:
                        mode = (mode + MODE_LIMIT - 1) % MODE_LIMIT;
                        ctr = -256;
                        Gdx.graphics.requestRendering();
                        break;
                    case EQUALS:                         
                        mode++;
                        mode %= MODE_LIMIT;
                        ctr = -256;
                        Gdx.graphics.requestRendering();
                        break;
                    case C:
                        ctr++;
                        Gdx.graphics.requestRendering();
                        break;
                    case E: //earlier seed
                        if(mode == 0) 
                            noise.setSeed(noise.getSeed() - 1);
                        else
                            foam.seed -= 0x9E3779B9;
                        Gdx.graphics.requestRendering();
                        break;
                    case S: //seed
                        if(mode == 0)
                            noise.setSeed(noise.getSeed() + 1);
                        else
                            foam.seed += 0x9E3779B9;
                        Gdx.graphics.requestRendering();
                        break;
                    case N: // noise type
                        if(mode == 0) 
                            noise.setNoiseType((noise.getNoiseType() + 1) % 10);
                        Gdx.graphics.requestRendering();
                        break;
                    case ENTER:
                    case D: //dimension
                        dim = (dim + 1) & 3;
                        Gdx.graphics.requestRendering();
                        break;
                    case F: // frequency
                        noise.setFrequency((float) Math.sin(freq += 0.125f) * 0.25f + 0.25f + 0x1p-7f);
                        Gdx.graphics.requestRendering();
                        break;
                    case R: // fRactal type
                        noise.setFractalType((noise.getFractalType() + 1) % 3);
                        Gdx.graphics.requestRendering();
                        break;
                    case H: // higher octaves
                        noise.setFractalOctaves((octaves = octaves + 1 & 7) + 1);
                        Gdx.graphics.requestRendering();
                        break;
                    case L: // lower octaves
                        noise.setFractalOctaves((octaves = octaves + 7 & 7) + 1);
                        Gdx.graphics.requestRendering();
                        break;
                    case I: // inverse mode
                        if (inverse = !inverse) {
                            noise.setFractalLacunarity(0.5f);
                            noise.setFractalGain(2f);
                        } else {
                            noise.setFractalLacunarity(2f);
                            noise.setFractalGain(0.5f);
                        }
                        Gdx.graphics.requestRendering();
                        break;
                    case K: // sKip
                        ctr += 1000;
                        Gdx.graphics.requestRendering();
                        break;
                    case Q:
                    case ESCAPE: {
                        Gdx.app.exit();
                    }
                    break;
                    case SPACE:
                        Gdx.graphics.requestRendering();
                }
                return true;
            }
        };
        Gdx.input.setInputProcessor(input);
    }

    public void putMap() {
        renderer.begin(view.getCamera().combined, GL_POINTS);
        float bright, nf = noise.getFrequency(), c = ctr * 0x1p-4f / nf, xx, yy;
        double db;
        ArrayTools.fill(imag, 0.0);
        if(mode == 0) {
            switch (dim) {
                case 0:
                    for (int x = 0; x < width; x++) {
                        for (int y = 0; y < height; y++) {
                            bright = basicPrepare(noise.getConfiguredNoise(x + c, y + c));
                            real[x][y] = bright;
                            renderer.color(bright, bright, bright, 1f);
                            renderer.vertex(x, y, 0);
                        }
                    }
                    break;
                case 1:
                    for (int x = 0; x < width; x++) {
                        for (int y = 0; y < height; y++) {
                            bright = basicPrepare(noise.getConfiguredNoise(x, y, c));
                            real[x][y] = bright;
                            renderer.color(bright, bright, bright, 1f);
                            renderer.vertex(x, y, 0);
                        }
                    }
                    break;
                case 2:
                    for (int x = 0; x < width; x++) {
                        for (int y = 0; y < height; y++) {
                            bright = basicPrepare(noise.getConfiguredNoise(x, y, c, 0x1p-4f * (x + y - c)));
                            real[x][y] = bright;
                            renderer.color(bright, bright, bright, 1f);
                            renderer.vertex(x, y, 0);
                        }
                    }
                    break;
                case 3:
                    for (int x = 0; x < width; x++) {
                        xx = x * 0.5f;
                        for (int y = 0; y < height; y++) {
                            yy = y * 0.5f;
                            bright = basicPrepare(noise.getConfiguredNoise(
                                    c + xx, xx - c, yy - c,
                                    c - yy, xx + yy, yy - xx));
                            real[x][y] = bright;
                            renderer.color(bright, bright, bright, 1f);
                            renderer.vertex(x, y, 0);
                        }
                    }
                    break;
            }
        } else if(mode == 1) {
            c *= nf;
            switch (dim) {
                case 0:
                    for (int x = 0; x < width; x++) {
                        xx = x * nf;
                        for (int y = 0; y < height; y++) {
                            yy = y * nf;
                            bright = (float) (db = basicPrepare(foam.getNoise(xx + c, yy + c)));
                            real[x][y] = db;
                            renderer.color(bright, bright, bright, 1f);
                            renderer.vertex(x, y, 0);
                        }
                    }
                    break;
                case 1:
                    for (int x = 0; x < width; x++) {
                        xx = x * nf;
                        for (int y = 0; y < height; y++) {
                            yy = y * nf;
                            bright = (float) (db = basicPrepare(foam.getNoise(xx, yy, c)));
                            real[x][y] = db;
                            renderer.color(bright, bright, bright, 1f);
                            renderer.vertex(x, y, 0);
                        }
                    }
                    break;
                case 2:
                    for (int x = 0; x < width; x++) {
                        xx = x * nf;
                        for (int y = 0; y < height; y++) {
                            yy = y * nf;
                            bright = (float) (db = basicPrepare(foam.getNoise(xx, yy, c, 0x1p-4f * (xx + yy - c))));
                            real[x][y] = db;
                            renderer.color(bright, bright, bright, 1f);
                            renderer.vertex(x, y, 0);
                        }
                    }
                    break;
                case 3:
                    nf *= 0.5;
                    for (int x = 0; x < width; x++) {
                        xx = x * nf;
                        for (int y = 0; y < height; y++) {
                            yy = y * nf;
                            bright = (float) (db = basicPrepare(foam.getNoise(
                                    c + xx, xx - c, yy - c,
                                    c - yy, xx + yy, yy - xx)));
                            real[x][y] = db;
                            renderer.color(bright, bright, bright, 1f);
                            renderer.vertex(x, y, 0);
                        }
                    }
                    break;
            }
        } else if(mode == 2){
            switch (dim) {
                case 0:
                    for (int x = 0; x < width; x++) {
                        for (int y = 0; y < height; y++) {
                            bright = (float) (db = 0x1p-8 * hash256(x, y, foam.seed));
                            real[x][y] = db;
                            renderer.color(bright, bright, bright, 1f);
                            renderer.vertex(x, y, 0);
                        }
                    }
                    break;
                case 1:
                    for (int x = 0; x < width; x++) {
                        for (int y = 0; y < height; y++) {
                            bright = (float) (db = 0x1p-8 * hash256(x, y, ctr, foam.seed));
                            real[x][y] = db;
                            renderer.color(bright, bright, bright, 1f);
                            renderer.vertex(x, y, 0);
                        }
                    }
                    break;
                case 2:
                    for (int x = 0; x < width; x++) {
                        for (int y = 0; y < height; y++) {
                            bright = (float) (db = 0x1p-8 * hash256(x, y, ctr, x + y - ctr, foam.seed));
                            real[x][y] = db;
                            renderer.color(bright, bright, bright, 1f);
                            renderer.vertex(x, y, 0);
                        }
                    }
                    break;
                case 3:
                    for (int x = 0; x < width; x++) {
                        for (int y = 0; y < height; y++) {
                            bright = (float) (db = 0x1p-8 * hash256(ctr + x, x - ctr, y - ctr, 
                                    ctr - y, x + y, y - x, foam.seed));
                            real[x][y] = db;
                            renderer.color(bright, bright, bright, 1f);
                            renderer.vertex(x, y, 0);
                        }
                    }
                    break;
            }
        }
        else{
            switch (dim & 1){
                case 0:
                    for (int x = 0; x < width; x++) {
                        for (int y = 0; y < height; y++) {
                            bright = (float) (db = 0x1p-8 * (getBlue(x, y, foam.seed) + 128));
                            real[x][y] = db;
                            renderer.color(bright, bright, bright, 1f);
                            renderer.vertex(x, y, 0);
                        }
                    }
                    break;
                case 1:

                    for (int x = 0; x < width; x++) {
                        for (int y = 0; y < height; y++) {
                            bright = (float) (db = 0x1p-8 * (BlueNoise.getSeeded(x, y, foam.seed) + 128));
                            real[x][y] = db;
                            renderer.color(bright, bright, bright, 1f);
                            renderer.vertex(x, y, 0);
                        }
                    }
                    break;
            }
        }
        Fft.transform2D(real, imag);
        Fft.getColors(real, imag, colors);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                renderer.color(colors[x][y]);
                renderer.vertex(x + width, y, 0);
            }
        }
        renderer.end();
    }
    
    public static byte getBlue(int x, int y, int s){
        final int m = Integer.bitCount(BlueNoise.ALT_NOISE[(x + 23 >>> 6) + (y + 41 >>> 6) + (s >>> 6) & 63][(x + 23 << 6 & 0xFC0) | (y + 41 & 0x3F)] + 128) 
                * Integer.bitCount(BlueNoise.ALT_NOISE[(y + 17 >>> 6) - (x + 47 >>> 7) + (s >>> 12) & 63][(y + 17 << 6 & 0xFC0) | (x + 47 & 0x3F)] + 128)
                * Integer.bitCount(BlueNoise.ALT_NOISE[(y + 33 >>> 7) + (x - 31 >>> 6) + (s >>> 18) & 63][(y + 33 << 6 & 0xFC0) | (x - 31 & 0x3F)] + 128)
                >>> 1;
        final int n = Integer.bitCount(BlueNoise.ALT_NOISE[(x + 53 >>> 6) - (y + 11 >>> 6) + (s >>> 9) & 63][(x + 53 << 6 & 0xFC0) | (y + 11 & 0x3F)] + 128)
                * Integer.bitCount(BlueNoise.ALT_NOISE[(y - 27 >>> 6) + (x - 37 >>> 7) + (s >>> 15) & 63][(y - 27 << 6 & 0xFC0) | (x - 37 & 0x3F)] + 128)
                * Integer.bitCount(BlueNoise.ALT_NOISE[-(x + 35 >>> 6) - (y - 29 >>> 7) + (s >>> 21) & 63][(x + 35 << 6 & 0xFC0) | (y - 29 & 0x3F)] + 128)
                >>> 1;
        return (byte) (BlueNoise.ALT_NOISE[s & 63][(y + (m >>> 7) - (n >>> 7) << 6 & 0xFC0) | (x + (n >>> 7) - (m >>> 7) & 0x3F)] ^ (m ^ n));
    }

    @Override
    public void render() {
        // not sure if this is always needed...
        Gdx.gl.glDisable(GL20.GL_BLEND);
        Gdx.graphics.setTitle(String.valueOf(Gdx.graphics.getFramesPerSecond()));
            // standard clear the background routine for libGDX
            Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
            ctr++;
            putMap();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        view.update(width, height, true);
        view.apply(true);
    }

    public static void main(String[] arg) {
        LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
        config.title = "SquidLib Test: FFT Visualization";
        config.width = width << 1;
        config.height = height;
        config.foregroundFPS = 0;
        config.vSyncEnabled = false;
        config.resizable = false;
        config.addIcon("Tentacle-16.png", Files.FileType.Internal);
        config.addIcon("Tentacle-32.png", Files.FileType.Internal);
        config.addIcon("Tentacle-128.png", Files.FileType.Internal);
        new LwjglApplication(new FFTVisualizer(), config);
    }
}
