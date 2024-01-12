package humanika.rafeki.james.utils;

class IntVertex {
    int x = 0;
    int y = 0;
    IntVertex() {
    }
    IntVertex(int x, int y) {
        this.x = x;
        this.y = y;
    }
    final double lengthSquared() {
        int lensq = x*x + y*y;
        if(lensq < 0)
            lensq = 0;
        return lensq;
    }
    final int dot(IntVertex o) {
        return x*o.x + y*o.y;
    }
    final IntVertex normalize() {
        double lensq = lengthSquared();
        double len = Math.sqrt(lensq);
        x /= len;
        y /= len;
        return this;
    }
    final IntVertex shift(int x, int y) {
        this.x += x;
        this.y += y;
        return this;
    }
    final IntVertex shiftBack(int x, int y) {
        this.x -= x;
        this.y -= y;
        return this;
    }
    final IntVertex shift(IntVertex o) {
        this.x += o.x;
        this.y += o.y;
        return this;
    }
    final IntVertex shiftBack(IntVertex o) {
        this.x -= o.x;
        this.y -= o.y;
        return this;
    }
    final IntVertex shiftBack(IntVertex o, int scale) {
        this.x -= o.x * scale;
        this.y -= o.y * scale;
        return this;
    }
    final IntVertex scale(int x, int y) {
        this.x *= x;
        this.y *= y;
        return this;
    }
    final IntVertex scale(int s) {
        this.x *= s;
        this.y *= s;
        return this;
    }
    final IntVertex assign(int x, int y) {
        this.x = x;
        this.y = y;
        return this;
    }
    final IntVertex assign(IntVertex v) {
        this.x = v.x;
        this.y = v.y;
        return this;
    }
    final IntVertex swap(IntVertex o) {
        int work = o.x;
        o.x = x;
        x = work;

        work = o.y;
        o.y = y;
        y = work;

        return this;
    }
}
