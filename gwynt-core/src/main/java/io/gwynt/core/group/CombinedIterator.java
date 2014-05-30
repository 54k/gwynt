package io.gwynt.core.group;

import java.util.Iterator;

public class CombinedIterator<E> implements Iterator<E> {

    private Iterator<E>[] iterators;
    private int position = 0;

    public CombinedIterator(Iterator<E>... iterators) {
        this.iterators = iterators;
    }

    private Iterator<E> currentIterator() {
        if (!isLastIterator() && !iterators[position].hasNext()) {
            return iterators[++position];
        }
        return iterators[position];
    }

    private boolean isLastIterator() {
        return position == iterators.length - 1;
    }

    @Override
    public boolean hasNext() {
        return currentIterator().hasNext();
    }

    @Override
    public E next() {
        return currentIterator().next();
    }

    @Override
    public void remove() {
        currentIterator().remove();
    }
}
