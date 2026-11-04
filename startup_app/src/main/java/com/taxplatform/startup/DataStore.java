package com.taxplatform.startup;

import java.util.Map;
import java.util.function.Function;

/** Atomic business-state unit of work. Storage does not grant tax or professional authority. */
public interface DataStore extends AutoCloseable {
    <T>T read(Function<Map<String,Object>,T> work);
    <T>T transaction(Function<Map<String,Object>,T> work);
    String backend();
    @Override void close()throws java.io.IOException;
}
