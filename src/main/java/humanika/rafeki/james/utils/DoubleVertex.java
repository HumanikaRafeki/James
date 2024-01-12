package humanika.rafeki.james.utils;

class DoubleVertex {
    double x = 0;
    double y = 0;
    DoubleVertex() {
    }
    DoubleVertex(double x, double y) {
        this.x = x;
        this.y = y;
    }
    final double lengthSquared() {
        double lensq = x*x + y*y;
        if(lensq < 0)
            lensq = 0;
        return lensq;
    }
    final double dot(DoubleVertex o) {
        return x*o.x + y*o.y;
    }
    final DoubleVertex normalize() {
        double lensq = lengthSquared();
        double len = Math.sqrt(lensq);
        x /= len;
        y /= len;
        return this;
    }
    final DoubleVertex shift(double x, double y) {
        this.x += x;
        this.y += y;
        return this;
    }
    final DoubleVertex shiftBack(double x, double y) {
        this.x -= x;
        this.y -= y;
        return this;
    }
    final DoubleVertex shift(DoubleVertex o) {
        this.x += o.x;
        this.y += o.y;
        return this;
    }
    final DoubleVertex shiftBack(DoubleVertex o) {
        this.x -= o.x;
        this.y -= o.y;
        return this;
    }
    final DoubleVertex shiftBack(DoubleVertex o, double scale) {
        this.x -= o.x * scale;
        this.y -= o.y * scale;
        return this;
    }
    final DoubleVertex scale(double x, double y) {
        this.x *= x;
        this.y *= y;
        return this;
    }
    final DoubleVertex scale(double s) {
        this.x *= s;
        this.y *= s;
        return this;
    }
    final DoubleVertex assign(double x, double y) {
        this.x = x;
        this.y = y;
        return this;
    }
    final DoubleVertex assign(DoubleVertex v) {
        this.x = v.x;
        this.y = v.y;
        return this;
    }
    final DoubleVertex swap(DoubleVertex o) {
        double work = o.x;
        o.x = x;
        x = work;

        work = o.y;
        o.y = y;
        y = work;

        return this;
    }
}
