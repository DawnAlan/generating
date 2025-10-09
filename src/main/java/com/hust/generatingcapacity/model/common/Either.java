package com.hust.generatingcapacity.model.common;

import lombok.Getter;

@Getter
public class Either<L, R> {
    private final L left;
    private final R right;

    private Either(L left, R right) {
        this.left = left;
        this.right = right;
    }

    public static <L, R> Either<L, R> left(L value) {
        return new Either<>(value, null);
    }

    public static <L, R> Either<L, R> right(R value) {
        return new Either<>(null, value);
    }

    public boolean isLeft() { return left != null; }
    public L getLeft() { return left; }
    public R getRight() { return right; }
}

