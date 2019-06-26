package ru.zaxar163.phosphor.api;

import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;

public final class FCOWIterator<E> implements ListIterator<E> {
    private final Object[] copy;
    private Object t = null;
    private int current;
	private final List<E> orig;

    public FCOWIterator(Object[] copy, int init, List<E> orig) {
    	current = init;
    	this.copy = copy;
        this.orig = orig;
    }

    public boolean hasNext() {
        return current < copy.length;
    }

    public boolean hasPrevious() {
        return current > 0;
    }

    @SuppressWarnings("unchecked")
    public E next() {
        if (! hasNext())
            throw new NoSuchElementException();
        return (E) (t = copy[current++]);
    }

    @SuppressWarnings("unchecked")
    public E previous() {
        if (! hasPrevious())
            throw new NoSuchElementException();
        return (E) (t = copy[--current]);
    }

    public int nextIndex() {
        return current;
    }

    public int previousIndex() {
        return current-1;
    }

    public void remove() {
        if (t != null) orig.remove(t);
    }

    public void set(E e) {
        throw new UnsupportedOperationException();
    }

    public void add(E e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void forEachRemaining(Consumer<? super E> action) {
        Objects.requireNonNull(action);
        Object[] elements = copy;
        final int size = elements.length;
        for (int i = current; i < size; i++) {
            @SuppressWarnings("unchecked") E e = (E) elements[i];
            action.accept(e);
        }
        current = size;
    }
}