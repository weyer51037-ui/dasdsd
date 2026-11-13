package dev.vortex.hud;

import java.util.Arrays;

/** Rolling frame time statistics, fed once per rendered frame. */
public final class FrameStats {
    private static final int N = 240;
    private static final float[] MS = new float[N];
    private static int head;
    private static int count;
    private static long last;
    private static long windowStart;
    private static long frames;
    private static int fps;
    private static int lowCache;
    private static long lowCacheTime;

    private FrameStats() {}

    public static void frame() {
        long now = System.nanoTime();
        if (last != 0) {
            MS[head] = (now - last) / 1_000_000f;
            head = (head + 1) % N;
            count = Math.min(count + 1, N);
        }
        last = now;
        frames++;
        if (windowStart == 0) windowStart = now;
        if (now - windowStart >= 500_000_000L) {
            fps = (int) Math.round(frames * 1e9 / (now - windowStart));
            frames = 0;
            windowStart = now;
        }
    }

    public static int fps() { return fps; }

    public static float frameMs() {
        return count == 0 ? 0 : MS[(head - 1 + N) % N];
    }

    /** Average FPS of the slowest 1% of recent frames. */
    public static int low() {
        long now = System.currentTimeMillis();
        if (now - lowCacheTime < 500 || count < 10) return lowCache == 0 ? fps : lowCache;
        lowCacheTime = now;
        float[] copy = new float[count];
        for (int i = 0; i < count; i++) copy[i] = MS[(head - 1 - i + N * 2) % N];
        Arrays.sort(copy);
        int take = Math.max(1, count / 100);
        float sum = 0;
        for (int i = 0; i < take; i++) sum += copy[count - 1 - i];
        float avg = sum / take;
        lowCache = avg <= 0 ? 0 : Math.round(1000f / avg);
        return lowCache;
    }

    /** The last n frame times in milliseconds, oldest first. Missing samples are 0. */
    public static float[] recent(int n) {
        float[] out = new float[n];
        for (int i = 0; i < n; i++) {
            int age = n - 1 - i;
            if (age < count) out[i] = MS[(head - 1 - age + N * 2) % N];
        }
        return out;
    }
}
