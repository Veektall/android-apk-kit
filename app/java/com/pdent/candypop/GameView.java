package com.pdent.candypop;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.MotionEvent;
import android.view.View;
import java.util.Random;

/** Native match-3 candy game drawn on a Canvas. Swipe a candy to swap it with a neighbour. */
public class GameView extends View {
    static final int N = 8, TYPES = 6, MOVES = 30;
    static final int[] COLORS = {0xFFFF3B6B, 0xFFFFB020, 0xFF3DDC84, 0xFF2F9BFF, 0xFFB15CFF, 0xFFFF7A2F};

    final int[][] grid = new int[N][N];
    final float[][] offY = new float[N][N];   // falling animation offset (in cells)
    final float[][] pop = new float[N][N];    // pop animation 1..0
    final Random rnd = new Random();
    final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    int score, moves;
    boolean gameOver, busy;
    float cell, left, top;
    float downX, downY; int downR = -1, downC = -1;

    public GameView(Context c) { super(c); newGame(); }

    void newGame() {
        score = 0; moves = MOVES; gameOver = false; busy = false;
        for (int r = 0; r < N; r++) for (int c = 0; c < N; c++) {
            do { grid[r][c] = rnd.nextInt(TYPES); } while (matchAt(r, c));
            offY[r][c] = -(N - r) - rnd.nextFloat();
        }
        invalidate();
    }

    boolean matchAt(int r, int c) {
        int t = grid[r][c];
        if (c >= 2 && grid[r][c-1] == t && grid[r][c-2] == t) return true;
        if (r >= 2 && grid[r-1][c] == t && grid[r-2][c] == t) return true;
        return false;
    }

    boolean[][] findMatches() {
        boolean[][] m = new boolean[N][N]; boolean any = false;
        for (int r = 0; r < N; r++) for (int c = 0; c < N - 2; c++) {
            int t = grid[r][c];
            if (t >= 0 && grid[r][c+1] == t && grid[r][c+2] == t) { m[r][c] = m[r][c+1] = m[r][c+2] = true; any = true; }
        }
        for (int c = 0; c < N; c++) for (int r = 0; r < N - 2; r++) {
            int t = grid[r][c];
            if (t >= 0 && grid[r+1][c] == t && grid[r+2][c] == t) { m[r][c] = m[r+1][c] = m[r+2][c] = true; any = true; }
        }
        return any ? m : null;
    }

    void swap(int r1, int c1, int r2, int c2) { int t = grid[r1][c1]; grid[r1][c1] = grid[r2][c2]; grid[r2][c2] = t; }

    void tryMove(int r1, int c1, int r2, int c2) {
        if (r2 < 0 || r2 >= N || c2 < 0 || c2 >= N || busy || gameOver) return;
        swap(r1, c1, r2, c2);
        if (findMatches() == null) { swap(r1, c1, r2, c2); return; }  // invalid: swap back
        moves--; busy = true; resolve(1);
    }

    /** Clear matches, drop candies, refill, repeat while cascades continue. */
    void resolve(final int combo) {
        boolean[][] m = findMatches();
        if (m == null) { busy = false; if (moves <= 0) gameOver = true; invalidate(); return; }
        int cleared = 0;
        for (int r = 0; r < N; r++) for (int c = 0; c < N; c++) if (m[r][c]) { grid[r][c] = -1; pop[r][c] = 1f; cleared++; }
        score += cleared * 10 * combo;
        for (int c = 0; c < N; c++) {
            int w = N - 1;
            for (int r = N - 1; r >= 0; r--) if (grid[r][c] >= 0) {
                if (w != r) { grid[w][c] = grid[r][c]; grid[r][c] = -1; offY[w][c] = -(w - r); }
                w--;
            }
            for (int r = w; r >= 0; r--) { grid[r][c] = rnd.nextInt(TYPES); offY[r][c] = -(w + 1) - 0.3f; }
        }
        invalidate();
        postDelayed(new Runnable() { public void run() { resolve(combo + 1); } }, 380);
    }

    @Override protected void onDraw(Canvas cv) {
        int w = getWidth(), h = getHeight();
        p.setShader(new LinearGradient(0, 0, 0, h, 0xFF2B0A3D, 0xFF5A1A6E, Shader.TileMode.CLAMP));
        cv.drawRect(0, 0, w, h, p); p.setShader(null);
        cell = Math.min(w, h * 0.8f) * 0.94f / N; left = (w - cell * N) / 2; top = h * 0.16f;

        p.setColor(Color.WHITE); p.setTextAlign(Paint.Align.LEFT); p.setTextSize(cell * 0.55f); p.setFakeBoldText(true);
        cv.drawText("Score " + score, left, top - cell * 0.6f, p);
        p.setTextAlign(Paint.Align.RIGHT);
        cv.drawText("Moves " + moves, left + cell * N, top - cell * 0.6f, p);

        p.setColor(0x33FFFFFF);
        cv.drawRoundRect(new RectF(left - 8, top - 8, left + cell * N + 8, top + cell * N + 8), 24, 24, p);

        boolean animating = false;
        for (int r = 0; r < N; r++) for (int c = 0; c < N; c++) {
            if (offY[r][c] < 0) { offY[r][c] = Math.min(0, offY[r][c] + 0.35f); animating = true; }
            if (pop[r][c] > 0) { pop[r][c] = Math.max(0, pop[r][c] - 0.08f); animating = true; }
            int t = grid[r][c]; if (t < 0) continue;
            float cx = left + c * cell + cell / 2, cy = top + (r + offY[r][c]) * cell + cell / 2, rad = cell * 0.4f;
            if (cy < top - cell / 2) continue;
            int col = COLORS[t];
            p.setShader(new RadialGradient(cx - rad * 0.35f, cy - rad * 0.35f, rad * 1.4f,
                    lighten(col), col, Shader.TileMode.CLAMP));
            cv.drawCircle(cx, cy, rad, p); p.setShader(null);
            p.setColor(0x88FFFFFF); cv.drawCircle(cx - rad * 0.35f, cy - rad * 0.4f, rad * 0.22f, p);
            if (pop[r][c] > 0) { p.setColor(Color.argb((int) (pop[r][c] * 200), 255, 255, 255)); cv.drawCircle(cx, cy, rad * (1.6f - pop[r][c] * 0.6f), p); }
        }
        if (gameOver) {
            p.setColor(0xCC000000); cv.drawRect(0, 0, w, h, p);
            p.setColor(Color.WHITE); p.setTextAlign(Paint.Align.CENTER); p.setTextSize(cell * 0.9f);
            cv.drawText("Game Over", w / 2f, h * 0.42f, p);
            p.setTextSize(cell * 0.55f); cv.drawText("Score " + score, w / 2f, h * 0.5f, p);
            p.setColor(0xFFFF3B6B); RectF b = new RectF(w / 2f - cell * 2, h * 0.56f, w / 2f + cell * 2, h * 0.56f + cell * 1.1f);
            cv.drawRoundRect(b, 40, 40, p); p.setColor(Color.WHITE);
            cv.drawText("Play Again", w / 2f, b.centerY() + cell * 0.2f, p);
        }
        if (animating) postInvalidateOnAnimation();
    }

    static int lighten(int c) {
        return Color.rgb(Math.min(255, Color.red(c) + 90), Math.min(255, Color.green(c) + 90), Math.min(255, Color.blue(c) + 90));
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        if (gameOver) { if (e.getAction() == MotionEvent.ACTION_UP) newGame(); return true; }
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downX = e.getX(); downY = e.getY();
                downC = (int) ((downX - left) / cell); downR = (int) ((downY - top) / cell);
                if (downX < left || downY < top || downR >= N || downC >= N) downR = -1;
                return true;
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
                if (downR < 0) return true;
                float dx = e.getX() - downX, dy = e.getY() - downY;
                if (Math.max(Math.abs(dx), Math.abs(dy)) > cell * 0.4f) {
                    if (Math.abs(dx) > Math.abs(dy)) tryMove(downR, downC, downR, downC + (dx > 0 ? 1 : -1));
                    else tryMove(downR, downC, downR + (dy > 0 ? 1 : -1), downC);
                    downR = -1;
                }
                return true;
        }
        return true;
    }
}
